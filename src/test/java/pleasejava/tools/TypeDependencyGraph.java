package pleasejava.tools;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import static com.google.common.collect.FluentIterable.from;

/**
 * Represents immutable oriented graph whose nodes are all types in given testcase
 * and edges represent dependency between types.
 * 
 * @author Tomas Zalusky
 */
public class TypeDependencyGraph {

	/**
	 * All nodes used in graph (including primitive types).
	 */
	private final Set<Type> allTypes;
	
	private final Table<Class<? extends Type>,String,Type> allTypesIndex;
	
	/**
	 * Key = graph node, values = all types which key node depends on.
	 * For example, for record of varchar2s, record is a key, varchar2 is a value.
	 */
	private final ListMultimap<Type,Type> children;
	
	private final List<Type> topologicalOrdering;
	
	private TypeDependencyGraph(Set<Type> allTypes, ListMultimap<Type,Type> children) {
		this.allTypes = ImmutableSet.copyOf(allTypes);
		ImmutableTable.Builder<Class<? extends Type>,String,Type> allTypesIndexBuilder = ImmutableTable.builder();
		for (Type type : allTypes) {
			allTypesIndexBuilder.put(type.getClass(), type.getName(), type);
		}
		this.allTypesIndex = allTypesIndexBuilder.build();
		ImmutableListMultimap<Type,Type> immChildren = ImmutableListMultimap.copyOf(children);
		this.children = immChildren;
		Multimap<Type,Type> predecessors = immChildren.inverse(); // for given key, values express which types is the key used in
		Multimap<Type,Type> exhaust = LinkedHashMultimap.create(predecessors); // copy of predecessors multimap, its elements are removed during build in order to reveal another nodes
		ImmutableList.Builder<Type> topologicalOrderingBuilder = ImmutableList.builder();
		Set<Type> seeds = Sets.difference(allTypes,predecessors.keySet()); // initial set of nodes with no incoming edge
		for (Deque<Type> queue = new ArrayDeque<Type>(seeds); !queue.isEmpty(); ) { // queue of nodes with no incoming edge
			Type top = queue.pollFirst();
			topologicalOrderingBuilder.add(top); // queue invariant: polled node has no incoming edge -> it is safe to push it to output
			for (Type child : from(top.getChildren().values()).toSet()) { // set prevents duplicate offer of child in case of duplicate children
				exhaust.get(child).remove(top); // removing edges to all children
				if (exhaust.get(child).isEmpty()) { // if no edge remains, child becomes top 
					queue.offerLast(child);
				}
			}
		}
		this.topologicalOrdering = topologicalOrderingBuilder.build();
	}
	
	/**
	 * Factory method, creates instance from XML.
	 * @param xml
	 * @return new instance
	 */
	public static TypeDependencyGraph createFrom(InputStream xml) {
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(xml);
			Element rootElement = doc.getRootElement();
			TypeFactory typeFactory = new TypeFactory(rootElement);
			ImmutableSet.Builder<Type> allTypesBuilder = ImmutableSet.builder();
			ImmutableListMultimap.Builder<Type,Type> childrenBuilder = ImmutableListMultimap.builder();
			for (Element typeElement : rootElement.getChildren()) {
				String name = typeElement.getAttributeValue("name");
				Type type = typeFactory.ensureType(name);
				Collection<Type> children = type.getChildren().values();
				allTypesBuilder.add(type).addAll(children);
				childrenBuilder.putAll(type,children);
			}
			ImmutableListMultimap<Type,Type> children = childrenBuilder.build();
			ImmutableSet<Type> allTypes = allTypesBuilder.build();
			return new TypeDependencyGraph(allTypes, children);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}
	
	/**
	 * Sequence of types which satisfies condition on topological ordering.
	 * For any types T1 and T2 where T2 is child of T1,
	 * indexOf(T1) &lt; indexOf(T2) for resulting list.
	 * @return
	 */
	public List<Type> getTopologicalOrdering() {
		return topologicalOrdering;
	}
	
	Type findType(Class<? extends Type> typeClass, String typeName) {
		Type result = allTypesIndex.get(typeClass,typeName);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		Set<Type> written = Sets.<Type>newHashSet();
		for (Set<Type> exhaust = Sets.newLinkedHashSet(getTopologicalOrdering()); !exhaust.isEmpty(); ) {
			Type first = Iterables.getFirst(exhaust,null);
			first.accept(new Type.ToString(result,written),0); // write top-level node and all its children
			for (Deque<Type> q = Lists.newLinkedList(ImmutableSet.of(first)); !q.isEmpty(); ) { // remove top-level node and transitively all its children from set of waiting nodes
				Type t = q.pollFirst();
				exhaust.remove(t);
				q.addAll(children.get(t));
			}
		}
		Type.ToString.align(result,false);
		return result.toString();
	}

}

/*
- TypeDependencyGraph -> TypeGraph
- metoda pro urceni identifikatoru
- zapracovat koncept JDBC-transferrable typu, a la ifc JdbcTransfer { convertForth(); convertBack(); }
- napojit na ukazkove priklady pro TDG, otestovat vystup
- dokumentaci typenode tree
*/
