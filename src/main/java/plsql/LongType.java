package plsql;

import java.lang.annotation.Annotation;

import plsql.Plsql.Long_;

/**
 * @author Tomas Zalusky
 */
class LongType extends AbstractPrimitiveType {

	public static class StringConverter extends TypeAnnotationStringConverter<Long_> {
	
		@Override
		public String toString(Long_ a) {
			return "long";
		}
	
		@Override
		public Long_ fromString(String input) {
			if (!"long".equals(input)) {
				return null;
			}
			return new Plsql.Long_() {
				@Override
				public Class<? extends Annotation> annotationType() {
					return Long_.class;
				}
			};
		}
		
	}

	LongType(plsql.Plsql.Long_ annotation) {
		super(annotation);
	}
	
}
