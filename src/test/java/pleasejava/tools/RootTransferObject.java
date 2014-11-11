package pleasejava.tools;

/**
 * Root of hierarchy of transfer objects, not intended to transfer any data itself.
 * @author Tomas Zalusky
 */
public class RootTransferObject extends TransferObject {

	public RootTransferObject(TypeNode typeNode) {
		super(null, typeNode, typeNode.id());
	}
	
	@Override
	protected String toStringDescription() {
		return "/";
	}
	
}
