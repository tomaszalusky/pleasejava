package plsql;

import java.lang.annotation.Annotation;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import plsql.Plsql.Varchar2;

/**
 * @author Tomas Zalusky
 */
class Varchar2Type extends AbstractPrimitiveType {

	public static class StringConverter extends TypeAnnotationStringConverter<Varchar2> {
		
		private static final Pattern PATTERN = Pattern.compile("varchar2\\((\\d+)\\)");
	
		@Override
		public String toString(Varchar2 a) {
			return String.format("varchar2(%d)",a.value());
		}
		
		@Override
		public Varchar2 fromString(String input) {
			Matcher matcher = PATTERN.matcher(input);
			if (!matcher.matches()) {
				return null;
			}
			final int size = Integer.parseInt(matcher.group(1));
			return new Plsql.Varchar2() {
				@Override
				public int value() {
					return size;
				}
				@Override
				public Class<? extends Annotation> annotationType() {
					return Varchar2.class;
				}
			};
		}
		
	}

	Varchar2Type(plsql.Plsql.Varchar2 annotation) {
		super(annotation);
	}
	
}
