package plsql;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;




import plsql.Plsql.Function;

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
	
	public static class StringConverter extends TypeAnnotationStringConverter<Function> {
	
		@Override
		public String toString(Function a) {
			return a.value();
		}
	
		@Override
		public Function fromString(String input) {
			return new Plsql.Function() {
				@Override
				public Class<? extends Annotation> annotationType() {
					return Function.class;
				}
				@Override
				public String value() {
					return input;
				}
			};
		}
		
	}

	static final String RETURN_LABEL = "(return)";
	
	private final AbstractType returnType;
	
	private final ImmutableMap<String,Parameter> parameters;

	/**
	 * @param name
	 * @param parameters names and types of parameters (ordering of map matters)
	 * @param returnType
	 */
	FunctionSignature(plsql.Plsql.Function annotation, Map<String,Parameter> parameters, AbstractType returnType) {
		super(annotation);
		this.parameters = ImmutableMap.copyOf(checkNotNull(parameters));
		this.returnType = returnType;
	}
	
	<R> R accept(TypeVisitorR<R> visitor) {
		return visitor.visitFunctionSignature(this);
	}

	<A,R> R accept(TypeVisitorAR<A,R> visitor, A arg) {
		return visitor.visitFunctionSignature(this, arg);
	}
	
	void accept(TypeVisitor visitor) {
		visitor.visitFunctionSignature(this);
	}
	
	<A> void accept(TypeVisitorA<A> visitor, A arg) {
		visitor.visitFunctionSignature(this, arg);
	}
	
	<A1,A2> void accept(TypeVisitorAA<A1,A2> visitor, A1 arg1, A2 arg2) {
		visitor.visitFunctionSignature(this, arg1, arg2);
	}
	
	<A1,A2,A3> void accept(TypeVisitorAAA<A1,A2,A3> visitor, A1 arg1, A2 arg2, A3 arg3) {
		visitor.visitFunctionSignature(this, arg1, arg2, arg3);
	}
	
	AbstractType getReturnType() {
		return returnType;
	}
	
	Map<String,Parameter> getParameters() {
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