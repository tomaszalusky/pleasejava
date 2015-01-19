package plsql;

import java.lang.annotation.Annotation;

import plsql.Plsql.Integer_;

/**
 * @author Tomas Zalusky
 */
class IntegerType extends AbstractPrimitiveType {

	public static class StringConverter extends TypeAnnotationStringConverter<Integer_> {
	
		@Override
		public String toString(Integer_ a) {
			return "integer";
		}
	
		@Override
		public Integer_ fromString(String input) {
			if (!"integer".equals(input)) {
				return null;
			}
			return new Plsql.Integer_() {
				@Override
				public Class<? extends Annotation> annotationType() {
					return Integer_.class;
				}
			};
		}
		
	}

	IntegerType(plsql.Plsql.Integer_ annotation) {
		super(annotation);
	}
	
}
