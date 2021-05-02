import syntaxtree.*;
import visitor.*;

import java.util.StringTokenizer;

import symbol.*;

public class TypeCheckVisitor extends GJDepthFirst<String, String[]> {
	private SymbolTable symbols;

	public TypeCheckVisitor(SymbolTable symbols) {
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
	public String visit(MainClass n, String[] argu) throws Exception {
		// Names of class and main method (only one method in the main class), so we can include it here
		String className = n.f1.accept(this, argu);
		String[] names = new String[] {className, "main"};

		n.f14.accept(this, names);

		n.f15.accept(this, names);

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

		if (n.f3.present())
			n.f3.accept(this, names);

		if (n.f4.present())
			n.f4.accept(this, names);

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

		if (n.f5.present())
			n.f5.accept(this, names);

		if (n.f6.present())
			n.f6.accept(this, names);

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

		if (!symbols.isValidType(varType))
			throw new TypeCheckException(argu, "Type " + varType + " of variable " + varName + " is not a basic type or a defined class");

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

		if (n.f4.present())
			n.f4.accept(this, argu);

		if (n.f7.present())
			n.f7.accept(this, argu);

		if (n.f8.present())
			n.f8.accept(this, argu);

		/* Return type of final expression */
		String exprType = n.f10.accept(this, argu);

		// Check if expression return type matched expected function signature
		if (!symbols.typesMatch(exprType, methodType))
			throw new TypeCheckException(argu, "Return type mismatch (Expected " + methodType + ", got " + exprType + ')');

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

		if (!symbols.isValidType(paramType))
			throw new TypeCheckException(argu, "Type " + paramType + " of parameter " + paramName + " is not a basic type or a defined class");

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

		String idType = symbols.getFieldType(argu[0], argu[1], idName);
		if (idType == null)
			throw new TypeCheckException(argu, "Identifier " + idName + " is undefined");

		String exprType = n.f2.accept(this, argu);
		if (!symbols.typesMatch(exprType, idType))
			throw new TypeCheckException(argu, "Type mismatch in assignment (" + exprType + " cannot be assigned to " + idType + ')');

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

		String idType = symbols.getFieldType(argu[0], argu[1], idName);
		if (!idType.equals("int[]"))
			throw new TypeCheckException(argu, "Identifier " + idName + " is not of type int[]");

		String indexType = n.f2.accept(this, argu);
		if (!indexType.equals("int"))
			throw new TypeCheckException(argu, "Indexing expression for array " + idName + "[] is not of type int");

		String exprType = n.f5.accept(this, argu);
		if (!exprType.equals("int"))
			throw new TypeCheckException(argu, "Expression assigned to member of array " + idName + "[] is not of type int");

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
		String exprType = n.f2.accept(this, argu);
		if (!exprType.equals("boolean"))
			throw new TypeCheckException(argu, "Expression used as \"if\" condition is not of type boolean");

		n.f4.accept(this, argu);
		n.f6.accept(this, argu);

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
		String exprType = n.f2.accept(this, argu);
		if (!exprType.equals("boolean"))
			throw new TypeCheckException(argu, "Expression used as \"while\" condition is not of type boolean");

		n.f4.accept(this, argu);

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
		String exprType = n.f2.accept(this, argu);
		if (!exprType.equals("int"))
			throw new TypeCheckException(argu, "Expression to be printed is not of type int");

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
		String exprType1 = n.f0.accept(this, argu);

		if (!exprType1.equals("boolean"))
			throw new TypeCheckException(argu, "Expression on left side of && is not of type boolean");

		String exprType2 = n.f2.accept(this, argu);

		if (!exprType2.equals("boolean"))
			throw new TypeCheckException(argu, "Expression on right side of && is not of type boolean");

		return "boolean";
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "<"
	 * f2 -> PrimaryExpression()
	*/
	@Override
	public String visit(CompareExpression n, String[] argu) throws Exception {
		String exprType1 = n.f0.accept(this, argu);

		if (!exprType1.equals("int"))
 			throw new TypeCheckException(argu, "Expression on left side of < is not of type int");

		String exprType2 = n.f2.accept(this, argu);

		if (!exprType2.equals("int"))
			throw new TypeCheckException(argu, "Expression on right side of < is not of type int");

		return "boolean";
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "+"
	 * f2 -> PrimaryExpression()
	*/
	@Override
	public String visit(PlusExpression n, String[] argu) throws Exception {
		String exprType1 = n.f0.accept(this, argu);

		if (!exprType1.equals("int"))
			throw new TypeCheckException(argu, "Expression on left side of + is not of type int");

		String exprType2 = n.f2.accept(this, argu);

		if (!exprType2.equals("int"))
			throw new TypeCheckException(argu, "Expression on right side of + is not of type int");

		return "int";
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "-"
	 * f2 -> PrimaryExpression()
	*/
	@Override
	public String visit(MinusExpression n, String[] argu) throws Exception {
		String exprType1 = n.f0.accept(this, argu);

		if (!exprType1.equals("int"))
			throw new TypeCheckException(argu, "Expression on left side of - is not of type int");

		String exprType2 = n.f2.accept(this, argu);

		if (!exprType2.equals("int"))
			throw new TypeCheckException(argu, "Expression on right side of - is not of type int");

		return "int";
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "*"
	 * f2 -> PrimaryExpression()
	*/
	@Override
	public String visit(TimesExpression n, String[] argu) throws Exception {
		String exprType1 = n.f0.accept(this, argu);

		if (!exprType1.equals("int"))
			throw new TypeCheckException(argu, "Expression on left side of * is not of type int");

		String exprType2 = n.f2.accept(this, argu);

		if (!exprType2.equals("int"))
			throw new TypeCheckException(argu, "Expression on right side of * is not of type int");

		return "int";
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "["
	 * f2 -> PrimaryExpression()
	 * f3 -> "]"
	*/
	@Override
	public String visit(ArrayLookup n, String[] argu) throws Exception {
		String exprType1 = n.f0.accept(this, argu);

		if (!exprType1.equals("int[]"))
			throw new TypeCheckException(argu, "Expression on left side of [] is not of type int[]");

		String exprType2 = n.f2.accept(this, argu);

		if (!exprType2.equals("int"))
			throw new TypeCheckException(argu, "Expression inside [] is not of type int");

		return "int";
	}

	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "."
	 * f2 -> "length"
	*/
	@Override
	public String visit(ArrayLength n, String[] argu) throws Exception {
		String exprType = n.f0.accept(this, argu);

		if (!exprType.equals("int[]"))
			throw new TypeCheckException(argu, "Expression on left side of array.length is not of type int[]");

		return "int";
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
	public String visit(MessageSend n, String[] argu) throws Exception {
		// Class name
		String exprType = n.f0.accept(this, argu);

		ClassSymbol callClass = symbols.getClassSymbol(exprType);
		if (callClass == null)
			throw new TypeCheckException(argu, "Attempt to call method on non-class (" + exprType + ')');

		// Method name
		String idName = n.f2.accept(this, argu);

		// Pick up arguments
		String argList = null;
		if (n.f4.present())
			argList = n.f4.accept(this, argu);

		MethodSymbol classMethod = callClass.getMethod(idName);
		if (classMethod == null)
			throw new TypeCheckException(argu, "class " + callClass.getName() + " has no method named " + idName + "()");

		// Compare temporary method construct with signature of normal class method
		MethodSymbol callMethod = new MethodSymbol(classMethod.getType(), idName);

		if (argList != null) {
			StringTokenizer args = new StringTokenizer(argList, ",");
			int counter = 0;       /* Used to get hashing to work */

			while (args.hasMoreTokens()) {
				String argType = args.nextToken();
				callMethod.addParameter(new Symbol(argType, "a" + counter++));
			}
		}

		if (!classMethod.equals(callMethod))
			throw new TypeCheckException(argu, "Incorrect use of method " + callClass.getName() + '.' + callMethod.getName() + "()");

		return callMethod.getType();
	}

	/**
	 * f0 -> Expression()
	 * f1 -> ExpressionTail()
	*/
	@Override
	public String visit(ExpressionList n, String[] argu) throws Exception {
		String[] argList = new String[3];

		/* Carry class and method names to lowe levels of recursion as normal */
		argList[0] = argu[0];
		argList[1] = argu[1];

		/* Append arguments types from expressions */
		argList[2] = n.f0.accept(this, argList);
		n.f1.accept(this, argList);

		return argList[2];
	}

	/**
	 * f0 -> ","
	 * f1 -> Expression()
	*/
	@Override
	public String visit(ExpressionTerm n, String[] argu) throws Exception {
		argu[2] += "," + n.f1.accept(this, argu);
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
	public String visit(PrimaryExpression n, String[] argu) throws Exception {
		String exprType = n.f0.accept(this, argu);

		if (symbols.isValidType(exprType))
			return exprType;

		if (exprType.equals("this"))
			return argu[0];                     /* The class name */

		/* At this point, exprType is an identifier */
		String idType = symbols.getFieldType(argu[0], argu[1], exprType);
		if (idType == null)
			throw new TypeCheckException(argu, "Identifier " + exprType + " is undefined");

		return idType;
	}

	//TODO line numbers in msg? Low priority

	/**
	 * f0 -> <INTEGER_LITERAL>
	*/
	@Override
	public String visit(IntegerLiteral n, String[] argu) throws Exception {
		return "int";
	}

	/**
	 * f0 -> "true"
	*/
	@Override
	public String visit(TrueLiteral n, String[] argu) throws Exception {
		return "boolean";
	}

	/**
	 * f0 -> "false"
	*/
	@Override
	public String visit(FalseLiteral n, String[] argu) throws Exception {
		return "boolean";
	}

	/**
	 * f0 -> "this"
	*/
	@Override
	public String visit(ThisExpression n, String[] argu) throws Exception {
		return "this";
	}

	/**
	 * f0 -> "new"
	 * f1 -> "int"
	 * f2 -> "["
	 * f3 -> Expression()
	 * f4 -> "]"
	*/
	@Override
	public String visit(ArrayAllocationExpression n, String[] argu) throws Exception {
		String exprType = n.f3.accept(this, argu);

		if (!exprType.equals("int"))
			throw new TypeCheckException(argu, "Expression used for size of array allocation must be int");

		return "int[]";
	}

	/**
	 * f0 -> "new"
	 * f1 -> Identifier()
	 * f2 -> "("
	 * f3 -> ")"
	*/
	@Override
	public String visit(AllocationExpression n, String[] argu) throws Exception {
		String className = n.f1.accept(this, argu);

		if (!symbols.hasClass(className))
			throw new TypeCheckException(argu, "Type " + className + "is not a defined class");

		return className;
	}

	/**
	 * f0 -> "!"
	 * f1 -> Clause()
	*/
	@Override
	public String visit(NotExpression n, String[] argu) throws Exception {
		String exprType = n.f1.accept(this, argu);

		if (!exprType.equals("boolean"))
			throw new TypeCheckException(argu, "Expression used in logical \"not\" is not of type boolean");

		return "boolean";
	}

	/**
	 * f0 -> "("
	 * f1 -> Expression()
	 * f2 -> ")"
	*/
	@Override
	public String visit(BracketExpression n, String[] argu) throws Exception {
		return n.f1.accept(this, argu);
	}
}
