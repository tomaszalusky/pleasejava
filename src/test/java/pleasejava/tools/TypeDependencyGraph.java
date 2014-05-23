package pleasejava.tools;

import static com.google.common.base.Functions.constant;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

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
import com.google.common.collect.ObjectArrays;
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
		
		public static Parameter create(ParameterMode mode, TypeNode typeNode) {
			return new Parameter(mode, typeNode);
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

		public ParameterMode getParameterMode() {
			return parameterMode;
		}
		
		public TypeNode getTypeNode() {
			return typeNode;
		}
		
		public static final Function<Parameter,TypeNode> _getTypeNode = new Function<Parameter,TypeNode>() {
			public TypeNode apply(Parameter input) {
				return input.getTypeNode();
			}
		};
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof Parameter)) return false;
			Parameter that = (Parameter)obj;
			boolean result = Objects.equals(this.parameterMode,that.parameterMode) && Objects.equals(this.typeNode,that.typeNode);
			return result;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.parameterMode,this.typeNode);
		}

	}
	
	static class RecognitionContext {
		
		private final Element rootElement;
		
		private final Map<String,Optional<TypeNode>> typeByName = Maps.newLinkedHashMap();

		RecognitionContext(Element rootElement) {
			this.rootElement = rootElement;
		}
		
		TypeNode ensureTypeNode(String name) {
			Optional<TypeNode> optionalTypeNode = typeByName.get(name);
			if (optionalTypeNode == null) { // type node has not been constructed yet
				typeByName.put(name, Optional.<TypeNode>absent()); // marked as being built
				TypeNode result;
				if (name.matches("integer|pls_integer|boolean|varchar2\\(\\d+\\)|varchar|string\\(\\d+\\)|string|number\\(\\d+\\)|binary_integer|long|clob")) {
					result = new PrimitiveNode(name);
				} else {
					XPathExpression<Element> xpath = XPathFactory.instance().compile("*[@name='" + name + "']", Filters.element());
					Element typeElement = Iterables.getOnlyElement(xpath.evaluate(rootElement),null);
					Preconditions.checkNotNull(typeElement,"Undeclared type '%s'.",name);
					String typeElementName = typeElement.getName();
					switch (typeElementName) {
						case "record" : {
							ImmutableMap.Builder<String,TypeNode> builder = ImmutableMap.builder();
							for (Element fieldElement : typeElement.getChildren("field")) {
								String fieldName = fieldElement.getAttributeValue("name");
								String fieldType = fieldElement.getAttributeValue("type");
								TypeNode fieldTypeNode = ensureTypeNode(fieldType);
								builder.put(fieldName,fieldTypeNode);
							}
							result = new RecordNode(typeElement.getAttributeValue("name"),builder.build());
							break;
						} case "varray" : {
							String elementType = typeElement.getAttributeValue("of");
							TypeNode elementTypeNode = ensureTypeNode(elementType);
							result = new VarrayNode(typeElement.getAttributeValue("name"),elementTypeNode);
							break;
						} case "nestedtable" : {
							String elementType = typeElement.getAttributeValue("of");
							TypeNode elementTypeNode = ensureTypeNode(elementType);
							result = new NestedTableNode(typeElement.getAttributeValue("name"),elementTypeNode);
							break;
						} case "indexbytable" : {
							String elementType = typeElement.getAttributeValue("of");
							TypeNode elementTypeNode = ensureTypeNode(elementType);
							String indexType = typeElement.getAttributeValue("indexby");
							PrimitiveNode indexTypeNode = (PrimitiveNode)ensureTypeNode(indexType);
							result = new IndexByTableNode(typeElement.getAttributeValue("name"),elementTypeNode,indexTypeNode);
							break;
						} case "procedure" : {
							ImmutableMap.Builder<String,Parameter> builder = ImmutableMap.builder();
							for (Element parameterElement : typeElement.getChildren()) {
								String modeName = parameterElement.getName();
								ParameterMode mode = ParameterMode.valueOf(modeName.toUpperCase());
								String parameterName = parameterElement.getAttributeValue("name");
								String parameterType = parameterElement.getAttributeValue("type");
								TypeNode parameterTypeNode = ensureTypeNode(parameterType);
								Parameter parameter = Parameter.create(mode, parameterTypeNode);
								builder.put(parameterName,parameter);
							}
							result = new ProcedureSignatureNode(typeElement.getAttributeValue("name"),builder.build());
							break;
						} case "function" : {
							ImmutableMap.Builder<String,Parameter> builder = ImmutableMap.builder();
							for (Element parameterElement : typeElement.getChildren()) {
								String modeName = parameterElement.getName();
								ParameterMode mode = ParameterMode.valueOf(modeName.toUpperCase());
								String parameterName = parameterElement.getAttributeValue("name");
								String parameterType = parameterElement.getAttributeValue("type");
								TypeNode parameterTypeNode = ensureTypeNode(parameterType);
								Parameter parameter = Parameter.create(mode, parameterTypeNode);
								builder.put(parameterName,parameter);
							}
							String returnType = typeElement.getAttributeValue("returntype");
							TypeNode returnTypeNode = ensureTypeNode(returnType);
							result = new FunctionSignatureNode(typeElement.getAttributeValue("name"),builder.build(),returnTypeNode);
							break;
						} default : {
							throw new IllegalStateException("The type " + name + " has not been recognized.");
						}
					}
				}
				typeByName.put(name, Optional.of(result));
				return result;
			} else if (!optionalTypeNode.isPresent()) { // type node is being built, which indicates circularity
				throw new IllegalStateException("The type " + name + " circularly depends on itself.");
			} else { // type node is already registered
				return optionalTypeNode.get();
			}
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
			return ImmutableList.of(node.getElementTypeNode());
		}

		@Override
		public List<TypeNode> visitIndexByTableNode(IndexByTableNode node) {
			return ImmutableList.of(node.getElementTypeNode());
		}

		@Override
		public List<TypeNode> visitProcedureSignatureNode(ProcedureSignatureNode node) {
			return from(node.getParameters().values()).transform(Parameter._getTypeNode).toList();
		}

		@Override
		public List<TypeNode> visitFunctionSignatureNode(FunctionSignatureNode node) {
			ImmutableList.Builder<TypeNode> builder = ImmutableList.builder();
			builder.add(node.getReturnTypeNode());
			builder.addAll(from(node.getParameters().values()).transform(Parameter._getTypeNode));
			return builder.build();
		}

		@Override
		public List<TypeNode> visitPrimitiveNode(PrimitiveNode node) {
			return ImmutableList.of();
		}
		
	}
	
	static class RecordNode extends TypeNode {
		
		private final ImmutableMap<String,TypeNode> fields;
		
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
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof RecordNode)) return false;
			RecordNode that = (RecordNode)obj;
			// cannot just use Objects.equals(this.fields,that.fields) because order matters
			boolean result = Objects.equals(this.name,that.name) && Iterables.elementsEqual(this.fields.entrySet(),that.fields.entrySet());
			return result;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(ObjectArrays.concat(this.name, this.fields.entrySet().toArray()));
		}
		
	}
	
	static class VarrayNode extends TypeNode {

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
		
		private final TypeNode elementTypeNode;

		private final PrimitiveNode indexTypeNode;

		IndexByTableNode(String name, TypeNode elementTypeNode, PrimitiveNode indexTypeNode) {
			super(name);
			Preconditions.checkArgument(indexTypeNode != null && indexTypeNode.getName().matches("(?i)binary_integer|pls_integer|varchar2\\(\\d+?\\)|varchar|string|long"), "Illegal index type '%s'.", indexTypeNode == null ? null : indexTypeNode.getName()); // http://docs.oracle.com/cd/B10500_01/appdev.920/a96624/05_colls.htm#19661
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
		
		private final ImmutableMap<String,Parameter> parameters;

		ProcedureSignatureNode(String name, Map<String,Parameter> parameters) {
			super(name);
			this.parameters = ImmutableMap.copyOf(checkNotNull(parameters));
		}
		
		<R> R accept(TypeNodeVisitor<R> visitor) {
			return visitor.visitProcedureSignatureNode(this);
		}
		
		public Map<String,Parameter> getParameters() {
			return parameters;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof ProcedureSignatureNode)) return false;
			ProcedureSignatureNode that = (ProcedureSignatureNode)obj;
			// cannot just use Objects.equals(this.parameters,that.parameters) because order matters
			boolean result = Objects.equals(this.name,that.name) && Iterables.elementsEqual(this.parameters.entrySet(),that.parameters.entrySet());
			return result;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(ObjectArrays.concat(this.name, this.parameters.entrySet().toArray()));
		}

	}
	
	static class FunctionSignatureNode extends TypeNode {
		
		private final TypeNode returnTypeNode;
		
		private final ImmutableMap<String,Parameter> parameters;

		FunctionSignatureNode(String name, Map<String,Parameter> parameters, TypeNode returnTypeNode) {
			super(name);
			this.parameters = ImmutableMap.copyOf(checkNotNull(parameters));
			this.returnTypeNode = returnTypeNode;
		}
		
		<R> R accept(TypeNodeVisitor<R> visitor) {
			return visitor.visitFunctionSignatureNode(this);
		}

		public TypeNode getReturnTypeNode() {
			return returnTypeNode;
		}
		
		public Map<String,Parameter> getParameters() {
			return parameters;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof FunctionSignatureNode)) return false;
			FunctionSignatureNode that = (FunctionSignatureNode)obj;
			// cannot just use Objects.equals(this.parameters,that.parameters) because order matters
			boolean result = Objects.equals(this.name,that.name)
					&& Iterables.elementsEqual(this.parameters.entrySet(),that.parameters.entrySet())
					&& Objects.equals(this.returnTypeNode,that.returnTypeNode);
			return result;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(ObjectArrays.concat(new Object[] {this.name,this.returnTypeNode}, this.parameters.entrySet().toArray(), Object.class));
		}

	}
	
	static class PrimitiveNode extends TypeNode {
		
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


- doplnit testcase na atp3, overit proti excelu
- doplnit nevalidni testcasy a dalsi krajni pripady
- TDG stavet pomoci buildru



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
