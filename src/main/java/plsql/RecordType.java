package plsql;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;

import plsql.Plsql.Record;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ObjectArrays;

/**
 * Represents record type.
 * 
 * @author Tomas Zalusky
 */
class RecordType extends AbstractType {
	
	public static class StringConverter extends TypeAnnotationStringConverter<Record> {
	
		@Override
		public String toString(Record a) {
			return a.value();
		}
	
		@Override
		public Record fromString(String input) {
			return new Plsql.Record() {
				@Override
				public Class<? extends Annotation> annotationType() {
					return Record.class;
				}
				@Override
				public String value() {
					return input;
				}
			};
		}
		
	}

	private final ImmutableMap<String,AbstractType> fields;
	
	/**
	 * @param name
	 * @param fields names and types of fields (ordering of map matters)
	 */
	RecordType(plsql.Plsql.Record annotation, Map<String,AbstractType> fields) {
		super(annotation);
		this.fields = ImmutableMap.copyOf(checkNotNull(fields));
	}

	<R> R accept(TypeVisitorR<R> visitor) {
		return visitor.visitRecord(this);
	}
	
	<A,R> R accept(TypeVisitorAR<A,R> visitor, A arg) {
		return visitor.visitRecord(this, arg);
	}
	
	void accept(TypeVisitor visitor) {
		visitor.visitRecord(this);
	}
	
	<A> void accept(TypeVisitorA<A> visitor, A arg) {
		visitor.visitRecord(this, arg);
	}
	
	<A1,A2> void accept(TypeVisitorAA<A1,A2> visitor, A1 arg1, A2 arg2) {
		visitor.visitRecord(this, arg1, arg2);
	}
	
	<A1,A2,A3> void accept(TypeVisitorAAA<A1,A2,A3> visitor, A1 arg1, A2 arg2, A3 arg3) {
		visitor.visitRecord(this, arg1, arg2, arg3);
	}

	Map<String,AbstractType> getFields() {
		return fields;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof RecordType)) return false;
		RecordType that = (RecordType)obj;
		// cannot just use Objects.equals(this.fields,that.fields) because order matters
		boolean result = Objects.equals(this.name,that.name) && Iterables.elementsEqual(this.fields.entrySet(),that.fields.entrySet());
		return result;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(ObjectArrays.concat(this.name, this.fields.entrySet().toArray()));
	}
	
}