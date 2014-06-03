package pleasejava.tools;

class InvalidXmlException extends RuntimeException {
	
	private final String detail;

	public InvalidXmlException(String detail) {
		this.detail = detail;
	}
	
	@Override
	public String getMessage() {
		return String.format("Invalid XML type, error: '%s'.",detail);
	}
	
}