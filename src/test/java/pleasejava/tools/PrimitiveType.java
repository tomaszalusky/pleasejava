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
	
	<R> R accept(TypeVisitorR<R> visitor) {
		return visitor.visitPrimitive(this);
	}

	<A> void accept(TypeVisitorA<A> visitor, A arg) {
		visitor.visitPrimitive(this, arg);
	}
	
	<A1,A2> void accept(TypeVisitorAA<A1,A2> visitor, A1 arg1, A2 arg2) {
		visitor.visitPrimitive(this, arg1, arg2);
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