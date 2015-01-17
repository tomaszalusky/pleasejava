package plsql;

/**
 * Represents method called on particular type instance.
 * @see AbstractType#accept(TypeVisitorAA, Object, Object)
 * 
 * @author Tomas Zalusky
 */
interface TypeVisitorAA<A1,A2> {

	void visitProcedureSignature(ProcedureSignature type, A1 arg1, A2 arg2);
	
	void visitFunctionSignature(FunctionSignature type, A1 arg1, A2 arg2);
	
	void visitRecord(RecordType type, A1 arg1, A2 arg2);

	void visitVarray(VarrayType type, A1 arg1, A2 arg2);
	
	void visitNestedTable(NestedTableType type, A1 arg1, A2 arg2);
	
	void visitIndexByTable(IndexByTableType type, A1 arg1, A2 arg2);
	
	void visitPrimitive(AbstractPrimitiveType type, A1 arg1, A2 arg2);

}
