package pleasejava.tools;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author Tomas Zalusky
 */
public class Deletions extends TransferObject {

	private List<Boolean> data = Lists.newArrayList();

	public Deletions(TransferObject parent, TypeNode typeNode) {
		super("{d}", parent, typeNode);
	}
	
	@Override
	protected String toStringDescription() {
		return "{d}";
	}

}
