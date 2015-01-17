package plsql;

/**
 * Represents method called on particular type instance.
 * @see AbstractType#accept(TypeVisitorR)
 * 
 * @author Tomas Zalusky
 */
interface TypeVisitorR<R> {
	
	R visitRecord(RecordType type);
	
	R visitVarray(VarrayType type);
	
	R visitNestedTable(NestedTableType type);
	
	R visitIndexByTable(IndexByTableType type);
	
	R visitProcedureSignature(ProcedureSignature type);
	
	R visitFunctionSignature(FunctionSignature type);
	
	R visitPrimitive(AbstractPrimitiveType type);

}
