package pleasejava.tools;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.google.common.base.Preconditions;

/**
 * Represents index-by table (aka associative array) type.
 * 
 * @author Tomas Zalusky
 */
class IndexByTable extends Type {
	
	private final Type elementType;

	private final PrimitiveType indexType;

	IndexByTable(String name, Type elementType, PrimitiveType indexType) {
		super(name);
		Preconditions.checkArgument(indexType != null && indexType.getName().matches("(?i)binary_integer|pls_integer|varchar2\\(\\d+?\\)|varchar|string|long"), "Illegal index type '%s'.", indexType == null ? null : indexType.getName()); // http://docs.oracle.com/cd/B10500_01/appdev.920/a96624/05_colls.htm#19661
		this.indexType = indexType;
		this.elementType = checkNotNull(elementType);
	}
	
	<R> R accept(TypeVisitor<R> visitor) {
		return visitor.visitIndexByTable(this);
	}
	
	public Type getElementType() {
		return elementType;
	}

	public PrimitiveType getIndexType() {
		return indexType;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof IndexByTable)) return false;
		IndexByTable that = (IndexByTable)obj;
		boolean result = Objects.equals(this.name,that.name) && Objects.equals(this.elementType,that.elementType) && Objects.equals(this.indexType,that.indexType);
		return result;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.name,this.elementType,this.indexType);
	}
}