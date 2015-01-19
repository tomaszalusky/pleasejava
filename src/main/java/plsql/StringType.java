package plsql;

import java.lang.annotation.Annotation;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import plsql.Plsql.String_;

/**
 * @author Tomas Zalusky
 */
class StringType extends AbstractPrimitiveType {

	public static class StringConverter extends TypeAnnotationStringConverter<String_> {
		
		private static final Pattern PATTERN = Pattern.compile("string\\((\\d+)\\)");
	
		@Override
		public String toString(String_ a) {
			return String.format("string(%d)",a.value());
		}
		
		@Override
		public String_ fromString(String input) {
			Matcher matcher = PATTERN.matcher(input);
			if (!matcher.matches()) {
				return null;
			}
			final int size = Integer.parseInt(matcher.group(1));
			return new Plsql.String_() {
				@Override
				public int value() {
					return size;
				}
				@Override
				public Class<? extends Annotation> annotationType() {
					return String_.class;
				}
			};
		}
		
	}

	StringType(plsql.Plsql.String_ annotation) {
		super(annotation);
	}
	
}
