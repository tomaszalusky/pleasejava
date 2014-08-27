package pleasejava.tools;

import static pleasejava.Utils.appendf;

import java.util.Map;

import pleasejava.Utils;

import com.google.common.collect.ImmutableMap;

/**
 * Represents one node in parameter type tree.
 * Unlike {@link Type}, {@link TypeNode} represents occurence of type
 * at concrete point in signature of procedure or function.
 * @author Tomas Zalusky
 */
class TypeNode {

	private final Type type;
	
	/**
	 * Points to type node representing type which depends on this type.
	 * Transitively leads to procedure or function whose signature is described by type tree with this node.
	 * The root of tree represents signature of procedure or function (holds condition
	 * <code>parent == null && (type instanceof {@link ProcedureSignature} || type instanceof {@link FunctionSignature})</code>).
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
	 * @param parent
	 * @param orderInParent
	 * @return
	 */
	private static String computeId(TypeNode parent, final int orderInParent) {
		String result = parent == null ? "" : parent.id() + parent.getType().accept(new TypeVisitorR<String>() {
			private String escapize(int value) {return value < 10 ? "" + value : "_" + value + "_";}
			@Override public String visitRecord(Record type) {return escapize(orderInParent + 1);}
			@Override public String visitVarray(Varray type) {return "e";}
			@Override public String visitNestedTable(NestedTable type) {return "e";}
			@Override public String visitIndexByTable(IndexByTable type) {return "e";}
			@Override public String visitProcedureSignature(ProcedureSignature type) {return escapize(orderInParent + 1);}
			@Override public String visitFunctionSignature(FunctionSignature type) {return escapize(orderInParent);}
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
		StringBuilder result = new StringBuilder();
		type.accept(new ToString(result),0,this);
		Type.ToString.align(result,true);
		return result.toString();
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

		/*
		 * Key type of indexby table doesn't have TypeNode
		 * since it would complicate creation of subsequent structures.
		 * Anyway, for clarity it is still written in indented manner.
		 */
		@Override
		public void visitIndexByTable(IndexByTable type, Integer level, TypeNode typeNode) {
			appendf(buf,"indexbytable \"%s\" #%s", type.getName(), typeNode.id());
			appendf(buf,"%n%s%s %s", indent(level + 1), IndexByTable.KEY_LABEL, type.getIndexType().toString());
			appendf(buf,"%n%s%s ", indent(level + 1), IndexByTable.ELEMENT_LABEL);
			type.getElementType().accept(this,level + 1,typeNode.getChildren().get(IndexByTable.ELEMENT_LABEL));
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
		public void visitPrimitive(PrimitiveType type, Integer level, TypeNode typeNode) {
			appendf(buf,"\"%s\" #%s", type.getName(), typeNode.id());
		}
		
	}

}

/*
PTT - Parameter Type Tree
=========================

je strom vyjadøující vnoøení PLSQL typù pro jednotlivé parametry PLSQL
procedury.

Pravidla:
- koøen je uzel reprezentující typ parametru PLSQL procedury
- uzel typu JDBC-transferrable typ nemá žádné potomky. Jinak:
- uzel typu nested table, index-by table a varray má jednoho potomka,
  který odpovídá typu prvku kolekce
- uzel typu record má tolik potomkù, kolik má record fieldù, 
  každý potomek odpovídá typu pøíslušného fieldu

Každý uzel PTT má øetìzcový identifikátor. 
Tento identifikátor se použije jako souèást PLSQL identifikátorù v generovaných PLSQL skriptech.
Smyslem je dosáhnout toho, aby i generovaný kód byl pøimìøenì intuitivnì èitelný
a zároveò v bìžných pøíkladech nepøekroèit omezení Oraclu na délku identifikátoru 30 znakù.

dosavadní návod: (zøejmì zastaralé)
Pravidla:
- koøen je uzel reprezentující typ parametru PLSQL procedury
- uzel typu JDBC-transferrable typ nemá žádné potomky. Jinak:
- uzel typu nested table má dva potomky:
	- potomka typu trojstavový boolean, který charakterizuje nastavení null u prvkù (true/false), a pøíznak smazání prvku (null) POZN. ZREVIDOVAT, NEZDA SE MI, ZE BY V JEDNOM MISTE BYLA INFORMACE O CELE TABULCE I O JEJICH PRVCICH, A ZE NEMA TOTEZ VARRAY
	- potomka typu, který odpovídá typu PLSQL elementu
- uzel typu index-by table má dva potomky:
	- potomka typu, který odpovídá typu indexu
	- potomka typu, který odpovídá typu hodnoty
- uzel typu varray má jednoho potomka:
	- potomka typu, který odpovídá typu PLSQL elementu

*/
