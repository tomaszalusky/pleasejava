package pleasejava.tools;

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