package plsql;

import java.lang.annotation.Annotation;

import plsql.Plsql.Blob;

/**
 * @author Tomas Zalusky
 */
class BlobType extends AbstractPrimitiveType {

	public static class StringConverter extends TypeAnnotationStringConverter<Blob> {
	
		@Override
		public String toString(Blob a) {
			return "blob";
		}
	
		@Override
		public Blob fromString(String input) {
			if (!"blob".equals(input)) {
				return null;
			}
			return new Plsql.Blob() {
				@Override
				public Class<? extends Annotation> annotationType() {
					return Blob.class;
				}
			};
		}
		
	}

	BlobType(plsql.Plsql.Blob annotation) {
		super(annotation);
	}
	
}
