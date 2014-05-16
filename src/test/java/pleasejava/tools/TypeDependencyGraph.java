package pleasejava.tools;

import static com.google.common.base.Functions.constant;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.type.PrimitiveType;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.xml.sax.SAXException;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * @author Tomas Zalusky
 */
public class TypeDependencyGraph {

	/**
	 * Key = graph node, values = all types which key node depends on
	 * @throws XMLStreamException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws JDOMException 
	 * @throws SAXException 
	 */
	//final ImmutableListMultimap<TypeNode,TypeNode> dependencies;
	
	final List<TypeNode> topologicalOrdering;
	
	public TypeDependencyGraph(InputStream xml) {
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc =  builder.build(xml);
			Element rootElement = doc.getRootElement();
			RecognitionContext rctx = new RecognitionContext(rootElement);
			ImmutableMultimap.Builder<TypeNode,TypeNode> predecessorsBuilder = ImmutableMultimap.builder();
			ImmutableSet.Builder<TypeNode> allTypeNodesBuilder = ImmutableSet.builder();
			for (Element typeElement : rootElement.getChildren()) {
				String name = typeElement.getAttributeValue("name");
				TypeNode typeNode = rctx.ensureTypeNode(name);
				List<TypeNode> children = typeNode.getChildren();
				allTypeNodesBuilder.add(typeNode).addAll(children);
				predecessorsBuilder.putAll(Maps.toMap(children,constant(typeNode)).asMultimap());
			}
			ImmutableMultimap<TypeNode,TypeNode> predecessors = predecessorsBuilder.build();
			ImmutableSet<TypeNode> allTypeNodes = allTypeNodesBuilder.build();
			
			Multimap<TypeNode,TypeNode> exhaust = LinkedHashMultimap.create(predecessors); // copy of predecessors multimap, its elements are removed during build in order to reveal another nodes
			ImmutableList.Builder<TypeNode> topologicalOrderingBuilder = ImmutableList.builder();
			Set<TypeNode> seed = Sets.difference(allTypeNodes,predecessors.keySet()); // initial set of nodes with no incoming edge
			for (Deque<TypeNode> queue = new ArrayDeque<TypeNode>(seed); !queue.isEmpty(); ) { // queue of nodes with no incoming edge
				TypeNode top = queue.pollFirst();
				topologicalOrderingBuilder.add(top); // queue invariant: polled node has no incoming edge -> it is safe to push it to output
				for (TypeNode child : ImmutableSet.copyOf(top.getChildren())) { // set prevents duplicate offer of child in case of duplicate children
					exhaust.get(child).remove(top); // removing edges to all children
					if (exhaust.get(child).isEmpty()) { // if no edge remains, child becomes top 
						queue.offerLast(child);
					}
				}
			}
			this.topologicalOrdering = topologicalOrderingBuilder.build();
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}
	
	public List<TypeNode> getTopologicalOrdering() {
		return topologicalOrdering;
	}
	
	abstract static class TypeNode {
		
		final String name;
		
		public TypeNode(String name) {
			this.name = checkNotNull(name);
		}
		
		final List<TypeNode> getChildren() {
			return accept(new GetChildren());
		}

		static final String PROTOTYPE_DUMMY_NAME = "#";
		
		static final TypeNode PROTOTYPE_DUMMY_TYPE_NODE = new TypeNode(PROTOTYPE_DUMMY_NAME) {
			<R> R accept(TypeNodeVisitor<R> visitor) {return null;}
		};

		static final PrimitiveNode PROTOTYPE_DUMMY_PRIMITIVE_TYPE_NODE = new PrimitiveNode(PROTOTYPE_DUMMY_NAME) {
			<R> R accept(TypeNodeVisitor<R> visitor) {return null;}
		};

		abstract <R> R accept(TypeNodeVisitor<R> visitor);
		
		public String getName() {
			return name;
		}
		
		public static Function<TypeNode,String> _getName = new Function<TypeNode,String>() {
			public String apply(TypeNode input) {
				return input.getName();
			}
		};
		
		@Override
		public String toString() {
			return name;
		}

	}
	
	/**
	 * http://docs.oracle.com/cd/E11882_01/appdev.112/e25519/subprograms.htm#LNPLS665
	 * @author Tomas Zalusky
	 */
	enum ParameterMode {
		IN,
		OUT,
		INOUT;
	}
	
	static final class Parameter {
		
		private final ParameterMode parameterMode;
		
		private final TypeNode typeNode;
		
		private Parameter(ParameterMode parameterMode, TypeNode typeNode) {
			this.parameterMode = parameterMode;
			this.typeNode = typeNode;
		}
		
		public static Parameter in(TypeNode typeNode) {
			return new Parameter(ParameterMode.IN, typeNode);
		}
		
		public static Parameter out(TypeNode typeNode) {
			return new Parameter(ParameterMode.OUT, typeNode);
		}
		
		public static Parameter inout(TypeNode typeNode) {
			return new Parameter(ParameterMode.INOUT, typeNode);
		}
		
	}
	
	static class RecognitionContext {
		
		private final Element rootElement;
		
		private final Map<String,Optional<TypeNode>> typeByName = Maps.newLinkedHashMap();

		private static final Set<TypeNode> PROTOTYPES = ImmutableSet.of(
				PrimitiveNode.PROTOTYPE,
				RecordNode.PROTOTYPE,
				VarrayNode.PROTOTYPE,
				NestedTableNode.PROTOTYPE,
				IndexByTableNode.PROTOTYPE,
				ProcedureSignatureNode.PROTOTYPE,
				FunctionSignatureNode.PROTOTYPE
		);

		RecognitionContext(Element rootElement) {
			this.rootElement = rootElement;
			
		}
		
		TypeNode ensureTypeNode(String name) {
			Optional<TypeNode> optionalTypeNode = typeByName.get(name);
			if (optionalTypeNode == null) { // type node has not been constructed yet
				typeByName.put(name, Optional.<TypeNode>absent()); // marked as being built
				XPathExpression<Element> xpath = XPathFactory.instance().compile("*[@name='" + name + "']", Filters.element());
				Element typeElement = Iterables.getOnlyElement(xpath.evaluate(rootElement),null);
				Recognizer recognizer = new Recognizer(this,name,typeElement);
				TypeNode recognized = null;
				for (TypeNode prototype : PROTOTYPES) {
					if ((recognized = prototype.accept(recognizer)) != null) {
						break;
					}
				}
				if (recognized == null) {
					throw new IllegalStateException("The type " + name + " has not been recognized.");
				}
				typeByName.put(name, Optional.of(recognized));
				return recognized;
			} else if (!optionalTypeNode.isPresent()) { // type node is being built, which indicates circularity
				throw new IllegalStateException("The type " + name + " circularly depends on itself.");
			} else { // type node is already registered
				return optionalTypeNode.get();
			}
		}
		
	}
	
	static class Recognizer implements TypeNodeVisitor<TypeNode> {

		private final RecognitionContext rctx;
		
		private final String name;
		
		private final Element typeElement;

		Recognizer(RecognitionContext rctx, String name, Element typeElement) {
			this.rctx = rctx;
			this.name = name;
			this.typeElement = typeElement;
		}
		
		@Override
		public RecordNode visitRecordNode(RecordNode node) {
			RecordNode result = null;
			if ("record".equals(typeElement.getName())) {
				ImmutableMap.Builder<String,TypeNode> builder = ImmutableMap.builder();
				for (Element fieldElement : typeElement.getChildren("field")) {
					String fieldName = fieldElement.getAttributeValue("name");
					String fieldType = fieldElement.getAttributeValue("type");
					TypeNode fieldTypeNode = rctx.ensureTypeNode(fieldType);
					builder.put(fieldName,fieldTypeNode);
				}
				result = new RecordNode(typeElement.getAttributeValue("name"),builder.build());
			}
			return result;
		}

		@Override
		public TypeNode visitVarrayNode(VarrayNode node) {
			VarrayNode result = null;
			if ("varray".equals(typeElement.getName())) {
				String elementType = typeElement.getAttributeValue("of");
				TypeNode elementTypeNode = rctx.ensureTypeNode(elementType);
				result = new VarrayNode(typeElement.getAttributeValue("name"),elementTypeNode);
			}
			return result;
		}

		@Override
		public TypeNode visitNestedTableNode(NestedTableNode node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public TypeNode visitIndexByTableNode(IndexByTableNode node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public TypeNode visitProcedureSignatureNode(ProcedureSignatureNode node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public TypeNode visitFunctionSignatureNode(FunctionSignatureNode node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public TypeNode visitPrimitiveNode(PrimitiveNode node) {
			PrimitiveNode result = null;
			if (name.matches("integer|pls_integer|boolean|varchar2\\(\\d+\\)")) {
				result = new PrimitiveNode(name);
			}
			return result;
		}

	}
	
	static class GetChildren implements TypeNodeVisitor<List<TypeNode>> {

		@Override
		public List<TypeNode> visitRecordNode(RecordNode node) {
			return ImmutableList.copyOf(node.getFields().values());
		}

		@Override
		public List<TypeNode> visitVarrayNode(VarrayNode node) {
			return ImmutableList.of(node.getElementTypeNode());
		}

		@Override
		public List<TypeNode> visitNestedTableNode(NestedTableNode node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<TypeNode> visitIndexByTableNode(IndexByTableNode node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<TypeNode> visitProcedureSignatureNode(ProcedureSignatureNode node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<TypeNode> visitFunctionSignatureNode(FunctionSignatureNode node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<TypeNode> visitPrimitiveNode(PrimitiveNode node) {
			return ImmutableList.of();
		}
		
	}
	
	static class RecordNode extends TypeNode {
		
		private static final RecordNode PROTOTYPE = new RecordNode(PROTOTYPE_DUMMY_NAME,ImmutableMap.<String,TypeNode>of());
		
		private final Map<String,TypeNode> fields;
		
		RecordNode(String name, Map<String,TypeNode> fields) {
			super(name);
			this.fields = ImmutableMap.copyOf(checkNotNull(fields));
		}

		<R> R accept(TypeNodeVisitor<R> visitor) {
			return visitor.visitRecordNode(this);
		}
		
		public Map<String,TypeNode> getFields() {
			return fields;
		}
		
		//TODO equals+hC
		
	}
	
	static class VarrayNode extends TypeNode {

		private static final VarrayNode PROTOTYPE = new VarrayNode(PROTOTYPE_DUMMY_NAME,PROTOTYPE_DUMMY_TYPE_NODE);
		
		private final TypeNode elementTypeNode;

		VarrayNode(String name, TypeNode elementTypeNode) {
			super(name);
			this.elementTypeNode = checkNotNull(elementTypeNode);
		}
		
		<R> R accept(TypeNodeVisitor<R> visitor) {
			return visitor.visitVarrayNode(this);
		}
		
		public TypeNode getElementTypeNode() {
			return elementTypeNode;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof VarrayNode)) return false;
			VarrayNode that = (VarrayNode)obj;
			boolean result = Objects.equals(this.name,that.name) && Objects.equals(this.elementTypeNode,that.elementTypeNode);
			return result;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.name,this.elementTypeNode);
		}

	}
	
	static class NestedTableNode extends TypeNode {
		
		private static final NestedTableNode PROTOTYPE = new NestedTableNode(PROTOTYPE_DUMMY_NAME,PROTOTYPE_DUMMY_TYPE_NODE);

		private final TypeNode elementTypeNode;

		NestedTableNode(String name, TypeNode elementTypeNode) {
			super(name);
			this.elementTypeNode = checkNotNull(elementTypeNode);
		}
		
		<R> R accept(TypeNodeVisitor<R> visitor) {
			return visitor.visitNestedTableNode(this);
		}
		
		public TypeNode getElementTypeNode() {
			return elementTypeNode;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof NestedTableNode)) return false;
			NestedTableNode that = (NestedTableNode)obj;
			boolean result = Objects.equals(this.name,that.name) && Objects.equals(this.elementTypeNode,that.elementTypeNode);
			return result;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.name,this.elementTypeNode);
		}

	}
	
	static class IndexByTableNode extends TypeNode {
		
		private static final IndexByTableNode PROTOTYPE = new IndexByTableNode(PROTOTYPE_DUMMY_NAME,PROTOTYPE_DUMMY_TYPE_NODE,PROTOTYPE_DUMMY_PRIMITIVE_TYPE_NODE);

		private final TypeNode elementTypeNode;

		private final PrimitiveNode indexTypeNode;

		IndexByTableNode(String name, TypeNode elementTypeNode, PrimitiveNode indexTypeNode) {
			super(name);
			Preconditions.checkArgument(elementTypeNode == PROTOTYPE_DUMMY_TYPE_NODE || indexTypeNode != null && indexTypeNode.getName().matches("(?i)binary_integer|pls_integer|varchar2\\(\\d+?\\)|varchar|string|long")); // http://docs.oracle.com/cd/B10500_01/appdev.920/a96624/05_colls.htm#19661
			this.indexTypeNode = indexTypeNode;
			this.elementTypeNode = checkNotNull(elementTypeNode);
		}
		
		<R> R accept(TypeNodeVisitor<R> visitor) {
			return visitor.visitIndexByTableNode(this);
		}
		
		public TypeNode getElementTypeNode() {
			return elementTypeNode;
		}

		public PrimitiveNode getIndexTypeNode() {
			return indexTypeNode;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof IndexByTableNode)) return false;
			IndexByTableNode that = (IndexByTableNode)obj;
			boolean result = Objects.equals(this.name,that.name) && Objects.equals(this.elementTypeNode,that.elementTypeNode) && Objects.equals(this.indexTypeNode,that.indexTypeNode);
			return result;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.name,this.elementTypeNode,this.indexTypeNode);
		}
	}
	
	static class ProcedureSignatureNode extends TypeNode {
		
		private static final ProcedureSignatureNode PROTOTYPE = new ProcedureSignatureNode(PROTOTYPE_DUMMY_NAME,ImmutableMap.<String,Parameter>of());

		private final Map<String,Parameter> parameters;
		
		ProcedureSignatureNode(String name, Map<String,Parameter> parameters) {
			super(name);
			this.parameters = ImmutableMap.copyOf(checkNotNull(parameters));
		}
		
		<R> R accept(TypeNodeVisitor<R> visitor) {
			return visitor.visitProcedureSignatureNode(this);
		}
		
		public Map<String, Parameter> getParameters() {
			return parameters;
		}
		
		//TODO equals+hC
	}
	
	static class FunctionSignatureNode extends TypeNode {
		
		private static final FunctionSignatureNode PROTOTYPE = new FunctionSignatureNode(PROTOTYPE_DUMMY_NAME);

		FunctionSignatureNode(String name) {
			super(name);
		}
		
		<R> R accept(TypeNodeVisitor<R> visitor) {
			return visitor.visitFunctionSignatureNode(this);
		}
		
	}
	
	static class PrimitiveNode extends TypeNode {
		
		private static final PrimitiveNode PROTOTYPE = new PrimitiveNode(PROTOTYPE_DUMMY_NAME);

		PrimitiveNode(String name) {
			super(name);
		}
		
		<R> R accept(TypeNodeVisitor<R> visitor) {
			return visitor.visitPrimitiveNode(this);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof PrimitiveNode)) return false;
			PrimitiveNode that = (PrimitiveNode)obj;
			boolean result = Objects.equals(this.name,that.name);
			return result;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.name);
		}

	}

	interface TypeNodeVisitor<R> {
		R visitRecordNode(RecordNode node);
		R visitVarrayNode(VarrayNode node);
		R visitNestedTableNode(NestedTableNode node);
		R visitIndexByTableNode(IndexByTableNode node);
		R visitProcedureSignatureNode(ProcedureSignatureNode node);
		R visitFunctionSignatureNode(FunctionSignatureNode node);
		R visitPrimitiveNode(PrimitiveNode node);
	}
	
	
//	public static void main(String[] args) throws ParserConfigurationException, IOException, JDOMException {
//		String xml = ""
//				+ "<tdg>                                                   \n"
//				+ "  <record name='a_test_package.rec1'>                   \n"
//				//+ "    <field name='f_varray' type='a_test_package.var1' />\n"
//				+ "    <field name='f_integer' type='integer' />           \n"
//				+ "    <field name='f_plsinteger' type='pls_integer' />    \n"
//				+ "    <field name='f_varchar' type='varchar2(100)' />     \n"
//				+ "  </record>                                             \n"
//				//+ "  <varray name='a_test_package.var1' of='a_test_package.rec2' />\n"
//				+ "</tdg>                                                  \n"
//		;
//		new TypeDependencyGraph(xml);
//	}
	
}

/*
XMLInputFactory inputFactory = XMLInputFactory.newInstance();
XMLStreamReader reader = inputFactory.createXMLStreamReader(stringReader);

Set<String> waiting = Sets.newLinkedHashSet();
Set<String> closed = Sets.newLinkedHashSet();


- doplnit dalsi testcasy vcetne nevalidnich
- TDG stavet pomoci buildru
- rozchodit vsechny typy



while (reader.hasNext()) {
	int eventType = reader.next();
	switch (eventType) {
		case XMLStreamReader.START_ELEMENT :
		case XMLStreamReader.END_ELEMENT :
			String localName = reader.getLocalName();
			System.out.println((eventType == XMLStreamReader.END_ELEMENT ? "/" : "") + localName);
			break;
		case XMLStreamReader.ATTRIBUTE :
			// handle attribute
	}
	System.out.println(eventType);
}
*/
