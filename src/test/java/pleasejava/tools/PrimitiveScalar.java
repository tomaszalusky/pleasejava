package pleasejava.tools;


/**
 * @author Tomas Zalusky
 */
public class PrimitiveScalar extends TransferObject {

	private final PrimitiveType type;
	
	private Object data;  // runtime type is compatible with type

	public PrimitiveScalar(PrimitiveType type, TransferObject parent, TypeNode typeNode) {
		super(type.name, parent, typeNode);
		this.type = type;
	}
	
}
