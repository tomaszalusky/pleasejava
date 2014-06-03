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
import com.google.common.base.Enums;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Sets;

/**
 * @author Tomas Zalusky
 */
public class TypeDependencyGraph {

	/**
	 * All nodes used in graph (including primitive types).
	 */
	private final Set<Type> allTypes;
	
	/**
	 * Key = graph node, values = all types which key node depends on.
	 * For example, for record of varchar2s, record is a key, varchar2 is a value.
	 */
	private final ListMultimap<Type,Type> children;
	
	private final List<Type> topologicalOrdering;
	
	public TypeDependencyGraph(Set<Type> allTypes, ListMultimap<Type,Type> children) {
		this.allTypes = ImmutableSet.copyOf(allTypes);
		ImmutableListMultimap<Type,Type> immChildren = ImmutableListMultimap.copyOf(children);
		this.children = immChildren;
		Multimap<Type,Type> predecessors = immChildren.inverse(); // for given key, values express which types is the key used in
		Multimap<Type,Type> exhaust = LinkedHashMultimap.create(predecessors); // copy of predecessors multimap, its elements are removed during build in order to reveal another nodes
		ImmutableList.Builder<Type> topologicalOrderingBuilder = ImmutableList.builder();
		Set<Type> seeds = Sets.difference(allTypes,predecessors.keySet()); // initial set of nodes with no incoming edge
		for (Deque<Type> queue = new ArrayDeque<Type>(seeds); !queue.isEmpty(); ) { // queue of nodes with no incoming edge
			Type top = queue.pollFirst();
			topologicalOrderingBuilder.add(top); // queue invariant: polled node has no incoming edge -> it is safe to push it to output
			for (Type child : ImmutableSet.copyOf(top.getChildren())) { // set prevents duplicate offer of child in case of duplicate children
				exhaust.get(child).remove(top); // removing edges to all children
				if (exhaust.get(child).isEmpty()) { // if no edge remains, child becomes top 
					queue.offerLast(child);
				}
			}
		}
		this.topologicalOrdering = topologicalOrderingBuilder.build();
	}
	
	public static TypeDependencyGraph createFrom(InputStream xml) {
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(xml);
			Element rootElement = doc.getRootElement();
			TypeFactory typeFactory = new TypeFactory(rootElement);
			ImmutableSet.Builder<Type> allTypesBuilder = ImmutableSet.builder();
			ImmutableListMultimap.Builder<Type,Type> childrenBuilder = ImmutableListMultimap.builder();
			for (Element typeElement : rootElement.getChildren()) {
				String name = typeElement.getAttributeValue("name");
				Type type = typeFactory.ensureType(name);
				List<Type> children = type.getChildren();
				allTypesBuilder.add(type).addAll(children);
				childrenBuilder.putAll(type,children);
			}
			ImmutableListMultimap<Type,Type> children = childrenBuilder.build();
			ImmutableSet<Type> allTypes = allTypesBuilder.build();
			return new TypeDependencyGraph(allTypes, children);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}
	
	public List<Type> getTopologicalOrdering() {
		return topologicalOrdering;
	}
	
	abstract static class Type {
		
		final String name;
		
		public Type(String name) {
			this.name = checkNotNull(name);
		}
		
		final List<Type> getChildren() {
			return accept(new GetChildren());
		}

		abstract <R> R accept(TypeVisitor<R> visitor);
		
		public String getName() {
			return name;
		}
		
		public static Function<Type,String> _getName = new Function<Type,String>() {
			public String apply(Type input) {
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
	
	/**
	 * Pair of parameter mode and type.
	 * (Parameter name is intentionally not included,
	 * it is maintained by procedure or function type,
	 * this class can be used also for return type of function.)
	 * @author Tomas Zalusky
	 */
	static final class Parameter {
		
		private final ParameterMode parameterMode;
		
		private final Type type;
		
		private Parameter(ParameterMode parameterMode, Type type) {
			this.parameterMode = parameterMode;
			this.type = type;
		}
		
		public static Parameter create(ParameterMode mode, Type type) {
			return new Parameter(mode, type);
		}
		
		public static Parameter in(Type type) {
			return new Parameter(ParameterMode.IN, type);
		}
		
		public static Parameter out(Type type) {
			return new Parameter(ParameterMode.OUT, type);
		}
		
		public static Parameter inout(Type type) {
			return new Parameter(ParameterMode.INOUT, type);
		}

		public ParameterMode getParameterMode() {
			return parameterMode;
		}
		
		public Type getType() {
			return type;
		}
		
		public static final Function<Parameter,Type> _getType = new Function<Parameter,Type>() {
			public Type apply(Parameter input) {
				return input.getType();
			}
		};
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof Parameter)) return false;
			Parameter that = (Parameter)obj;
			boolean result = Objects.equals(this.parameterMode,that.parameterMode) && Objects.equals(this.type,that.type);
			return result;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.parameterMode,this.type);
		}

	}

	static class UndeclaredTypeException extends RuntimeException {
		
		private final String undeclaredType;

		public UndeclaredTypeException(String undeclaredType) {
			this.undeclaredType = undeclaredType;
		}
		
		@Override
		public String getMessage() {
			return String.format("Undeclared type '%s'.",undeclaredType);
		}
		
	}
	
	static class InvalidPlsqlConstructException extends RuntimeException {
		
		private final String constructName;
		
		public InvalidPlsqlConstructException(String constructName) {
			this.constructName = constructName;
		}
		
		@Override
		public String getMessage() {
			return String.format("Invalid PLSQL construct '%s'.",constructName);
		}
		
	}
	
	static class TypeCircularityException extends RuntimeException {
		
		private final String typeName;
		
		public TypeCircularityException(String typeName) {
			this.typeName = typeName;
		}
		
		@Override
		public String getMessage() {
			return String.format("The type '%s' circularly depends on itself.",typeName);
		}
		
	}
	
	static class InvalidXmlException extends RuntimeException {
		
		private final String detail;

		public InvalidXmlException(String detail) {
			this.detail = detail;
		}
		
		@Override
		public String getMessage() {
			return String.format("Invalid XML type, error: '%s'.",detail);
		}
		
	}
	
	static class TypeFactory {
		
		private final Element rootElement;
		
		private final Map<String,Optional<Type>> typeByName = Maps.newLinkedHashMap();

		TypeFactory(Element rootElement) {
			this.rootElement = rootElement;
		}
		
		private static String attr(Element element, String name) {
			String result = element.getAttributeValue(name);
			if (result == null) {
				throw new InvalidXmlException("missing attribute " + name);
			}
			return result;
		}

		private static ParameterMode parameterMode(Element parameterElement) {
			String modeName = parameterElement.getName();
			Optional<ParameterMode> result = Enums.getIfPresent(ParameterMode.class, modeName.toUpperCase());
			if (!result.isPresent()) {
				throw new InvalidXmlException("invalid parameter mode " + modeName);
			}
			return result.get();
		}

		Type ensureType(String name) {
			Optional<Type> optionalType = typeByName.get(name);
			if (optionalType == null) { // type node has not been constructed yet
				typeByName.put(name, Optional.<Type>absent()); // marked as being built
				Type result;
				if (name.matches("integer|pls_integer|boolean|varchar2\\(\\d+\\)|varchar|string\\(\\d+\\)|string|number\\(\\d+\\)|binary_integer|long|clob")) {
					result = new PrimitiveType(name);
				} else {
					XPathExpression<Element> xpath = XPathFactory.instance().compile("*[@name='" + name + "']", Filters.element());
					Element typeElement = Iterables.getOnlyElement(xpath.evaluate(rootElement),null);
					if (typeElement == null) {
						throw new UndeclaredTypeException(name);
					}
					String typeElementName = typeElement.getName();
					switch (typeElementName) {
						case "record" : {
							ImmutableMap.Builder<String,Type> builder = ImmutableMap.builder();
							List<Element> children = typeElement.getChildren("field");
							if (children.isEmpty()) {
								throw new InvalidXmlException("empty record");
							}
							for (Element fieldElement : children) {
								String fieldName = attr(fieldElement,"name");
								String fieldTypeName = attr(fieldElement,"type");
								Type fieldType = ensureType(fieldTypeName);
								builder.put(fieldName,fieldType);
							}
							result = new Record(name,builder.build());
							break;
						} case "varray" : {
							String elementTypeName = attr(typeElement,"of");
							Type elementType = ensureType(elementTypeName);
							result = new Varray(name,elementType);
							break;
						} case "nestedtable" : {
							String elementTypeName = attr(typeElement,"of");
							Type elementType = ensureType(elementTypeName);
							result = new NestedTable(name,elementType);
							break;
						} case "indexbytable" : {
							String elementTypeName = attr(typeElement,"of");
							Type elementType = ensureType(elementTypeName);
							String indexTypeName = attr(typeElement,"indexby");
							PrimitiveType indexType = (PrimitiveType)ensureType(indexTypeName);
							result = new IndexByTable(name,elementType,indexType);
							break;
						} case "procedure" : {
							ImmutableMap.Builder<String,Parameter> builder = ImmutableMap.builder();
							for (Element parameterElement : typeElement.getChildren()) {
								ParameterMode mode = parameterMode(parameterElement);
								String parameterName = attr(parameterElement,"name");
								String parameterTypeName = attr(parameterElement,"type");
								Type parameterType = ensureType(parameterTypeName);
								Parameter parameter = Parameter.create(mode, parameterType);
								builder.put(parameterName,parameter);
							}
							result = new ProcedureSignature(name,builder.build());
							break;
						} case "function" : {
							ImmutableMap.Builder<String,Parameter> builder = ImmutableMap.builder();
							for (Element parameterElement : typeElement.getChildren()) {
								ParameterMode mode = parameterMode(parameterElement);
								String parameterName = attr(parameterElement,"name");
								String parameterTypeName = attr(parameterElement,"type");
								Type parameterType = ensureType(parameterTypeName);
								Parameter parameter = Parameter.create(mode, parameterType);
								builder.put(parameterName,parameter);
							}
							String returnTypeName = attr(typeElement,"returntype");
							Type returnType = ensureType(returnTypeName);
							result = new FunctionSignature(name,builder.build(),returnType);
							break;
						} default : {
							throw new InvalidPlsqlConstructException(typeElementName);
						}
					}
				}
				typeByName.put(name, Optional.of(result));
				return result;
			} else if (!optionalType.isPresent()) { // type node is being built, which indicates circularity
				throw new TypeCircularityException(name);
			} else { // type node is already registered
				return optionalType.get();
			}
		}
		
	}
	
	static class GetChildren implements TypeVisitor<List<Type>> {

		@Override
		public List<Type> visitRecord(Record type) {
			return ImmutableList.copyOf(type.getFields().values());
		}

		@Override
		public List<Type> visitVarray(Varray type) {
			return ImmutableList.of(type.getElementType());
		}

		@Override
		public List<Type> visitNestedTable(NestedTable type) {
			return ImmutableList.of(type.getElementType());
		}

		@Override
		public List<Type> visitIndexByTable(IndexByTable type) {
			return ImmutableList.of(type.getElementType());
		}

		@Override
		public List<Type> visitProcedureSignature(ProcedureSignature type) {
			return from(type.getParameters().values()).transform(Parameter._getType).toList();
		}

		@Override
		public List<Type> visitFunctionSignature(FunctionSignature type) {
			ImmutableList.Builder<Type> builder = ImmutableList.builder();
			builder.add(type.getReturnType());
			builder.addAll(from(type.getParameters().values()).transform(Parameter._getType));
			return builder.build();
		}

		@Override
		public List<Type> visitPrimitive(PrimitiveType type) {
			return ImmutableList.of();
		}
		
	}
	
	static class Record extends Type {
		
		private final ImmutableMap<String,Type> fields;
		
		Record(String name, Map<String,Type> fields) {
			super(name);
			this.fields = ImmutableMap.copyOf(checkNotNull(fields));
		}

		<R> R accept(TypeVisitor<R> visitor) {
			return visitor.visitRecord(this);
		}
		
		public Map<String,Type> getFields() {
			return fields;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof Record)) return false;
			Record that = (Record)obj;
			// cannot just use Objects.equals(this.fields,that.fields) because order matters
			boolean result = Objects.equals(this.name,that.name) && Iterables.elementsEqual(this.fields.entrySet(),that.fields.entrySet());
			return result;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(ObjectArrays.concat(this.name, this.fields.entrySet().toArray()));
		}
		
	}
	
	static class Varray extends Type {

		private final Type elementType;

		Varray(String name, Type elementType) {
			super(name);
			this.elementType = checkNotNull(elementType);
		}
		
		<R> R accept(TypeVisitor<R> visitor) {
			return visitor.visitVarray(this);
		}
		
		public Type getElementType() {
			return elementType;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof Varray)) return false;
			Varray that = (Varray)obj;
			boolean result = Objects.equals(this.name,that.name) && Objects.equals(this.elementType,that.elementType);
			return result;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.name,this.elementType);
		}

	}
	
	static class NestedTable extends Type {
		
		private final Type elementType;

		NestedTable(String name, Type elementType) {
			super(name);
			this.elementType = checkNotNull(elementType);
		}
		
		<R> R accept(TypeVisitor<R> visitor) {
			return visitor.visitNestedTable(this);
		}
		
		public Type getElementType() {
			return elementType;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof NestedTable)) return false;
			NestedTable that = (NestedTable)obj;
			boolean result = Objects.equals(this.name,that.name) && Objects.equals(this.elementType,that.elementType);
			return result;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.name,this.elementType);
		}

	}
	
	static class IndexByTable extends Type {
		
		private final Type elementType;

		private final PrimitiveType indexType;

		IndexByTable(String name, Type elementType, PrimitiveType indexType) {
			super(name);
			Preconditions.checkArgument(indexType != null && indexType.getName().matches("(?i)binary_integer|pls_integer|varchar2\\(\\d+?\\)|varchar|string|long"), "Illegal index type '%s'.", indexType == null ? null : indexType.getName()); // http://docs.oracle.com/cd/B10500_01/appdev.920/a96624/05_colls.htm#19661
			this.indexType = indexType;
			this.elementType = checkNotNull(elementType);
		}
		
		<R> R accept(TypeVisitor<R> visitor) {
			return visitor.visitIndexByTable(this);
		}
		
		public Type getElementType() {
			return elementType;
		}

		public PrimitiveType getIndexType() {
			return indexType;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof IndexByTable)) return false;
			IndexByTable that = (IndexByTable)obj;
			boolean result = Objects.equals(this.name,that.name) && Objects.equals(this.elementType,that.elementType) && Objects.equals(this.indexType,that.indexType);
			return result;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.name,this.elementType,this.indexType);
		}
	}
	
	static class ProcedureSignature extends Type {
		
		private final ImmutableMap<String,Parameter> parameters;

		ProcedureSignature(String name, Map<String,Parameter> parameters) {
			super(name);
			this.parameters = ImmutableMap.copyOf(checkNotNull(parameters));
		}
		
		<R> R accept(TypeVisitor<R> visitor) {
			return visitor.visitProcedureSignature(this);
		}
		
		public Map<String,Parameter> getParameters() {
			return parameters;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof ProcedureSignature)) return false;
			ProcedureSignature that = (ProcedureSignature)obj;
			// cannot just use Objects.equals(this.parameters,that.parameters) because order matters
			boolean result = Objects.equals(this.name,that.name) && Iterables.elementsEqual(this.parameters.entrySet(),that.parameters.entrySet());
			return result;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(ObjectArrays.concat(this.name, this.parameters.entrySet().toArray()));
		}

	}
	
	static class FunctionSignature extends Type {
		
		private final Type returnType;
		
		private final ImmutableMap<String,Parameter> parameters;

		FunctionSignature(String name, Map<String,Parameter> parameters, Type returnType) {
			super(name);
			this.parameters = ImmutableMap.copyOf(checkNotNull(parameters));
			this.returnType = returnType;
		}
		
		<R> R accept(TypeVisitor<R> visitor) {
			return visitor.visitFunctionSignature(this);
		}

		public Type getReturnType() {
			return returnType;
		}
		
		public Map<String,Parameter> getParameters() {
			return parameters;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof FunctionSignature)) return false;
			FunctionSignature that = (FunctionSignature)obj;
			// cannot just use Objects.equals(this.parameters,that.parameters) because order matters
			boolean result = Objects.equals(this.name,that.name)
					&& Iterables.elementsEqual(this.parameters.entrySet(),that.parameters.entrySet())
					&& Objects.equals(this.returnType,that.returnType);
			return result;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(ObjectArrays.concat(new Object[] {this.name,this.returnType}, this.parameters.entrySet().toArray(), Object.class));
		}

	}
	
	static class PrimitiveType extends Type {
		
		PrimitiveType(String name) {
			super(name);
		}
		
		<R> R accept(TypeVisitor<R> visitor) {
			return visitor.visitPrimitive(this);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof PrimitiveType)) return false;
			PrimitiveType that = (PrimitiveType)obj;
			boolean result = Objects.equals(this.name,that.name);
			return result;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.name);
		}

	}

	interface TypeVisitor<R> {
		R visitRecord(Record type);
		R visitVarray(Varray type);
		R visitNestedTable(NestedTable type);
		R visitIndexByTable(IndexByTable type);
		R visitProcedureSignature(ProcedureSignature type);
		R visitFunctionSignature(FunctionSignature type);
		R visitPrimitive(PrimitiveType type);
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


- TDG.predecessors -> children
- presunout do samostatnych trid
- javadoc



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
