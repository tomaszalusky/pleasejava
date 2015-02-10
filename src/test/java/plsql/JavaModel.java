package plsql;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import pleasejava.Utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;

/**
 * @author Tomas Zalusky
 */
class JavaModel {

	static JavaModel from(TypeGraph typeGraph, Element rootElement) {
		JavaModel javaModel = new JavaModel();
		Generator generator = new Generator(javaModel,rootElement);
		for (AbstractType type : typeGraph.getTopologicalOrdering()) {
			type.accept(generator);
		}
		return javaModel;
	}
	
	static class Generator implements TypeVisitorR<Void> {

		private final JavaModel javaModel;
		private final Element rootElement;

		private Generator(JavaModel javaModel, Element rootElement) {
			this.javaModel = javaModel;
			this.rootElement = rootElement;
		}

		@Override
		public Void visitProcedureSignature(ProcedureSignature type) {
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
					//parameterModel.type = TypeToken.
				}
			}
			return null;
		}
		
		@Override
		public Void visitFunctionSignature(FunctionSignature type) {
			return null;
		}
		
		@Override
		public Void visitRecord(RecordType type) {
			return null;
		}

		@Override
		public Void visitVarray(VarrayType type) {
			return null;
		}

		@Override
		public Void visitNestedTable(NestedTableType type) {
			return null;
		}

		@Override
		public Void visitIndexByTable(IndexByTableType type) {
			return null;
		}

		@Override
		public Void visitPrimitive(AbstractPrimitiveType type) {
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
		
		private TypeToken<?> type;
		
		private List<Annotation> annotations;
		
		public ParameterModel(String name) {
			this.name = name;
		}

		public String toString() {
			StringBuilder result = new StringBuilder();
			Utils.appendf(result, "\t\t\tPARAMETER MODEL (%s)", name);
			return result.toString();
		}

	}

}
