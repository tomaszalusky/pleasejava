package plsql;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.annotation.Annotation;
import java.util.Objects;

import plsql.Plsql.IndexByTable;

import com.google.common.base.Preconditions;

/**
 * Represents index-by table (aka associative array) type.
 * 
 * @author Tomas Zalusky
 */
class IndexByTableType extends AbstractType {
	
	public static class StringConverter extends TypeAnnotationStringConverter<IndexByTable> {
	
		@Override
		public String toString(IndexByTable a) {
			return a.value();
		}
	
		@Override
		public IndexByTable fromString(String input) {
			return new Plsql.IndexByTable() {
				@Override
				public Class<? extends Annotation> annotationType() {
					return IndexByTable.class;
				}
				@Override
				public String value() {
					return input;
				}
			};
		}
		
	}

	static final String KEY_LABEL = "(key)";
	
	static final String ELEMENT_LABEL = "(element)";
	
	private final AbstractType elementType;

	private final AbstractPrimitiveType indexType;

	IndexByTableType(plsql.Plsql.IndexByTable annotation, AbstractType elementType, AbstractPrimitiveType indexType) {
		super(annotation);
		Preconditions.checkArgument(indexType instanceof BinaryIntegerType || indexType instanceof PlsIntegerType || indexType instanceof Varchar2Type || indexType instanceof StringType || indexType instanceof LongType, "Illegal index type '%s'.", indexType == null ? null : indexType.getName()); // TODO: improve according to http://docs.oracle.com/cd/B10500_01/appdev.920/a96624/05_colls.htm#19661 - check index bounds etc.
		this.indexType = indexType;
		this.elementType = checkNotNull(elementType);
	}
	
	<R> R accept(TypeVisitorR<R> visitor) {
		return visitor.visitIndexByTable(this);
	}
	
	<A,R> R accept(TypeVisitorAR<A,R> visitor, A arg) {
		return visitor.visitIndexByTable(this, arg);
	}
	
	void accept(TypeVisitor visitor) {
		visitor.visitIndexByTable(this);
	}
	
	<A> void accept(TypeVisitorA<A> visitor, A arg) {
		visitor.visitIndexByTable(this, arg);
	}
	
	<A1,A2> void accept(TypeVisitorAA<A1,A2> visitor, A1 arg1, A2 arg2) {
		visitor.visitIndexByTable(this, arg1, arg2);
	}
	
	<A1,A2,A3> void accept(TypeVisitorAAA<A1,A2,A3> visitor, A1 arg1, A2 arg2, A3 arg3) {
		visitor.visitIndexByTable(this, arg1, arg2, arg3);
	}

	AbstractType getElementType() {
		return elementType;
	}

	AbstractPrimitiveType getIndexType() {
		return indexType;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof IndexByTableType)) return false;
		IndexByTableType that = (IndexByTableType)obj;
		boolean result = Objects.equals(this.name,that.name) && Objects.equals(this.elementType,that.elementType) && Objects.equals(this.indexType,that.indexType);
		return result;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.name,this.elementType,this.indexType);
	}

	/**
	 * Index-by tables are never JDBC-transferrable because they are PLSQL types and not database types.
	 * @see plsql.AbstractType#isJdbcTransferrable()
	 */
	@Override
	boolean isJdbcTransferrable() {
		if (super.isJdbcTransferrable()) {
			throw new Error("System error: wrong detection of JDBC-transferrability for index-by table " + getName());
		}
		return false;
	}

}
