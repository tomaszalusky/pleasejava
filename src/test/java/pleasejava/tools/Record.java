package pleasejava.tools;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ObjectArrays;

/**
 * Represents record type.
 * 
 * @author Tomas Zalusky
 */
class Record extends Type {
	
	private final ImmutableMap<String,Type> fields;
	
	/**
	 * @param name
	 * @param fields names and types of fields (ordering of map matters)
	 */
	Record(plsql.Plsql.Record annotation, Map<String,Type> fields) {
		super(annotation);
		this.fields = ImmutableMap.copyOf(checkNotNull(fields));
	}

	<R> R accept(TypeVisitorR<R> visitor) {
		return visitor.visitRecord(this);
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

	public Map<String,Type> getFields() {
		return fields;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Record)) return false;
		Record that = (Record)obj;
		// cannot just use Objects.equals(this.fields,that.fields) because order matters
		boolean result = Objects.equals(this.name,that.name) && Iterables.elementsEqual(this.fields.entrySet(),that.fields.entrySet());
		return result;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(ObjectArrays.concat(this.name, this.fields.entrySet().toArray()));
	}
	
}