package pleasejava.tools;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ObjectArrays;

/**
 * Represents fictive type which behaves similarly to record
 * and describes all parameters of a PLSQL procedure.
 * 
 * @author Tomas Zalusky
 */
class ProcedureSignature extends Type {
	
	private final ImmutableMap<String,Parameter> parameters;

	/**
	 * @param name
	 * @param parameters names and types of parameters (ordering of map matters)
	 */
	ProcedureSignature(String name, Map<String,Parameter> parameters) {
		super(name);
		this.parameters = ImmutableMap.copyOf(checkNotNull(parameters));
	}
	
	<R> R accept(TypeVisitor<R> visitor) {
		return visitor.visitProcedureSignature(this);
	}
	
	public Map<String,Parameter> getParameters() {
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