package pleasejava.tools;

/**
 * Direction (mode) of parameter of PLSQL procedure or function according to
 * http://docs.oracle.com/cd/E11882_01/appdev.112/e25519/subprograms.htm#LNPLS665
 *
 * @author Tomas Zalusky
 */
enum ParameterMode {
	IN,
	OUT,
	INOUT;
}