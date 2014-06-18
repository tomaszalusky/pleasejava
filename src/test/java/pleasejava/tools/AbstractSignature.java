package pleasejava.tools;

/**
 * Common ancestor of helper types which represent fictive record-like types
 * describing procedure and function signatures.
 * @author Tomas Zalusky
 */
public abstract class AbstractSignature extends Type {

	public AbstractSignature(String name) {
		super(name);
	}

}