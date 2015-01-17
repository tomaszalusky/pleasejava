package plsql;

import static com.google.common.collect.FluentIterable.from;

import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;

/**
 * Tree of transfer objects.
 * Core data structure for all data transferred between PLSQL and Java
 * in single procedure (or function) call.
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
	private final SetMultimap<TypeNode,TransferObject> associations;
	
	public TransferObjectTree(TypeNode typeTreeRoot, TransferObject root, SetMultimap<TypeNode,TransferObject> associations) {
		this.typeTreeRoot = typeTreeRoot;
		this.root = root;
		this.associations = associations;
	}

	/**
	 * Returns transfer object associated with given type node.
	 * Only at most one element of given class is supposed to be associated with one type node.
	 * @param typeNode
	 * @param transferObjectClass
	 * @return transfer object; null, if no transfer object of required class is associated with given type node
	 */
	public <T extends TransferObject> T getTransferObject(TypeNode typeNode, Class<T> transferObjectClass) {
		Set<TransferObject> all = associations.get(typeNode);
		T result = Iterables.getOnlyElement(from(all).filter(transferObjectClass),null);
		return result;
	}
	
	/**
	 * Returns true if there exists transfer object for given type node.
	 * @param typeNode
	 * @return
	 */
	public boolean hasTransferObject(TypeNode typeNode) {
		boolean result = associations.containsKey(typeNode);
		return result;
	}
	
	@Override
	public String toString() {
		return root.toString();
	}

}
