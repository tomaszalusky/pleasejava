package pleasejava.tools;

import java.util.Objects;

/**
 * Represents simple types (scalar and LOB according to http://docs.oracle.com/cd/A97630_01/appdev.920/a96624/03_types.htm ).
 * 
 * @author Tomas Zalusky
 */
class PrimitiveType extends Type {
	
	PrimitiveType(String name) {
		super(name,null);
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

	<A1,A2,A3> void accept(TypeVisitorAAA<A1,A2,A3> visitor, A1 arg1, A2 arg2, A3 arg3) {
		visitor.visitPrimitive(this, arg1, arg2, arg3);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name);
	}

}