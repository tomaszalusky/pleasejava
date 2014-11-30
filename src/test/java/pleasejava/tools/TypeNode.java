package pleasejava.tools;

import static com.google.common.collect.Iterables.getOnlyElement;

import java.sql.Array;
import java.sql.Struct;
import java.util.List;
import java.util.Map;

import pleasejava.Utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Represents one node in type tree.
 * <p>
 * Every node has multipurpose string identifier, see {@link #computeId(TypeNode, int)}.
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
	
	@Override
	public String toString() {
		ToString visitor = new ToString(null);
		type.accept(visitor,0,this);
		String result = visitor.toString();
		return result;
	}

	public String toString(TransferObjectTree transferObjectTree) {
		ToString visitor = new ToString(transferObjectTree);
		type.accept(visitor,0,this);
		String result = visitor.toString();
		return result;
	}

	/**
	 * Flexible support for toString method.
	 * @author Tomas Zalusky
	 */
	static class ToString extends Utils.ToStringSupport implements TypeVisitorAA<Integer,TypeNode> {

		private final TransferObjectTree transferObjectTree;

		/**
		 * @param transferObjectTree concrete case of transfer objects
		 * which are associated with this type tree and will be dumped together;
		 * null, if only type tree should be dumped
		 */
		ToString(TransferObjectTree transferObjectTree) {
			this.transferObjectTree = transferObjectTree;
		}

		@Override
		public void visitProcedureSignature(ProcedureSignature type, Integer level, TypeNode typeNode) {
			appendToLastCell("procedure").append("\"" + type.getName() + "\"").append("#" + typeNode.id());
			if (transferObjectTree != null) {
				TransferObject to = getOnlyElement(transferObjectTree.getAssociations().get(typeNode));
				append("| " + indent(to.getDepth()) + to.toStringDescription()).append("#" + to.getId());
			}
			for (Map.Entry<String,Parameter> entry : type.getParameters().entrySet()) {
				String key = entry.getKey();
				newLine().append(indent(level + 1) + key + " " + entry.getValue().getParameterMode().name().toLowerCase() + " ");
				entry.getValue().getType().accept(this,level + 1,typeNode.getChildren().get(key));
			}
		}

		@Override
		public void visitFunctionSignature(FunctionSignature type, Integer level, TypeNode typeNode) {
			appendToLastCell("function").append("\"" + type.getName() + "\"").append("#" + typeNode.id());
			if (transferObjectTree != null) {
				TransferObject to = getOnlyElement(transferObjectTree.getAssociations().get(typeNode));
				append("| " + indent(to.getDepth()) + to.toStringDescription()).append("#" + to.getId());
			}
			newLine().append(indent(level + 1) + FunctionSignature.RETURN_LABEL + " ");
			type.getReturnType().accept(this,level + 1,typeNode.getChildren().get(FunctionSignature.RETURN_LABEL));
			for (Map.Entry<String,Parameter> entry : type.getParameters().entrySet()) {
				String key = entry.getKey();
				newLine().append(indent(level + 1) + key + " " + entry.getValue().getParameterMode().name().toLowerCase() + " ");
				entry.getValue().getType().accept(this,level + 1,typeNode.getChildren().get(key));
			}
		}

		@Override
		public void visitRecord(Record type, Integer level, TypeNode typeNode) {
			appendToLastCell("record").append("\"" + type.getName() + "\"").append("#" + typeNode.id());
			if (transferObjectTree != null) {
				List<TransferObject> tos = transferObjectTree.getAssociations().get(typeNode);
				if (tos.isEmpty()) { // part of more complex transferrable type
					append("|");
				} else { // JDBC-transferrable record needs TO, other records nodes never have associated TO due to decomposition
					TransferObject to = getOnlyElement(tos);
					append("| " + indent(to.getDepth()) + to.toStringDescription()).append("#" + to.getId());
				}
			}
			for (Map.Entry<String,Type> entry : type.getFields().entrySet()) {
				String key = entry.getKey();
				newLine().append(indent(level + 1) + key + " ");
				entry.getValue().accept(this,level + 1,typeNode.getChildren().get(key));
			}
		}

		@Override
		public void visitVarray(Varray type, Integer level, TypeNode typeNode) {
			appendToLastCell("varray").append("\"" + type.getName() + "\"").append("#" + typeNode.id());
			if (transferObjectTree != null) {
				List<TransferObject> tos = transferObjectTree.getAssociations().get(typeNode);
				if (tos.isEmpty()) { // part of more complex transferrable type
					append("|");
				} else {
					TransferObject to = getOnlyElement(tos);
					append("| " + indent(to.getDepth()) + to.toStringDescription()).append("#" + to.getId());
				}
			}
			newLine().append(indent(level + 1) + Varray.ELEMENT_LABEL + " ");
			type.getElementType().accept(this,level + 1,typeNode.getChildren().get(Varray.ELEMENT_LABEL));
		}

		@Override
		public void visitNestedTable(NestedTable type, Integer level, TypeNode typeNode) {
			appendToLastCell("nestedtable").append("\"" + type.getName() + "\"").append("#" + typeNode.id());
			if (transferObjectTree != null) {
				List<TransferObject> tos = transferObjectTree.getAssociations().get(typeNode);
				if (tos.isEmpty()) { // part of more complex transferrable type
					append("|");
				} else {
					Preconditions.checkState(tos.size() == 2 || tos.size() == 3,"wrong number of associations: %s",tos.size());
					TransferObject toPointers = tos.get(0);
					TransferObject toDeletions = tos.get(1);
					append("| " + indent(toPointers.getDepth()) + toPointers.toStringDescription()).append("#" + toPointers.getId())
					.newLine().append("").append("").append("")
					.append("| " + indent(toDeletions.getDepth()) + toDeletions.toStringDescription()).append("#" + toDeletions.getId());
					if (tos.size() == 3) { // JDBC-transferrable nested table is associated with type node too (yet)
						TransferObject toData = tos.get(2);
						newLine().append("").append("").append("")
						.append("| " + indent(toData.getDepth()) + toData.toStringDescription()).append("#" + toData.getId());
					}
				}
			}
			newLine().append(indent(level + 1) + NestedTable.ELEMENT_LABEL + " ");
			type.getElementType().accept(this,level + 1,typeNode.getChildren().get(NestedTable.ELEMENT_LABEL));
		}

		@Override
		public void visitIndexByTable(IndexByTable type, Integer level, TypeNode typeNode) {
			appendToLastCell("indexbytable").append("\"" + type.getName() + "\"").append("#" + typeNode.id());
			if (transferObjectTree != null) {
				List<TransferObject> tos = transferObjectTree.getAssociations().get(typeNode);
				if (tos.isEmpty()) { // part of more complex transferrable type
					append("|");
				} else {
					Preconditions.checkState(tos.size() == 2,"wrong number of associations: %s",tos.size());
					TransferObject toPointers = tos.get(0);
					append("| " + indent(toPointers.getDepth()) + toPointers.toStringDescription()).append("#" + toPointers.getId());
				}
			}
			newLine().append(indent(level + 1) + IndexByTable.KEY_LABEL).append(type.getIndexType().toString());
			if (transferObjectTree != null) {
				List<TransferObject> tos = transferObjectTree.getAssociations().get(typeNode);
				append(""); // skipping id column
				if (tos.isEmpty()) { // part of more complex transferrable type
					append("|");
				} else {
					Preconditions.checkState(tos.size() == 2,"wrong number of associations: %s",tos.size());
					TransferObject toIndexes = tos.get(1);
					append("| " + indent(toIndexes.getDepth()) + toIndexes.toStringDescription()).append("#" + toIndexes.getId());
				}
			}
			newLine().append(indent(level + 1) + IndexByTable.ELEMENT_LABEL + " ");
			type.getElementType().accept(this,level + 1,typeNode.getChildren().get(IndexByTable.ELEMENT_LABEL));
		}

		@Override
		public void visitPrimitive(PrimitiveType type, Integer level, TypeNode typeNode) {
			append("\"" + type.getName() + "\"").append("#" + typeNode.id());
			if (transferObjectTree != null) {
				List<TransferObject> tos = transferObjectTree.getAssociations().get(typeNode);
				if (tos.isEmpty()) { // part of more complex transferrable type
					append("|");
				} else { // decomposition reaches primitive node in which case it must have exactly one TO
					TransferObject to = getOnlyElement(tos);
					append("| " + indent(to.getDepth()) + to.toStringDescription()).append("#" + to.getId());
				}
			}
		}
		
	}

}
