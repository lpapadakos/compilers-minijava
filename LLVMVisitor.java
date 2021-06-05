import java.io.BufferedWriter;
import java.util.*;

import syntaxtree.*;
import visitor.*;
import symbol.*;

class Session {
	private String className;
	private String methodName;
	private String objectType;
	private List<String> callArgs = new ArrayList<>();

	public Session(String className) {
		this(className, null);
	}

	public Session(String className, String methodName) {
		this.className = className;
		setMethod(methodName);
	}

	public void setMethod(String methodName) {
		this.methodName = methodName;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public void addCallArg(String argExpr) {
		callArgs.add(argExpr);
	}

	public String getClassName() {
		return className;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getObjectType() {
		return objectType;
	}

	public List<String> getCallArgs() {
		return callArgs;
	}
}
public class LLVMVisitor extends GJDepthFirst<String, Session> {
	private final SymbolTable symbols;
	private final BufferedWriter ll;
	private final Counters counters = new Counters();

	private class Counters {
		private int register;

		/* Labels */
		private int arr_alloc;
		private int oob;
		private int _if;
		private int loop;
		private int clause;
		private int exit;

		public Counters() {
			reset();
		}

		public void reset() {
			register = 0;
			arr_alloc = 0;
			oob = 0;
			_if = 0;
			loop = 0;
			clause = 0;
			exit = 0;
		}

		public String nextRegister() {
			return "%_" + register++;
		}

		//tODO is alloc label needed?
		public String nextArray() {
			return "alloc" + arr_alloc++;
		}

		public String[] nextOob() {
			return new String[] { "not_oob" + oob, "oob" + oob++ };
		}

		public String[] nextIf() {
			return new String[] { "if" + _if, "else" + _if++ };
		}

		public String[] nextLoop() {
			return new String[] { "loop_start" + loop, "loop_body" + loop++ };
		}

		public String nextClause() {
			return "clause" + clause++;
		}

		public String nextExit() {
			return "exit" + exit++;
		}
	};

	public LLVMVisitor(SymbolTable symbols, BufferedWriter ll) throws Exception {
		this.symbols = symbols;
		this.ll = ll;

		emit("; Helper functions\n" +
		     "declare i8* @calloc(i32, i32)\n" +
		     "declare i32 @printf(i8*, ...)\n" +
		     "declare void @exit(i32)\n\n" +

		     "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n" +
		     "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n" +
		     "define void @print_int(i32 %i) {\n" +
		     "\t%_str = bitcast [4 x i8]* @_cint to i8*\n" +
		     "\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" +
		     "\tret void\n" +
		     "}\n\n" +

		     "define void @throw_oob() {\n" +
		     "\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n" +
		     "\tcall i32 (i8*, ...) @printf(i8* %_str)\n" +
		     "\tcall void @exit(i32 1)\n" +
		     "\tret void\n" +
		     "}\n");
	}

	private String llType(String miniJavaType) {
		switch (miniJavaType) {
			case "boolean":
				return "i1";

			case "int":
				return "i32";

			case "int[]":
				return "i32*";

			default:
				return "i8*";
		}
	}

	private String llNull(String miniJavaType) {
		String llTypeStr = llType(miniJavaType);

		if (llTypeStr.equals("i1"))
			return "false";
		else if (llTypeStr.contains("*"))
			return "null";
		else
			return "0";
	}

	private void emit(String line) throws Exception {
		ll.write(line);
		ll.newLine();
	}

	private void emit_vtable(String className) throws Exception {
		ClassSymbol thisClass = symbols.getClass(className);

		if (thisClass == null)
			return;

		emit("@" + className + "_vtable = global [" + thisClass.getMethodsAmount() + " x i8*] [");

		StringBuilder methods = new StringBuilder();
		for (MethodSymbol method: thisClass.getMethods()) {
			methods.append("\ti8* bitcast (" + llType(method.getType()) + "(i8*");

			for (Symbol parameter: method.getParameters())
				methods.append(", " + llType(parameter.getType()));

			methods.append(")* @" + method.getOwner().getName() + '.' + method.getName() + " to i8*),\n");
		}

		/* Delete trailing comma */
		methods.delete(methods.length() - 2, methods.length());

		emit(methods.toString());
		emit("]\n");
	}

	/**
	 * f0 -> "class"
	 * f1 -> Identifier()
	 * f2 -> "{"
	 * f3 -> "public"
	 * f4 -> "static"
	 * f5 -> "void"
	 * f6 -> "main"
	 * f7 -> "("
	 * f8 -> "String"
	 * f9 -> "["
	 * f10 -> "]"
	 * f11 -> Identifier()
	 * f12 -> ")"
	 * f13 -> "{"
	 * f14 -> ( VarDeclaration() )*
	 * f15 -> ( Statement() )*
	 * f16 -> "}"
	 * f17 -> "}"
	*/
	@Override
	public String visit(MainClass n, Session argu) throws Exception {
		// Names of class and main method (only one method in the main class, so we can include it here)
		argu = new Session(n.f1.accept(this, argu), "main");

		emit("; Program body\n" +
		     "define i32 @main() {");

		n.f14.accept(this, argu); // Main Method Local Variables
		n.f15.accept(this, argu); // Main Method Body

		emit("\tret i32 0\n" +
		     "}\n");

		return null;
	}

	/**
	 * f0 -> "class"
	 * f1 -> Identifier()
	 * f2 -> "{"
	 * f3 -> ( VarDeclaration() )*
	 * f4 -> ( MethodDeclaration() )*
	 * f5 -> "}"
	*/
	@Override
	public String visit(ClassDeclaration n, Session argu) throws Exception {
		// Initialize argu with name of visited class for now
		argu = new Session(n.f1.accept(this, argu));

		emit("; class " + argu.getClassName());
		emit_vtable(argu.getClassName());

		n.f4.accept(this, argu); // Class Methods

		return null;
	}

	/**
	 * f0 -> "class"
	 * f1 -> Identifier()
	 * f2 -> "extends"
	 * f3 -> Identifier()
	 * f4 -> "{"
	 * f5 -> ( VarDeclaration() )*
	 * f6 -> ( MethodDeclaration() )*
	 * f7 -> "}"
	*/
	@Override
	public String visit(ClassExtendsDeclaration n, Session argu) throws Exception {
		// Initialize argu with name of visited class for now
		argu = new Session(n.f1.accept(this, argu));

		emit("; class " + argu.getClassName() + " extends " + n.f3.accept(this, argu));
		emit_vtable(argu.getClassName());

		n.f6.accept(this, argu); // Class Methods

		return null;
	}

	/**
	 * f0 -> Type()
	 * f1 -> Identifier()
	 * f2 -> ";"
	*/
	@Override
	public String visit(VarDeclaration n, Session argu) throws Exception {
		String varType = n.f0.accept(this, argu);
		String varName = n.f1.accept(this, argu);

		String varLlType = llType(varType);

		emit("\t%" + varName + " = alloca " + varLlType);

		// Zero-initialized
		emit("\tstore " + varLlType + ' ' + llNull(varType) + ", " + varLlType + "* %" + varName);

		return null;
	}

	/**
	 * f0 -> "public"
	 * f1 -> Type()
	 * f2 -> Identifier()
	 * f3 -> "("
	 * f4 -> ( FormalParameterList() )?
	 * f5 -> ")"
	 * f6 -> "{"
	 * f7 -> ( VarDeclaration() )*
	 * f8 -> ( Statement() )*
	 * f9 -> "return"
	 * f10 -> Expression()
	 * f11 -> ";"
	 * f12 -> "}"
	*/
	@Override
	public String visit(MethodDeclaration n, Session argu) throws Exception {
		/* Declared method return type */
		String methodType = n.f1.accept(this, argu);

		/* add function name to array passed from Class Declaration */
		argu.setMethod(n.f2.accept(this, argu));

		StringBuilder parameters = new StringBuilder("i8* %this");
		Collection<Symbol> parameterList = symbols.getClass(argu.getClassName()).getMethod(argu.getMethodName()).getParameters();

		for (Symbol parameter: parameterList)
			parameters.append(", " + llType(parameter.getType()) + " %_" + parameter.getName());

		emit("define " + llType(methodType) + " @" + argu.getClassName() + '.' + argu.getMethodName() + '(' + parameters + ") {");

		// Allocate space for method parameters
		if (n.f4.present()) {
			emit("\t; Parameters (pass by value)");
			n.f4.accept(this, argu);
			emit(""); // Cosmetic separation
		}

		// Allocate space for local method variables
		if (n.f7.present()) {
			emit("\t; Local variables (zero-initialized)");
			n.f7.accept(this, argu);
			emit(""); // Cosmetic separation
		}

		// Method body
		n.f8.accept(this, argu);

		/* Return type and value of final expression */
		String retExpr = n.f10.accept(this, argu);

		emit("\tret " + retExpr + "\n" +
		     "}\n");

		counters.reset();

		return null;
	}

	/**
	 * f0 -> Type()
	 * f1 -> Identifier()
	*/
	@Override
	public String visit(FormalParameter n, Session argu) throws Exception {
		String paramType = n.f0.accept(this, argu);
		String paramName = n.f1.accept(this, argu);

		String paramLlType = llType(paramType);

		// Pass by value: Copy arguments to local variables
		emit("\t%" + paramName + " = alloca " + paramLlType);
 		emit("\tstore " + paramLlType + " %_" + paramName + ", " + paramLlType + "* %" + paramName);

		return null;
	}

	/**
	 * f0 -> "int"
	 * f1 -> "["
	 * f2 -> "]"
	*/
	@Override
	public String visit(ArrayType n, Session argu) throws Exception {
		return n.f0.toString() + n.f1.toString() + n.f2.toString();
	}

	/**
	 * f0 -> "boolean"
	*/
	@Override
	public String visit(BooleanType n, Session argu) throws Exception {
		return n.f0.toString();
	}

	/**
	 * f0 -> "int"
	*/
	@Override
	public String visit(IntegerType n, Session argu) throws Exception {
		return n.f0.toString();
	}

	/**
	 * f0 -> <IDENTIFIER>
	*/
	@Override
	public String visit(Identifier n, Session argu) throws Exception {
		return n.f0.toString();
	}

	/* Statement Family */

	/**
	 * f0 -> Identifier()
	 * f1 -> "="
	 * f2 -> Expression()
	 * f3 -> ";"
	*/
	@Override
	public String visit(AssignmentStatement n, Session argu) throws Exception {
		String idName = n.f0.accept(this, argu);
		String exprRegister = n.f2.accept(this, argu);

		ClassSymbol classSymbol = symbols.getClass(argu.getClassName());
		MethodSymbol method = classSymbol.getMethod(argu.getMethodName());
		Symbol variable;


		if ((variable = method.getParameter(idName)) != null ||
		    (variable = method.getField(idName)) != null) {
			// store value to alloc'd pointer
			emit("\tstore " + exprRegister + ", " + llType(variable.getType()) + "* %" + idName);
		} else { // We have a class member on our hands: Get field pointer, THEN store.
			emit("\t; " + argu.getClassName() + '.' + idName + " = " + exprRegister);

			variable = classSymbol.getField(idName);

			String fieldAddr = counters.nextRegister();
			emit('\t' + fieldAddr + " = getelementptr i8, i8* %this, i32 " + (8 + variable.getOffset()));

			String fieldPointer = counters.nextRegister();
			emit('\t' + fieldPointer + " = bitcast i8* " + fieldAddr + " to " + llType(variable.getType()) + "*");

			emit("\tstore " + exprRegister + ", " + llType(variable.getType()) + "* " + fieldPointer + '\n');
		}

		return null;
	}

	/**
	 * f0 -> Identifier()
	 * f1 -> "["
	 * f2 -> Expression()
	 * f3 -> "]"
	 * f4 -> "="
	 * f5 -> Expression()
	 * f6 -> ";"
	*/
	@Override
	public String visit(ArrayAssignmentStatement n, Session argu) throws Exception {
		String idName = n.f0.accept(this, argu);
		String indexRegister = n.f2.accept(this, argu);

		// Part 1: Acquire pointer to the array

		ClassSymbol classSymbol = symbols.getClass(argu.getClassName());
		MethodSymbol method = classSymbol.getMethod(argu.getMethodName());
		Symbol array;

		String arrayRegister;

		emit("\t; " + idName + '[' + indexRegister + "] = ...");
		if ((array = method.getParameter(idName)) != null ||
		    (array = method.getField(idName)) != null) {
			// load address of first element from alloc'd pointer
			arrayRegister = counters.nextRegister();
			emit('\t' + arrayRegister + " = load i32*, i32** %" + idName);
		} else { // We have a class member on our hands: Get field pointer, THEN load array pointer.
			array = classSymbol.getField(idName);

			String fieldAddr = counters.nextRegister();
			emit('\t' + fieldAddr + " = getelementptr i8, i8* %this, i32 " + (8 + array.getOffset()));

			String arrayPointer = counters.nextRegister();
			emit('\t' + arrayPointer + " = bitcast i8* " + fieldAddr + " to i32**");

			arrayRegister = counters.nextRegister();
			emit('\t' + arrayRegister + " = load i32*, i32** " + arrayPointer);
		}

		// Part 2: Check index compared to array length, if bad throw_oob()

		String arrayLength = counters.nextRegister();
		// Convention: First "member" of array is actually its length, as an int
		emit('\t' + arrayLength + " = load i32, i32* " + arrayRegister);

		String oobCheck = counters.nextRegister();
		emit('\t' + oobCheck + " = icmp ult " + indexRegister + ", " + arrayLength);

		String[] oobLabel = counters.nextOob();
		emit('\t' + "br i1 " + oobCheck + ", label %" + oobLabel[0] + ", label %" + oobLabel[1]);

		String exitLabel = counters.nextExit();

		// Path 1: Correct indexing -> store
		emit(oobLabel[0] + ':');

		// Because, as mentioned, the 0th element is the array length, and MiniJava arrays properly start at 0
		String actualIndex = counters.nextRegister();
		emit('\t' + actualIndex + " = add " + indexRegister + ", 1");

		String elementPointer = counters.nextRegister();
		emit('\t' + elementPointer + " = getelementptr i32, i32* " + arrayRegister + ", i32 " + actualIndex);

		String exprRegister = n.f5.accept(this, argu);
		emit("\tstore " + exprRegister + ", i32* " + elementPointer);
		emit("\tbr label %" + exitLabel);

		// Path 2: Ya dun goofed
		emit(oobLabel[1] + ':');
		emit("\tcall void @throw_oob()");
		emit("\tbr label %" + exitLabel + '\n');

		emit(exitLabel + ':');

		return null;
	}

	/**
	 * f0 -> "if"
	 * f1 -> "("
	 * f2 -> Expression()
	 * f3 -> ")"
	 * f4 -> Statement()
	 * f5 -> "else"
	 * f6 -> Statement()
	*/
	@Override
	public String visit(IfStatement n, Session argu) throws Exception {
		String exprRegister = n.f2.accept(this, argu);

		String[] ifLabel = counters.nextIf();
		String exitLabel = counters.nextExit();
		emit("\tbr " + exprRegister + ", label %" + ifLabel[0] + ", label %" + ifLabel[1]);

		// if
		emit(ifLabel[0] + ':');
		n.f4.accept(this, argu);
		emit("\tbr label %" + exitLabel);

		// else
		emit(ifLabel[1] + ':');
		n.f6.accept(this, argu);
		emit("\tbr label %" + exitLabel + '\n');

		emit(exitLabel + ':');

		return null;
	}

	/**
	 * f0 -> "while"
	 * f1 -> "("
	 * f2 -> Expression()
	 * f3 -> ")"
	 * f4 -> Statement()
	*/
	@Override
	public String visit(WhileStatement n, Session argu) throws Exception {
		String whileLabel[] = counters.nextLoop();
		String exitLabel = counters.nextExit();

		// Previous basic block must end with branch
		emit("\tbr label %" + whileLabel[0] + '\n');

		// Loop condition
		emit(whileLabel[0] + ':');
		String exprRegister = n.f2.accept(this, argu);
		emit("\tbr " + exprRegister + ", label %" + whileLabel[1] + ", label %" + exitLabel);

		// Loop body
		emit(whileLabel[1] + ':');
		n.f4.accept(this, argu);
		emit("\tbr label %" + whileLabel[0] + '\n');

		emit(exitLabel + ':');

		return null;
	}

	/**
	 * f0 -> "System.out.println"
	 * f1 -> "("
	 * f2 -> Expression()
	 * f3 -> ")"
	 * f4 -> ";"
	*/
	@Override
	public String visit(PrintStatement n, Session argu) throws Exception {
		String expRegister = n.f2.accept(this, argu);

		emit("\tcall void (i32) @print_int(" + expRegister + ')');

		return null;
	}

	/* Expression Family */

	/**
	 * f0 -> Clause()
	 * f1 -> "&&"
	 * f2 -> Clause()
	*/
	@Override
	public String visit(AndExpression n, Session argu) throws Exception {
		String[] exprRegister = new String[2];
		String[] clauseLabel = new String[] { counters.nextClause(), counters.nextClause() };
		String exitLabel = counters.nextExit();

		// Previous basic block must end with branch
		emit("\tbr label %" + clauseLabel[0]);

		emit(clauseLabel[0] + ':');
		exprRegister[0] = n.f0.accept(this, argu);
		emit("\tbr " + exprRegister[0] + ", label %" + clauseLabel[1] + ", label %" + exitLabel);

		emit(clauseLabel[1] + ':');
		exprRegister[1] = n.f2.accept(this, argu);
		emit("\tbr label %" + exitLabel + '\n');

		emit(exitLabel + ':');
		String resultRegister = counters.nextRegister();
 		emit('\t' + resultRegister + " = phi i1 [ 0, %" + clauseLabel[0] + " ], [ " + exprRegister[1].split(" ")[1] + ", %" + clauseLabel[1] + " ]");

		return "i1 " + resultRegister;
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "<"
	 * f2 -> PrimaryExpression()
	*/
	@Override
	public String visit(CompareExpression n, Session argu) throws Exception {
		String[] exprRegister = new String[2];

		exprRegister[0] = n.f0.accept(this, argu);
		exprRegister[1] = n.f2.accept(this, argu).split(" ")[1]; // only need reg name

		String cmpResult = counters.nextRegister();
		emit('\t' + cmpResult + " = icmp slt " + exprRegister[0] + ", " + exprRegister[1]);

		return "i1 " + cmpResult;
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "+"
	 * f2 -> PrimaryExpression()
	*/
	@Override
	public String visit(PlusExpression n, Session argu) throws Exception {
		String[] exprRegister = new String[2];

		exprRegister[0] = n.f0.accept(this, argu);
		exprRegister[1] = n.f2.accept(this, argu).split(" ")[1]; // only need reg name

		String addResult = counters.nextRegister();
		emit('\t' + addResult + " = add " + exprRegister[0] + ", " + exprRegister[1]);

		return "i32 " + addResult;
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "-"
	 * f2 -> PrimaryExpression()
	*/
	@Override
	public String visit(MinusExpression n, Session argu) throws Exception {
		String[] exprRegister = new String[2];

		exprRegister[0] = n.f0.accept(this, argu);
		exprRegister[1] = n.f2.accept(this, argu).split(" ")[1]; // only need reg name

		String subResult = counters.nextRegister();
		emit('\t' + subResult + " = sub " + exprRegister[0] + ", " + exprRegister[1]);

		return "i32 " + subResult;
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "*"
	 * f2 -> PrimaryExpression()
	*/
	@Override
	public String visit(TimesExpression n, Session argu) throws Exception {
		String[] exprRegister = new String[2];

		exprRegister[0] = n.f0.accept(this, argu);
		exprRegister[1] = n.f2.accept(this, argu).split(" ")[1]; // only need reg name

		String mulResult = counters.nextRegister();
		emit('\t' + mulResult + " = mul " + exprRegister[0] + ", " + exprRegister[1]);

		return "i32 " + mulResult;
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "["
	 * f2 -> PrimaryExpression()
	 * f3 -> "]"
	*/
	@Override
	public String visit(ArrayLookup n, Session argu) throws Exception {
		String arrayRegister = n.f0.accept(this, argu);
		String indexRegister = n.f2.accept(this, argu);

		// Check index compared to array length, if bad throw_oob()

		String arrayLength = counters.nextRegister();
		// Convention: First "member" of array is actually its length, as an int
		emit('\t' + arrayLength + " = load i32, " + arrayRegister);

		String oobCheck = counters.nextRegister();
		emit('\t' + oobCheck + " = icmp ult " + indexRegister + ", " + arrayLength);

		String[] oobLabel = counters.nextOob();
		emit('\t' + "br i1 " + oobCheck + ", label %" + oobLabel[0] + ", label %" + oobLabel[1]);

		String exitLabel = counters.nextExit();

		// Path 1: Correct indexing -> load
		emit(oobLabel[0] + ':');

		// Because, as mentioned, the 0th element is the array length, and MiniJava arrays properly start at 0
		String actualIndex = counters.nextRegister();
		emit('\t' + actualIndex + " = add " + indexRegister + ", 1");

		String elementPointer = counters.nextRegister();
		emit('\t' + elementPointer + " = getelementptr i32, " + arrayRegister + ", i32 " + actualIndex);

		String elementValue = counters.nextRegister();
		emit('\t' + elementValue + " = load i32, i32* " + elementPointer);
		emit("\tbr label %" + exitLabel);

		// Path 2: Ya dun goofed
		emit(oobLabel[1] + ':');
		emit("\tcall void @throw_oob()");
		emit("\tbr label %" + exitLabel + '\n');

		emit(exitLabel + ':');

		return "i32 " + elementValue;
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "."
	 * f2 -> "length"
	*/
	@Override
	public String visit(ArrayLength n, Session argu) throws Exception {
		String arrayRegister = n.f0.accept(this, argu);

		String arrayLength = counters.nextRegister();
		emit('\t' + arrayLength + " = load i32, " + arrayRegister);

		return "i32 " + arrayLength;
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "."
	 * f2 -> Identifier()
	 * f3 -> "("
	 * f4 -> ( ExpressionList() )?
	 * f5 -> ")"
	*/
	@Override
	public String visit(MessageSend n, Session argu) throws Exception {
		String objRegister = n.f0.accept(this, argu);

		ClassSymbol callClass = symbols.getClass(argu.getObjectType());
		MethodSymbol callMethod = callClass.getMethod(n.f2.accept(this, argu));
		int methodIndex = callMethod.getOffset() / 8;

		// Part 1: Acquire method offset

		emit("\t; " + callClass.getName() + "." + callMethod.getName() + "() : " + methodIndex);

		// Vtable is pointed to by the first 8 bytes of an object
		String vTableAddr = counters.nextRegister();
		emit('\t' + vTableAddr + " = bitcast " + objRegister + " to i8***");

		String vTablePointer = counters.nextRegister();
		emit('\t' + vTablePointer + " = load i8**, i8*** " + vTableAddr);

		// This is the pointer to the method pointer, in the vtable array (bruh)
		String methodElementPointer = counters.nextRegister();
		emit('\t' + methodElementPointer + " = getelementptr i8*, i8** " + vTablePointer + ", i32 " + methodIndex);

		String methodAddr = counters.nextRegister();
		emit('\t' + methodAddr + " = load i8*, i8** " + methodElementPointer);

		StringBuilder parameters = new StringBuilder("i8*");
		for (Symbol parameter: callMethod.getParameters())
			parameters.append(", " + llType(parameter.getType()));

		// It took a while to get here, but here's the actual bitcasted method pointer...
		String methodPointer = counters.nextRegister();
		emit('\t' + methodPointer + " = bitcast i8* " + methodAddr + " to " + llType(callMethod.getType()) + " (" + parameters.toString() + ")*");

		// Part 2: Call method with arg expressions (stored in virtual registers by the time we call)

		StringBuilder arguments = new StringBuilder(objRegister);

		// Get any additional arguments
		n.f4.accept(this, argu);
		for (String argumentRegister: argu.getCallArgs())
			arguments.append(", " + argumentRegister);
		argu.getCallArgs().clear();

		String methodCall = counters.nextRegister();
		emit('\t' + methodCall + " = call " + llType(callMethod.getType()) + ' ' + methodPointer + '(' + arguments.toString() + ")\n");

		argu.setObjectType(callMethod.getType());
		return llType(callMethod.getType()) + ' ' + methodCall;
	}

	/**
	 * f0 -> Expression()
	 * f1 -> ExpressionTail()
	*/
	@Override
	public String visit(ExpressionList n, Session argu) throws Exception {
		argu.addCallArg(n.f0.accept(this, argu));
		return n.f1.accept(this, argu);
	}

	/**
	 * f0 -> ","
	 * f1 -> Expression()
	*/
	@Override
	public String visit(ExpressionTerm n, Session argu) throws Exception {
		argu.addCallArg(n.f1.accept(this, argu));
		return null;
	}

	/**
	 * f0 -> IntegerLiteral()
	 *       | TrueLiteral()
	 *       | FalseLiteral()
	 *       | Identifier()
	 *       | ThisExpression()
	 *       | ArrayAllocationExpression()
	 *       | AllocationExpression()
	 *       | BracketExpression()
	*/
	@Override
	public String visit(PrimaryExpression n, Session argu) throws Exception {
		String exprRegister = n.f0.accept(this, argu);

		// If it's not an Identifier, then it will be a virtual register returned from another function.
		// Return that unchanged
		if (n.f0.which != 3)
			return exprRegister;

		// It's an identifier. Find the register we need (AssignmentStatement vibes)
		String idName = exprRegister;

		ClassSymbol classSymbol = symbols.getClass(argu.getClassName());
		MethodSymbol method = classSymbol.getMethod(argu.getMethodName());
		Symbol variable;

		if ((variable = method.getParameter(idName)) != null ||
		    (variable = method.getField(idName)) != null) {
			// load value from alloc'd pointer
			exprRegister = counters.nextRegister();
			emit('\t' + exprRegister + " = load " + llType(variable.getType()) + ", " + llType(variable.getType()) + "* %" + idName);
		} else { // We have a class member on our hands: Get field pointer, THEN load.
			variable = classSymbol.getField(idName);

			String fieldAddr = counters.nextRegister();
			emit('\t' + fieldAddr + " = getelementptr i8, i8* %this, i32 " + (8 + variable.getOffset()));

			String fieldPointer = counters.nextRegister();
			emit('\t' + fieldPointer + " = bitcast i8* " + fieldAddr + " to " + llType(variable.getType()) + "*");

			exprRegister = counters.nextRegister();
			emit('\t' + exprRegister + " = load " + llType(variable.getType()) + ", " + llType(variable.getType()) + "* " + fieldPointer + '\n');
		}

		argu.setObjectType(variable.getType());
		return llType(variable.getType()) + ' ' + exprRegister;
	}

	/**
	 * f0 -> <INTEGER_LITERAL>
	*/
	@Override
	public String visit(IntegerLiteral n, Session argu) throws Exception {
		return "i32 " + n.f0.toString();
	}

	/**
	 * f0 -> "true"
	*/
	@Override
	public String visit(TrueLiteral n, Session argu) throws Exception {
		return "i1 1";
	}

	/**
	 * f0 -> "false"
	*/
	@Override
	public String visit(FalseLiteral n, Session argu) throws Exception {
		return "i1 0";
	}

	/**
	 * f0 -> "this"
	*/
	@Override
	public String visit(ThisExpression n, Session argu) throws Exception {
		argu.setObjectType(argu.getClassName());
		return "i8* %this";
	}

	/**
	 * f0 -> "new"
	 * f1 -> "int"
	 * f2 -> "["
	 * f3 -> Expression()
	 * f4 -> "]"
	*/
	@Override
	public String visit(ArrayAllocationExpression n, Session argu) throws Exception {
		String lengthRegister = n.f3.accept(this, argu);

		String oobCheck = counters.nextRegister();
		emit('\t' + oobCheck + " = icmp sge " + lengthRegister + ", 0");

		String[] oobLabel = counters.nextOob();
		emit('\t' + "br i1 " + oobCheck + ", label %" + oobLabel[0] + ", label %" + oobLabel[1]);

		String exitLabel = counters.nextExit();

		// Path 1: Correct indexing -> allocate space for array
		emit(oobLabel[0] + ':');

 		// Because, as mentioned, the 0th element is the array length,we'll actually store length + 1 elements
		String actualLength = counters.nextRegister();
		emit('\t' + actualLength + " = add " + lengthRegister + ", 1");

		String arrayAddr = counters.nextRegister();
		emit('\t' + arrayAddr + " = call i8* @calloc(i32 4, i32 " + actualLength + ")");

		//TODO make names consistent
		String arrayPointer = counters.nextRegister();
		emit('\t' + arrayPointer + " = bitcast i8* " + arrayAddr + " to i32*");

		// Stick the length on it like a post-it note
		emit("\tstore " + lengthRegister + ", i32* " + arrayPointer);
		emit("\tbr label %" + exitLabel);

		// Path 2: Ya dun goofed
		emit(oobLabel[1] + ':');
		emit("\tcall void @throw_oob()");
		emit("\tbr label %" + exitLabel + '\n');

		emit(exitLabel + ':');

		return "i32* " + arrayPointer;
	}

	/**
	 * f0 -> "new"
	 * f1 -> Identifier()
	 * f2 -> "("
	 * f3 -> ")"
	*/
	@Override
	public String visit(AllocationExpression n, Session argu) throws Exception {
		String className = n.f1.accept(this, argu);

		ClassSymbol newClass = symbols.getClass(className);

		// Part 1: Allocate space for class (zero initialized by calloc)
		String objRegister = counters.nextRegister();
		emit('\t' + objRegister + " = call i8* @calloc(i32 1, i32 " + (8 + newClass.getSize()) + ')');

		// Part 2: Store V-Table address as first field
		String objAddr = counters.nextRegister();
		emit('\t' + objAddr + " = bitcast i8* " + objRegister + " to i8***");

		String vTableAddr = counters.nextRegister();
		emit('\t' + vTableAddr + " = getelementptr [" + newClass.getMethodsAmount() + " x i8*], [" + newClass.getMethodsAmount() + " x i8*]* @" + className + "_vtable, i32 0, i32 0");

		// Store vtable address at start of object area (the fabled 8 bytes we keep adding to those offsets)
		emit("\tstore i8** " + vTableAddr + ", i8*** " + objAddr + '\n');

		argu.setObjectType(className);
		return "i8* " + objRegister;
	}

	/**
	 * f0 -> "!"
	 * f1 -> Clause()
	*/
	@Override
	public String visit(NotExpression n, Session argu) throws Exception {
		String exprRegister = n.f1.accept(this, argu);

		String notRegister = counters.nextRegister();
 		emit('\t' + notRegister + " = xor i1 1, " + exprRegister.split(" ")[1]);

		return "i1 " + notRegister;
	}

	/**
	 * f0 -> "("
	 * f1 -> Expression()
	 * f2 -> ")"
	*/
	@Override
	public String visit(BracketExpression n, Session argu) throws Exception {
		return n.f1.accept(this, argu);
	}
}
