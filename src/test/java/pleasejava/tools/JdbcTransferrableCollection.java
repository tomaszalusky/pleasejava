package pleasejava.tools;

import java.util.Collection;

import com.google.common.collect.Lists;

/**
 * Represents collection which can be transferred via JDBC as whole.
 * This can be possible only for top-level collections.
 * @author Tomas Zalusky
 */
public class JdbcTransferrableCollection extends TransferObject {

	private final Type type;
	
	private final Collection<?> data = Lists.newArrayList();

	/**
	 * @param type type of transferred collection
	 * @param parent
	 * @param typeNode
	 */
	public JdbcTransferrableCollection(Type type, TransferObject parent, TypeNode typeNode) {
		super(parent, typeNode, typeNode.id() + "a"); // a for "array" (java.sql.Array)
		this.type = type;
	}
	
	@Override
	protected String toStringDescription() {
		return type.getName();
	}
	
}
