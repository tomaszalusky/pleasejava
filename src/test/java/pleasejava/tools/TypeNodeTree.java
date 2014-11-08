package pleasejava.tools;

import java.util.Map;

import com.google.common.collect.ImmutableListMultimap;

/**
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
		ImmutableListMultimap.Builder<TypeNode,TransferObject> associationsBuilder = ImmutableListMultimap.builder();
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
	 * Ensures adding appropriate transfer object to type node.
	 * First generic argument is type node of visited {@link Type}. The visit... methods generate transfer objects for this type node.
	 * Second generic argument is parent in transfer object tree under which transfer objects are added.
	 * Third argument is a flag denoting descend into collection structure, which results into generating vector rather than scalar structures.
	 * @author Tomas Zalusky
	 */
	static class AddToTransferObject implements TypeVisitorAAA<TypeNode,TransferObject,Boolean> {

		private final ImmutableListMultimap.Builder<TypeNode,TransferObject> associationsBuilder;
		
		public AddToTransferObject(ImmutableListMultimap.Builder<TypeNode,TransferObject> associationsBuilder) {
			this.associationsBuilder = associationsBuilder;
		}

		/**
		 * Since procedure and function (just like root transfer object) are artificial constructs,
		 * their type nodes simply delegates addition to parent transfer object to its children. 
		 * @see pleasejava.tools.TypeVisitorAAA#visitProcedureSignature(pleasejava.tools.ProcedureSignature, java.lang.Object, java.lang.Object, java.lang.Object)
		 */
		@Override
		public void visitProcedureSignature(ProcedureSignature type, TypeNode typeNode, TransferObject parent, Boolean inCollection) {
			for (Map.Entry<String,Parameter> entry : type.getParameters().entrySet()) {
				TypeNode childTypeNode = typeNode.getChildren().get(entry.getKey());
				childTypeNode.getType().accept(this,childTypeNode,parent,inCollection);
			}
		}

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
		 * If JDBC-transferrable type is part of collection
		 * (which is not JDBC-transferrable - otherwise it would be processed in its visitor method)
		 * they must be decomposed as if they weren't transferrable
		 * because there are no means how to send such a sequence to db.
		 * @see pleasejava.tools.TypeVisitorAAA#visitRecord(pleasejava.tools.Record, java.lang.Object, java.lang.Object, java.lang.Object)
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
		 * For JDBC-transferrable (toplevel) collection which is element of another collection,
		 * toplevel collection collects elements across all particular collections.
		 * Hence, there is no need for further decomposition,
		 * we need only one more pointer collection to distinguish particular collections.
		 * 
		 * For JDBC-transferrable collection which is not element of another collection,
		 * (the case when toplevel collection is directly used as procedure parameter or at most as field in record (possibly in other records)),
		 * the collection it self is transfer object and can be used as is.
		 * 
		 * Non-JDBC-transferrable collections must be decomposed,
		 * e.g. only pointer collection is currently known to be needed,
		 * children of transfer object will be constructed later. 
		 * 
		 * @see pleasejava.tools.TypeVisitorAAA#visitVarray(pleasejava.tools.Varray, java.lang.Object, java.lang.Object, java.lang.Object)
		 */
		@Override
		public void visitVarray(Varray type, TypeNode typeNode, TransferObject parent, Boolean inCollection) {
			if (type.isJdbcTransferrable()) {
				if (inCollection) {
					TransferObject child = new Pointers(false,parent,typeNode);
					associationsBuilder.put(typeNode, child);
					parent.addChild(child);
					TypeNode childTypeNode = typeNode.getChildren().get(Varray.ELEMENT_LABEL);
					TransferObject grandchild = new JdbcTransferrableCollection(type, child, childTypeNode);
					associationsBuilder.put(childTypeNode, grandchild);
					child.addChild(grandchild);
				} else {
					TransferObject child = new JdbcTransferrableCollection(type, parent, typeNode);
					associationsBuilder.put(typeNode, child);
					parent.addChild(child);
				}
			} else {
				TransferObject pointers = new Pointers(!inCollection,parent,typeNode);
				associationsBuilder.put(typeNode, pointers);
				parent.addChild(pointers);
				TypeNode childTypeNode = typeNode.getChildren().get(Varray.ELEMENT_LABEL);
				childTypeNode.getType().accept(this,childTypeNode,pointers,true);
			}
		}

		@Override
		public void visitNestedTable(NestedTable type, TypeNode typeNode, TransferObject parent, Boolean inCollection) {
			if (type.isJdbcTransferrable()) {
				if (inCollection) { // TODO deletion
					TransferObject child = new Pointers(false,parent,typeNode);
					associationsBuilder.put(typeNode, child);
					parent.addChild(child);
					TypeNode childTypeNode = typeNode.getChildren().get(NestedTable.ELEMENT_LABEL);
					TransferObject grandchild = new JdbcTransferrableCollection(type, child, childTypeNode);
					associationsBuilder.put(childTypeNode, grandchild);
					child.addChild(grandchild);
				} else {
					TransferObject child = new JdbcTransferrableCollection(type, parent, typeNode);
					associationsBuilder.put(typeNode, child);
					parent.addChild(child);
				}
			} else {
				TransferObject pointers = new Pointers(!inCollection,parent,typeNode);
				associationsBuilder.put(typeNode, pointers);
				parent.addChild(pointers);
				TransferObject deletions = new Deletions(pointers,typeNode);
				associationsBuilder.put(typeNode, deletions);
				pointers.addChild(deletions);
				TypeNode childTypeNode = typeNode.getChildren().get(NestedTable.ELEMENT_LABEL);
				childTypeNode.getType().accept(this,childTypeNode,pointers,true);
			}
		}

		@Override
		public void visitIndexByTable(IndexByTable type, TypeNode typeNode, TransferObject parent, Boolean inCollection) {
			if (type.isJdbcTransferrable()) {
				if (inCollection) { // TODO indexes
					TransferObject child = new Pointers(false,parent,typeNode);
					associationsBuilder.put(typeNode, child);
					parent.addChild(child);
					TypeNode childTypeNode = typeNode.getChildren().get(IndexByTable.ELEMENT_LABEL);
					TransferObject grandchild = new JdbcTransferrableCollection(type, child, childTypeNode);
					associationsBuilder.put(childTypeNode, grandchild);
					child.addChild(grandchild);
				} else {
					TransferObject child = new JdbcTransferrableCollection(type, parent, typeNode);
					associationsBuilder.put(typeNode, child);
					parent.addChild(child);
				}
			} else {
				TransferObject pointers = new Pointers(!inCollection,parent,typeNode);
				associationsBuilder.put(typeNode, pointers);
				parent.addChild(pointers);
				TransferObject indexes = new Indexes(type.getIndexType(), pointers, typeNode);
				associationsBuilder.put(typeNode, indexes);
				pointers.addChild(indexes);
				TypeNode childTypeNode = typeNode.getChildren().get(IndexByTable.ELEMENT_LABEL);
				childTypeNode.getType().accept(this,childTypeNode,pointers,true);
			}
		}

		@Override
		public void visitPrimitive(PrimitiveType type, TypeNode typeNode, TransferObject parent, Boolean inCollection) {
			TransferObject child;
			if (inCollection) {
				child = new PrimitiveCollection(type, parent, typeNode);
			} else {
				child = new PrimitiveScalar(type,parent,typeNode);
			}
			associationsBuilder.put(typeNode, child);
			parent.addChild(child);
		}
		
	}

}
