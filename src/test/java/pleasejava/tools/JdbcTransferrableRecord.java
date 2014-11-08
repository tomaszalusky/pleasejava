package pleasejava.tools;

import java.util.Collection;

import com.google.common.collect.Lists;

/**
 * @author Tomas Zalusky
 */
public class JdbcTransferrableRecord extends TransferObject {

	private Object data;

	/**
	 * @param type type of transferred record
	 * @param parent
	 * @param typeNode
	 */
	public JdbcTransferrableRecord(Type type, TransferObject parent, TypeNode typeNode) {
		super(type.getName(), parent, typeNode);
	}
	
}
