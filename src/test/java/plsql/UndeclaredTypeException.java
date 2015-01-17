package plsql;

/**
 * Indicates a situation where a (non-primitive) type is referenced but has not been declared anywhere.
 * @author Tomas Zalusky
 */
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