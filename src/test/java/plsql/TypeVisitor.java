package plsql;

/**
 * Represents method called on particular type instance.
 * @see AbstractType#accept(TypeVisitor)
 * 
 * @author Tomas Zalusky
 */
interface TypeVisitor {

	void visitProcedureSignature(ProcedureSignature type);
	
	void visitFunctionSignature(FunctionSignature type);
	
	void visitRecord(RecordType type);

	void visitVarray(VarrayType type);
	
	void visitNestedTable(NestedTableType type);
	
	void visitIndexByTable(IndexByTableType type);
	
	void visitPrimitive(AbstractPrimitiveType type);

}
