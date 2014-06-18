package pleasejava.tools;

import static pleasejava.Utils.appendf;

import java.util.List;

import pleasejava.Utils;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

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
	
	private final List<TypeNode> children;

	TypeNode(Type type, List<TypeNode> children) {
		this.type = type;
		this.children = ImmutableList.copyOf(children);
		for (TypeNode child : children) {
			child.setParent(this);
		}
	}
	
	private void setParent(TypeNode parent) {
		this.parent = parent;
	}
	
	int level() {
		return parent == null ? 0 : parent.level() + 1;
	}
	
	public String toString(boolean recursive) {
		StringBuilder buf = new StringBuilder();
		if (recursive) {
			appendf(buf, "%s%s", Strings.repeat("  ",level()), toString(false));
			for (TypeNode child : children) {
				appendf(buf,"%n%s",child.toString(true));
			}
		} else {
			appendf(buf, "%s %s", type.getClass().getSimpleName(), type.getName());
		}
		return buf.toString();
	}
	
	@Override
	public String toString() {
		return toString(true);
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