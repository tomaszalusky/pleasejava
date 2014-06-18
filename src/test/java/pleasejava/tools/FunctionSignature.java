package pleasejava.tools;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Objects;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ObjectArrays;

/**
 * Represents fictive type which behaves similarly to record
 * and describes all parameters and return type of a PLSQL function.
 * 
 * @author Tomas Zalusky
 */
class FunctionSignature extends AbstractSignature {
	
	private final Type returnType;
	
	private final ImmutableMap<String,Parameter> parameters;

	/**
	 * @param name
	 * @param parameters names and types of parameters (ordering of map matters)
	 * @param returnType
	 */
	FunctionSignature(String name, Map<String,Parameter> parameters, Type returnType) {
		super(name);
		this.parameters = ImmutableMap.copyOf(checkNotNull(parameters));
		this.returnType = returnType;
	}
	
	<R> R accept(TypeVisitor<R> visitor) {
		return visitor.visitFunctionSignature(this);
	}

	public Type getReturnType() {
		return returnType;
	}
	
	public Map<String,Parameter> getParameters() {
		return parameters;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof FunctionSignature)) return false;
		FunctionSignature that = (FunctionSignature)obj;
		// cannot just use Objects.equals(this.parameters,that.parameters) because order matters
		boolean result = Objects.equals(this.name,that.name)
				&& Iterables.elementsEqual(this.parameters.entrySet(),that.parameters.entrySet())
				&& Objects.equals(this.returnType,that.returnType);
		return result;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(ObjectArrays.concat(new Object[] {this.name,this.returnType}, this.parameters.entrySet().toArray(), Object.class));
	}

}