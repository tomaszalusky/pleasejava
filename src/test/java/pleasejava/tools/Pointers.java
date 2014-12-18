package pleasejava.tools;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;

/**
 * <p>
 * Collection of indexes describing boundaries of collections merged into single collection.
 * This facilitates decomposing records and multilevel collections, which is the key idea of library.
 * Every element and its successor provide enough information 
 * for inferring offset of particular collection and its length.
 * This can be applied recursively onto collections in arbitrarily many nested levels.
 * For sake of uniqueness it's applied onto single-level collection and collection on the top of multilevel hierarchy too
 * (having degenerated content [1,length+1]) without real generating JDBC parameter in this case.  
 * </p>
 * <p>
 * Pointers specify interval in child collection. Let <em>p</em> be the parent collection of pointers,
 * <em>c</em> be the child collection (of pointers or data).
 * For (1-based) <em>i<sup>th</sup></em> index in <em>p</em>, the child collection is specified in following way:
 * </p>
 * <ul>
 * <li>if <em>i = p.count</em> (i.e. last element), the element denotes no child collection
 * and serves only as upper bound for previous element.
 * Thus, the length of real collection represented by collection <em>p</em> is always <em>p.count - 1</em>.
 * <li>negative value denotes null collection (which is different situation than empty collection),
 * however its absolute value still holds information necessary for computing upper bound of previous child collection,
 * see below</li>
 * <li>positive value denotes interval in child collection <em>{@literal <c[p[i]], abs(c[p[i+1]))}</em>.
 * The interval is closed-open, i.e. lower bound is included, upper bound is not included, range <em>{@literal <x,x)}</em>
 * denotes empty set (same semantics as in Guava {@link Range#closedOpen(Comparable, Comparable)}).
 * The <em>abs</em> function sanitizes potential null collection in succeeding element.</li>
 * </ul>
 * <p>
 * For example, collection of collections of varchars [[A,B],null,[C],[],[D,E,F]] is sent to database
 * as single merged {@link PrimitiveCollection} [A,B,C,D,E,F]
 * surrounded with collections of navigation {@link Pointers} [1,-3,3,4,4,7]
 * which itself is surrounded with trivial collection [1,6].
 * </p>
 * @author Tomas Zalusky
 */
public class Pointers extends TransferObject {

	private final List<Integer> data = Lists.newArrayList();

	/**
	 * Indicates usage as trivial navigation collections for single-level collections
	 * or collections on the top of multilevel hierarchy.
	 */
	private final boolean simple;

	/**
	 * If true, the pointer list points into collections of deleted indexes.
	 * If false, the pointer list points into collection of data.
	 * Utilized only for nested tables.
	 */
	private final boolean deletions;
	
	public Pointers(boolean simple, boolean deletions, TransferObject parent, TypeNode typeNode) {
		super(parent, typeNode, typeNode.id() + (deletions ? "q" : "p"));
		this.simple = simple;
		this.deletions = deletions;
	}
	
	@Override
	protected String toStringDescription() {
		return String.format("{%s%s}", simple ? "s" : "", deletions ? "q" : "p");
	}
	
}
