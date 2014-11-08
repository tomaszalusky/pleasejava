package pleasejava.tools;


/**
 * @author Tomas Zalusky
 */
public class JdbcTransferrableRecord extends TransferObject {

	private Type type;
	
	private Object data;

	/**
	 * @param type type of transferred record
	 * @param parent
	 * @param typeNode
	 */
	public JdbcTransferrableRecord(Type type, TransferObject parent, TypeNode typeNode) {
		super(type.getName(), parent, typeNode);
		this.type = type;
	}
	
	@Override
	protected String toStringDescription() {
		return type.getName();
	}
	
}
