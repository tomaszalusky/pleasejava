package plsql;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * Collection of indexes in index-by table.
 * This specific {@link TransferObject} is utilized only for index-by tables.
 * Values in this collection represent values of indexes in index-by table, i.e. keys of associative array,
 * table values are in corresponding {@link PrimitiveCollection} at matching index.
 * @author Tomas Zalusky
 */
public class Indexes extends TransferObject {

	private final PrimitiveType indexType;
	
	private final List<Object> data = Lists.newArrayList();  // elements of type which is compatible with indexType

	public Indexes(PrimitiveType indexType, TransferObject parent, TypeNode typeNode) {
		super(parent, typeNode, typeNode.id() + "i");
		this.indexType = indexType;
	}
	
	@Override
	protected String toStringDescription() {
		return "{i:" + indexType.name + "}";
	}
	
}
