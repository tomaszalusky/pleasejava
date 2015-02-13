package plsql;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import pleasejava.Utils;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;

/**
 * @author Tomas Zalusky
 */
class JavaModel {

	static JavaModel from(TypeGraph typeGraph, Element rootElement) {
		JavaModel javaModel = new JavaModel();
		Generator generator = new Generator(javaModel,typeGraph,rootElement);
		for (AbstractType type : typeGraph.getTopologicalOrdering()) {
			type.accept(generator);
		}
		return javaModel;
	}
	
	static class Generator implements TypeVisitor {

		private final JavaModel javaModel;
		private final Element rootElement;
		private TypeGraph typeGraph;

		private Generator(JavaModel javaModel, TypeGraph typeGraph, Element rootElement) {
			this.javaModel = javaModel;
			this.typeGraph = typeGraph;
			this.rootElement = rootElement;
		}

		@Override
		public void visitProcedureSignature(ProcedureSignature type) {
			String name = type.getName();
			XPathExpression<Element> xpath = XPathFactory.instance().compile("procedure[@name='" + name + "']", Filters.element());
			Element typeElement = Iterables.getOnlyElement(xpath.evaluate(rootElement),null);
			List<Namespace> javaNs = typeElement.getAdditionalNamespaces().stream().filter(n -> n.getPrefix().startsWith("java")).collect(toList());
			Map<String,String> prefixToRepresentation = javaNs.stream().collect(Collectors.toMap(n -> n.getPrefix(), n -> n.getURI()));
			Preconditions.checkState(Objects.equals(javaNs.size(),prefixToRepresentation.values().stream().collect(toSet()).size()),"Duplicit representation of procedure %s.",type.getName());
			for (Map.Entry<String,String> entry : prefixToRepresentation.entrySet()) {
				String prefix = entry.getKey();
				String representation = entry.getValue();
				String className = representation.substring(0,representation.lastIndexOf('.'));
				String methodName = representation.substring(representation.lastIndexOf('.') + 1);
				ClassModel classModel = javaModel.ensureClassModel(className);
				classModel.isInterface = true;
				MethodModel methodModel = classModel.ensureMethodModel(methodName);
				System.out.printf("%s = %s%n",prefix,representation);
				for (Map.Entry<String,Parameter> parameterEntry : type.getParameters().entrySet()) {
					String parameterName = parameterEntry.getKey();
					Parameter parameter = parameterEntry.getValue();
					XPathExpression<Attribute> parameterXPath = XPathFactory.instance().compile("*[@name='" + parameterName + "']/@ns:type",
							Filters.attribute(),Collections.emptyMap(),
							new Namespace[] {Namespace.getNamespace("ns",representation)}
					);
					String attrValue = parameterXPath.evaluateFirst(typeElement).getValue().replace('[','<').replace(']','>');
					int spc = attrValue.indexOf(' ');
					String typeString = attrValue.substring(0, spc == -1 ? attrValue.length() : spc);
					String variableName = spc == -1 ? parameterName : attrValue.substring(spc + 1);
					System.out.printf("\t%s = %s,%s%n",parameterName,typeString,variableName);
					ParameterModel parameterModel = methodModel.ensureParameterModel(variableName);
					if (parameter.getParameterMode() == ParameterMode.OUT) {
						parameterModel.annotations.add("@" + Plsql.Out.class.getName());
					}
					if (parameter.getParameterMode() == ParameterMode.INOUT) {
						parameterModel.annotations.add("@" + Plsql.InOut.class.getName());
					}
					String typeAnnotation = parameter.getType().accept(new AnnotateType());
					if (typeAnnotation != null) {
						parameterModel.annotations.add(typeAnnotation);
					}
					String javaType = parameter.getType().accept(new ComputeJavaType(),typeString);
					// TODO generate type with annotated type arguments
					// TODO sanitize imports
				}
			}
		}
		
		@Override
		public void visitFunctionSignature(FunctionSignature type) {
			String name = type.getName();
			XPathExpression<Element> xpath = XPathFactory.instance().compile("function[@name='" + name + "']", Filters.element());
			Element typeElement = Iterables.getOnlyElement(xpath.evaluate(rootElement),null);
			List<Namespace> javaNs = typeElement.getAdditionalNamespaces().stream().filter(n -> n.getPrefix().startsWith("java")).collect(toList());
			Map<String,String> prefixToRepresentation = javaNs.stream().collect(Collectors.toMap(n -> n.getPrefix(), n -> n.getURI()));
			Preconditions.checkState(Objects.equals(javaNs.size(),prefixToRepresentation.values().stream().collect(toSet()).size()),"Duplicit representation of procedure %s.",type.getName());
			for (Map.Entry<String,String> entry : prefixToRepresentation.entrySet()) {
				String prefix = entry.getKey();
				String representation = entry.getValue();
				String className = representation.substring(0,representation.lastIndexOf('.'));
				String methodName = representation.substring(representation.lastIndexOf('.') + 1);
				ClassModel classModel = javaModel.ensureClassModel(className);
				classModel.isInterface = true;
				MethodModel methodModel = classModel.ensureMethodModel(methodName);
				System.out.printf("%s = %s%n",prefix,representation);
				for (Map.Entry<String,Parameter> parameterEntry : type.getParameters().entrySet()) {
					String parameterName = parameterEntry.getKey();
					Parameter parameter = parameterEntry.getValue();
					XPathExpression<Attribute> parameterXPath = XPathFactory.instance().compile("*[@name='" + parameterName + "']/@ns:type",
							Filters.attribute(),Collections.emptyMap(),
							new Namespace[] {Namespace.getNamespace("ns",representation)}
					);
					String attrValue = parameterXPath.evaluateFirst(typeElement).getValue().replace('[','<').replace(']','>');
					int spc = attrValue.indexOf(' ');
					String typeString = attrValue.substring(0, spc == -1 ? attrValue.length() : spc);
					String variableName = spc == -1 ? parameterName : attrValue.substring(spc + 1);
					System.out.printf("\t%s = %s,%s%n",parameterName,typeString,variableName);
					ParameterModel parameterModel = methodModel.ensureParameterModel(variableName);
					if (parameter.getParameterMode() == ParameterMode.OUT) {
						parameterModel.annotations.add("@" + Plsql.Out.class.getName());
					}
					if (parameter.getParameterMode() == ParameterMode.INOUT) {
						parameterModel.annotations.add("@" + Plsql.InOut.class.getName());
					}
					String typeAnnotation = parameter.getType().accept(new AnnotateType());
					if (typeAnnotation != null) {
						parameterModel.annotations.add(typeAnnotation);
					}
					// TODO generate type with annotated type arguments
					// TODO sanitize imports
					// TODO return type
					// TODO ensure, isInterface - opravit
					// TODO DRY
					// TODO overit nutnost kontroly ruznosti NS
				}
			}
		}
		
		@Override
		public void visitRecord(RecordType type) {
		}

		@Override
		public void visitVarray(VarrayType type) {
		}

		@Override
		public void visitNestedTable(NestedTableType type) {
		}

		@Override
		public void visitIndexByTable(IndexByTableType type) {
		}

		@Override
		public void visitPrimitive(AbstractPrimitiveType type) {
		}
		
	}

	static class AnnotateType implements TypeVisitorR<String> {

		@Override
		public String visitProcedureSignature(ProcedureSignature type) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public String visitFunctionSignature(FunctionSignature type) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public String visitRecord(RecordType type) {
			return null;
		}

		@Override
		public String visitVarray(VarrayType type) {
			return "@" + Plsql.Varray.class.getName() + "(\"" + type.getName() + "\")";
		}

		@Override
		public String visitNestedTable(NestedTableType type) {
			return "@" + Plsql.NestedTable.class.getName() + "(\"" + type.getName() + "\")";
		}

		@Override
		public String visitIndexByTable(IndexByTableType type) {
			return "@" + Plsql.IndexByTable.class.getName() + "(\"" + type.getName() + "\")";
		}

		@Override
		public String visitPrimitive(AbstractPrimitiveType type) {
			Annotation annotation = type.getAnnotation();
			return "@" + annotation.annotationType().getName() + annotationStateToString(annotation);
		}
		
	}
	
	public static String annotationStateToString(Annotation a) {
		StringBuilder result = new StringBuilder();
		try {
			Class<? extends Annotation> annotationType = a.annotationType();
			Method[] declaredMethods = annotationType.getDeclaredMethods();
			if (declaredMethods.length != 0) {
				result.append("(");
				if (declaredMethods.length == 1 && "value".equals(declaredMethods[0].getName())) {
					Object value = declaredMethods[0].invoke(a);
					result.append(value);
				} else {
					String s = Stream.of(annotationType.getDeclaredMethods())
							.map(m -> {
									try {
										return m.getName() + "=" + m.invoke(a);
									} catch (Exception e) {
										throw Throwables.propagate(e);
									}
							})
							.collect(Collectors.joining(", "));
					result.append(s);
				}
				result.append(")");
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw Throwables.propagate(e);
		}
		return result.toString();
	}

	/**
	 * For given PLSQL type (acceptor) and Java representation (visitor argument), as declared in XML,
	 * verifies if Java representation is legal for PLSQL type
	 * and returns String describing Java representation in generated code.
	 * Resulting String is similar to visitor argument, though not the same,
	 * it may contain annotations on inner types, also the auxiliary construct java.lang.Array<T>
	 * is translated into Java-legal T[].
	 * @author Tomas Zalusky
	 */
	static class ComputeJavaType implements TypeVisitorAR<String,String> {

		@Override
		public String visitProcedureSignature(ProcedureSignature type, String typeString) {
			throw new IllegalStateException();
		}

		@Override
		public String visitFunctionSignature(FunctionSignature type, String typeString) {
			throw new IllegalStateException();
		}

		@Override
		public String visitRecord(RecordType type, String typeString) {
			System.out.println("computing java type for PLSQL record " + type.getName() + " and proposed representation " + typeString);
			return null;
		}

		@Override
		public String visitVarray(VarrayType type, String typeString) {
			System.out.println("computing java type for PLSQL varray " + type.getName() + " and proposed representation " + typeString);
			return null;
		}

		@Override
		public String visitNestedTable(NestedTableType type, String typeString) {
			System.out.println("computing java type for PLSQL table " + type.getName() + " and proposed representation " + typeString);
			return null;
		}

		@Override
		public String visitIndexByTable(IndexByTableType type, String typeString) {
			System.out.println("computing java type for PLSQL indexbytable " + type.getName() + " and proposed representation " + typeString);
			return null;
		}

		@Override
		public String visitPrimitive(AbstractPrimitiveType type, String typeString) {
			System.out.println("computing java type for PLSQL primitive " + type.getName() + " and proposed representation " + typeString);
			return null;
		}

		
	}	

	private static class ImportMapper {
		
	}
	
	private Map<String,ClassModel> classes = new LinkedHashMap<>();

	public String toString() {
		StringBuilder result = new StringBuilder();
		Utils.appendf(result, "JAVA CODE MODEL%n");
		for (Map.Entry<String,ClassModel> e : classes.entrySet()) {
			Utils.appendf(result, "\t%s:%n%s%n", e.getKey(), e.getValue());
		}
		return result.toString();
	}
	
	ClassModel ensureClassModel(String className) {
		return classes.computeIfAbsent(className, cn -> new ClassModel(cn));
	}

	private static class ClassModel {
		
		private final String name;
		
		private ImportMapper importMapper;
		
		private boolean isInterface;
		
		private Map<String,FieldModel> fields = new LinkedHashMap<>();
		
		private Map<String,MethodModel> methods = new LinkedHashMap<>();
		
		ClassModel(String name) {
			this.name = name;
		}

		MethodModel ensureMethodModel(String methodName) {
			return methods.computeIfAbsent(methodName, mn -> new MethodModel(mn));
		}
		
		public String toString() {
			StringBuilder result = new StringBuilder();
			Utils.appendf(result, "\tCLASS MODEL (%s %s)", isInterface ? "interface" : "class", name);
			for (Map.Entry<String,MethodModel> e : methods.entrySet()) {
				Utils.appendf(result, "%n\t\t%s:%n%s", e.getKey(), e.getValue());
			}
			return result.toString();
		}
		
	}
	
	private static class FieldModel {
		
		private String name;
		
		private TypeToken<?> type;
		
		private List<Annotation> annotations;
		
	}

	private static class MethodModel {

		private final String name;
		
		private TypeToken<?> returnType;
		
		private List<Annotation> annotations;
		
		private Map<String,ParameterModel> parameters = new LinkedHashMap<>();

		private String body;

		private MethodModel(String name) {
			this.name = name;
		}

		ParameterModel ensureParameterModel(String parameterName) {
			return parameters.computeIfAbsent(parameterName, pn -> new ParameterModel(pn));
		}

		public String toString() {
			StringBuilder result = new StringBuilder();
			Utils.appendf(result, "\t\tMETHOD MODEL (%s)", name);
			for (Map.Entry<String,ParameterModel> e : parameters.entrySet()) {
				Utils.appendf(result, "%n\t\t\t%s:%n%s", e.getKey(), e.getValue());
			}
			return result.toString();
		}

	}
	
	private static class ParameterModel {
		
		private String name;
		
		private String type;
		
		private List<String> annotations = new ArrayList<>();
		
		public ParameterModel(String name) {
			this.name = name;
		}

		public String toString() {
			StringBuilder result = new StringBuilder();
			Utils.appendf(result, "\t\t\tPARAMETER MODEL (%s)", name);
			for (String annotation : annotations) {
				Utils.appendf(result, "%n\t\t\t\t%s",annotation);
			}
			return result.toString();
		}

	}

}
