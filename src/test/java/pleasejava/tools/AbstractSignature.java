package pleasejava.tools;

/**
 * Common ancestor of helper types which describe procedure and function signatures.
 * Despite they don't represent real PLSQL types, it's handy to model them as subclass of {@link Type}
 * because their structure is very similar to record type. 
 * @author Tomas Zalusky
 */
public abstract class AbstractSignature extends Type {

	public AbstractSignature(String name) {
		super(name);
	}

}