package plsql;

import java.lang.annotation.Annotation;

import plsql.Plsql.Date;

/**
 * @author Tomas Zalusky
 */
class DateType extends AbstractPrimitiveType {

	public static class StringConverter extends TypeAnnotationStringConverter<Date> {
	
		@Override
		public String toString(Date a) {
			return "date";
		}
	
		@Override
		public Date fromString(String input) {
			if (!"date".equals(input)) {
				return null;
			}
			return new Plsql.Date() {
				@Override
				public Class<? extends Annotation> annotationType() {
					return Date.class;
				}
			};
		}
		
	}

	DateType(plsql.Plsql.Date annotation) {
		super(annotation);
	}
	
}
