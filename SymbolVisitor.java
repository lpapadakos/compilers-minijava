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
			throw new TypeCheckException(className, "Redefinition of existing class");

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
			throw new TypeCheckException(className, "Redefinition of existing class");

		String parentName = n.f3.accept(this, argu);

		/* In MiniJava, a class has to be defined before a subclass */
		if (!symbols.hasClass(parentName))
			throw new TypeCheckException(className, "Parent class " + parentName + " has not been defined");

		ClassSymbol parentClass = symbols.getClassSymbol(parentName);
		ClassSymbol newClass = new ClassSymbol(className, parentClass);

		if (n.f5.present())
			n.f5.accept(this, newClass);

		if (n.f6.present())
			n.f6.accept(this, newClass);

		symbols.addClass(newClass);

		return null;
	}

	/**
	 * f0 -> Type()
	 * f1 -> Identifier()
	 * f2 -> ";"
	*/
	@Override
	public String visit(VarDeclaration n, Symbol argu) throws Exception {
		String varType = n.f0.accept(this, argu);
		String varName = n.f1.accept(this, argu);

		Symbol newVar = new Symbol(varType, varName);

		/* Abstraction ContainerSymbol covers Classes AND Methods */
		if (!(argu instanceof FieldContainerSymbol))
			throw new TypeCheckException("Trying to visit VarDeclaration outside of <ClassSymbol, MethodSymbol>");

		FieldContainerSymbol argContainer = (FieldContainerSymbol) argu;

		/* Duplicate variable */
		//TODO maybe it's ok if redefined in parent
		if (argContainer.hasField(varName))
			throw new TypeCheckException(argu.getName(), "Redefinition of variable " + varName);

		if (argu instanceof MethodSymbol) {
			MethodSymbol argMethod = (MethodSymbol) argu;

			/* Conflict with method parameter */
			if (argMethod.hasParameter(varName))
				throw new TypeCheckException(argu.getName(), "Name conflict with existing method parameter " + varName);
		}

		argContainer.addField(newVar);

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
	public String visit(MethodDeclaration n, Symbol argu) throws Exception {
		String methodType = n.f1.accept(this, argu);
		String methodName = n.f2.accept(this, argu);

		MethodSymbol newMethod = new MethodSymbol(methodType, methodName);

		if (n.f4.present())
			n.f4.accept(this, newMethod);

		if (n.f7.present())
			n.f7.accept(this, newMethod);

		if (!(argu instanceof ClassSymbol))
			throw new TypeCheckException("Trying to visit MethodDeclaration outside of ClassSymbol");

		ClassSymbol argClass = (ClassSymbol) argu;

		if (argClass.isOverload(newMethod))
			throw new TypeCheckException(argClass.getName(), newMethod.getName(), "Attempt to overload method");

		argClass.addMethod(newMethod);

		return null;
	}

	/**
	* f0 -> Type()
	* f1 -> Identifier()
	*/
	@Override
	public String visit(FormalParameter n, Symbol argu) throws Exception {
		String paramType = n.f0.accept(this, argu);
		String paramName = n.f1.accept(this, argu);

		Symbol newParam = new Symbol(paramType, paramName);

		if (!(argu instanceof MethodSymbol))
			throw new TypeCheckException("Trying to visit FormalParameter outside of MethodSymbol");

		MethodSymbol argMethod = (MethodSymbol) argu;

		if (argMethod.hasParameter(paramName))
			throw new TypeCheckException(argu.getName(), "Redeclaration of parameter " + paramName);

		argMethod.addParameter(newParam);

		return null;
	}

	/**
	 * f0 -> "int"
	 * f1 -> "["
	 * f2 -> "]"
	*/
	@Override
	public String visit(ArrayType n, Symbol argu) throws Exception {
		return n.f0.toString() + n.f1.toString() + n.f2.toString();
	}

	/**
	 * f0 -> "boolean"
	*/
	@Override
	public String visit(BooleanType n, Symbol argu) throws Exception {
		return n.f0.toString();
	}

	/**
	 * f0 -> "int"
	*/
	@Override
	public String visit(IntegerType n, Symbol argu) throws Exception {
		return n.f0.toString();
	}

	/**
	 * f0 -> <IDENTIFIER>
	 */
	@Override
	public String visit(Identifier n, Symbol argu) throws Exception {
		return n.f0.toString();
	}
}
