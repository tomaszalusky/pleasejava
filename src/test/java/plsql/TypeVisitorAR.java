package plsql;

/**
 * Represents method called on particular type instance.
 * @see AbstractType#accept(TypeVisitorAR, Object)
 * 
 * @author Tomas Zalusky
 */
interface TypeVisitorAR<A,R> {

	R visitProcedureSignature(ProcedureSignature type, A arg);
	
	R visitFunctionSignature(FunctionSignature type, A arg);
	
	R visitRecord(RecordType type, A arg);

	R visitVarray(VarrayType type, A arg);
	
	R visitNestedTable(NestedTableType type, A arg);
	
	R visitIndexByTable(IndexByTableType type, A arg);
	
	R visitPrimitive(AbstractPrimitiveType type, A arg);

}
