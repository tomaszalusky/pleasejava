package pleasejava.tools;

/**
 * Indicates situation where illegal XML element was used where XML element denoting PLSQL type was expected.
 * 
 * @author Tomas Zalusky
 */
class InvalidPlsqlConstructException extends RuntimeException {
	
	private final String constructName;
	
	public InvalidPlsqlConstructException(String constructName) {
		this.constructName = constructName;
	}
	
	@Override
	public String getMessage() {
		return String.format("Invalid PLSQL construct '%s'.",constructName);
	}
	
}