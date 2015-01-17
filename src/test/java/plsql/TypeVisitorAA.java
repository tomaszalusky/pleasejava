package plsql;

/**
 * Represents method called on particular type instance.
 * @see Type#accept(TypeVisitorAA, Object, Object)
 * 
 * @author Tomas Zalusky
 */
interface TypeVisitorAA<A1,A2> {

	void visitProcedureSignature(ProcedureSignature type, A1 arg1, A2 arg2);
	
	void visitFunctionSignature(FunctionSignature type, A1 arg1, A2 arg2);
	
	void visitRecord(Record type, A1 arg1, A2 arg2);

	void visitVarray(Varray type, A1 arg1, A2 arg2);
	
	void visitNestedTable(NestedTable type, A1 arg1, A2 arg2);
	
	void visitIndexByTable(IndexByTable type, A1 arg1, A2 arg2);
	
	void visitPrimitive(PrimitiveType type, A1 arg1, A2 arg2);

}
