package plsql;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;

import plsql.Plsql.Procedure;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ObjectArrays;

/**
 * Represents fictive type which behaves similarly to record
 * and describes all parameters of a PLSQL procedure.
 * 
 * @author Tomas Zalusky
 */
class ProcedureSignature extends AbstractSignature {
	
	private final ImmutableMap<String,Parameter> parameters;

	static class StringConverter extends TypeAnnotationStringConverter<Procedure> {

		@Override
		public String toString(Procedure a) {
			return a.value();
		}

		@Override
		public Procedure fromString(String input) {
			return new Plsql.Procedure() {
				@Override
				public Class<? extends Annotation> annotationType() {
					return Procedure.class;
				}
				@Override
				public String value() {
					return input;
				}
			};
		}
		
	}

	/**
	 * @param name
	 * @param parameters names and types of parameters (ordering of map matters)
	 */
	ProcedureSignature(plsql.Plsql.Procedure annotation, Map<String,Parameter> parameters) {
		super(annotation);
		this.parameters = ImmutableMap.copyOf(checkNotNull(parameters));
	}
	
	<R> R accept(TypeVisitorR<R> visitor) {
		return visitor.visitProcedureSignature(this);
	}
	
	<A,R> R accept(TypeVisitorAR<A,R> visitor, A arg) {
		return visitor.visitProcedureSignature(this, arg);
	}
	
	void accept(TypeVisitor visitor) {
		visitor.visitProcedureSignature(this);
	}
	
	<A> void accept(TypeVisitorA<A> visitor, A arg) {
		visitor.visitProcedureSignature(this, arg);
	}

	<A1,A2> void accept(TypeVisitorAA<A1,A2> visitor, A1 arg1, A2 arg2) {
		visitor.visitProcedureSignature(this, arg1, arg2);
	}
	
	<A1,A2,A3> void accept(TypeVisitorAAA<A1,A2,A3> visitor, A1 arg1, A2 arg2, A3 arg3) {
		visitor.visitProcedureSignature(this, arg1, arg2, arg3);
	}

	Map<String,Parameter> getParameters() {
		return parameters;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof ProcedureSignature)) return false;
		ProcedureSignature that = (ProcedureSignature)obj;
		// cannot just use Objects.equals(this.parameters,that.parameters) because order matters
		boolean result = Objects.equals(this.name,that.name) && Iterables.elementsEqual(this.parameters.entrySet(),that.parameters.entrySet());
		return result;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(ObjectArrays.concat(this.name, this.parameters.entrySet().toArray()));
	}

}