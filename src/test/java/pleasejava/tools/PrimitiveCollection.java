package pleasejava.tools;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author Tomas Zalusky
 */
public class PrimitiveCollection extends TransferObject {

	private final PrimitiveType type;
	
	private final List<Object> data = Lists.newArrayList();  // elements of type which is compatible with type

	public PrimitiveCollection(PrimitiveType type, TransferObject parent, TypeNode typeNode) {
		super(parent, typeNode);
		this.type = type;
	}

	protected String toStringDescription() {
		return "{" + type.name + "}";
	}
	
}
