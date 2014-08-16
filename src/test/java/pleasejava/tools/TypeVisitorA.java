package pleasejava.tools;

/**
 * Represents method called on particular type instance.
 * @see Type#accept(TypeVisitorA, Object)
 * 
 * @author Tomas Zalusky
 */
interface TypeVisitorA<A> {

	void visitRecord(Record type, A arg);

	void visitVarray(Varray type, A arg);
	
	void visitNestedTable(NestedTable type, A arg);
	
	void visitIndexByTable(IndexByTable type, A arg);
	
	void visitProcedureSignature(ProcedureSignature type, A arg);
	
	void visitFunctionSignature(FunctionSignature type, A arg);
	
	void visitPrimitive(PrimitiveType type, A arg);

}
