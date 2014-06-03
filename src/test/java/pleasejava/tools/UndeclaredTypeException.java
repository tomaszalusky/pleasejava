package pleasejava.tools;

class UndeclaredTypeException extends RuntimeException {
	
	private final String undeclaredType;

	public UndeclaredTypeException(String undeclaredType) {
		this.undeclaredType = undeclaredType;
	}
	
	@Override
	public String getMessage() {
		return String.format("Undeclared type '%s'.",undeclaredType);
	}
	
}