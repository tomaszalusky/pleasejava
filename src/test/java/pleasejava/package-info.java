/**
 * - rozmyslet anotace
 *   - smysl anotaci: pokryti PLSQL typu Java typy
 *   - vycet podporovanych typu
 *   - vyresit vnorovani pro kolekce
 *   - vyresit a zduvodnit zda konvence PlsqlFoo nebo Plsql.Foo, urcit package pro anotace
 *   - vyresit subtypy a zda bude treba metaanotace pro typ, pripadne moznost implementace vlastnich subtypu pomoci metaanotaci (napr. @Integer_(3) public @interface MyInteger) - jak by to pomohlo generovani?
 * - mapovani anotaci na PLSQL/SQL konstrukty
 * - uprava typegraph.xml - popis java testu (trid a packagu, typu)
 * - vyresit overloading
 * - vygenerovat java tridy z xml
 * - vyresit zivotnost class vuci generatoru
 * - postavit graf z anotaci
 * - update yEd schematu
 * 
 * later,technicke:
 * - J8
 * - guavu pryc
 * - zvazit zavedeni hierarchie pro TypeNode uzly podle typu (mozna by se tim zjednodusily visitory, nebylo potreba nejasne getChildren a vyresila i potreba dodatecne kvalifikovat potomky uzlu podle nejake vlastnosti (napr. konvence ze 1. je navratovy typ apod.))
 * 
 * later:
 * - komentar patri az k TO: potomka typu trojstavový boolean, který charakterizuje nastavení null u prvkù (true/false), a pøíznak smazání prvku (null) POZN. ZREVIDOVAT, NEZDA SE MI, ZE BY V JEDNOM MISTE BYLA INFORMACE O CELE TABULCE I O JEJICH PRVCICH, A ZE NEMA TOTEZ VARRAY
 * - vyresit algoritmus plneni TOT z PIT pro jdbct kolekce v  kolekci
 * - vyresit globalni PLSQLConfig, inspirovat se v JOOQ
 * - vyresit prevod JDBC-transferrable typu, a la ifc JdbcTransfer { convertForth(); convertBack(); } - to bude az u TO, mozna i dale
 * ? nacist derave nst pres JDBC - existuje lepsi reseni nez posilat si {q} ?
 * - zvazit odstraneni param. testu - puvodni poznamka kvuli nemoznosti volat test pro oddeleny parametr, ale dle http://blog.moritz.eysholdt.de/2014/11/new-eclipse-junit-feature-run-subtrees.html to od Eclipse Mars pujde a stejne to takto nuti volat vse -> zatim pockat
 * - nepovinne value u anotaci (odvodit z nazvu Java konstruktu) - osemetne kvuli kolekcim, ktere nemaji ekvivalentni Java typ a musi se uvadet vzdy s celym nazvem package. To by pak bylo v nesouladu (princip jednotnosti a minimalniho prekvapeni) u metod. Priklady: 1. anotace Procedure("foo") znamena foo v packagi (anotovany interface) nebo toplevel? 2. Procedure s default value znamena prebrat nazev z Java identifikatoru i s packagem nebo top level? I kdyby se pravidla urcila, nebyla by intuitivni.
 * - reseni kanonickeho tvaru identifikatoru, pokud sam nazev v SQL je v uvozovkach 
 */
package pleasejava;
