package pleasejava.tools;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;
import static pleasejava.Utils.appendf;

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
	
	/**
	 * Provides all types which this type depends on.
	 * For example, all fields' types for record or element type for nested table.
	 * Returns empty collection for primitive types.
	 * @see GetChildren
	 * @return set of entries, entry key is name identifying child type in parent type (or symbolic name), entry value is child type
	 */
	final Set<Map.Entry<String,Type>> getChildren() {
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
	
	TypeNode toTypeNode() {
		ImmutableMap.Builder<String,TypeNode> childNodes = ImmutableMap.builder();
		for (Map.Entry<String,Type> e : this.getChildren()) {
			childNodes.put(e.getKey(), e.getValue().toTypeNode());
		}
		TypeNode result = new TypeNode(this,childNodes.build());
		return result;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		accept(new ToString(0,null,result));
		Type.ToString.align(result);
		return result.toString();
	}

	private static class GetChildren implements TypeVisitor<Set<Map.Entry<String,Type>>> {

		@Override
		public Set<Map.Entry<String,Type>> visitRecord(Record type) {
			return ImmutableSet.copyOf(type.getFields().entrySet());
		}

		@Override
		public Set<Map.Entry<String,Type>> visitVarray(Varray type) {
			return ImmutableSet.of(Maps.immutableEntry(Varray.ELEMENT_LABEL, type.getElementType()));
		}

		@Override
		public Set<Map.Entry<String,Type>> visitNestedTable(NestedTable type) {
			return ImmutableSet.of(Maps.immutableEntry(NestedTable.ELEMENT_LABEL, type.getElementType()));
		}

		@Override
		public Set<Map.Entry<String,Type>> visitIndexByTable(IndexByTable type) {
			return ImmutableSet.of(Maps.immutableEntry(IndexByTable.ELEMENT_LABEL, type.getElementType()));
		}

		@Override
		public Set<Map.Entry<String,Type>> visitProcedureSignature(ProcedureSignature type) {
			return ImmutableSet.copyOf(Maps.transformValues(type.getParameters(),Parameter._getType).entrySet());
		}

		@Override
		public Set<Map.Entry<String,Type>> visitFunctionSignature(FunctionSignature type) {
			ImmutableSet.Builder<Map.Entry<String,Type>> builder = ImmutableSet.builder();
			builder.add(Maps.immutableEntry(IndexByTable.ELEMENT_LABEL, type.getReturnType()));
			builder.addAll(Maps.transformValues(type.getParameters(),Parameter._getType).entrySet());
			return builder.build();
		}

		@Override
		public Set<Map.Entry<String,Type>> visitPrimitive(PrimitiveType type) {
			return ImmutableSet.of();
		}
		
	}

	/**
	 * Flexible support for toString method.
	 * @author Tomas Zalusky
	 */
	static class ToString implements TypeVisitor<Void> {

		private static final int TAB_SPACES = 2;
		
		private final int level;
		
		private final Set<Type> written;
		
		private final StringBuilder buf;
		
		/**
		 * @param level amount of indentation
		 * @param written guard set of type which have already been written in full format.
		 * Ensures only first occurence of each type is listed in full format,
		 * remaining occurences are listed only in concise format.
		 * Can be <code>null</code> for always using full format (guard disabled).
		 * @param buf buffer for result string
		 */
		ToString(int level, Set<Type> written, StringBuilder buf) {
			this.level = level;
			this.written = written;
			this.buf = buf;
		}

		private static String indent(int level) {
			return Strings.repeat(" ",level * TAB_SPACES);
		}

		private boolean checkWritten(Type type) {
			if (written == null) {
				return false;
			}
			boolean result = written.contains(type);
			if (result) {
				appendf(buf," ...");
			} else {
				written.add(type);
			}
			return result;
		}

		@Override
		public Void visitRecord(Record type) {
			appendf(buf,"record \"%s\"", type.getName());
			if (!checkWritten(type)) {
				for (Map.Entry<String,Type> entry : type.getFields().entrySet()) {
					appendf(buf,"%n%s%s ", indent(level + 1), entry.getKey());
					entry.getValue().accept(new ToString(level + 1,written,buf));
				}
			}
			return null;
		}

		@Override
		public Void visitVarray(Varray type) {
			appendf(buf,"varray \"%s\"", type.getName());
			if (!checkWritten(type)) {
				appendf(buf,"%n%s%s ", indent(level + 1), Varray.ELEMENT_LABEL);
				type.getElementType().accept(new ToString(level + 1,written,buf));
			}
			return null;
		}

		@Override
		public Void visitNestedTable(NestedTable type) {
			appendf(buf,"nestedtable \"%s\"", type.getName());
			if (!checkWritten(type)) {
				appendf(buf,"%n%s%s ", indent(level + 1), NestedTable.ELEMENT_LABEL);
				type.getElementType().accept(new ToString(level + 1,written,buf));
			}
			return null;
		}

		@Override
		public Void visitIndexByTable(IndexByTable type) {
			appendf(buf,"indexbytable \"%s\"", type.getName());
			if (!checkWritten(type)) {
				appendf(buf,"%n%s%s %s%n%s%s ", indent(level + 1), IndexByTable.KEY_LABEL,
						type.getIndexType().toString(),
						indent(level + 1), IndexByTable.ELEMENT_LABEL);
				type.getElementType().accept(new ToString(level + 1,written,buf));
			}
			return null;
		}

		@Override
		public Void visitProcedureSignature(ProcedureSignature type) {
			appendf(buf,"procedure \"%s\"", type.getName());
			if (!checkWritten(type)) {
				for (Entry<String,Parameter> entry : type.getParameters().entrySet()) {
					appendf(buf,"%n%s%s %s ", indent(level + 1), entry.getKey(), entry.getValue().getParameterMode().name().toLowerCase());
					entry.getValue().getType().accept(new ToString(level + 1,written,buf));
				}
			}
			return null;
		}

		@Override
		public Void visitFunctionSignature(FunctionSignature type) {
			appendf(buf,"function \"%s\"", type.getName());
			if (!checkWritten(type)) {
				appendf(buf,"%n%s%s ", indent(level + 1), FunctionSignature.RETURN_LABEL);
				type.getReturnType().accept(new ToString(level + 1,written,buf));
				for (Entry<String,Parameter> entry : type.getParameters().entrySet()) {
					appendf(buf,"%n%s%s %s ", indent(level + 1), entry.getKey(), entry.getValue().getParameterMode().name().toLowerCase());
					entry.getValue().getType().accept(new ToString(level + 1,written,buf));
				}
			}
			return null;
		}

		@Override
		public Void visitPrimitive(PrimitiveType type) {
			appendf(buf,"\"%s\"", type.getName()); // always written regardless guard set
			return null;
		}
		
		@Override
		public String toString() {
			return buf.toString();
		}

		/**
		 * Aligns type names into same column.
		 * @param buf
		 */
		public static void align(StringBuilder buf) {
			int lastNewline = -1;
			boolean wasFirstQuote = false;
			int maxWidth = -1;
			for (int i = 0, l = buf.length(); i < l; i++) {
				char c = buf.charAt(i);
				if (c == '\n' || c == '\r') {lastNewline = i; wasFirstQuote = false;}
				if (c == '"' && !wasFirstQuote) {wasFirstQuote = true; maxWidth = Math.max(i - lastNewline - 1,maxWidth);}
			}
			StringBuilder result = new StringBuilder();
			lastNewline = -1;
			wasFirstQuote = false;
			for (int i = 0, l = buf.length(); i < l; i++) {
				char c = buf.charAt(i);
				if (c == '\n' || c == '\r') {lastNewline = i; wasFirstQuote = false;}
				if (c == '"' && !wasFirstQuote) {wasFirstQuote = true; result.append(Strings.repeat(" ",maxWidth - (i - lastNewline - 1)));}
				result.append(c);
			}
			buf.delete(0, buf.length()).append(result);
		}
		
	}

}
