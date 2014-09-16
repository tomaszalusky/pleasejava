package pleasejava.tools;

import static pleasejava.Utils.appendf;

import java.sql.Array;
import java.sql.Struct;
import java.util.Map;

import pleasejava.Utils;

import com.google.common.collect.ImmutableMap;

/**
 * Represents one node in type tree.
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
 * These types are artificial ({@link AbstractSignature more info}) and only those can occur as a root.
 * Each child represents parameter of PLSQL procedure or function in order of declaration.
 * In the case of PLSQL function, extra child represents return type and occurs as the very first child.
 * </li>
 * <li>node for {@link PrimitiveType} is leaf of tree, which corresponds with intuitive fact that
 * every complex type finally breaks down into primitive types.
 * (Note that type tree represents decomposition of all complex types, even those that are top-level
 * and can be sent via JDBC {@link Array} or {@link Struct}.
 * The ability of being transferred by JDBC is under responsibility of different classes and doesn't influence type tree.)
 * </li>
 * <li>node for {@link NestedTable} has one child which represents nested table element type.
 * </li>
 * <li>node for {@link Varray} has one child which represents varray element type.
 * </li>
 * <li>node for {@link IndexByTable} has one child which represents index-by table element type.
 * Note that key type doesn't have {@link TypeNode} since it would complicate creation of subsequent structures.
 * (For convenience it is still written in indented manner in
 * {@link ToString#visitIndexByTable(IndexByTable, Integer, TypeNode) toString} anyway).
 * </li>
 * <li>node for {@link Record} has at least one child, 
 * each child represents field of PLSQL record in order of declaration.
 * </li>
 * </ul>
 * <p>
 * Every node has multipurpose string identifier, see {@link #computeId(TypeNode, int)}.
 * </p>
 * <p>
 * Note that unlike {@link Type}, {@link TypeNode} represents occurence of type
 * at concrete point in procedure or function signature and cannot be shared.
 * For example, a procedure <code>foo(a1 tableofrecord, a2 tableofrecord)</code>
 * has two children of type {@link NestedTable},
 * where <em>each of them</em> has its own child of type {@link Record}.
 * </p>
 * @author Tomas Zalusky
 */
class TypeNode {

	private final Type type;
	
	/**
	 * Points to type node representing type which depends on this type.
	 * Transitively leads to procedure or function whose signature is described by type tree with this node.
	 * For root of tree, the parent node is {@code null}.
	 */
	private final TypeNode parent;
	
	private /*not final, assigned only once*/ Map<String,TypeNode> children;
	
	private final int depth;
	
	private final String id;

	/**
	 * Constructs new type node for type {@code type} under given {@code parent}
	 * with additional argument denoting 0-based order of this node within parent.
	 * @param type
	 * @param parent
	 * @param orderInParent
	 */
	TypeNode(Type type, TypeNode parent, int orderInParent) {
		this.type = type;
		this.parent = parent;
		this.depth = parent == null ? 0 : parent.depth + 1;
		this.id = computeId(parent,orderInParent);
	}

	/**
	 * Computes identifier of node. The identifier describes position of node in type tree
	 * and is utilized as a base for names of PLSQL variables in generated code.
	 * <p>Algorithm:</p>
	 * <ul>
	 * <li>id of root node is <code>""</code></li>
	 * <li>id of node which represents
	 *   <ul>
	 *     <li>i-th field of record</li>
	 *     <li>or i-th parameter of procedure</li>
	 *     <li>or i-th parameter of function</li>
	 *     <li>or return type of function (in which case i is treated to be 0, otherwise i is 1-based)</li>
	 *   </ul>
	 * is
	 *   <ul>
	 *     <li><code>parent.id() + i</code> for id &lt; 10</li>
	 *     <li><code>parent.id() + "_" + i + "_"</code> for id &ge; 10</li>
	 *   </ul>
	 * (rationale for such a convention:
	 * not to waste underscores when number of children is expected to be small in major cases
	 * and also avoid ambiguity for reconstructing path from id)
	 * </li>
	 * <li>id of node which is element of collection type, is <code>parent.id() + "e"</code></li>
	 * </ul>
	 * <p>For example, the meaning of <code>2e_11_e</code> is: second parameter of procedure is a collection
	 * whose element is record whose 11th field is collection whose element is described type node.
	 * </p>
	 * <p>The aim of identifier design is
	 * </p>
	 * <ul>
	 * <li>to provide clue of node position comprehensible for both human and computer</li>
	 * <li>to make possible unambiguous reconstruction of node position</li>
	 * <li>and to avoid exceeding Oracle identifier name limit of 30 characters in practical cases</li>
	 * </ul>
	 * @param parent
	 * @param orderInParent
	 * @return
	 */
	private static String computeId(TypeNode parent, final int orderInParent) {
		String result = parent == null ? "" : parent.id() + parent.getType().accept(new TypeVisitorR<String>() {
			private String escapize(int value) {return value < 10 ? "" + value : "_" + value + "_";}
			@Override public String visitProcedureSignature(ProcedureSignature type) {return escapize(orderInParent + 1);}
			@Override public String visitFunctionSignature(FunctionSignature type) {return escapize(orderInParent);}
			@Override public String visitRecord(Record type) {return escapize(orderInParent + 1);}
			@Override public String visitVarray(Varray type) {return "e";}
			@Override public String visitNestedTable(NestedTable type) {return "e";}
			@Override public String visitIndexByTable(IndexByTable type) {return "e";}
			@Override public String visitPrimitive(PrimitiveType type) {throw new IllegalStateException("primitive type is not expected to be parent of any type");}
		});
		return result;
	}

	Type getType() {
		return type;
	}

	TypeNode getParent() {
		return parent;
	}
	
	Map<String,TypeNode> getChildren() {
		return children;
	}

	void setChildren(Map<String,TypeNode> children) {
		this.children = ImmutableMap.copyOf(children);
	}
	
	int depth() {
		return depth;
	}
	
	String id() {
		return id;
	}
	
	TransferObjectTree toTransferObjectTree() {
		return null; // TODO
	}
	
	TransferObject toTransferObject() {
		TransferObject result = new TransferObject("/",null);
		type.accept(new AddToTransferObject(),this,result,false);
		return result;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		type.accept(new ToString(result),0,this);
		Utils.align(result);
		return result.toString();
	}

	static class AddToTransferObject implements TypeVisitorAAA<TypeNode,TransferObject,Boolean> {

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
				TransferObject child = new TransferObject(type.getName(),parent);
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
					TransferObject child = new TransferObject("{p}",parent);
					parent.addChild(child);
					TransferObject grandchild = new TransferObject(type.getName(),child);
					child.addChild(grandchild);
				} else {
					TransferObject child = new TransferObject(type.getName(),parent);
					parent.addChild(child);
				}
			} else {
				TransferObject pointers = new TransferObject("{p}",parent);
				parent.addChild(pointers);
				TypeNode childTypeNode = typeNode.getChildren().get(Varray.ELEMENT_LABEL);
				childTypeNode.getType().accept(this,childTypeNode,pointers,true);
			}
		}

		@Override
		public void visitNestedTable(NestedTable type, TypeNode typeNode, TransferObject parent, Boolean inCollection) {
			if (type.isJdbcTransferrable()) {
				if (inCollection) { // TODO deletion
					TransferObject child = new TransferObject("{p}",parent);
					parent.addChild(child);
					TransferObject grandchild = new TransferObject(type.getName(),child);
					child.addChild(grandchild);
				} else {
					TransferObject child = new TransferObject(type.getName(),parent);
					parent.addChild(child);
				}
			} else {
				TransferObject pointers = new TransferObject("{p}",parent);
				parent.addChild(pointers);
				TransferObject deletions = new TransferObject("{d}",parent);
				pointers.addChild(deletions);
				TypeNode childTypeNode = typeNode.getChildren().get(NestedTable.ELEMENT_LABEL);
				childTypeNode.getType().accept(this,childTypeNode,pointers,true);
			}
		}

		@Override
		public void visitIndexByTable(IndexByTable type, TypeNode typeNode, TransferObject parent, Boolean inCollection) {
			if (type.isJdbcTransferrable()) {
				if (inCollection) { // TODO indexes
					TransferObject child = new TransferObject("{p}",parent);
					parent.addChild(child);
					TransferObject grandchild = new TransferObject(type.getName(),child);
					child.addChild(grandchild);
				} else {
					TransferObject child = new TransferObject(type.getName(),parent);
					parent.addChild(child);
				}
			} else {
				TransferObject pointers = new TransferObject("{p}",parent);
				parent.addChild(pointers);
				TransferObject indexes = new TransferObject("{i:" + type.getIndexType().name + "}",parent);
				pointers.addChild(indexes);
				TypeNode childTypeNode = typeNode.getChildren().get(IndexByTable.ELEMENT_LABEL);
				childTypeNode.getType().accept(this,childTypeNode,pointers,true);
			}
		}

		@Override
		public void visitPrimitive(PrimitiveType type, TypeNode typeNode, TransferObject parent, Boolean inCollection) {
			TransferObject child;
			if (inCollection) {
				child = new TransferObject("{" + type.getName() + "}",parent);
			} else {
				child = new TransferObject(type.getName(),parent);
			}
			parent.addChild(child);
		}
		
	}
	
	/**
	 * Flexible support for toString method.
	 * @author Tomas Zalusky
	 */
	static class ToString extends Utils.ToStringSupport implements TypeVisitorAA<Integer,TypeNode> {

		/**
		 * @param buf buffer for result string
		 */
		ToString(StringBuilder buf) {
			super(buf);
		}

		@Override
		public void visitProcedureSignature(ProcedureSignature type, Integer level, TypeNode typeNode) {
			appendf(buf,"procedure \"%s\" #%s", type.getName(), typeNode.id());
			for (Map.Entry<String,Parameter> entry : type.getParameters().entrySet()) {
				String key = entry.getKey();
				appendf(buf,"%n%s%s %s ", indent(level + 1), key, entry.getValue().getParameterMode().name().toLowerCase());
				entry.getValue().getType().accept(this,level + 1,typeNode.getChildren().get(key));
			}
		}

		@Override
		public void visitFunctionSignature(FunctionSignature type, Integer level, TypeNode typeNode) {
			appendf(buf,"function \"%s\" #%s", type.getName(), typeNode.id());
			appendf(buf,"%n%s%s ", indent(level + 1), FunctionSignature.RETURN_LABEL);
			type.getReturnType().accept(this,level + 1,typeNode.getChildren().get(FunctionSignature.RETURN_LABEL));
			for (Map.Entry<String,Parameter> entry : type.getParameters().entrySet()) {
				String key = entry.getKey();
				appendf(buf,"%n%s%s %s ", indent(level + 1), key, entry.getValue().getParameterMode().name().toLowerCase());
				entry.getValue().getType().accept(this,level + 1,typeNode.getChildren().get(key));
			}
		}

		@Override
		public void visitRecord(Record type, Integer level, TypeNode typeNode) {
			appendf(buf,"record \"%s\" #%s", type.getName(), typeNode.id());
			for (Map.Entry<String,Type> entry : type.getFields().entrySet()) {
				String key = entry.getKey();
				appendf(buf,"%n%s%s ", indent(level + 1), key);
				entry.getValue().accept(this,level + 1,typeNode.getChildren().get(key));
			}
		}

		@Override
		public void visitVarray(Varray type, Integer level, TypeNode typeNode) {
			appendf(buf,"varray \"%s\" #%s", type.getName(), typeNode.id());
			appendf(buf,"%n%s%s ", indent(level + 1), Varray.ELEMENT_LABEL);
			type.getElementType().accept(this,level + 1,typeNode.getChildren().get(Varray.ELEMENT_LABEL));
		}

		@Override
		public void visitNestedTable(NestedTable type, Integer level, TypeNode typeNode) {
			appendf(buf,"nestedtable \"%s\" #%s", type.getName(), typeNode.id());
			appendf(buf,"%n%s%s ", indent(level + 1), NestedTable.ELEMENT_LABEL);
			type.getElementType().accept(this,level + 1,typeNode.getChildren().get(NestedTable.ELEMENT_LABEL));
		}

		@Override
		public void visitIndexByTable(IndexByTable type, Integer level, TypeNode typeNode) {
			appendf(buf,"indexbytable \"%s\" #%s", type.getName(), typeNode.id());
			appendf(buf,"%n%s%s %s", indent(level + 1), IndexByTable.KEY_LABEL, type.getIndexType().toString());
			appendf(buf,"%n%s%s ", indent(level + 1), IndexByTable.ELEMENT_LABEL);
			type.getElementType().accept(this,level + 1,typeNode.getChildren().get(IndexByTable.ELEMENT_LABEL));
		}

		@Override
		public void visitPrimitive(PrimitiveType type, Integer level, TypeNode typeNode) {
			appendf(buf,"\"%s\" #%s", type.getName(), typeNode.id());
		}
		
	}

}
