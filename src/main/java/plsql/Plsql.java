package plsql;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Provides Java annotations for marking Java counterparts of PLSQL constructs.
 * </p>
 * <p>
 * <a name="plsql_names">
 * Note to PLSQL names: If value attribute represents name of PLSQL construct (procedure, function or type),
 * then value without dot represents top-level PLSQL construct,
 * value in the form "pkg.name" represents package-level PLSQL construct.
 * Values in any other forms are illegal.
 * </p>
 * <p>
 * <em>Java naming convention note: all Java identifiers have same name as their PLSQL counterparts where possible,
 * preserving Java code conventions. For PLSQL construct whose name is Java keyword or is in java.lang package,
 * additional underscore is added at the end.
 * </em>
 * </p>
 * 
 * @author Tomas Zalusky
 */
public class Plsql {

	@Target(ElementType.ANNOTATION_TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Type {
		Class<? extends TypeAnnotationStringConverter<? extends Annotation>> nameConverter();
	}
	
	/**
	 * Interface annotated with this annotation represents PLSQL package.
	 * If Java construct (method or class) is directly or indirectly (via declaring class) placed in {@link Package_}-annotated interface,
	 * then the method or class is treated as packaged PLSQL construct
	 * with package inferred from innermost {@link Package_}-annotated type,
	 * otherwise it's treated as top level PLSQL construct.
	 * @author Tomas Zalusky
	 */
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Deprecated
	public @interface Package_ {
		
		/**
		 * Name of PLSQL package. If not specified, TODO name is inferred from Java interface name using name conversion strategy.  
		 * @return
		 */
		String value() default "";
		
	}

	/**
	 * Method annotated with this annotation represents PLSQL procedure.
	 * Each parameter must be annotated with corresponding PLSQL type annotation. TODO may be not, if default PLSQL can be inferred from Java type
	 * Each OUT and IN OUT parametr must also be annotated with {@link Out} or {@link InOut} annotation, respectively.
	 * @author Tomas Zalusky
	 */
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@Type(nameConverter=Procedure.StringConverter.class)
	public @interface Procedure {

		/**
		 * Name of PLSQL procedure.
		 * @return name in <a href="#plsql_names">top-level or package-level form</a>
		 */
		String value();
		
		static class StringConverter extends TypeAnnotationStringConverter<Procedure> {

			@Override
			public String toString(Procedure a) {
				return a.value();
			}

			@Override
			public Procedure fromString(String input) {
				return new Plsql.Procedure() {
					@Override
					public Class<? extends Annotation> annotationType() {
						return Procedure.class;
					}
					@Override
					public String value() {
						return input;
					}
				};
			}
			
		}

	}
	
	/**
	 * Method annotated with this annotation represents PLSQL function.
	 * Each parameter must be annotated with corresponding PLSQL type annotation
	 * (for describing return type use annotation just on method). TODO may be not, if default PLSQL can be inferred from Java type
	 * Each OUT and IN OUT parameter must also be annotated with {@link Out} or {@link InOut} annotation, respectively.
	 * @author Tomas Zalusky
	 */
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@Type(nameConverter=Function.StringConverter.class)
	public @interface Function {
		
		/**
		 * Name of PLSQL function.
		 * @return name in <a href="#plsql_names">top-level or package-level form</a>
		 */
		String value();
		
		static class StringConverter extends TypeAnnotationStringConverter<Function> {

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

	}
	
	/**
	 * Class annotated with this annotation represents PLSQL record.
	 * Each field must be annotated with corresponding PLSQL type annotation. TODO Inherited?
	 * @author Tomas Zalusky
	 */
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Record {
		
		/**
		 * Name of PLSQL record.
		 * @return name in <a href="#plsql_names">top-level or package-level form</a>
		 */
		String value();
		
	}
	
	/**
	 * Class field or method parameter annotated with this annotation represents PLSQL nested table
	 * as a member of record or parameter of procedure or function, respectively.
	 * PLSQL collections don't require user-written Java constructs as executables or records do,
	 * they just map onto Java collections and maps.
	 * @author Tomas Zalusky
	 */
	@Target({ElementType.FIELD,ElementType.PARAMETER})
	@Retention(RetentionPolicy.RUNTIME)
	@Type(nameConverter=NestedTable.StringConverter.class)
	public @interface NestedTable {
		
		/**
		 * Name of PLSQL nested table.
		 * @return name in <a href="#plsql_names">top-level or package-level form</a>
		 */
		String value();
		
		static class StringConverter extends TypeAnnotationStringConverter<NestedTable> {

			@Override
			public String toString(NestedTable a) {
				return a.value();
			}

			@Override
			public NestedTable fromString(String input) {
				return new Plsql.NestedTable() {
					@Override
					public Class<? extends Annotation> annotationType() {
						return NestedTable.class;
					}
					@Override
					public String value() {
						return input;
					}
				};
			}
			
		}

	}
	
	/**
	 * TODO
	 * @author Tomas Zalusky
	 */
	@Target({ElementType.FIELD,ElementType.PARAMETER})
	@Retention(RetentionPolicy.RUNTIME)
	@Type(nameConverter=Varray.StringConverter.class)
	public @interface Varray {
		
		/**
		 * Name of PLSQL varray.
		 * @return name in <a href="#plsql_names">top-level or package-level form</a>
		 */
		String value();

		static class StringConverter extends TypeAnnotationStringConverter<Varray> {

			@Override
			public String toString(Varray a) {
				return a.value();
			}

			@Override
			public Varray fromString(String input) {
				return new Plsql.Varray() {
					@Override
					public Class<? extends Annotation> annotationType() {
						return Varray.class;
					}
					@Override
					public String value() {
						return input;
					}
				};
			}
			
		}

	}
	
	/**
	 * TODO
	 * @author Tomas Zalusky
	 */
	@Target({ElementType.FIELD,ElementType.PARAMETER})
	@Retention(RetentionPolicy.RUNTIME)
	@Type(nameConverter=IndexByTable.StringConverter.class)
	public @interface IndexByTable {
		
		/**
		 * Name of PLSQL index-by table.
		 * @return name in <a href="#plsql_names">top-level or package-level form</a>
		 */
		String value();
		
		static class StringConverter extends TypeAnnotationStringConverter<IndexByTable> {

			@Override
			public String toString(IndexByTable a) {
				return a.value();
			}

			@Override
			public IndexByTable fromString(String input) {
				return new Plsql.IndexByTable() {
					@Override
					public Class<? extends Annotation> annotationType() {
						return IndexByTable.class;
					}
					@Override
					public String value() {
						return input;
					}
				};
			}
			
		}

	}
	
	/**
	 * Represents PLSQL VARCHAR2 type.
	 * @author Tomas Zalusky
	 */
	@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	//@Type(nameTemplate="varchar2($value)")
	public @interface Varchar2 {
		
		int value();
		
	}
	
	/**
	 * Represents PLSQL VARCHAR type.
	 * @author Tomas Zalusky
	 */
	@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Varchar {
		
		int value();
		
	}
	
	/**
	 * Represents PLSQL STRING type.
	 * @author Tomas Zalusky
	 */
	@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface String_ {
		
		int value();
		
	}
	
	/**
	 * Represents PLSQL BLOB type.
	 * @author Tomas Zalusky
	 */
	@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Blob {}
	
	/**
	 * Represents PLSQL CHAR type.
	 * @author Tomas Zalusky
	 */
	@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Char_ {
		
		int value();
		
	}
 	
	/**
	 * Represents PLSQL CLOB type.
	 * @author Tomas Zalusky
	 */
	@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Clob {}
	
	/**
	 * Represents PLSQL DATE type.
	 * @author Tomas Zalusky
	 */
	@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Date {
		
		int value();
		
	}
	
	/**
	 * Represents PLSQL LONG type.
	 * @author Tomas Zalusky
	 */
	@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Long_ {}
	
	/**
	 * Represents PLSQL NUMBER type.
	 * @author Tomas Zalusky
	 */
	@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Number_ {
		
		/**
		 * @return precision
		 */
		int value();
		
		int scale();
		
	}
	
	/**
	 * Represents PLSQL INTEGER type.
	 * @author Tomas Zalusky
	 */
	@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Integer_ {}
	
	/**
	 * Represents PLSQL PLS_INTEGER type.
	 * @author Tomas Zalusky
	 */
	@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface PlsInteger {}

	/**
	 * Represents PLSQL BINARY_INTEGER type.
	 * @author Tomas Zalusky
	 */
	@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface BinaryInteger {}
	
	/**
	 * Indicates that method parameter annotated with this annotation represents PLSQL OUT parameter.
	 * @author Tomas Zalusky
	 */
	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Out {}
	
	/**
	 * Indicates that method parameter annotated with this annotation represents PLSQL IN OUT parameter.
	 * @author Tomas Zalusky
	 */
	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface InOut {}
	
	
	public static abstract class TypeAnnotationStringConverter<A extends Annotation> { //TODO make package visible after moving types into plsql package
		
		public abstract String toString(A input);
		
		@SuppressWarnings("unchecked")
		public final String toStringErased(Annotation input) {
			return toString((A)input);
		}
		
		public abstract A fromString(String input);
		
	}
	

}


/**
@PlsqlInteger(precision = 38)

@PlsqlBoolean



@PlsqlRecord("ipmg_port.ds_data")
class IpmtPort {

	@PlsqlInteger
	private BigDecimal id;
	
	@PlsqlNestedTable("ipmg_prop.dci_data")
	private List<IpmtProp> propojeniList;

}



@PlsqlPackage
interface IpmgPortBl {

	@PlsqlProcedure
	void p_ins(IpmtPort port);

	@PlsqlFunction(returnType = PlsqlBoolean.class nepovinne)
	boolean f_over(IpmtPort port);

	@PlsqlRecord
	class DsData {

		@PlsqlInteger
		private BigDecimal id;
		
		@PlsqlNestedTable("ipmg_prop.dci_data")
		private List<IpmtProp> propojeniList;

	}

}

class Service {

	@Autowired
	Plsql plsql;

	void method() {
		plsql.execute(IpmgPortBl.class).p_ins(port);
		Plsql.plsql(IpmgPortBl.class).p_ins(port);
		Plsql.execute(IpmgPortBl.class).p_ins(port);
	}

}

Port port = plsql.package_(IpmgProp.class).f_dej(portId);

Port port = plsql(IpmtProp.class).f_dej(portId);

Out<Integer> result = plsql.out(Integer.class);
plsql(ImpgProp.class).some_function(1,



@PlsqlPackage(name = "ipmg_prop")
interface IpmgProp {

	@PlsqlFunction(resultType = @PlsqlRecord ?)
	Port f_dej(BigDecimal id);

	@PlsqlFunction
	int some_function(int x, Out<Integer> result);

	@PlsqlProcedure
	void override(String arg)

	@PlsqlProcedure
	void override(int arg)

	
}


@PlsqlRecord("ipmg_port_bl.ds_data")
class Port {

	@PlsqlInteger(name = "id")
	private int foo;

	@PlsqlNestedTable(name = "spicky")
	private List<Spicka>
}



Idea: Verejne API knihovny obsahuje pouze 
- metodu pro zawrapovani proxy
- wrapper OUT parametru
- anotace pro jednotlive PLSQL typy
@PlsqlInteger
@PlsqlBoolean
@PlsqlVarray
@PlsqlNestedTable
@PlsqlIndexByTable
@PlsqlRecord
@PlsqlVarchar2



@Package
interface IpmgPortBl {

	@Procedure
	void p_ins(IpmtPort port);

	@Function
	@Boolean_
	boolean f_over(@Out IpmtPortDsData port, @NestedTable("...") List<IpmtPort> propojeniList);

	@Record
	class IpmtPortDsData {

		@Integer_
		private BigDecimal id;
		
		@NestedTable("ipmg_prop.dci_data")
		private List<IpmtProp> propojeniList;

		@NestedTable("ipmg_prop.dci_data")
		@NestedTable("");
		private List<List<IpmtProp>> propojeniList;

	}

}

nst1
	nst2
		ibt3
			nst4
				var5
					integer
.           .    .          .    .    .
Map<Integer,List<Map<String,List<List<Integer>>>>>

JDK8
@NestedTable(package="pkg",name="nst1",level=1)
@NestedTable(package="pkg",name="nst2",level=2)
@IndexByTable(package="pkg",name="ibt3",level=3)
@Varchar2(level=3)
@NestedTable(package="pkg",name="nst4",level=4)
@Varray(package="pkg",name="var5",level=5)
@Integer_(level=6)

JDK7
@NestedTables({
	@NestedTable(package="pkg",name="nst1",level=1),
	@NestedTable(package="pkg",name="nst2",level=2),
	@NestedTable(package="pkg",name="nst4",level=4),
})
@Varchar2(level=3)
@IndexByTable(package="pkg",name="ibt3",level=3)
@Varray(package="pkg",name="var5",level=5)
@Integer_(level=6)

JDK8+APT

@NestedTable(package="pkg",name="nst1") Map<Integer,
	@NestedTable(package="pkg",name="nst2") List<
		@IndexByTable(package="pkg",name="ibt3") Map<@Varchar2 String,
			@NestedTable(package="pkg",name="nst4") List<
				@Varray(package="pkg",name="var5") List<
					@Integer_ Integer
				>
			>
		>
	>
>

ibt of Record indexed by varchar2

Map<@Varchar2(10) String,Record>
Map<@Plsql.Varchar2(10) String,Record>


CONVENTION OVER CONFIGURATION!

*/
