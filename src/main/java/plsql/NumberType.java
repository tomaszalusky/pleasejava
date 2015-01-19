package plsql;

import java.lang.annotation.Annotation;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import plsql.Plsql.Number_;

/**
 * @author Tomas Zalusky
 */
class NumberType extends AbstractPrimitiveType {

	public static class StringConverter extends TypeAnnotationStringConverter<Number_> {
		
		private static final Pattern PATTERN = Pattern.compile("number\\((\\d+)(,(\\d+))?\\)");
	
		@Override
		public String toString(Number_ a) {
			return String.format("number(%d%s)",a.value(),a.scale() == 0 ? "" : String.format(",%d",a.scale()));
		}
		
		@Override
		public Number_ fromString(String input) {
			Matcher matcher = PATTERN.matcher(input);
			if (!matcher.matches()) {
				return null;
			}
			final int precision = Integer.parseInt(matcher.group(1));
			final int scale = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
			return new Plsql.Number_() {
				@Override
				public int value() {
					return precision;
				}
				@Override
				public int scale() {
					return scale;
				}
				@Override
				public Class<? extends Annotation> annotationType() {
					return Number_.class;
				}
			};
		}
		
	}

	NumberType(plsql.Plsql.Number_ annotation) {
		super(annotation);
	}
	
}
