package pleasejava.tools;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * Collection of primitive values.
 * Can be sent via JDBC as a whole (i.e. using java.sql.Array).
 * @author Tomas Zalusky
 */
public class PrimitiveCollection extends TransferObject {

	private final PrimitiveType type;
	
	private final List<Object> data = Lists.newArrayList();  // elements of type which is compatible with type

	public PrimitiveCollection(PrimitiveType type, TransferObject parent, TypeNode typeNode) {
		super(parent, typeNode, typeNode.id());
		this.type = type;
	}

	protected String toStringDescription() {
		return "{" + type.name + "}";
	}
	
}