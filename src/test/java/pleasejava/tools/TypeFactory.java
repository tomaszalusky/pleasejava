package pleasejava.tools;

import java.util.List;
import java.util.Map;

import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;


import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

class TypeFactory {
	
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