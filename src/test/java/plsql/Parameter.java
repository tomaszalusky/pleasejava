package plsql;

import java.util.Objects;

import com.google.common.base.Function;

/**
 * Pair of parameter mode and type.
 * (Parameter name is intentionally not included,
 * it is maintained by procedure or function type,
 * this class can be used also for return type of function.)
 * 
 * @author Tomas Zalusky
 */
final class Parameter {
	
	private final ParameterMode parameterMode;
	
	private final Type type;
	
	private Parameter(ParameterMode parameterMode, Type type) {
		this.parameterMode = parameterMode;
		this.type = type;
	}
	
	public static Parameter create(ParameterMode mode, Type type) {
		return new Parameter(mode, type);
	}
	
	public static Parameter in(Type type) {
		return new Parameter(ParameterMode.IN, type);
	}
	
	public static Parameter out(Type type) {
		return new Parameter(ParameterMode.OUT, type);
	}
	
	public static Parameter inout(Type type) {
		return new Parameter(ParameterMode.INOUT, type);
	}

	public ParameterMode getParameterMode() {
		return parameterMode;
	}
	
	public Type getType() {
		return type;
	}
	
	public static final Function<Parameter,Type> _getType = new Function<Parameter,Type>() {
		public Type apply(Parameter input) {
			return input.getType();
		}
	};
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Parameter)) return false;
		Parameter that = (Parameter)obj;
		boolean result = Objects.equals(this.parameterMode,that.parameterMode) && Objects.equals(this.type,that.type);
		return result;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.parameterMode,this.type);
	}

}