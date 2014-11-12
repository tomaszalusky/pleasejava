package pleasejava.tools;

import java.util.List;

import pleasejava.Utils;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * <p>
 * Represents one value which can be transferred via JDBC.
 * It essentially corresponds with one "?" (questionmark) in JDBC notation of SQL call.
 * </p>
 * <p>
 * Concrete subclasses describe particular parts of structure sent between PLSQL and Java.
 * </p>
 * <p>
 * Every transfer object is associated with a {@link TypeNode}.
 * Inverse association is also stored and maintained by {@link TypeNodeTree}.
 * Transfer objects form a tree roughly corresponding {@link TypeNodeTree}, see {@link TransferObjectTree} for more detail.
 * </p>
 * <p>
 * Transfer object have own identifier.
 * It is derived from its {@link TypeNode} identifier
 * and used for creating PLSQL identifiers in generated code in consistent, systematic and readable manner.
 * </p>
 * @author Tomas Zalusky
 */
public abstract class TransferObject {

	private final TransferObject parent;
	
	private final List<TransferObject> children = Lists.newArrayList(); // mutable
	
	private final int depth;

	private final TypeNode typeNode;

	private final String id;
	
	protected TransferObject(TransferObject parent, TypeNode typeNode, String id) {
		this.parent = parent;
		this.typeNode = typeNode;
		this.depth = parent == null ? 0 : parent.depth + 1;
		this.id = id;
	}

	public int getDepth() {
		return depth;
	}
	
	public void addChild(TransferObject child) {
		children.add(child);
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		toStringBuilder(buf);
		return buf.toString();
	}

	private void toStringBuilder(StringBuilder buf) {
		Utils.appendf(buf,"%s%s%n",Strings.repeat(" ",4*depth),toStringDescription());
		for (TransferObject child : children) {
			child.toStringBuilder(buf);
		}
	}
	
	protected abstract String toStringDescription();
	
	public String getId() {
		return id;
	}

}
