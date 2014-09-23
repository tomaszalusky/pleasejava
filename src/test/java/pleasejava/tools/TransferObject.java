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

je hodnota odpovidajici typu, ktery je JDBCT, a která popisuje èást struktury pøedávané mezi PLSQL a Javou.
Odpovida jednomu otazniku v JDBC zapisu SQL dotazu.

Typy ITO objektù:
- skalární (napø. 1 varchar2 nebo top-level record apod. - data vyjadrena jednou promennou JDBCT typu) - JDBCT
- kolekce pointerù - {p}
- kolekce deleted pøíznakù - {d}
- kolekce klíèù v index-by table {i}
- kolekce skalárních hodnot - {JDBCT}, coz je vlastne JDBCTC

Vıznam pointeru: 
- vymezuje interval v dìtské kolekci:
  - nech p je rodièovská kolekce pointerù
  - nech c je dìtská kolekce (pointeru nebo dat)
  - pro i-tı pointer v p je dìtská kolekce popsána následovnì:
    - záporná hodnota znaèí null kolekci (její absolutní hodnota ale nese informaci o horní mezi pøedchozí kolekce)
    - kladná hodnota vymezuje interval <c[p[i]],abs(c[p[i+1]))
      - sémantika mezí je pøesnì shodná jako u guava Range.closedOpen, tj.
        - dolní mez je inclusive
        - horní exclusive
        - <x,x) vyjadøuje prázdnou mnoinu
      - abs je kvùli moné následné hodnotì null
  - poslední prvek je zaráka, tj. {p} kolekce mají vdy o 1 prvek víc ne normální kolekce

Kadı ITO je asociován s nìjakım uzel PTT a je mu pøidìlen identifikátor, kterı vychází z identifikátoru uzlu PTT a pridava k nemu dalsi informaci.
ITO objekty tvoøí také strom.

Algoritmus tvorby ITO a asociace s PTT:
---------------------------------------
- nech N je uzel PTT
- je-li N typu JDBCT a je souèástí kolekce  (nemusi byt list)
  - pøidej ITO {JDBCT}
  - id = id(N)
  - break
- je-li N typu JDBCT a není souèástí kolekce   (nemusi byt list)
  - pøidej ITO JDBCT
  - id = id(N)
  - break
- pro N typu kolekce
  - pøidej ITO {p}
  - rodièem ITO je ITO {p} nejbliší vyšší kolekce, pokud existuje
  - id = id(N)
- pro N typu nested table
  - pøidej k potomku ITO {d}
  - rodièem ITO je ITO {p} u N
  - id = id(N) + "d"
- pro N typu index-by table
  - pøidej k potomku ITO {i}
  - rodièem ITO je ITO {p} u N
  - id = id(N) + "i"
*/
