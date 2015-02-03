package plsql;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

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
				System.out.printf("%s = %s%n",prefix,representation);
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
	
	private String packageName;
	
	private static class ImportMapper {
		
	}
	
	private Map<String,ClassModel> classes;
	
	private static class ClassModel {
		
		private String name;
		
		private ImportMapper importMapper;
		
		private boolean isInterface;
		
		private Map<String,FieldModel> fields;
		
		private Map<String,MethodModel> methods;
		
	}
	
	private static class FieldModel {
		
		private String name;
		
		private TypeToken<?> type;
		
		private List<Annotation> annotations;
		
	}

	private static class MethodModel {

		private String name;
		
		private TypeToken<?> returnType;
		
		private List<Annotation> annotations;
		
		private Map<String,ParameterModel> parameters;

		private String body;
		
	}
	
	private static class ParameterModel {
		
		private String name;
		
		private TypeToken<?> type;
		
		private List<Annotation> annotations;
		
	}
	
}
