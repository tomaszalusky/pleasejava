package plsql;

import java.sql.Array;
import java.sql.Struct;
import java.util.Map;

import plsql.TypeNode.ToString;

import com.google.common.collect.ImmutableSetMultimap;

/**
 * Represents hierarchy of type nodes for concrete procedure or function.
 * <p>
 * Type tree is a tree which describes parameters of PLSQL procedure or function and their dependencies on another types.
 * For example, given <code>tableofnum</code> is declared as <code>table of number</code>,
 * <code>procedure foo(a1 tableofnum, a2 varchar2(20))</code> is described by tree:
 * </p>
 * <pre>
 * foo
 * |
 * |------- tableofnum
 * |        |
 * |         ---------- number
 * |
 *  ------- varchar2(20)
 * </pre> 
 * <p>
 * Following rules hold for type tree:
 * </p>
 * <ul>
 * <li>node for {@link ProcedureSignature} or {@link FunctionSignature} type is root of tree.
 * These types are artificial ({@link AbstractSignature more info}) and only node of one of those types can occur as a root.
 * Each child represents parameter of PLSQL procedure or function in order of declaration.
 * In the case of PLSQL function, extra child represents return type and occurs as the very first child.
 * </li>
 * <li>node for {@link PrimitiveType} is leaf of tree, which corresponds with intuitive fact that
 * every complex type finally breaks down into primitive types.
 * (Note that type tree represents decomposition of all complex types, even those that are top-level
 * and can be sent via JDBC {@link Array} or {@link Struct}.
 * The ability of being transferred by JDBC is under responsibility of another classes and doesn't influence type tree.)
 * </li>
 * <li>node for {@link NestedTable} has one child which represents nested table element type.
 * </li>
 * <li>node for {@link Varray} has one child which represents varray element type.
 * </li>
 * <li>node for {@link IndexByTable} has one child which represents index-by table element type.
 * Note that key type doesn't have {@link TypeNode} since it would complicate creation of subsequent structures.
 * (For convenience it is still written in indented manner in
 * {@link ToString#visitIndexByTable(IndexByTable, Integer, TypeNode) toString} anyway.)
 * </li>
 * <li>node for {@link Record} has at least one child, 
 * each child represents field of PLSQL record in order of declaration.
 * </li>
 * </ul>
 * <p>
 * Note that unlike {@link Type}, {@link TypeNode} represents occurence of type
 * at concrete point in procedure or function signature and cannot be shared.
 * For example, a procedure <code>foo(a1 tableofrecord, a2 tableofrecord)</code>
 * has two children of type {@link NestedTable},
 * where <em>each of them</em> has its <em>own</em> child of type {@link Record}.
 * </p>
 * @author Tomas Zalusky
 */
public class TypeNodeTree {

	private final TypeNode rootNode;

	public TypeNodeTree(TypeNode rootNode) {
		if (!(rootNode.getType() instanceof AbstractSignature)) {
			throw new IllegalStateException("Type node tree may have only Procedure or Function root type.");
		}
		this.rootNode = rootNode;
	}

	TransferObjectTree toTransferObjectTree() {
		ImmutableSetMultimap.Builder<TypeNode,TransferObject> associationsBuilder = ImmutableSetMultimap.builder();
		TransferObject root = new RootTransferObject(rootNode);
		associationsBuilder.put(rootNode,root);
		rootNode.getType().accept(new AddToTransferObject(associationsBuilder),rootNode,root,false);
		TransferObjectTree result = new TransferObjectTree(rootNode, root, associationsBuilder.build());
		return result;
	}
	
	@Override
	public String toString() {
		return rootNode.toString();
	}
	
	public String toString(TransferObjectTree tot) {
		return rootNode.toString(tot);
	}
	
	/**
	 * <p>
	 * Ensures adding appropriate transfer object to type node.
	 * First generic argument is type node of visited {@link Type}.
	 * The visit... methods generate transfer objects for this type node,
	 * see concrete algorithms in javadoc of particular methods.
	 * Second generic argument is parent in transfer object tree under which transfer objects are added.
	 * Third argument is a flag denoting descend into collection structure,
	 * which results into generating vector rather than scalar structures.
	 * </p>
	 * @author Tomas Zalusky
	 */
	static class AddToTransferObject implements TypeVisitorAAA<TypeNode,TransferObject,Boolean> {

		private final ImmutableSetMultimap.Builder<TypeNode,TransferObject> associationsBuilder;
		
		public AddToTransferObject(ImmutableSetMultimap.Builder<TypeNode,TransferObject> associationsBuilder) {
			this.associationsBuilder = associationsBuilder;
		}

		/**
		 * Since procedure and function (just like root transfer object) are artificial constructs,
		 * their type nodes simply delegates addition to parent transfer object to its children. 
		 * @see plsql.TypeVisitorAAA#visitProcedureSignature(plsql.ProcedureSignature, java.lang.Object, java.lang.Object, java.lang.Object)
		 */
		@Override
		public void visitProcedureSignature(ProcedureSignature type, TypeNode typeNode, TransferObject parent, Boolean inCollection) {
			for (Map.Entry<String,Parameter> entry : type.getParameters().entrySet()) {
				TypeNode childTypeNode = typeNode.getChildren().get(entry.getKey());
				childTypeNode.getType().accept(this,childTypeNode,parent,inCollection);
			}
		}

		/**
		 * Same as {@link #visitProcedureSignature(ProcedureSignature, TypeNode, TransferObject, Boolean)}.
		 * @see plsql.TypeVisitorAAA#visitFunctionSignature(plsql.FunctionSignature, java.lang.Object, java.lang.Object, java.lang.Object)
		 */
		@Override
		public void visitFunctionSignature(FunctionSignature type, TypeNode typeNode, TransferObject parent, Boolean inCollection) {
			TypeNode returnTypeNode = typeNode.getChildren().get(FunctionSignature.RETURN_LABEL);
			type.getReturnType().accept(this,returnTypeNode,parent,inCollection);
			for (Map.Entry<String,Parameter> entry : type.getParameters().entrySet()) {
				TypeNode childTypeNode = typeNode.getChildren().get(entry.getKey());
				childTypeNode.getType().accept(this,childTypeNode,parent,inCollection);
			}
		}

		/**
		 * If JDBC-transferrable record type is part of collection
		 * (which is not JDBC-transferrable - otherwise it would be processed in its visitor method)
		 * then it must be decomposed as if it wasn't transferrable
		 * because there are no means how to send such a sequence to db.
		 * @see plsql.TypeVisitorAAA#visitRecord(plsql.Record, java.lang.Object, java.lang.Object, java.lang.Object)
		 */
		@Override
		public void visitRecord(Record type, TypeNode typeNode, TransferObject parent, Boolean inCollection) {
			if (type.isJdbcTransferrable() && inCollection || !type.isJdbcTransferrable()) {
				for (Map.Entry<String,Type> entry : type.getFields().entrySet()) {
					TypeNode childTypeNode = typeNode.getChildren().get(entry.getKey());
					childTypeNode.getType().accept(this,childTypeNode,parent,inCollection);
				}
			} else {
				TransferObject child = new JdbcTransferrableRecord(type,parent,typeNode);
				associationsBuilder.put(typeNode,child);
				parent.addChild(child);
			}
		}

		/**
		 * <p>
		 * For JDBC-transferrable (toplevel) collection which is element of another collection,
		 * toplevel collection collects elements across all particular collections.
		 * Neither collection nor its elements are further decomposed.
		 * Hence, {@link Pointers} collection (which distinguishes particular collections) is just added to parent,
		 * {@link JdbcTransferrableCollection} (which represents data of particular collections) is added to pointers.
		 * </p>
		 * <p>
		 * For JDBC-transferrable collection which is not element of another collection,
		 * the collection itself is transfer object and can be used as is.
		 * This is the case when toplevel collection is directly used as procedure parameter
		 * or at most as field in record (possibly in other records).
		 * Hence, {@link JdbcTransferrableCollection} suffices to be added to parent.
		 * </p>
		 * <p>
		 * Non-JDBC-transferrable collections must be decomposed,
		 * e.g. only pointer collection is currently known to be needed,
		 * children of transfer object will be constructed later.
		 * Hence, {@link Pointers} collection is just added to parent
		 * (the parent represents transfer object for the nearest outer collection
		 * or {@link RootTransferObject} if such collection doesn't exist). 
		 * </p>
		 * @see plsql.TypeVisitorAAA#visitVarray(plsql.Varray, java.lang.Object, java.lang.Object, java.lang.Object)
		 */
		@Override
		public void visitVarray(Varray type, TypeNode typeNode, TransferObject parent, Boolean inCollection) {
			if (type.isJdbcTransferrable()) {
				if (inCollection) {
					TransferObject pointers = new DataPointers(false,parent,typeNode);
					associationsBuilder.put(typeNode, pointers);
					parent.addChild(pointers);
					TransferObject child = new JdbcTransferrableCollection(type, pointers, typeNode); // not typeNode.getChildren().get(Varray.ELEMENT_LABEL); - see below for reason
					associationsBuilder.put(typeNode, child);
					pointers.addChild(child);
				} else {
					TransferObject pointers = new DataPointers(true,parent,typeNode);
					associationsBuilder.put(typeNode, pointers);
					parent.addChild(pointers);
					TransferObject child = new JdbcTransferrableCollection(type, pointers, typeNode);
					associationsBuilder.put(typeNode, child);
					pointers.addChild(child);
				}
			} else {
				TransferObject pointers = new DataPointers(!inCollection,parent,typeNode);
				associationsBuilder.put(typeNode, pointers);
				parent.addChild(pointers);
				TypeNode childTypeNode = typeNode.getChildren().get(Varray.ELEMENT_LABEL);
				childTypeNode.getType().accept(this,childTypeNode,pointers,true);
			}
		}

		/**
		 * Same as {@link #visitVarray(Varray, TypeNode, TransferObject, Boolean)},
		 * enriched with deletion information.
		 * Every collection is represented by two pointer collections:
		 * first points into {@link Deletions} transfer object (q-pointers in toString),
		 * second points into data (p-pointers in toString).
		 * The indices of these two collections are independent and
		 * together represent the content of whole collection.
		 * For example, let <code>q</code> be <code>[1,4]</code>
		 * and <code>p</code> be <code>[b,c,e]</code>.
		 * Then <code>q</code> and <code>p</code> represent collection <code>[a,b,c,d,e]</code>
		 * after performing <code>delete(1)</code> and <code>delete(4)</code>.
		 * @see plsql.TypeVisitorAAA#visitNestedTable(plsql.NestedTable, java.lang.Object, java.lang.Object, java.lang.Object)
		 */
		@Override
		public void visitNestedTable(NestedTable type, TypeNode typeNode, TransferObject parent, Boolean inCollection) {
			if (type.isJdbcTransferrable()) {
				if (inCollection) {
					TransferObject pointersToDeletions = new DeletionsPointers(false,parent,typeNode);
					associationsBuilder.put(typeNode, pointersToDeletions);
					parent.addChild(pointersToDeletions);
					TransferObject deletions = new Deletions(pointersToDeletions,typeNode);
					associationsBuilder.put(typeNode, deletions);
					pointersToDeletions.addChild(deletions);
					TransferObject pointers = new DataPointers(false,parent,typeNode);
					associationsBuilder.put(typeNode, pointers);
					parent.addChild(pointers);
					TransferObject child = new JdbcTransferrableCollection(type, pointers, typeNode); // not typeNode.getChildren().get(NestedTable.ELEMENT_LABEL); because JTC TO must be associated with TN representing nested table, not its element
					associationsBuilder.put(typeNode, child);
					pointers.addChild(child);
				} else { // TODO not sure which of pointers,child should be associated with typenode, will be clarified when developing data transfer algorithm (if shows as unnecessary, need to correct also ID generation)
					TransferObject pointersToDeletions = new DeletionsPointers(true,parent,typeNode);
					associationsBuilder.put(typeNode, pointersToDeletions);
					parent.addChild(pointersToDeletions);
					TransferObject deletions = new Deletions(pointersToDeletions,typeNode);
					associationsBuilder.put(typeNode, deletions);
					pointersToDeletions.addChild(deletions);
					TransferObject pointers = new DataPointers(true,parent,typeNode);
					associationsBuilder.put(typeNode, pointers);
					parent.addChild(pointers);
					TransferObject child = new JdbcTransferrableCollection(type, pointers, typeNode);
					associationsBuilder.put(typeNode, child);
					pointers.addChild(child);
				}
			} else {
				TransferObject pointersToDeletions = new DeletionsPointers(!inCollection,parent,typeNode);
				associationsBuilder.put(typeNode, pointersToDeletions);
				parent.addChild(pointersToDeletions);
				TransferObject deletions = new Deletions(pointersToDeletions,typeNode);
				associationsBuilder.put(typeNode, deletions);
				pointersToDeletions.addChild(deletions);
				TransferObject pointers = new DataPointers(!inCollection,parent,typeNode);
				associationsBuilder.put(typeNode, pointers);
				parent.addChild(pointers);
				TypeNode childTypeNode = typeNode.getChildren().get(NestedTable.ELEMENT_LABEL);
				childTypeNode.getType().accept(this,childTypeNode,pointers,true);
			}
		}

		/**
		 * Same as {@link #visitVarray(Varray, TypeNode, TransferObject, Boolean)},
		 * except following exceptions:
		 * <ul>
		 * <li>only non-JDBC-transferrable types decomposition is considered (since index-by tables are never JDBC-transferrable)</li>
		 * <li>type node tree is enriched with index information and {@link Indexes} transfer object is added as child of {@link Pointers}.
		 * Pointers point not only into data collection(s) but also into collection of index values.</li>
		 * </ul>
		 * @see plsql.TypeVisitorAAA#visitNestedTable(plsql.NestedTable, java.lang.Object, java.lang.Object, java.lang.Object)
		 */
		@Override
		public void visitIndexByTable(IndexByTable type, TypeNode typeNode, TransferObject parent, Boolean inCollection) {
			TransferObject pointers = new DataPointers(!inCollection,parent,typeNode);
			associationsBuilder.put(typeNode, pointers);
			parent.addChild(pointers);
			TransferObject indexes = new Indexes(type.getIndexType(), pointers, typeNode);
			associationsBuilder.put(typeNode, indexes);
			pointers.addChild(indexes);
			TypeNode childTypeNode = typeNode.getChildren().get(IndexByTable.ELEMENT_LABEL);
			childTypeNode.getType().accept(this,childTypeNode,pointers,true);
		}

		/**
		 * <p>
		 * For primitive type which is element of collection,
		 * the {@link PrimitiveCollection} must be emitted.
		 * </p>
		 * <p>
		 * For primitive type which is not element of collection
		 * (i.e. for example simple procedure parameters),
		 * the {@link PrimitiveScalar} suffices.
		 * </p>
		 * @see plsql.TypeVisitorAAA#visitPrimitive(plsql.PrimitiveType, java.lang.Object, java.lang.Object, java.lang.Object)
		 */
		@Override
		public void visitPrimitive(PrimitiveType type, TypeNode typeNode, TransferObject parent, Boolean inCollection) {
			TransferObject child;
			if (inCollection) {
				child = new PrimitiveCollection(type, parent, typeNode);
			} else {
				child = new PrimitiveScalar(type, parent, typeNode);
			}
			associationsBuilder.put(typeNode, child);
			parent.addChild(child);
		}
		
	}

}
