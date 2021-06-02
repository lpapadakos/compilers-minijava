import java.io.BufferedWriter;
import java.util.*;

import syntaxtree.*;
import visitor.*;
import symbol.*;

public class LLVMVisitor extends GJDepthFirst<String, String[]> {
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
	public String visit(MainClass n, String[] argu) throws Exception {
		// Names of class and main method (only one method in the main class), so we can include it here
		String className = n.f1.accept(this, argu);
		String[] names = new String[] { className, "main" };

		emit("; Program body\n" +
		     "define i32 @main() {");

		n.f14.accept(this, names); // Main Method Local Variables
		n.f15.accept(this, names); // Main Method Body

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
	public String visit(ClassDeclaration n, String[] argu) throws Exception {
		String[] names = new String[2];

		names[0] = n.f1.accept(this, names);

		emit("; class " + names[0]);
		emit_vtable(names[0]);

		n.f4.accept(this, names); // Class Methods

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
	public String visit(ClassExtendsDeclaration n, String[] argu) throws Exception {
		String[] names = new String[2];

		names[0] = n.f1.accept(this, names);

		emit("; class " + names[0] + " extends " + n.f3.accept(this, names));
		emit_vtable(names[0]);

		n.f6.accept(this, names); // Class Methods

		return null;
	}

	/**
	 * f0 -> Type()
	 * f1 -> Identifier()
	 * f2 -> ";"
	*/
	@Override
	public String visit(VarDeclaration n, String[] argu) throws Exception {
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
	public String visit(MethodDeclaration n, String[] argu) throws Exception {
		/* Declared method return type */
		String methodType = n.f1.accept(this, argu);

		/* add function name to array passed from Class Declaration */
		argu[1] = n.f2.accept(this, argu);

		StringBuilder parameters = new StringBuilder("i8* %this");
		Collection<Symbol> parameterList = symbols.getClass(argu[0]).getMethod(argu[1]).getParameters();

		for (Symbol parameter: parameterList)
			parameters.append(", " + llType(parameter.getType()) + " %_" + parameter.getName());

		emit("define " + llType(methodType) + " @" + argu[0] + '.' + argu[1] + '(' + parameters + ") {");

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
	public String visit(FormalParameter n, String[] argu) throws Exception {
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
	public String visit(ArrayType n, String[] argu) throws Exception {
		return n.f0.toString() + n.f1.toString() + n.f2.toString();
	}

	/**
	 * f0 -> "boolean"
	*/
	@Override
	public String visit(BooleanType n, String[] argu) throws Exception {
		return n.f0.toString();
	}

	/**
	 * f0 -> "int"
	*/
	@Override
	public String visit(IntegerType n, String[] argu) throws Exception {
		return n.f0.toString();
	}

	/**
	 * f0 -> <IDENTIFIER>
	*/
	@Override
	public String visit(Identifier n, String[] argu) throws Exception {
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
	public String visit(AssignmentStatement n, String[] argu) throws Exception {
		String idName = n.f0.accept(this, argu);
		String exprRegister = n.f2.accept(this, argu);

		ClassSymbol classSymbol = symbols.getClass(argu[0]);
		MethodSymbol method = classSymbol.getMethod(argu[1]);
		Symbol variable;

		if ((variable = method.getParameter(idName)) != null ||
		    (variable = method.getField(idName)) != null) {
			emit("\tstore " + exprRegister + ", " + llType(variable.getType()) + "* %" + idName);
		} else { // We have a class member on our hands: Get field pointer, THEN store.
			variable = classSymbol.getField(idName);

			String fieldAddr = counters.nextRegister();
			emit('\t' + fieldAddr + " = getelementptr i8, i8* %this, i32 " + (8 + variable.getOffset()));

			String fieldPointer = counters.nextRegister();
			emit('\t' + fieldPointer + " = bitcast i8* " + fieldAddr + " to " + llType(variable.getType()) + "*");

			emit("\tstore " + exprRegister + ", " + llType(variable.getType()) + "* " + fieldPointer);
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
	public String visit(ArrayAssignmentStatement n, String[] argu) throws Exception {
		String idName = n.f0.accept(this, argu);
		String indexRegister = n.f2.accept(this, argu);

		// Part 1: Acquire pointer to the array

		ClassSymbol classSymbol = symbols.getClass(argu[0]);
		MethodSymbol method = classSymbol.getMethod(argu[1]);
		Symbol array;

		String arrayRegister;

		emit("");
		if ((array = method.getParameter(idName)) != null ||
		    (array = method.getField(idName)) != null) {
			//arrayRegister = counters.nextRegister();
			//TODO are local arrays actually i32**? If not, %idName should do
			//emit("\t" + arrayRegister + " = load i32*, i32** %" + idName);
			arrayRegister = '%' + idName;
		} else { // We have a class member on our hands: Get field pointer, THEN load array pointer.
			array = classSymbol.getField(idName);

			String fieldAddr = counters.nextRegister();
			emit('\t' + fieldAddr + " = getelementptr i8, i8* %this, i32 " + (8 + array.getOffset()));

			String arrayPointer = counters.nextRegister();
			emit('\t' + arrayPointer + " = bitcast i8* " + fieldAddr + " to i32**");

			arrayRegister = counters.nextRegister();
			emit("\t" + arrayRegister + " = load i32*, i32** " + arrayPointer);
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
	public String visit(IfStatement n, String[] argu) throws Exception {
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
		emit("\tbr label %" + exitLabel);

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
	public String visit(WhileStatement n, String[] argu) throws Exception {
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
		emit("\tbr label %" + whileLabel[0]);

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
	public String visit(PrintStatement n, String[] argu) throws Exception {
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
	public String visit(AndExpression n, String[] argu) throws Exception {
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
		emit("\tbr label %" + exitLabel);

		emit(exitLabel + ':');
		String resultRegister = counters.nextRegister();
 		emit("\t" + resultRegister + " = phi i1 [ 0, %" + clauseLabel[0] + " ], [ " + exprRegister[1].split(" ")[1] + ", %" + clauseLabel[1] + " ]");

		return "i1 " + resultRegister;
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "<"
	 * f2 -> PrimaryExpression()
	*/
	@Override
	public String visit(CompareExpression n, String[] argu) throws Exception {
		String[] exprRegister = new String[2];

		exprRegister[0] = n.f0.accept(this, argu);
		exprRegister[1] = n.f2.accept(this, argu).split(" ")[1]; // only need reg name

		String cmpResult = counters.nextRegister();
		emit("\t" + cmpResult + " = icmp slt " + exprRegister[0] + ", " + exprRegister[1]);

		return "i1 " + cmpResult;
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "+"
	 * f2 -> PrimaryExpression()
	*/
	@Override
	public String visit(PlusExpression n, String[] argu) throws Exception {
		String[] exprRegister = new String[2];

		exprRegister[0] = n.f0.accept(this, argu);
		exprRegister[1] = n.f2.accept(this, argu).split(" ")[1]; // only need reg name

		String addResult = counters.nextRegister();
		emit("\t" + addResult + " = add " + exprRegister[0] + ", " + exprRegister[1]);

		return "i32 " + addResult;
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "-"
	 * f2 -> PrimaryExpression()
	*/
	@Override
	public String visit(MinusExpression n, String[] argu) throws Exception {
		String[] exprRegister = new String[2];

		exprRegister[0] = n.f0.accept(this, argu);
		exprRegister[1] = n.f2.accept(this, argu).split(" ")[1]; // only need reg name

		String subResult = counters.nextRegister();
		emit("\t" + subResult + " = sub " + exprRegister[0] + ", " + exprRegister[1]);

		return "i32 " + subResult;
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "*"
	 * f2 -> PrimaryExpression()
	*/
	@Override
	public String visit(TimesExpression n, String[] argu) throws Exception {
		String[] exprRegister = new String[2];

		exprRegister[0] = n.f0.accept(this, argu);
		exprRegister[1] = n.f2.accept(this, argu).split(" ")[1]; // only need reg name

		String mulResult = counters.nextRegister();
		emit("\t" + mulResult + " = mul " + exprRegister[0] + ", " + exprRegister[1]);

		return "i32 " + mulResult;
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "["
	 * f2 -> PrimaryExpression()
	 * f3 -> "]"
	*/
	@Override
	public String visit(ArrayLookup n, String[] argu) throws Exception {
		String arrayRegister = n.f0.accept(this, argu);
		String indexRegister = n.f2.accept(this, argu);


		return "i32 array_TODO";
	}

	// TODO continue here
}
