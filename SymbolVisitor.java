import visitor.*;
import syntaxtree.*;
import symbol_table.*;

public class SymbolVisitor extends GJDepthFirst<String, Symbol> {
	private SymbolTable symbols;

	public SymbolVisitor(SymbolTable symbols) {
		this.symbols = symbols;
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
	public String visit(MainClass n, Symbol argu) throws Exception {
		String className = n.f1.accept(this, argu);
		ClassSymbol mainClass = new ClassSymbol(className);

		MethodSymbol mainMethod = new MethodSymbol("void", "main");

		String paramName = n.f11.accept(this, argu);
		Symbol mainParam = new Symbol("String[]", paramName);

		mainMethod.addParameter(mainParam);

		n.f14.accept(this, mainMethod);

		mainClass.addMethod(mainMethod);
		symbols.addClass(mainClass);

		return null;
	}

	//TODO: this or f1.f0?
	/**
	 * f0 -> <IDENTIFIER>
	 */
	@Override
	public String visit(Identifier n, Symbol argu) throws Exception {
		return n.f0.toString();
	}
}
