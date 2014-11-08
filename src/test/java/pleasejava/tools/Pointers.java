package pleasejava.tools;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author Tomas Zalusky
 */
public class Pointers extends TransferObject {

	private final List<Integer> data = Lists.newArrayList();

	private final boolean simple;
	
	public Pointers(TransferObject parent, TypeNode typeNode, boolean simple) {
		super(simple ? "{sp}" : "{p}", parent, typeNode);
		this.simple = simple;
	}
	
	@Override
	protected String toStringDescription() {
		return simple ? "{sp}" : "{p}";
	}
	
}
