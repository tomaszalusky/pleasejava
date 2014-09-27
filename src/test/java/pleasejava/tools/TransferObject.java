package pleasejava.tools;

import java.util.List;

import pleasejava.Utils;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * @author Tomas Zalusky
 */
public class TransferObject {

	private String desc; // TODO some string characteristics, only temporary, will be replaced by subclass of TO and another data 
	
	private final TransferObject parent;
	
	private final List<TransferObject> children = Lists.newArrayList(); // mutable
	
	private final int depth;

	private final TypeNode typeNode;
	
	public TransferObject(String desc, TransferObject parent, TypeNode typeNode) {
		this.desc = desc;
		this.parent = parent;
		this.typeNode = typeNode;
		this.depth = parent == null ? 0 : parent.depth + 1;
	}

	public String getDesc() {
		return desc;
	}

	public int getDepth() {
		return depth;
	}
	
	public void addChild(TransferObject child) {
		children.add(child);
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		toStringBuilder(buf);
		return buf.toString();
	}

	private void toStringBuilder(StringBuilder buf) {
		Utils.appendf(buf,"%s%s%n",Strings.repeat(" ",4*depth),desc);
		for (TransferObject child : children) {
			child.toStringBuilder(buf);
		}
	}

	public String getId() {
		return "id";
	}

}

/**
ITO - Intermediate Transfer Object
==================================

je hodnota odpovidajici typu, ktery je JDBCT, a kter� popisuje ��st struktury p�ed�van� mezi PLSQL a Javou.
Odpovida jednomu otazniku v JDBC zapisu SQL dotazu.

Typy ITO objekt�:
- skal�rn� (nap�. 1 varchar2 nebo top-level record apod. - data vyjadrena jednou promennou JDBCT typu) - JDBCT
- kolekce pointer� - {p}
- kolekce deleted p��znak� - {d}
- kolekce kl��� v index-by table {i}
- kolekce skal�rn�ch hodnot - {JDBCT}, coz je vlastne JDBCTC

V�znam pointeru: 
- vymezuje interval v d�tsk� kolekci:
  - nech� p je rodi�ovsk� kolekce pointer�
  - nech� c je d�tsk� kolekce (pointeru nebo dat)
  - pro i-t� pointer v p je d�tsk� kolekce pops�na n�sledovn�:
    - z�porn� hodnota zna�� null kolekci (jej� absolutn� hodnota ale nese informaci o horn� mezi p�edchoz� kolekce)
    - kladn� hodnota vymezuje interval <c[p[i]],abs(c[p[i+1]))
      - s�mantika mez� je p�esn� shodn� jako u guava Range.closedOpen, tj.
        - doln� mez je inclusive
        - horn� exclusive
        - <x,x) vyjad�uje pr�zdnou mno�inu
      - abs je kv�li mo�n� n�sledn� hodnot� null
  - posledn� prvek je zar�ka, tj. {p} kolekce maj� v�dy o 1 prvek v�c ne� norm�ln� kolekce

Ka�d� ITO je asociov�n s n�jak�m uzel PTT a je mu p�id�len identifik�tor, kter� vych�z� z identifik�toru uzlu PTT a pridava k nemu dalsi informaci.
ITO objekty tvo�� tak� strom.

Algoritmus tvorby ITO a asociace s PTT:
---------------------------------------
- nech� N je uzel PTT
- je-li N typu JDBCT a je sou��st� kolekce  (nemusi byt list)
  - p�idej ITO {JDBCT}
  - id = id(N)
  - break
- je-li N typu JDBCT a nen� sou��st� kolekce   (nemusi byt list)
  - p�idej ITO JDBCT
  - id = id(N)
  - break
- pro N typu kolekce
  - p�idej ITO {p}
  - rodi�em ITO je ITO {p} nejbli��� vy��� kolekce, pokud existuje
  - id = id(N)
- pro N typu nested table
  - p�idej k potomku ITO {d}
  - rodi�em ITO je ITO {p} u N
  - id = id(N) + "d"
- pro N typu index-by table
  - p�idej k potomku ITO {i}
  - rodi�em ITO je ITO {p} u N
  - id = id(N) + "i"
*/
