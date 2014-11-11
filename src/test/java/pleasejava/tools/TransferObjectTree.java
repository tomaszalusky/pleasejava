package pleasejava.tools;

import com.google.common.collect.ListMultimap;

/**
 * @author Tomas Zalusky
 */
public class TransferObjectTree {

	private final TypeNode typeTreeRoot;
	
	private final TransferObject root;
	
	/**
	 * Represents assignment of {@link TransferObject}s to {@link TypeNode}.
	 * It cannot be stored in {@link TypeNode} since
	 * {@link TypeNode} is intended to be universal for many calls.
	 * Inverse association is stored in {@link TransferObject}.
	 */
	private final ListMultimap<TypeNode,TransferObject> associations;
	
	public TransferObjectTree(TypeNode typeTreeRoot, TransferObject root, ListMultimap<TypeNode,TransferObject> associations) {
		this.typeTreeRoot = typeTreeRoot;
		this.root = root;
		this.associations = associations;
	}

	public ListMultimap<TypeNode,TransferObject> getAssociations() {
		return associations;
	}
	
	@Override
	public String toString() {
		return root.toString();
	}

}
