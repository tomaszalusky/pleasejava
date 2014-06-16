package pleasejava.tools;

import java.util.List;

/**
 * Represents one node in parameter type tree.
 * Unlike {@link Type}, {@link TypeNode} represents occurence of type
 * at concrete point in signature of procedure or function.
 * @author Tomas Zalusky
 */
public class TypeNode <T extends Type> {

	private T type;
	
	/**
	 * Points to type node representing type which depends on this type.
	 * Transitively leads to procedure or function whose signature is described by type tree with this node.
	 * The root of tree represents signature of procedure or function (holds condition
	 * <code>parent == null && (type instanceof {@link ProcedureSignature} || type instanceof {@link FunctionSignature})</code>).
	 */
	private TypeNode<?> parent;
	
	private List<TypeNode<?>> child;
	
}
