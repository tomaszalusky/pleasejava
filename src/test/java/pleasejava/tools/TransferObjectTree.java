package pleasejava.tools;

import com.google.common.collect.ListMultimap;

/**
 * @author Tomas Zalusky
 */
public class TransferObjectTree {

	private final TypeNode typeTreeRoot;
	
	private final TransferObject root;
	
	private final ListMultimap<TypeNode,TransferObject> associations;
	
	public TransferObjectTree(TypeNode typeTreeRoot, TransferObject root, ListMultimap<TypeNode,TransferObject> associations) {
		this.typeTreeRoot = typeTreeRoot;
		this.root = root;
		this.associations = associations;
	}

	@Override
	public String toString() {
		return root.toString();
	}

}
