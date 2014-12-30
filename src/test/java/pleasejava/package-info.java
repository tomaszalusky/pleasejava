/**
 * - sloucit type graph pro vic testu
 *   - rfct TNT
 *   - rfct TOT
 *   - rfct TNTTTOT
 * - strucnejsi nazvovou konvenci pro testy
 * 
 * later,technicke:
 * - J8
 * - guavu pryc
 * - zvazit zavedeni hierarchie pro TypeNode uzly podle typu (mozna by se tim zjednodusily visitory, nebylo potreba nejasne getChildren a vyresila i potreba dodatecne kvalifikovat potomky uzlu podle nejake vlastnosti (napr. konvence ze 1. je navratovy typ apod.))
 * 
 * later:
 * - komentar patri az k TO: potomka typu trojstavovı boolean, kterı charakterizuje nastavení null u prvkù (true/false), a pøíznak smazání prvku (null) POZN. ZREVIDOVAT, NEZDA SE MI, ZE BY V JEDNOM MISTE BYLA INFORMACE O CELE TABULCE I O JEJICH PRVCICH, A ZE NEMA TOTEZ VARRAY
 * - vyresit algoritmus plneni TOT z PIT pro jdbct kolekce v  kolekci
 * - vyresit globalni PLSQLConfig, inspirovat se v JOOQ
 * - vyresit prevod JDBC-transferrable typu, a la ifc JdbcTransfer { convertForth(); convertBack(); } - to bude az u TO, mozna i dale
 * ? nacist derave nst pres JDBC - existuje lepsi reseni nez posilat si {q} ?
 * - zvazit odstraneni param. testu - puvodni poznamka kvuli nemoznosti volat test pro oddeleny parametr, ale dle http://blog.moritz.eysholdt.de/2014/11/new-eclipse-junit-feature-run-subtrees.html to od Eclipse Mars pujde a stejne to takto nuti volat vse -> zatim pockat
 * 
 */
package pleasejava;
