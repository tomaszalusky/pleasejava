package pleasejava.tools;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

/**
 * Ancestor of all PLSQL types.
 * 
 * @author Tomas Zalusky
 */
abstract class Type {
	
	final String name;
	
	public Type(String name) {
		this.name = checkNotNull(name);
	}
	
	final List<Type> getChildren() {
		return accept(new GetChildren());
	}

	/**
	 * Any operation on types should be defined as visitor
	 * (since implementations of one operation across all types
	 * are expected to be more similar and reusable
	 * than implementations of different operations on particular type).
	 * @param visitor
	 * @return
	 */
	abstract <R> R accept(TypeVisitor<R> visitor);
	
	public String getName() {
		return name;
	}
	
	public static Function<Type,String> _getName = new Function<Type,String>() {
		public String apply(Type input) {
			return input.getName();
		}
	};
	
	@Override
	public String toString() {
		return name;
	}

	static class GetChildren implements TypeVisitor<List<Type>> {

		@Override
		public List<Type> visitRecord(Record type) {
			return ImmutableList.copyOf(type.getFields().values());
		}

		@Override
		public List<Type> visitVarray(Varray type) {
			return ImmutableList.of(type.getElementType());
		}

		@Override
		public List<Type> visitNestedTable(NestedTable type) {
			return ImmutableList.of(type.getElementType());
		}

		@Override
		public List<Type> visitIndexByTable(IndexByTable type) {
			return ImmutableList.of(type.getElementType());
		}

		@Override
		public List<Type> visitProcedureSignature(ProcedureSignature type) {
			return from(type.getParameters().values()).transform(Parameter._getType).toList();
		}

		@Override
		public List<Type> visitFunctionSignature(FunctionSignature type) {
			ImmutableList.Builder<Type> builder = ImmutableList.builder();
			builder.add(type.getReturnType());
			builder.addAll(from(type.getParameters().values()).transform(Parameter._getType));
			return builder.build();
		}

		@Override
		public List<Type> visitPrimitive(PrimitiveType type) {
			return ImmutableList.of();
		}
		
	}

}