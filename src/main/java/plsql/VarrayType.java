package plsql;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Represents varray (varying array) type.
 * 
 * @author Tomas Zalusky
 */
class VarrayType extends AbstractType {

	static final String ELEMENT_LABEL = "(element)";
	
	private final AbstractType elementType;

	VarrayType(plsql.Plsql.Varray annotation, AbstractType elementType) {
		super(annotation);
		this.elementType = checkNotNull(elementType);
	}
	
	<R> R accept(TypeVisitorR<R> visitor) {
		return visitor.visitVarray(this);
	}
	
	<A> void accept(TypeVisitorA<A> visitor, A arg) {
		visitor.visitVarray(this, arg);
	}
	
	<A1,A2> void accept(TypeVisitorAA<A1,A2> visitor, A1 arg1, A2 arg2) {
		visitor.visitVarray(this, arg1, arg2);
	}
	
	<A1,A2,A3> void accept(TypeVisitorAAA<A1,A2,A3> visitor, A1 arg1, A2 arg2, A3 arg3) {
		visitor.visitVarray(this, arg1, arg2, arg3);
	}

	AbstractType getElementType() {
		return elementType;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof VarrayType)) return false;
		VarrayType that = (VarrayType)obj;
		boolean result = Objects.equals(this.name,that.name) && Objects.equals(this.elementType,that.elementType);
		return result;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.name,this.elementType);
	}

}