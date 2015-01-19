package plsql;

import java.lang.annotation.Annotation;

import plsql.Plsql.Boolean_;

/**
 * @author Tomas Zalusky
 */
class BooleanType extends AbstractPrimitiveType {

	public static class StringConverter extends TypeAnnotationStringConverter<Boolean_> {
		
		@Override
		public String toString(Boolean_ a) {
			return "boolean";
		}
		
		@Override
		public Boolean_ fromString(String input) {
			if (!"boolean".equals(input)) {
				return null;
			}
			return new Plsql.Boolean_() {
				@Override
				public Class<? extends Annotation> annotationType() {
					return Boolean_.class;
				}
			};
		}
		
	}

	BooleanType(plsql.Plsql.Boolean_ annotation) {
		super(annotation);
	}
	
}
