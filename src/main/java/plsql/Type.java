package plsql;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import plsql.AbstractType.TypeAnnotationStringConverter;

/**
 * This metaannotation marks annotation as representing some PLSQL type.
 * @author Tomas Zalusky
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@interface Type {
	
	/**
	 * @return standard means for handling types
	 */
	Class<? extends TypeAnnotationStringConverter<? extends Annotation>> nameConverter();

}
