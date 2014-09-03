package pleasejava.tools;

/**
 * Represents method called on particular type instance.
 * @see Type#accept(TypeVisitorAAA, Object, Object, Object)
 * 
 * @author Tomas Zalusky
 */
interface TypeVisitorAAA<A1,A2,A3> {

	void visitRecord(Record type, A1 arg1, A2 arg2, A3 arg3);

	void visitVarray(Varray type, A1 arg1, A2 arg2, A3 arg3);
	
	void visitNestedTable(NestedTable type, A1 arg1, A2 arg2, A3 arg3);
	
	void visitIndexByTable(IndexByTable type, A1 arg1, A2 arg2, A3 arg3);
	
	void visitProcedureSignature(ProcedureSignature type, A1 arg1, A2 arg2, A3 arg3);
	
	void visitFunctionSignature(FunctionSignature type, A1 arg1, A2 arg2, A3 arg3);
	
	void visitPrimitive(PrimitiveType type, A1 arg1, A2 arg2, A3 arg3);

}
