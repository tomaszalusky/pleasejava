package plsql;

/**
 * Indicates circular dependency of type on itself.
 * @author Tomas Zalusky
 */
class TypeCircularityException extends RuntimeException {
	
	private final String typeName;
	
	public TypeCircularityException(String typeName) {
		this.typeName = typeName;
	}
	
	@Override
	public String getMessage() {
		return String.format("The type '%s' circularly depends on itself.",typeName);
	}
	
}