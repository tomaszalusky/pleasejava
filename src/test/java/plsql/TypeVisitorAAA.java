package plsql;

/**
 * Represents method called on particular type instance.
 * @see AbstractType#accept(TypeVisitorAAA, Object, Object, Object)
 * 
 * @author Tomas Zalusky
 */
interface TypeVisitorAAA<A1,A2,A3> {

	void visitProcedureSignature(ProcedureSignature type, A1 arg1, A2 arg2, A3 arg3);
	
	void visitFunctionSignature(FunctionSignature type, A1 arg1, A2 arg2, A3 arg3);
	
	void visitRecord(RecordType type, A1 arg1, A2 arg2, A3 arg3);

	void visitVarray(VarrayType type, A1 arg1, A2 arg2, A3 arg3);
	
	void visitNestedTable(NestedTableType type, A1 arg1, A2 arg2, A3 arg3);
	
	void visitIndexByTable(IndexByTableType type, A1 arg1, A2 arg2, A3 arg3);
	
	void visitPrimitive(AbstractPrimitiveType type, A1 arg1, A2 arg2, A3 arg3);

}
