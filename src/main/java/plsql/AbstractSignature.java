package plsql;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Common ancestor of helper types which describe procedure and function signatures.
 * Despite they don't represent real PLSQL types, it's handy to model them as subclass of {@link AbstractType}
 * because their structure is very similar to record type. 
 * @author Tomas Zalusky
 */
abstract class AbstractSignature extends AbstractType {

	AbstractSignature(Annotation annotation) {
		super(annotation);
	}

	abstract Map<String,Parameter> getParameters();

}
