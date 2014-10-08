package pleasejava.tools;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author Tomas Zalusky
 */
public class Pointers extends TransferObject {

	private List<Integer> data = Lists.newArrayList();

	private boolean simple;
	
	public Pointers(TransferObject parent, TypeNode typeNode, boolean simple) {
		super(simple ? "{sp}" : "{p}", parent, typeNode);
	}
	
}
