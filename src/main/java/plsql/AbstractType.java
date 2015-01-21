package plsql;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pleasejava.Utils;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Ancestor of all PLSQL types.
 * 
 * @author Tomas Zalusky
 */
abstract class AbstractType {
	
	final String name;
	
	final Annotation annotation;
	
	static abstract class TypeAnnotationStringConverter<A extends Annotation> {
		
		public abstract String toString(A input);
		
		@SuppressWarnings("unchecked")
		public final String toStringErased(Annotation input) {
			return toString((A)input);
		}
		
		public abstract A fromString(String input);
		
	}
	
	AbstractType(Annotation annotation) {
		this.annotation = annotation;
		Class<? extends Annotation> annotationClass = annotation.getClass();
		plsql.Type typeAnnotation = (annotationClass.isAnnotation() ? annotationClass : annotationClass.getInterfaces()[0]).getAnnotation(plsql.Type.class); // false for mocked annotations
		Class<? extends TypeAnnotationStringConverter<? extends Annotation>> nameConverterClass = typeAnnotation.nameConverter();
		TypeAnnotationStringConverter<? extends Annotation> nameConverter;
		try {
			nameConverter = nameConverterClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw Throwables.propagate(e);
		}
		this.name = nameConverter.toStringErased(annotation);
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
	final Map<String,AbstractType> getChildren() {
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
	
	String getName() {
		return name;
	}
	
	static Function<AbstractType,String> _getName = new Function<AbstractType,String>() {
		public String apply(AbstractType input) {
			return input.getName();
		}
	};
	
	TypeNode toTypeNode(TypeNode parent, int orderInParent) {
		TypeNode result = new TypeNode(this,parent,orderInParent);
		ImmutableMap.Builder<String,TypeNode> children = ImmutableMap.builder();
		int o = 0;
		for (Map.Entry<String,AbstractType> e : this.getChildren().entrySet()) {
			children.put(e.getKey(), e.getValue().toTypeNode(result, o++));
		}
		result.setChildren(children.build());
		return result;
	}

	@Override
	public String toString() {
		ToString visitor = new ToString(null);
		accept(visitor,0);
		String result = visitor.toString();
		return result;
	}

	private static class GetChildren implements TypeVisitorR<Map<String,AbstractType>> {

		@Override
		public Map<String,AbstractType> visitProcedureSignature(ProcedureSignature type) {
			return ImmutableMap.copyOf(Maps.transformValues(type.getParameters(),Parameter._getType));
		}

		@Override
		public Map<String,AbstractType> visitFunctionSignature(FunctionSignature type) {
			ImmutableMap.Builder<String,AbstractType> builder = ImmutableMap.builder();
			builder.put(FunctionSignature.RETURN_LABEL, type.getReturnType());
			builder.putAll(Maps.transformValues(type.getParameters(),Parameter._getType));
			return builder.build();
		}

		@Override
		public Map<String,AbstractType> visitRecord(RecordType type) {
			return ImmutableMap.copyOf(type.getFields());
		}

		@Override
		public Map<String,AbstractType> visitVarray(VarrayType type) {
			return ImmutableMap.of(VarrayType.ELEMENT_LABEL, type.getElementType());
		}

		@Override
		public Map<String,AbstractType> visitNestedTable(NestedTableType type) {
			return ImmutableMap.of(NestedTableType.ELEMENT_LABEL, type.getElementType());
		}

		@Override
		public Map<String,AbstractType> visitIndexByTable(IndexByTableType type) {
			return ImmutableMap.of(IndexByTableType.ELEMENT_LABEL, type.getElementType());
		}

		@Override
		public Map<String,AbstractType> visitPrimitive(AbstractPrimitiveType type) {
			return ImmutableMap.of();
		}
		
	}

	/**
	 * Flexible support for toString method.
	 * @author Tomas Zalusky
	 */
	static class ToString extends Utils.ToStringSupport implements TypeVisitorA<Integer> {

		private final Set<AbstractType> written;
		
		/**
		 * @param written guard set of type which have already been written in full format.
		 * Ensures only first occurence of each type is listed in full format,
		 * remaining occurences are listed only in concise format.
		 * Can be <code>null</code> for always using full format (guard disabled).
		 */
		ToString(Set<AbstractType> written) {
			this.written = written;
		}

		private boolean checkWritten(AbstractType type) {
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
			newLine().append("procedure").append("\"" + type.getName() + "\"");
			if (!checkWritten(type)) {
				for (Entry<String,Parameter> entry : type.getParameters().entrySet()) {
					newLine().append(indent(level + 1) + entry.getKey() + " " + entry.getValue().getParameterMode().name().toLowerCase() + " ");
					entry.getValue().getType().accept(this,level + 1);
				}
			}
		}

		@Override
		public void visitFunctionSignature(FunctionSignature type, Integer level) {
			newLine().append("function").append("\"" + type.getName() + "\"");
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
		public void visitRecord(RecordType type, Integer level) {
			appendToLastCell("record").append("\"" + type.getName() + "\"");
			if (!checkWritten(type)) {
				for (Map.Entry<String,AbstractType> entry : type.getFields().entrySet()) {
					newLine().append(indent(level + 1) + entry.getKey() + " ");
					entry.getValue().accept(this,level + 1);
				}
			}
		}

		@Override
		public void visitVarray(VarrayType type, Integer level) {
			appendToLastCell("varray").append("\"" + type.getName() + "\"");
			if (!checkWritten(type)) {
				newLine().append(indent(level + 1) + VarrayType.ELEMENT_LABEL + " ");
				type.getElementType().accept(this,level + 1);
			}
		}

		@Override
		public void visitNestedTable(NestedTableType type, Integer level) {
			appendToLastCell("nestedtable").append("\"" + type.getName() + "\"");
			if (!checkWritten(type)) {
				newLine().append(indent(level + 1) + NestedTableType.ELEMENT_LABEL + " ");
				type.getElementType().accept(this,level + 1);
			}
		}

		@Override
		public void visitIndexByTable(IndexByTableType type, Integer level) {
			appendToLastCell("indexbytable").append("\"" + type.getName() + "\"");
			if (!checkWritten(type)) {
				newLine().append(indent(level + 1) + IndexByTableType.KEY_LABEL).append(type.getIndexType().toString())
				.newLine().append(indent(level + 1) + IndexByTableType.ELEMENT_LABEL + " ");
				type.getElementType().accept(this,level + 1);
			}
		}

		@Override
		public void visitPrimitive(AbstractPrimitiveType type, Integer level) {
			append("\"" + type.getName() + "\""); // always written regardless guard set
		}
		
	}

}
