package pleasejava.tools;

import java.util.Map;

/**
 * @author Tomas Zalusky
 */
public class TransferObject {

//	private final TransferObject parent;
//	
//	private /*not final, assigned only once*/ Map<String,TransferObject> children;

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
- je-li N list, je typu JDBCT a je souèástí kolekce
  - pøidej ITO {JDBCT}
  - id = id(N)
  - break
- je-li N list, je typu JDBCT a není souèástí kolekce
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
