package plsql;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;

/**
 * Represents simple types (scalar and LOB according to http://docs.oracle.com/cd/A97630_01/appdev.920/a96624/03_types.htm ).
 * 
 * @author Tomas Zalusky
 */
class AbstractPrimitiveType extends AbstractType {
	
	AbstractPrimitiveType(Annotation annotation) {
		super(annotation);
	}
	
	<R> R accept(TypeVisitorR<R> visitor) {
		return visitor.visitPrimitive(this);
	}

	<A> void accept(TypeVisitorA<A> visitor, A arg) {
		visitor.visitPrimitive(this, arg);
	}
	
	<A1,A2> void accept(TypeVisitorAA<A1,A2> visitor, A1 arg1, A2 arg2) {
		visitor.visitPrimitive(this, arg1, arg2);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof AbstractPrimitiveType)) return false;
		AbstractPrimitiveType that = (AbstractPrimitiveType)obj;
		boolean result = Objects.equals(this.name,that.name);
		return result;
	}

	<A1,A2,A3> void accept(TypeVisitorAAA<A1,A2,A3> visitor, A1 arg1, A2 arg2, A3 arg3) {
		visitor.visitPrimitive(this, arg1, arg2, arg3);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name);
	}

	private static final List<AnnotationToConstructor<?,?>> CONSTRUCTORS = ImmutableList.of(
			new AnnotationToConstructor<>(new BinaryIntegerType.StringConverter(), BinaryIntegerType::new),
			new AnnotationToConstructor<>(new BlobType         .StringConverter(), BlobType         ::new),
			new AnnotationToConstructor<>(new BooleanType      .StringConverter(), BooleanType      ::new),
			new AnnotationToConstructor<>(new CharType         .StringConverter(), CharType         ::new),
			new AnnotationToConstructor<>(new ClobType         .StringConverter(), ClobType         ::new),
			new AnnotationToConstructor<>(new DateType         .StringConverter(), DateType         ::new),
			new AnnotationToConstructor<>(new IntegerType      .StringConverter(), IntegerType      ::new),
			new AnnotationToConstructor<>(new LongType         .StringConverter(), LongType         ::new),
			new AnnotationToConstructor<>(new NumberType       .StringConverter(), NumberType       ::new),
			new AnnotationToConstructor<>(new PlsIntegerType   .StringConverter(), PlsIntegerType   ::new),
			new AnnotationToConstructor<>(new Varchar2Type     .StringConverter(), Varchar2Type     ::new),
			new AnnotationToConstructor<>(new StringType       .StringConverter(), StringType       ::new)
	);
	
	private static final class AnnotationToConstructor <T extends AbstractPrimitiveType,A extends Annotation> {
		private final AbstractType.TypeAnnotationStringConverter<A> converter;
		private Function<A,T> typeConstructor;
		AnnotationToConstructor(AbstractType.TypeAnnotationStringConverter<A> converter, Function<A,T> typeConstructor) {
			this.converter = converter;
			this.typeConstructor = typeConstructor;
		}
		T toType(String name) {
			A a = converter.fromString(name);
			T result = a == null ? null : typeConstructor.apply(a);
			return result;
		}
	}

	static AbstractType recognizePrimitiveType(String name) {
		AbstractType result = null;
		for (AnnotationToConstructor<?,?> h : CONSTRUCTORS) {
			result = h.toType(name);
			if (result != null) break;
		}
		return result;
	}
	
}
