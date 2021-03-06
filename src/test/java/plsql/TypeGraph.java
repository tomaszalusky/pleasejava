package plsql;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import pleasejava.Utils;
import plsql.AbstractType.ToString;

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
 * Represents immutable oriented graph whose nodes represent some subset of all types
 * (including procedure or function signature)
 * and edges represent dependency between types.
 * 
 * @author Tomas Zalusky
 */
public class TypeGraph {

	/**
	 * All nodes used in graph (including primitive types).
	 */
	private final Set<AbstractType> allTypes;
	
	private final Table<Class<? extends AbstractType>,String,AbstractType> allTypesIndex;
	
	/**
	 * Key = graph node, values = all types which key node depends on.
	 * For example, for record of varchar2s, record is a key, varchar2 is a value.
	 */
	private final ListMultimap<AbstractType,AbstractType> children;
	
	private final List<AbstractType> topologicalOrdering;
	
	private TypeGraph(Set<AbstractType> allTypes, ListMultimap<AbstractType,AbstractType> children) {
		this.allTypes = ImmutableSet.copyOf(allTypes);
		ImmutableTable.Builder<Class<? extends AbstractType>,String,AbstractType> allTypesIndexBuilder = ImmutableTable.builder();
		for (AbstractType type : allTypes) {
			allTypesIndexBuilder.put(type.getClass(), type.getName(), type);
		}
		this.allTypesIndex = allTypesIndexBuilder.build();
		ImmutableListMultimap<AbstractType,AbstractType> immChildren = ImmutableListMultimap.copyOf(children);
		this.children = immChildren;
		Multimap<AbstractType,AbstractType> predecessors = immChildren.inverse(); // for given key, values express which types is the key used in
		Multimap<AbstractType,AbstractType> exhaust = LinkedHashMultimap.create(predecessors); // copy of predecessors multimap, its elements are removed during build in order to reveal another nodes
		ImmutableList.Builder<AbstractType> topologicalOrderingBuilder = ImmutableList.builder();
		Set<AbstractType> seeds = Sets.difference(allTypes,predecessors.keySet()); // initial set of nodes with no incoming edge
		for (Deque<AbstractType> queue = new ArrayDeque<AbstractType>(seeds); !queue.isEmpty(); ) { // queue of nodes with no incoming edge
			AbstractType top = queue.pollFirst();
			topologicalOrderingBuilder.add(top); // queue invariant: polled node has no incoming edge -> it is safe to push it to output
			for (AbstractType child : from(top.getChildren().values()).toSet()) { // set prevents duplicate offer of child in case of duplicate children
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
	public static TypeGraph createFrom(InputStream xml) {
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(xml);
			Element rootElement = doc.getRootElement();
			TypeFactory typeFactory = new TypeFactory(rootElement);
			ImmutableSet.Builder<AbstractType> allTypesBuilder = ImmutableSet.builder();
			ImmutableListMultimap.Builder<AbstractType,AbstractType> childrenBuilder = ImmutableListMultimap.builder();
			for (Element typeElement : rootElement.getChildren()) {
				String name = typeElement.getAttributeValue("name");
				AbstractType type = typeFactory.ensureType(name);
				Collection<AbstractType> children = type.getChildren().values();
				allTypesBuilder.add(type).addAll(children);
				childrenBuilder.putAll(type,children);
			}
			ImmutableListMultimap<AbstractType,AbstractType> children = childrenBuilder.build();
			ImmutableSet<AbstractType> allTypes = allTypesBuilder.build();
			return new TypeGraph(allTypes, children);
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
	public List<AbstractType> getTopologicalOrdering() {
		return topologicalOrdering;
	}
	
	/**
	 * Finds type of given class and name.
	 * @param typeClass type class. Note the type is seeked by its exact runtime class,
	 * not by instanceof operator. This is particularly a pitfall in case of procedures and functions,
	 * where no type can be found using {@link AbstractSignature} class, concrete subclass must be used.
	 * @param typeName type name
	 * @return type instance; null if no type found
	 */
	<T extends AbstractType> T findType(Class<T> typeClass, String typeName) {
		AbstractType result = allTypesIndex.get(typeClass,typeName);
		return typeClass.cast(result);
	}

	TypeNodeTree toTypeNodeTree(AbstractSignature rootType) {
		TypeNode rootNode = rootType.toTypeNode(null,0);
		TypeNodeTree result = new TypeNodeTree(rootNode);
		return result;
	}
	
	@Override
	public String toString() {
		Set<AbstractType> written = Sets.<AbstractType>newHashSet();
		AbstractType.ToString visitor = new AbstractType.ToString(written);
		for (Set<AbstractType> exhaust = Sets.newLinkedHashSet(getTopologicalOrdering()); !exhaust.isEmpty(); ) {
			AbstractType first = Iterables.getFirst(exhaust,null);
			first.accept(visitor,0); // write top-level node and all its children
			for (Deque<AbstractType> q = Lists.newLinkedList(ImmutableSet.of(first)); !q.isEmpty(); ) { // remove top-level node and transitively all its children from set of waiting nodes
				AbstractType t = q.pollFirst();
				exhaust.remove(t);
				q.addAll(children.get(t));
			}
		}
		String result = visitor.toString();
		return result;
	}

}

