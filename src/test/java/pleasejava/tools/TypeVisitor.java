package pleasejava.tools;

interface TypeVisitor<R> {
	R visitRecord(Record type);
	R visitVarray(Varray type);
	R visitNestedTable(NestedTable type);
	R visitIndexByTable(IndexByTable type);
	R visitProcedureSignature(ProcedureSignature type);
	R visitFunctionSignature(FunctionSignature type);
	R visitPrimitive(PrimitiveType type);
}