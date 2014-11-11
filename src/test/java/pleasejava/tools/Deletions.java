package pleasejava.tools;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * Collection of flags representing holes in nested table.
 * This specific {@link TransferObject} is utilized only for nested tables
 * and only for those of them which don't have continous sequence of indexes due to deletions.
 * @author Tomas Zalusky
 */
public class Deletions extends TransferObject {

	private List<Boolean> data = Lists.newArrayList();

	public Deletions(TransferObject parent, TypeNode typeNode) {
		super(parent, typeNode, typeNode.id() + "d");
	}
	
	@Override
	protected String toStringDescription() {
		return "{d}";
	}

}
