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

/**
ITO - Intermediate Transfer Object
==================================

Algoritmus tvorby ITO a asociace s PTT:
---------------------------------------
- nechù N je uzel PTT
- je-li N typu JDBCT a je souË·stÌ kolekce  (nemusi byt list)
  - p¯idej ITO {JDBCT}
  - id = id(N)
  - break
- je-li N typu JDBCT a nenÌ souË·stÌ kolekce   (nemusi byt list)
  - p¯idej ITO JDBCT
  - id = id(N)
  - break
- pro N typu kolekce
  - p¯idej ITO {p}
  - rodiËem ITO je ITO {p} nejbliûöÌ vyööÌ kolekce, pokud existuje
  - id = id(N)
- pro N typu nested table
  - p¯idej k potomku ITO {d}
  - rodiËem ITO je ITO {p} u N
  - id = id(N) + "d"
- pro N typu index-by table
  - p¯idej k potomku ITO {i}
  - rodiËem ITO je ITO {p} u N
  - id = id(N) + "i"
*/
