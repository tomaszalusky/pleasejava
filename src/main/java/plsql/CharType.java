package plsql;

import java.lang.annotation.Annotation;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import plsql.Plsql.Char_;

/**
 * @author Tomas Zalusky
 */
class CharType extends AbstractPrimitiveType {

	public static class StringConverter extends TypeAnnotationStringConverter<Char_> {
		
		private static final Pattern PATTERN = Pattern.compile("char\\((\\d+)\\)");
	
		@Override
		public String toString(Char_ a) {
			return String.format("char(%d)",a.value());
		}
		
		@Override
		public Char_ fromString(String input) {
			Matcher matcher = PATTERN.matcher(input);
			if (!matcher.matches()) {
				return null;
			}
			final int size = Integer.parseInt(matcher.group(1));
			return new Plsql.Char_() {
				@Override
				public int value() {
					return size;
				}
				@Override
				public Class<? extends Annotation> annotationType() {
					return Char_.class;
				}
			};
		}
		
	}

	CharType(plsql.Plsql.Char_ annotation) {
		super(annotation);
	}
	
}
