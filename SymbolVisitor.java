import syntaxtree.*;
import visitor.*;
import symbol.*;

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

	/**
	 * f0 -> "class"
	 * f1 -> Identifier()
	 * f2 -> "{"
	 * f3 -> ( VarDeclaration() )*
	 * f4 -> ( MethodDeclaration() )*
	 * f5 -> "}"
	*/
	@Override
	public String visit(ClassDeclaration n, Symbol argu) throws Exception {
		String className = n.f1.accept(this, argu);

		if (symbols.hasClass(className))
			throw new TypeCheckException("Redefinition of class " + className + "!");

		ClassSymbol newClass = new ClassSymbol(className);

		if (n.f3.present())
			n.f3.accept(this, newClass);

		if (n.f4.present())
			n.f4.accept(this, newClass);

		symbols.addClass(newClass);

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
	public String visit(ClassExtendsDeclaration n, Symbol argu) throws Exception {
		String className = n.f1.accept(this, argu);

		if (symbols.hasClass(className))
			throw new TypeCheckException("Redefinition of class " + className + "!");

		String parentName = n.f3.accept(this, argu);

		/* In MiniJava, a class has to be defined before a subclass */
		if (!symbols.hasClass(parentName))
			throw new TypeCheckException("class " + className + " extends " + parentName + ", but " + parentName + " has not been defined!");

		ClassSymbol newClass = new ClassSymbol(className, parentName);

		if (n.f5.present())
			n.f5.accept(this, newClass);

		if (n.f6.present())
			n.f6.accept(this, newClass);

		symbols.addClass(newClass);

		return null;
	}
	//TODO: Maybe R should be Void?

	/**
	 * f0 -> Type()
	 * f1 -> Identifier()
	 * f2 -> ";"
	*/
	@Override
	public String visit(VarDeclaration n, Symbol argu) throws Exception {
		String _ret=null;

		String varType = n.f0.accept(this, argu);
		String varName = n.f1.accept(this, argu);

		if (argu.hasField(varName))
			throw new TypeCheckException("Variable " + varName + " redefined in " + argu.getType().toString().toLowerCase() + " " + argu.getName() + "!");

		Symbol newVar = new Symbol(varType, varName);
		argu.addField(newVar);

		return null;
	}

	//TODO continue from here

	//TODO: this or f1.f0?
	/**
	 * f0 -> <IDENTIFIER>
	 */
	@Override
	public String visit(Identifier n, Symbol argu) throws Exception {
		return n.f0.toString();
	}
}
