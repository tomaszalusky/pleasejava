package pleasejava.tools;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pleasejava.Utils;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

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
	
	boolean isJdbcTransferrable() { // TODO temporary implementation - package types are not transferrable while toplevel and primitive type are
		return !name.contains(".");
	}
	
	/**
	 * Provides all types which this type depends on.
	 * For example, all fields' types for record or element type for nested table.
	 * Returns empty collection for primitive types.
	 * @see GetChildren
	 * @return set of entries, entry key is name identifying child type in parent type (or symbolic name), entry value is child type
	 */
	final Map<String,Type> getChildren() {
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
	abstract <R> R accept(TypeVisitorR<R> visitor);
	
	abstract <A> void accept(TypeVisitorA<A> visitor, A arg);
	
	abstract <A1,A2> void accept(TypeVisitorAA<A1,A2> visitor, A1 arg1, A2 arg2);
	
	abstract <A1,A2,A3> void accept(TypeVisitorAAA<A1,A2,A3> visitor, A1 arg1, A2 arg2, A3 arg3);
	
	public String getName() {
		return name;
	}
	
	public static Function<Type,String> _getName = new Function<Type,String>() {
		public String apply(Type input) {
			return input.getName();
		}
	};
	
	TypeNode toTypeNode(TypeNode parent, int orderInParent) {
		TypeNode result = new TypeNode(this,parent,orderInParent);
		ImmutableMap.Builder<String,TypeNode> children = ImmutableMap.builder();
		int o = 0;
		for (Map.Entry<String,Type> e : this.getChildren().entrySet()) {
			children.put(e.getKey(), e.getValue().toTypeNode(result, o++));
		}
		result.setChildren(children.build());
		return result;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		ToString visitor = new ToString(result,null);
		accept(visitor,0);
		//Utils.align(result);
		return visitor.toString();
	}

	private static class GetChildren implements TypeVisitorR<Map<String,Type>> {

		@Override
		public Map<String,Type> visitProcedureSignature(ProcedureSignature type) {
			return ImmutableMap.copyOf(Maps.transformValues(type.getParameters(),Parameter._getType));
		}

		@Override
		public Map<String,Type> visitFunctionSignature(FunctionSignature type) {
			ImmutableMap.Builder<String,Type> builder = ImmutableMap.builder();
			builder.put(FunctionSignature.RETURN_LABEL, type.getReturnType());
			builder.putAll(Maps.transformValues(type.getParameters(),Parameter._getType));
			return builder.build();
		}

		@Override
		public Map<String,Type> visitRecord(Record type) {
			return ImmutableMap.copyOf(type.getFields());
		}

		@Override
		public Map<String,Type> visitVarray(Varray type) {
			return ImmutableMap.of(Varray.ELEMENT_LABEL, type.getElementType());
		}

		@Override
		public Map<String,Type> visitNestedTable(NestedTable type) {
			return ImmutableMap.of(NestedTable.ELEMENT_LABEL, type.getElementType());
		}

		@Override
		public Map<String,Type> visitIndexByTable(IndexByTable type) {
			return ImmutableMap.of(IndexByTable.ELEMENT_LABEL, type.getElementType());
		}

		@Override
		public Map<String,Type> visitPrimitive(PrimitiveType type) {
			return ImmutableMap.of();
		}
		
	}

	/**
	 * Flexible support for toString method.
	 * @author Tomas Zalusky
	 */
	static class ToString extends Utils.ToStringSupport implements TypeVisitorA<Integer> {

		private final Set<Type> written;
		
		/**
		 * @param buf buffer for result string
		 * @param written guard set of type which have already been written in full format.
		 * Ensures only first occurence of each type is listed in full format,
		 * remaining occurences are listed only in concise format.
		 * Can be <code>null</code> for always using full format (guard disabled).
		 */
		ToString(StringBuilder buf, Set<Type> written) {
			super(buf);
			this.written = written;
		}

		private boolean checkWritten(Type type) {
			if (written == null) {
				return false;
			}
			boolean result = written.contains(type);
			if (result) {
				appendToLastCell(" ...");
			} else {
				written.add(type);
			}
			return result;
		}

		@Override
		public void visitProcedureSignature(ProcedureSignature type, Integer level) {
			appendToLastCell("procedure").append("\"" + type.getName() + "\"");
			if (!checkWritten(type)) {
				for (Entry<String,Parameter> entry : type.getParameters().entrySet()) {
					newLine().append(indent(level + 1) + entry.getKey() + " " + entry.getValue().getParameterMode().name().toLowerCase() + " ");
					entry.getValue().getType().accept(this,level + 1);
				}
			}
		}

		@Override
		public void visitFunctionSignature(FunctionSignature type, Integer level) {
			appendToLastCell("function").append("\"" + type.getName() + "\"");
			if (!checkWritten(type)) {
				newLine().append(indent(level + 1) + FunctionSignature.RETURN_LABEL + " ");
				type.getReturnType().accept(this,level + 1);
				for (Entry<String,Parameter> entry : type.getParameters().entrySet()) {
					newLine().append(indent(level + 1) + entry.getKey() + " " + entry.getValue().getParameterMode().name().toLowerCase() + " ");
					entry.getValue().getType().accept(this,level + 1);
				}
			}
		}

		@Override
		public void visitRecord(Record type, Integer level) {
			appendToLastCell("record").append("\"" + type.getName() + "\"");
			if (!checkWritten(type)) {
				for (Map.Entry<String,Type> entry : type.getFields().entrySet()) {
					newLine().append(indent(level + 1) + entry.getKey() + " ");
					entry.getValue().accept(this,level + 1);
				}
			}
		}

		@Override
		public void visitVarray(Varray type, Integer level) {
			appendToLastCell("varray").append("\"" + type.getName() + "\"");
			if (!checkWritten(type)) {
				newLine().append(indent(level + 1) + Varray.ELEMENT_LABEL + " ");
				type.getElementType().accept(this,level + 1);
			}
		}

		@Override
		public void visitNestedTable(NestedTable type, Integer level) {
			appendToLastCell("nestedtable").append("\"" + type.getName() + "\"");
			if (!checkWritten(type)) {
				newLine().append(indent(level + 1) + NestedTable.ELEMENT_LABEL + " ");
				type.getElementType().accept(this,level + 1);
			}
		}

		@Override
		public void visitIndexByTable(IndexByTable type, Integer level) {
			appendToLastCell("indexbytable").append("\"" + type.getName() + "\"");
			if (!checkWritten(type)) {
				newLine().append(indent(level + 1) + IndexByTable.KEY_LABEL).append(type.getIndexType().toString())
				.newLine().append(indent(level + 1) + IndexByTable.ELEMENT_LABEL + " ");
				type.getElementType().accept(this,level + 1);
			}
		}

		@Override
		public void visitPrimitive(PrimitiveType type, Integer level) {
			append("\"" + type.getName() + "\""); // always written regardless guard set
		}
		
	}

}
