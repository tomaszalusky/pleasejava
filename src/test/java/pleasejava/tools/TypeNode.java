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

	TypeNode(Type type, TypeNode parent) {
		this.type = type;
		this.parent = parent;
		this.depth = parent == null ? 0 : parent.depth + 1;
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
		return String.format("id=%s",System.identityHashCode(this));
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


Algoritmus stanovení identifikátoru uzlu PTT:
---------------------------------------------
- všechny parametry metody tvoøí fiktivní record, jehož id = ""
- je-li uzel i-tým fieldem recordu (i je 1-based, 0 je vyhrazena pouze pro návratový typ funkcí), je
  - pro id<10 id = id_rodièe + i
  - pro id>=10 id = id_rodièe + "_" + i + "_" 
  (smyslem je neplýtvat na oddìlovaèích délkou pro malé recordy a zároveò zachovat jednoznaènost)
- je-li uzel prvkem kolekce, je id = id_rodièe + "e"
Pøíklad:
2e_11_e = druhým parametrem metody je kolekce, jejíž prvkem je record,
jehož 11. fieldem je kolekce, jejíž prvek je popisovaný uzel PTT



*/