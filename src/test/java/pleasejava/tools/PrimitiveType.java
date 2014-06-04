package pleasejava.tools;

import java.util.Objects;

/**
 * Represents simple types (scalar and LOB according to http://docs.oracle.com/cd/A97630_01/appdev.920/a96624/03_types.htm ).
 * 
 * @author Tomas Zalusky
 */
class PrimitiveType extends Type {
	
	PrimitiveType(String name) {
		super(name);
	}
	
	<R> R accept(TypeVisitor<R> visitor) {
		return visitor.visitPrimitive(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof PrimitiveType)) return false;
		PrimitiveType that = (PrimitiveType)obj;
		boolean result = Objects.equals(this.name,that.name);
		return result;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.name);
	}

}