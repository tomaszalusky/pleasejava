package plsql;

import java.lang.annotation.Annotation;

import plsql.Plsql.PlsInteger;

/**
 * @author Tomas Zalusky
 */
class PlsIntegerType extends AbstractPrimitiveType {

	public static class StringConverter extends TypeAnnotationStringConverter<PlsInteger> {
	
		@Override
		public String toString(PlsInteger a) {
			return "pls_integer";
		}
	
		@Override
		public PlsInteger fromString(String input) {
			if (!"pls_integer".equals(input)) {
				return null;
			}
			return new Plsql.PlsInteger() {
				@Override
				public Class<? extends Annotation> annotationType() {
					return PlsInteger.class;
				}
			};
		}
		
	}

	PlsIntegerType(plsql.Plsql.PlsInteger annotation) {
		super(annotation);
	}
	
}
