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
	
	private final AbstractType type;
	
	private Parameter(ParameterMode parameterMode, AbstractType type) {
		this.parameterMode = parameterMode;
		this.type = type;
	}
	
	public static Parameter create(ParameterMode mode, AbstractType type) {
		return new Parameter(mode, type);
	}
	
	public static Parameter in(AbstractType type) {
		return new Parameter(ParameterMode.IN, type);
	}
	
	public static Parameter out(AbstractType type) {
		return new Parameter(ParameterMode.OUT, type);
	}
	
	public static Parameter inout(AbstractType type) {
		return new Parameter(ParameterMode.INOUT, type);
	}

	public ParameterMode getParameterMode() {
		return parameterMode;
	}
	
	public AbstractType getType() {
		return type;
	}
	
	public static final Function<Parameter,AbstractType> _getType = new Function<Parameter,AbstractType>() {
		public AbstractType apply(Parameter input) {
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