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

je strom vyjad�uj�c� vno�en� PLSQL typ� pro jednotliv� parametry PLSQL
procedury.

Pravidla:
- ko�en je uzel reprezentuj�c� typ parametru PLSQL procedury
- uzel typu JDBC-transferrable typ nem� ��dn� potomky. Jinak:
- uzel typu nested table, index-by table a varray m� jednoho potomka,
  kter� odpov�d� typu prvku kolekce
- uzel typu record m� tolik potomk�, kolik m� record field�, 
  ka�d� potomek odpov�d� typu p��slu�n�ho fieldu

Ka�d� uzel PTT m� �et�zcov� identifik�tor. 
Tento identifik�tor se pou�ije jako sou��st PLSQL identifik�tor� v generovan�ch PLSQL skriptech.
Smyslem je dos�hnout toho, aby i generovan� k�d byl p�im��en� intuitivn� �iteln�
a z�rove� v b�n�ch p��kladech nep�ekro�it omezen� Oraclu na d�lku identifik�toru 30 znak�.

dosavadn� n�vod: (z�ejm� zastaral�)
Pravidla:
- ko�en je uzel reprezentuj�c� typ parametru PLSQL procedury
- uzel typu JDBC-transferrable typ nem� ��dn� potomky. Jinak:
- uzel typu nested table m� dva potomky:
	- potomka typu trojstavov� boolean, kter� charakterizuje nastaven� null u prvk� (true/false), a p��znak smaz�n� prvku (null) POZN. ZREVIDOVAT, NEZDA SE MI, ZE BY V JEDNOM MISTE BYLA INFORMACE O CELE TABULCE I O JEJICH PRVCICH, A ZE NEMA TOTEZ VARRAY
	- potomka typu, kter� odpov�d� typu PLSQL elementu
- uzel typu index-by table m� dva potomky:
	- potomka typu, kter� odpov�d� typu indexu
	- potomka typu, kter� odpov�d� typu hodnoty
- uzel typu varray m� jednoho potomka:
	- potomka typu, kter� odpov�d� typu PLSQL elementu


Algoritmus stanoven� identifik�toru uzlu PTT:
---------------------------------------------
- v�echny parametry metody tvo�� fiktivn� record, jeho� id = ""
- je-li uzel i-t�m fieldem recordu (i je 1-based, 0 je vyhrazena pouze pro n�vratov� typ funkc�), je
  - pro id<10 id = id_rodi�e + i
  - pro id>=10 id = id_rodi�e + "_" + i + "_" 
  (smyslem je nepl�tvat na odd�lova��ch d�lkou pro mal� recordy a z�rove� zachovat jednozna�nost)
- je-li uzel prvkem kolekce, je id = id_rodi�e + "e"
P��klad:
2e_11_e = druh�m parametrem metody je kolekce, jej� prvkem je record,
jeho� 11. fieldem je kolekce, jej� prvek je popisovan� uzel PTT



*/