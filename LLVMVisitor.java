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

		//TODO else?
		public String nextIf() {
			return "if" + _if++;
		}

		public String nextLoop() {
			return "loop" + loop++;
		}

		public String nextClause() {
			return "clause" + clause++;
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

	private void emit(String line) throws Exception {
		ll.write(line);
		ll.newLine();
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
		String[] names = new String[] {className, "main"};

		emit("; Program body\n" +
		     "define i32 @main() {");

		n.f14.accept(this, names);
		n.f15.accept(this, names);

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

		/* V-Table time! */
		emit("; class " + names[0]);
		ClassSymbol thisClass = symbols.getClass(names[0]);

		emit("@" + names[0] + ".vtable = global [" + thisClass.getMethodsAmount() + " x i8*] [");

		StringBuffer methods = new StringBuffer();
		for (MethodSymbol method: thisClass.getMethods()) {
			methods.append("\ti8* bitcast (" + llType(method.getType()) + "(i8*");

			for (Symbol parameter: method.getParameters())
				methods.append(", " + llType(parameter.getType()));

			methods.append(")* @" + method.getOwner().getName() + '.' + method.getName() + " to i8*),\n");
		}

		/* Delete trailing comma */
		methods.deleteCharAt(methods.length() - 2);
		emit(methods.toString());

		emit("]");

		// TODO is present() needed?
		if (n.f3.present())
			n.f3.accept(this, names);

		if (n.f4.present())
			n.f4.accept(this, names);

		return null;
	}

	/**
	 * f0 -> <IDENTIFIER>
	*/
	@Override
	public String visit(Identifier n, String[] argu) throws Exception {
		return n.f0.toString();
	}
}
