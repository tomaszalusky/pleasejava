package plsql;

/**
 * Single (scalar) value of primitive type.
 * @author Tomas Zalusky
 */
public class PrimitiveScalar extends PrimitiveHolder {

	private final AbstractPrimitiveType type;
	
	private Object data;  // runtime type is compatible with type

	public PrimitiveScalar(AbstractPrimitiveType type, TransferObject parent, TypeNode typeNode) {
		super(parent, typeNode, typeNode.id());
		this.type = type;
	}
	
	@Override
	protected String toStringDescription() {
		return type.getName();
	}
	
}
