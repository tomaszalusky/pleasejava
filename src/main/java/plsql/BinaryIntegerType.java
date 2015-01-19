package plsql;

import java.lang.annotation.Annotation;

import plsql.Plsql.BinaryInteger;

/**
 * @author Tomas Zalusky
 */
class BinaryIntegerType extends AbstractPrimitiveType {

	public static class StringConverter extends TypeAnnotationStringConverter<BinaryInteger> {
	
		@Override
		public String toString(BinaryInteger a) {
			return "binary_integer";
		}
	
		@Override
		public BinaryInteger fromString(String input) {
			if (!"binary_integer".equals(input)) {
				return null;
			}
			return new Plsql.BinaryInteger() {
				@Override
				public Class<? extends Annotation> annotationType() {
					return BinaryInteger.class;
				}
			};
		}
		
	}

	BinaryIntegerType(plsql.Plsql.BinaryInteger annotation) {
		super(annotation);
	}
	
}
