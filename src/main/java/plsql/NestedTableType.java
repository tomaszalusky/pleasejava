package plsql;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Represents nested table type.
 * 
 * @author Tomas Zalusky
 */
class NestedTableType extends AbstractType {
	
	static final String ELEMENT_LABEL = "(element)";
	
	private final AbstractType elementType;

	NestedTableType(plsql.Plsql.NestedTable annotation, AbstractType elementType) {
		super(annotation);
		this.elementType = checkNotNull(elementType);
	}
	
	<R> R accept(TypeVisitorR<R> visitor) {
		return visitor.visitNestedTable(this);
	}
	
	<A> void accept(TypeVisitorA<A> visitor, A arg) {
		visitor.visitNestedTable(this, arg);
	}
	
	<A1,A2> void accept(TypeVisitorAA<A1,A2> visitor, A1 arg1, A2 arg2) {
		visitor.visitNestedTable(this, arg1, arg2);
	}
	
	<A1,A2,A3> void accept(TypeVisitorAAA<A1,A2,A3> visitor, A1 arg1, A2 arg2, A3 arg3) {
		visitor.visitNestedTable(this, arg1, arg2, arg3);
	}

	AbstractType getElementType() {
		return elementType;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof NestedTableType)) return false;
		NestedTableType that = (NestedTableType)obj;
		boolean result = Objects.equals(this.name,that.name) && Objects.equals(this.elementType,that.elementType);
		return result;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.name,this.elementType);
	}

}