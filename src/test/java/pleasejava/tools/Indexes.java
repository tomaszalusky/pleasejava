package pleasejava.tools;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author Tomas Zalusky
 */
public class Indexes extends TransferObject {

	private final PrimitiveType indexType;
	
	private final List<Object> data = Lists.newArrayList();  // elements of type which is compatible with indexType

	public Indexes(PrimitiveType indexType, TransferObject parent, TypeNode typeNode) {
		super("{i:" + indexType.name + "}", parent, typeNode);
		this.indexType = indexType;
	}
	
	@Override
	protected String toStringDescription() {
		return "{i:" + indexType.name + "}";
	}
	
}
