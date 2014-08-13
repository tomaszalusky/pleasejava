package pleasejava.tools;

import static pleasejava.Utils.appendf;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import pleasejava.Utils;
import pleasejava.tools.Type.ToString;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
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
	private TypeNode parent;
	
	private final Map<String,TypeNode> children;

	TypeNode(Type type, Map<String,TypeNode> children) {
		this.type = type;
		this.children = ImmutableMap.copyOf(children);
		for (TypeNode child : children.values()) {
			child.setParent(this);
		}
	}
	
	public Type getType() {
		return type;
	}
	
	private void setParent(TypeNode parent) {
		this.parent = parent;
	}
	
	Map<String,TypeNode> getChildren() {
		return children;
	}
	
	int level() {
		return parent == null ? 0 : parent.level() + 1;
	}
	
	String id() {
		return "" + System.identityHashCode(this);
	}
	
	public String toString(boolean recursive) {
		StringBuilder buf = new StringBuilder();
		if (recursive) {
			appendf(buf, "%s%s", Strings.repeat("  ",level()), toString(false));
			for (TypeNode child : children.values()) {
				appendf(buf,"%n%s",child.toString(true));
			}
		} else {
			appendf(buf, "%s %s", type.getClass().getSimpleName(), type.getName());
		}
		return buf.toString();
	}
	
	@Override
	public String toString() {
//		StringBuilder result = new StringBuilder();
//		type.accept(new ToString(0,null,result,this));
//		Type.ToString.align(result);
//		return result.toString();
		return toString(true);
	}
	
//	/**
//	 * Flexible support for toString method.
//	 * @author Tomas Zalusky
//	 */
//	static class ToString implements TypeVisitor<Void> {
//
//		private static final int TAB_SPACES = 2;
//		
//		private final int level;
//		
//		private final Set<Type> written;
//		
//		private final StringBuilder buf;
//
//		private final TypeNode typeNode;
//		
//		/**
//		 * @param level amount of indentation
//		 * @param written guard set of type which have already been written in full format.
//		 * Ensures only first occurence of each type is listed in full format,
//		 * remaining occurences are listed only in concise format.
//		 * Can be <code>null</code> for always using full format (guard disabled).
//		 * @param buf buffer for result string
//		 * @param typeNode 
//		 */
//		ToString(int level, Set<Type> written, StringBuilder buf, TypeNode typeNode) {
//			this.level = level;
//			this.written = written;
//			this.buf = buf;
//			this.typeNode = typeNode;
//		}
//
//		private static String indent(int level) {
//			return Strings.repeat(" ",level * TAB_SPACES);
//		}
//
//		@Override
//		public Void visitRecord(Record type) {
//			appendf(buf,"record \"%s\" #%s", type.getName(), typeNode.id());
//			for (Map.Entry<String,Type> entry : type.getFields().entrySet()) {
//				appendf(buf,"%n%s%s ", indent(level + 1), entry.getKey());
//				entry.getValue().accept(new ToString(level + 1,written,buf));
//			}
//			return null;
//		}
//
//		@Override
//		public Void visitVarray(Varray type) {
//			appendf(buf,"varray \"%s\"", type.getName());
//			appendf(buf,"%n%s%s ", indent(level + 1), Varray.ELEMENT_LABEL);
//			type.getElementType().accept(new ToString(level + 1,written,buf));
//			return null;
//		}
//
//		@Override
//		public Void visitNestedTable(NestedTable type) {
//			appendf(buf,"nestedtable \"%s\"", type.getName());
//			appendf(buf,"%n%s%s ", indent(level + 1), NestedTable.ELEMENT_LABEL);
//			type.getElementType().accept(new ToString(level + 1,written,buf));
//			return null;
//		}
//
//		@Override
//		public Void visitIndexByTable(IndexByTable type) {
//			appendf(buf,"indexbytable \"%s\"", type.getName());
//			appendf(buf,"%n%s%s %s%n%s%s ", indent(level + 1), IndexByTable.KEY_LABEL,
//					type.getIndexType().toString(),
//					indent(level + 1), IndexByTable.ELEMENT_LABEL);
//			type.getElementType().accept(new ToString(level + 1,written,buf));
//			return null;
//		}
//
//		@Override
//		public Void visitProcedureSignature(ProcedureSignature type) {
//			appendf(buf,"procedure \"%s\"", type.getName());
//			for (Entry<String,Parameter> entry : type.getParameters().entrySet()) {
//				appendf(buf,"%n%s%s %s ", indent(level + 1), entry.getKey(), entry.getValue().getParameterMode().name().toLowerCase());
//				entry.getValue().getType().accept(new ToString(level + 1,written,buf));
//			}
//			return null;
//		}
//
//		@Override
//		public Void visitFunctionSignature(FunctionSignature type) {
//			appendf(buf,"function \"%s\"", type.getName());
//			appendf(buf,"%n%s%s ", indent(level + 1), FunctionSignature.RETURN_LABEL);
//			type.getReturnType().accept(new ToString(level + 1,written,buf));
//			for (Entry<String,Parameter> entry : type.getParameters().entrySet()) {
//				appendf(buf,"%n%s%s %s ", indent(level + 1), entry.getKey(), entry.getValue().getParameterMode().name().toLowerCase());
//				entry.getValue().getType().accept(new ToString(level + 1,written,buf));
//			}
//			return null;
//		}
//
//		@Override
//		public Void visitPrimitive(PrimitiveType type) {
//			appendf(buf,"\"%s\"", type.getName()); // always written regardless guard set
//			return null;
//		}
//		
//		@Override
//		public String toString() {
//			return buf.toString();
//		}
//
//	}

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