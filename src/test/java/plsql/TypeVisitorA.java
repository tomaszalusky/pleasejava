package plsql;

/**
 * Represents method called on particular type instance.
 * @see AbstractType#accept(TypeVisitorA, Object)
 * 
 * @author Tomas Zalusky
 */
interface TypeVisitorA<A> {

	void visitProcedureSignature(ProcedureSignature type, A arg);
	
	void visitFunctionSignature(FunctionSignature type, A arg);
	
	void visitRecord(RecordType type, A arg);

	void visitVarray(VarrayType type, A arg);
	
	void visitNestedTable(NestedTableType type, A arg);
	
	void visitIndexByTable(IndexByTableType type, A arg);
	
	void visitPrimitive(AbstractPrimitiveType type, A arg);

}
