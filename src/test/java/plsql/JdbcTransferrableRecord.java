package plsql;


/**
 * Represents record which can be transferred via JDBC as whole.
 * This can be possible only for top-level records.
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
		super(parent, typeNode, typeNode.id() + "s"); // s for "struct" (java.sql.Struct)
		this.type = type;
	}
	
	@Override
	protected String toStringDescription() {
		return type.getName();
	}
	
}
