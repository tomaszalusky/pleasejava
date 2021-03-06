package plsql;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.jdom2.filter.Filters.attribute;
import static org.jdom2.filter.Filters.element;
import static pleasejava.Utils.findOnly;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import pleasejava.Utils;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.primitives.Primitives;

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

	private static String extractPackageName(String fullClassNameAndMaybeMethod) {
		return fullClassNameAndMaybeMethod.substring(0,fullClassNameAndMaybeMethod.lastIndexOf('.'));
	}

	private static String extractFullClassName(String fullClassNameAndMaybeMethod) {
		String result = extractPackageName(fullClassNameAndMaybeMethod) + "." + extractSimpleClassName(fullClassNameAndMaybeMethod);
		return result;
	}

	private static String extractSimpleClassName(String fullClassNameAndMaybeMethod) {
		String classAndMaybeMethod = fullClassNameAndMaybeMethod.substring(fullClassNameAndMaybeMethod.lastIndexOf('.') + 1);
		int doubleColon = classAndMaybeMethod.indexOf("::");
		String result = doubleColon == -1 ? classAndMaybeMethod : classAndMaybeMethod.substring(0,doubleColon);
		return result;
	}

	private static String extractMethodName(String fullClassNameAndMaybeMethod) {
		String classAndMaybeMethod = fullClassNameAndMaybeMethod.substring(fullClassNameAndMaybeMethod.lastIndexOf('.') + 1);
		int doubleColon = classAndMaybeMethod.indexOf("::");
		String result = doubleColon == -1 ? null : classAndMaybeMethod.substring(doubleColon + 2);
		return result;
	}

	private static String computeJavaGetterName(String javaFieldName, String type) {
		String result = ("@Boolean_ boolean".equals(type) ? "is" : "get")
				+ javaFieldName.substring(0,1).toUpperCase() + javaFieldName.substring(1);
		return result;
	}

	private static String computeJavaSetterName(String javaFieldName) {
		String result = "set" + javaFieldName.substring(0,1).toUpperCase() + javaFieldName.substring(1);
		return result;
	}

	private static String indent(String input) {
		return input.replaceAll("(?<=\\n|^)","\t");
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

		private static <T> List<T> evalXpath(Object context, String xpath, Filter<T> filter, Namespace... namespaces) {
			XPathExpression<T> xpathExpr = XPathFactory.instance().compile(xpath,filter,Collections.emptyMap(),namespaces);
			List<T> result = xpathExpr.evaluate(context);
			return result;
		}
		
		@Override
		public void visitProcedureSignature(ProcedureSignature type) {
			String name = type.getName();
			Element typeElement = evalXpath(rootElement,"procedure[@name='" + name + "']",element())
					.stream().collect(findOnly()).get();
			List<String> representations = typeElement.getAdditionalNamespaces()
					.stream().map(n -> n.getURI()).collect(toList());
			for (String representation : representations) {
				String className = extractFullClassName(representation);
				String methodName = extractMethodName(representation);
				ClassModel classModel = javaModel.classes.computeIfAbsent(className, cn -> new ClassModel(cn,true));
				MethodModel methodModel = classModel.methods.computeIfAbsent(methodName, mn -> new MethodModel(EnumSet.noneOf(Modifier.class),mn));
				methodModel.annotations.add(type.accept(new ComputeJavaAnnotation(classModel.importModel)));
				generateParameters(type, typeElement, representation, classModel.importModel, methodModel);
			}
		}

		@Override
		public void visitFunctionSignature(FunctionSignature type) {
			String name = type.getName();
			Element typeElement = evalXpath(rootElement,"function[@name='" + name + "']",element())
					.stream().collect(findOnly()).get();
			List<String> representations = typeElement.getAdditionalNamespaces()
					.stream().map(n -> n.getURI()).collect(toList());
			for (String representation : representations) {
				String className = extractFullClassName(representation);
				String methodName = extractMethodName(representation);
				ClassModel classModel = javaModel.classes.computeIfAbsent(className, cn -> new ClassModel(cn,true));
				MethodModel methodModel = classModel.methods.computeIfAbsent(methodName, mn -> new MethodModel(EnumSet.noneOf(Modifier.class),mn));
				methodModel.annotations.add(type.accept(new ComputeJavaAnnotation(classModel.importModel)));
				String javaTypeString = evalXpath(typeElement, "return/@ns:type", attribute(), Namespace.getNamespace("ns",representation))
						.stream().findFirst().get().getValue().replace('[','<').replace(']','>');
				ParameterModel returnTypeModel = methodModel.parameters.computeIfAbsent(null, pn -> new ParameterModel(pn));
				returnTypeModel.type = type.getReturnType().accept(new ComputeJavaType(classModel.importModel),javaTypeString);
				generateParameters(type, typeElement, representation, classModel.importModel, methodModel);
			}
		}

		private static void generateParameters(AbstractSignature type, Element typeElement, String representation, ImportModel importModel, MethodModel methodModel) {
			for (Map.Entry<String,Parameter> parameterEntry : type.getParameters().entrySet()) {
				String parameterName = parameterEntry.getKey();
				Parameter parameter = parameterEntry.getValue();
				String javaTypeStringAndParameterName = evalXpath(typeElement, "*[@name='" + parameterName + "']/@ns:type", attribute(), Namespace.getNamespace("ns",representation))
						.stream().findFirst().get().getValue().replace('[','<').replace(']','>');
				int space = javaTypeStringAndParameterName.indexOf(' ');
				String javaTypeString = javaTypeStringAndParameterName.substring(0, space == -1 ? javaTypeStringAndParameterName.length() : space);
				String javaParameterName = space == -1 ? parameterName : javaTypeStringAndParameterName.substring(space + 1);
				ParameterModel parameterModel = methodModel.parameters.computeIfAbsent(javaParameterName, pn -> new ParameterModel(pn));
				if (parameter.getParameterMode() == ParameterMode.OUT) {
					parameterModel.annotations.add("@" + importModel.add(Plsql.Out.class.getName()));
				}
				if (parameter.getParameterMode() == ParameterMode.INOUT) {
					parameterModel.annotations.add("@" + importModel.add(Plsql.InOut.class.getName()));
				}
				parameterModel.type = parameter.getType().accept(new ComputeJavaType(importModel),javaTypeString);
			}
		}

		@Override
		public void visitRecord(RecordType type) {
			String name = type.getName();
			Element typeElement = evalXpath(rootElement,"record[@name='" + name + "']",element())
					.stream().collect(findOnly()).get();
			List<String> representations = typeElement.getAdditionalNamespaces()
					.stream().map(n -> n.getURI()).collect(toList());
			for (String representation : representations) {
				String className = representation;
				ClassModel classModel = javaModel.classes.computeIfAbsent(className, cn -> new ClassModel(cn,false));
				for (Map.Entry<String,AbstractType> fieldEntry : type.getFields().entrySet()) {
					String fieldName = fieldEntry.getKey();
					AbstractType fieldType = fieldEntry.getValue();
					String javaTypeStringAndFieldName = evalXpath(typeElement, "field[@name='" + fieldName + "']/@ns:type", attribute(), Namespace.getNamespace("ns",representation))
							.stream().findFirst().get().getValue().replace('[','<').replace(']','>');
					int space = javaTypeStringAndFieldName.indexOf(' ');
					String javaTypeString = javaTypeStringAndFieldName.substring(0, space == -1 ? javaTypeStringAndFieldName.length() : space);
					String javaFieldName = space == -1 ? fieldName : javaTypeStringAndFieldName.substring(space + 1);
					FieldModel fieldModel = classModel.fields.computeIfAbsent(javaFieldName, fn -> new FieldModel(EnumSet.of(PRIVATE),fn));
					fieldModel.type = fieldType.accept(new ComputeJavaType(classModel.importModel),javaTypeString);
					String javaGetterName = computeJavaGetterName(javaFieldName,fieldModel.type); 
					MethodModel getterModel = classModel.methods.computeIfAbsent(javaGetterName, mn ->
							new MethodModel(EnumSet.of(PUBLIC),mn,String.format("return %s;",javaFieldName)));
					String javaSetterName = computeJavaSetterName(javaFieldName); 
					MethodModel setterModel = classModel.methods.computeIfAbsent(javaSetterName, mn ->
							new MethodModel(EnumSet.of(PUBLIC),mn,String.format("this.%1$s = %1$s;",javaFieldName)));
					setterModel.parameters.computeIfAbsent(javaFieldName, pn -> {
						ParameterModel pm = new ParameterModel(pn);
						pm.type = fieldModel.type.replaceAll(" ?@[A-Za-z0-9_]++(\\([^\\)]*\\))? *+","");
						return pm;
					});
				}
			}
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

	/**
	 * Generates Java annotation for particular use of PLSQL type. 
	 * @author Tomas Zalusky
	 */
	static class ComputeJavaAnnotation implements TypeVisitorR<String> {

		private final ImportModel importModel;

		ComputeJavaAnnotation(ImportModel importModel) {
			this.importModel = importModel;
		}

		@Override
		public String visitProcedureSignature(ProcedureSignature type) {
			return "@" + importModel.add(Plsql.Procedure.class.getName()) + "(\"" + type.getName() + "\")";
		}
		
		@Override
		public String visitFunctionSignature(FunctionSignature type) {
			return "@" + importModel.add(Plsql.Function.class.getName()) + "(\"" + type.getName() + "\")";
		}
		
		@Override
		public String visitRecord(RecordType type) {
			return "";
		}

		@Override
		public String visitVarray(VarrayType type) {
			return "@" + importModel.add(Plsql.Varray.class.getName()) + "(\"" + type.getName() + "\")";
		}

		@Override
		public String visitNestedTable(NestedTableType type) {
			return "@" + importModel.add(Plsql.NestedTable.class.getName()) + "(\"" + type.getName() + "\")";
		}

		@Override
		public String visitIndexByTable(IndexByTableType type) {
			return "@" + importModel.add(Plsql.IndexByTable.class.getName()) + "(\"" + type.getName() + "\")";
		}

		@Override
		public String visitPrimitive(AbstractPrimitiveType type) {
			Annotation annotation = type.getAnnotation();
			return "@" + importModel.add(annotation.annotationType().getName()) + annotationStateToString(annotation);
		}
		
		static String annotationStateToString(Annotation a) {
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
								.collect(Collectors.joining(","));
						result.append(s);
					}
					result.append(")");
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw Throwables.propagate(e);
			}
			return result.toString();
		}
		
	}
	
	/**
	 * For given PLSQL type (acceptor) and Java representation (visitor argument), as declared in XML,
	 * TODO(not true: "verifies if Java representation is legal for PLSQL type" - verification will be done during reading java code, not during generation)
	 * and returns String describing Java representation in generated code.
	 * Resulting String is similar to visitor argument, though not the same,
	 * it may contain annotations on inner types, also the auxiliary construct java.lang.Array<T>
	 * is translated into Java-legal T[].
	 * @author Tomas Zalusky
	 */
	static class ComputeJavaType implements TypeVisitorAR<String,String> {

		private final ImportModel importModel;
		
		private final ComputeJavaAnnotation computeJavaAnnotation;
		
		public ComputeJavaType(ImportModel importModel) {
			this.importModel = importModel;
			this.computeJavaAnnotation = new ComputeJavaAnnotation(importModel);
		}

		/**
		 * Compute insertion point of array declaration into array element declaration.
		 * Unlike composite collection types where source code representing collection type just wraps
		 * the source code representing element type
		 * (for example, <code>List</code> of <code>List&lt;String></code> is <code>List&lt;List&lt;String>></code>),
		 * composite array types are built "from the middle".
		 * For example, array of <code>String[]</code> is <code>String<b>[]</b>[]</code>.
		 * The outer array is denoted by first pair of brackets, which is important for proper annotating.
		 * @param elementJavaType source code declaration of annotated type of array element
		 * @return valid index into elementJavaType string which the source code declaration
		 * of annotated array type will be inserted at.
		 * Separating space is part of inserted string.
		 */
		private static int findJavaArrayElementDeclarationInsertionPoint(String elementJavaType) {
			int firstBrackets = elementJavaType.indexOf("[]"); // find leftmost bracket pair  (TODO bug: for alternating nesting of array and list, for example List<String[]>[], doesn't work, finds wrong index, but for testing purposes simple algorithm is sufficient)
			int result;
			if (firstBrackets == -1) { // no inner array (for example @Varchar2 String) -> insertion will be performed at the end
				result = elementJavaType.length();
			} else { // find annotation of bracket pair
				int lastAtsign = elementJavaType.substring(0,firstBrackets).lastIndexOf('@');
				for (result = lastAtsign; elementJavaType.charAt(result - 1) == ' '; result--);
			}
			return result;
		}
		
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
			return importModel.add(typeString); /* record classes are annotated themselves, no need to annotate them at the point of use */
		}

		@Override
		public String visitVarray(VarrayType type, String typeString) {
			StringBuilder result = new StringBuilder();
			int l = typeString.indexOf('<');
			int r = typeString.lastIndexOf('>');
			Preconditions.checkArgument(l != -1 && r != -1);
			String typeName = typeString.substring(0,l);
			String typeAnnotation = type.accept(computeJavaAnnotation);
			AbstractType elementType = type.getElementType();
			switch (typeName) {
				case "java.lang.Array" : {
					String elementTypeString = typeString.substring(l + 1,r);
					String elementJavaType = elementType.accept(this,elementTypeString);
					int splitPoint = findJavaArrayElementDeclarationInsertionPoint(elementJavaType);
					String before = elementJavaType.substring(0,splitPoint);
					String after = elementJavaType.substring(splitPoint);
					Utils.appendf(result, "%s %s []%s",before,typeAnnotation,(after.isEmpty() ? "" : " ") + after);
					break;
				} case "java.util.List" : case "java.util.Vector" : {
					String elementTypeString = typeString.substring(l + 1,r);
					String elementJavaType = elementType.accept(this,elementTypeString);
					Utils.appendf(result, "%s %s<%s>",typeAnnotation,importModel.add(typeName),elementJavaType);
					break;
				} default : {
					throw new IllegalStateException("java type " + typeName + " cannot be used for varrays");
				}
			}
			return result.toString();
		}

		@Override
		public String visitNestedTable(NestedTableType type, String typeString) {
			StringBuilder result = new StringBuilder();
			int l = typeString.indexOf('<');
			int r = typeString.lastIndexOf('>');
			Preconditions.checkArgument(l != -1 && r != -1);
			String typeName = typeString.substring(0,l);
			String typeAnnotation = type.accept(computeJavaAnnotation);
			AbstractType elementType = type.getElementType();
			switch (typeName) {
				case "java.util.Map" : {
					int c = typeString.indexOf(',',l);
					Preconditions.checkArgument(c != -1);
					String keyJavaType = typeString.substring(l + 1,c);
					String elementTypeString = typeString.substring(c + 1,r);
					String elementJavaType = elementType.accept(this,elementTypeString);
					Utils.appendf(result, "%s %s<%s,%s>",typeAnnotation,importModel.add(typeName),importModel.add(keyJavaType),elementJavaType);
					break;
				} case "java.lang.Array" : {
					String elementTypeString = typeString.substring(l + 1,r);
					String elementJavaType = elementType.accept(this,elementTypeString);
					int splitPoint = findJavaArrayElementDeclarationInsertionPoint(elementJavaType);
					String before = elementJavaType.substring(0,splitPoint), after = elementJavaType.substring(splitPoint);
					Utils.appendf(result, "%s %s []%s",before,typeAnnotation,(after.isEmpty() ? "" : " ") + after);
					break;
				} case "java.util.List" : case "java.util.Vector" : {
					String elementTypeString = typeString.substring(l + 1,r);
					String elementJavaType = elementType.accept(this,elementTypeString);
					Utils.appendf(result, "%s %s<%s>",typeAnnotation,importModel.add(typeName),elementJavaType);
					break;
				} default : {
					throw new IllegalStateException("java type " + typeName + " cannot be used for nested table");
				}
			}
			return result.toString();
		}

		@Override
		public String visitIndexByTable(IndexByTableType type, String typeString) {
			StringBuilder result = new StringBuilder();
			int l = typeString.indexOf('<');
			int r = typeString.lastIndexOf('>');
			Preconditions.checkArgument(l != -1 && r != -1);
			String typeName = typeString.substring(0,l);
			String typeAnnotation = type.accept(computeJavaAnnotation);
			AbstractType elementType = type.getElementType();
			switch (typeName) {
				case "java.util.Map" : case "java.util.SortedMap" : {
					int c = typeString.indexOf(',',l);
					Preconditions.checkArgument(c != -1);
					String keyJavaType = typeString.substring(l + 1,c);
					String keyTypeAnnotation = type.getIndexType().accept(computeJavaAnnotation);
					String elementTypeString = typeString.substring(c + 1,r);
					String elementJavaType = elementType.accept(this,elementTypeString);
					Utils.appendf(result, "%s %s<%s %s,%s>",typeAnnotation,importModel.add(typeName),keyTypeAnnotation,importModel.add(keyJavaType),elementJavaType);
					break;
				} default : {
					throw new IllegalStateException("java type " + typeName + " cannot be used for nested table");
				}
			}
			return result.toString();
		}

		@Override
		public String visitPrimitive(AbstractPrimitiveType type, String typeString) {
			String typeAnnotation = type.accept(computeJavaAnnotation);
			String result = String.format("%s %s",typeAnnotation,importModel.add(typeString));
			return result;
		}
		
	}

	private static class ImportModel {
		
		private SortedSet<String> imports = new TreeSet<>();
		
		String add(String className) {
			String fullJavaName = className.replace('$','.');
			if (!fullJavaName.startsWith("java.lang.")
					&& !Primitives.allPrimitiveTypes().stream().anyMatch(c -> c.getName().equals(className))) {
				imports.add(fullJavaName);
			}
			int lastDot = fullJavaName.lastIndexOf('.');
			String shortJavaName = className.substring(lastDot + 1);
			return shortJavaName;
		}
		
		public String toString() {
			String result = String.format("\t\tIMPORTS:%s",imports.stream().map(s -> String.format("%n\t\t\t%s",s)).collect(joining()));
			return result;
		}

		public String toJavaSource() {
			StringBuilder result = new StringBuilder();
			imports.stream().forEach(i -> Utils.appendf(result,"import %s;%n",i));
			return result.toString();
		}
		
	}
	
	private static class ClassModel {
		
		private final String name;
		
		private final boolean isInterface;
		
		private final ImportModel importModel = new ImportModel();
		
		private final Map<String,FieldModel> fields = new LinkedHashMap<>();
		
		private final Map<String,MethodModel> methods = new LinkedHashMap<>();
		
		ClassModel(String name, boolean isInterface) {
			this.name = name;
			this.isInterface = isInterface;
		}

		public String toString() {
			StringBuilder result = new StringBuilder();
			Utils.appendf(result, "CLASS MODEL (%s %s)%n%s%n\t\tFIELDS:", isInterface ? "interface" : "class", name, importModel);
			for (Map.Entry<String,FieldModel> e : fields.entrySet()) {
				Utils.appendf(result, "%n\t\t\t%s = %s", e.getKey(), e.getValue());
			}
			Utils.appendf(result,"%n\t\tMETHODS:");
			for (Map.Entry<String,MethodModel> e : methods.entrySet()) {
				Utils.appendf(result, "%n\t\t\t%s = %s", e.getKey(), e.getValue());
			}
			return result.toString();
		}

		String toJavaSource() {
			StringBuilder buf = new StringBuilder();
			Utils.appendf(buf, "package %s;%n%n", extractPackageName(name));
			Utils.appendf(buf, "%s%n", importModel.toJavaSource());
			Utils.appendf(buf, "public %s %s {%n%n", isInterface ? "interface" : "class", extractSimpleClassName(name));
			fields.values().forEach(fm -> Utils.appendf(buf, "%s%n", fm.toJavaSource()));
			Utils.appendf(buf, "}%n",extractSimpleClassName(name));
			// TODO
			return buf.toString();
		}
		
	}
	
	private static class FieldModel {
		
		private final Set<Modifier> modifiers;
		
		private final String name;
		
		private final List<String> annotations = new ArrayList<>();
		
		private String type;

		public FieldModel(Set<Modifier> modifiers, String name) {
			this.modifiers = modifiers;
			this.name = name;
		}

		public String toString() {
			StringBuilder result = new StringBuilder();
			Utils.appendf(result, "FIELD MODEL (%s %s)%n\t\t\t\tANNOTATIONS:", modifiers, name);
			for (String annotation : annotations) {
				Utils.appendf(result, "%n\t\t\t\t\t%s",annotation);
			}
			Utils.appendf(result, "%n\t\t\t\tTYPE:%n\t\t\t\t\t%s",type);
			return result.toString();
		}

		public String toJavaSource() {
			StringBuilder result = new StringBuilder();
			annotations.stream().forEach(a -> Utils.appendf(result, "\t%s%n", a));
			Utils.appendf(result, "\t%s%s %s;%n",modifiers.stream().map(m -> m.toString() + " ").collect(joining()),type,name);
			return result.toString();
		}
		
	}

	private static class MethodModel {

		private final Set<Modifier> modifiers;
		
		private final String name;
		
		private final List<String> annotations = new ArrayList<>();
		
		/**
		 * All parameters.
		 * For simplicity, return type is stored under null key.
		 */
		private final Map<String,ParameterModel> parameters = new LinkedHashMap<>();
		
		private final String body;

		private MethodModel(Set<Modifier> modifiers, String name) {
			this(modifiers,name,null);
		}

		private MethodModel(Set<Modifier> modifiers, String name, String body) {
			this.modifiers = modifiers;
			this.name = name;
			this.body = body;
		}
		
		public String toString() {
			StringBuilder result = new StringBuilder();
			Utils.appendf(result, "METHOD MODEL (%s %s)%n\t\t\t\tANNOTATIONS:", modifiers, name);
			for (String annotation : annotations) {
				Utils.appendf(result, "%n\t\t\t\t\t%s", annotation);
			}
			Utils.appendf(result, "%n\t\t\t\tPARAMETERS:", name);
			for (Map.Entry<String,ParameterModel> e : parameters.entrySet()) {
				Utils.appendf(result, "%n\t\t\t\t\t%s = %s", e.getKey(), e.getValue());
			}
			if (body != null) {
				Utils.appendf(result, "%n\t\t\t\tBODY:%n%s",indent(indent(indent(indent(indent(body))))));
			}
			return result.toString();
		}
		
	}
	
	private static class ParameterModel {
		
		private final String name;
		
		private final List<String> annotations = new ArrayList<>();
		
		private String type;
		
		public ParameterModel(String name) {
			this.name = name;
		}

		public String toString() {
			StringBuilder result = new StringBuilder();
			Utils.appendf(result, "PARAMETER MODEL (%s)%n\t\t\t\t\t\tANNOTATIONS:", name);
			for (String annotation : annotations) {
				Utils.appendf(result, "%n\t\t\t\t\t\t\t%s",annotation);
			}
			Utils.appendf(result, "%n\t\t\t\t\t\tTYPE:%n\t\t\t\t\t\t\t%s",type);
			return result.toString();
		}

	}

	private Map<String,ClassModel> classes = new LinkedHashMap<>();

	public String toString() {
		StringBuilder result = new StringBuilder();
		Utils.appendf(result, "JAVA MODEL:%n");
		for (Map.Entry<String,ClassModel> e : classes.entrySet()) {
			Utils.appendf(result, "\t%s = %s%n", e.getKey(), e.getValue());
		}
		return result.toString();
	}
	
	Map<String,String> toJavaSources() {
		Map<String,String> result = new LinkedHashMap<>();
		for (Map.Entry<String,ClassModel> e : classes.entrySet()) {
			result.put(e.getKey(),e.getValue().toJavaSource());
		}
		return result;
	}
	
}
