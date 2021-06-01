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
		private int cond;

		public Counters() {
			reset();
		}

		public void reset() {
			register = 0;
			arr_alloc = 0;
			oob = 0;
			_if = 0;
			loop = 0;
			cond = 0;
		}

		public String nextRegister() {
			return "%_" + register++;
		}

		public String nextArray() {
			return "alloc" + arr_alloc++;
		}

		public String nextOob() {
			return "oob" + oob++;
		}

		public String nextIf() {
			return "if" + _if++;
		}

		public String nextLoop() {
			return "loop" + loop++;
		}

		public String nextCond() {
			return "cond" + cond++;
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

		emit("\t%" + varName + " = alloca " + llType(varType));
		emit("\tstore " + llType(varType) + " 0, " + llType(varType) + "* %" + varName);

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

		// Pass by value: Copy arguments to local variables
		emit("\t%" + paramName + " = alloca " + llType(paramType));
		emit("\tstore " + llType(paramType) + " %_" + paramName + ", " + llType(paramType) + "* %" + paramName);

		return null;
	}

	// TODO continue here

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
}
