package pleasejava.tools;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import plsql.Plsql;
import plsql.Plsql.Varchar2;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

/**
 * Recognizes types from XML.
 * Guards circular dependencies and other mismatches.
 * @author Tomas Zalusky
 */
class TypeFactory {
	
	private final Element rootElement;
	
	/**
	 * Map of types identified by their names.
	 * In the time between first request for given name and finishing the instantiation of respective type,
	 * the {@link Optional#absent()} serves as map value.
	 * Encountering {@link Optional#absent()} value during construction indicates circular dependency.
	 */
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

	/**
	 * Returns instance of type for given name.
	 * If such an instance does not exist, it is recursively created.
	 * @param name
	 * @return
	 */
	Type ensureType(String name) {
		Optional<Type> optionalType = typeByName.get(name);
		if (optionalType == null) { // type node has not been constructed yet
			typeByName.put(name, Optional.<Type>absent()); // marked as being built
			Type result;
			if (name.matches("integer|pls_integer|boolean|varchar2\\(\\d+\\)|varchar|string\\(\\d+\\)|string|number\\(\\d+\\)|binary_integer|long|clob")) {
//				Plsql.Varchar2 anno = new Plsql.Varchar2() {
//					@Override
//					public int value() {
//						return 1;
//					}
//
//					@Override
//					public Class<? extends Annotation> annotationType() {
//						return Plsql.Varchar2.class;
//					}
//				};
//				Utils.makeAnnotation(Plsql.Varchar2.class) // a la http://stackoverflow.com/a/16326389/653539
//				a = Utils.annotation(Plsql.Varchar2.class)
//				mock(a,a.value(),1);
//				mock(a,Plsql.Varchar2::value,) // mock(T,Supplier<R>,R);
				// TODO build Annotation from name string and pass to PrimitiveType ctor
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
						plsql.Plsql.Varray annotation = new Plsql.Varray.StringConverter().fromString(name);
						result = new Varray(annotation,elementType);
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
						plsql.Plsql.Procedure annotation = new Plsql.Procedure.StringConverter().fromString(name);
						result = new ProcedureSignature(annotation,builder.build());
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
						plsql.Plsql.Function annotation = new Plsql.Function.StringConverter().fromString(name);
						result = new FunctionSignature(annotation,builder.build(),returnType);
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