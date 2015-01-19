package plsql;

import java.lang.annotation.Annotation;

import plsql.Plsql.Clob;

/**
 * @author Tomas Zalusky
 */
class ClobType extends AbstractPrimitiveType {

	public static class StringConverter extends TypeAnnotationStringConverter<Clob> {
	
		@Override
		public String toString(Clob a) {
			return "clob";
		}
	
		@Override
		public Clob fromString(String input) {
			if (!"clob".equals(input)) {
				return null;
			}
			return new Plsql.Clob() {
				@Override
				public Class<? extends Annotation> annotationType() {
					return Clob.class;
				}
			};
		}
		
	}

	ClobType(plsql.Plsql.Clob annotation) {
		super(annotation);
	}
	
}
