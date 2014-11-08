package pleasejava.tools;

import java.util.Collection;

import com.google.common.collect.Lists;

/**
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
		super(parent, typeNode, typeNode.id());
		this.type = type;
	}
	
	@Override
	protected String toStringDescription() {
		return type.getName();
	}
	
}
