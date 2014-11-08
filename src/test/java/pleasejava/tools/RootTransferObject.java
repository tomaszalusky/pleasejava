package pleasejava.tools;

/**
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
