package pleasejava.tools;

/**
 * Transfer objects attached to primitive type node.
 * Common superclass facilitates finding transfer objects of any of its subclasses. 
 * @author Tomas Zalusky
 */
public abstract class PrimitiveHolder extends TransferObject {

	public PrimitiveHolder(TransferObject parent, TypeNode typeNode, String id) {
		super(parent, typeNode, id);
	}

}