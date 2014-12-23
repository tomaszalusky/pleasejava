/**
 * ? nacist derave nst pres JDBC
 * - zrevidovat tvorbu TO u jdbct struktur
 *   - jdbct record
 *   - vratit suffix TO pro transferrable kolekci na t - 1eee u TO plete, kdyz u TN je to id recordu
 * - zapracovat koncept JDBC-transferrable typu, a la ifc JdbcTransfer { convertForth(); convertBack(); } - to bude az u TO, mozna i dale
 * - overit funkcnost pro dosavadni testcasy
 * - overit funkcnost pro nove jdbct typy
 * 
 * technicke
 * - J8
 * - guavu pryc
 * - param. testy pryc
 * - sloucit type graph pro vic testu
 * - robustni toString u TO
 * - zvazit zavedeni hierarchie pro TypeNode uzly podle typu (mozna by se tim zjednodusily visitory, nebylo potreba nejasne getChildren a vyresila i potreba dodatecne kvalifikovat potomky uzlu podle nejake vlastnosti (napr. konvence ze 1. je navratovy typ apod.))
 * 
 * later:
 * - komentar patri az k TO: potomka typu trojstavový boolean, který charakterizuje nastavení null u prvkù (true/false), a pøíznak smazání prvku (null) POZN. ZREVIDOVAT, NEZDA SE MI, ZE BY V JEDNOM MISTE BYLA INFORMACE O CELE TABULCE I O JEJICH PRVCICH, A ZE NEMA TOTEZ VARRAY
 * - vyresit algoritmus plneni TOT z PIT pro jdbct kolekce v  kolekci
 * - vyresit globalni PLSQLConfig, inspirovat se v JOOQ
 * 
 */
package pleasejava;
