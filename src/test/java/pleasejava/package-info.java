/**
 * ? nacist derave nst pres JDBC
 * - zrevidovat tvorbu TO u jdbct struktur
 *   - varray
 *     - vnorene 2
 *     - vnorene 3
 *     - nst ve varray
 *     - varray v nst
 *   - jdbct record
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
 * 
 * later:
 * - komentar patri az k TO: potomka typu trojstavov� boolean, kter� charakterizuje nastaven� null u prvk� (true/false), a p��znak smaz�n� prvku (null) POZN. ZREVIDOVAT, NEZDA SE MI, ZE BY V JEDNOM MISTE BYLA INFORMACE O CELE TABULCE I O JEJICH PRVCICH, A ZE NEMA TOTEZ VARRAY
 * - vyresit algoritmus plneni TOT z PIT pro jdbct kolekce v  kolekci
 * - vyresit globalni PLSQLConfig, inspirovat se v JOOQ
 * 
 */
package pleasejava;
