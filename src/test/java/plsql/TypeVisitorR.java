package plsql;

/**
 * Represents method called on particular type instance.
 * @see Type#accept(TypeVisitorR)
 * 
 * @author Tomas Zalusky
 */
interface TypeVisitorR<R> {
	
	R visitRecord(Record type);
	
	R visitVarray(Varray type);
	
	R visitNestedTable(NestedTable type);
	
	R visitIndexByTable(IndexByTable type);
	
	R visitProcedureSignature(ProcedureSignature type);
	
	R visitFunctionSignature(FunctionSignature type);
	
	R visitPrimitive(PrimitiveType type);

}
