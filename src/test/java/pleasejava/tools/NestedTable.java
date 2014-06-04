package pleasejava.tools;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Represents nested table type.
 * 
 * @author Tomas Zalusky
 */
class NestedTable extends Type {
	
	private final Type elementType;

	NestedTable(String name, Type elementType) {
		super(name);
		this.elementType = checkNotNull(elementType);
	}
	
	<R> R accept(TypeVisitor<R> visitor) {
		return visitor.visitNestedTable(this);
	}
	
	public Type getElementType() {
		return elementType;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof NestedTable)) return false;
		NestedTable that = (NestedTable)obj;
		boolean result = Objects.equals(this.name,that.name) && Objects.equals(this.elementType,that.elementType);
		return result;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.name,this.elementType);
	}

}