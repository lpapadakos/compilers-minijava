package symbol;
//TODO package all the things, might help with import?

import java.util.*;

public class ClassSymbol extends FieldContainerSymbol {
	private ClassSymbol parent;           /* MiniJava: Single Inheritance */
	private Map<String, MethodSymbol> methods = new LinkedHashMap<>();

	// ClassExtendsDeclaration
	public ClassSymbol(String name, ClassSymbol parent) throws Exception {
		super("class", name);
		this.parent = parent;
	}

	// ClassDeclaration
	public ClassSymbol(String name) throws Exception {
		this(name, null);
	}

	public void addMethod(MethodSymbol method) {
		methods.put(method.getName(), method);
	}

	/* These 3 recurse up the inheritance tree to locate a field or function */
	@Override
	public Symbol getField(String name) {
		Symbol field = super.getField(name);

		if (field != null)
			return field;
		else if (parent == null)
			return null;
		else
			return parent.getField(name);
	}

	public MethodSymbol getMethod(String name) {
		MethodSymbol method = methods.get(name);

		if (method != null)
			return method;
		else if (parent == null)
			return null;
		else
			return parent.getMethod(name);
	}

	public boolean hasMethod(String name) {
		return (getMethod(name) != null);
	}

	/* isOverload() is used only during the initial filling of the Symbol Table,
	 * to determine if function overload is occurring (illegal in MiniJava) */
	//TODO maybe global with classname
	public boolean isOverload(MethodSymbol method) {
		/* This depends on the recursive definition of getMethod() */
		MethodSymbol candidate = getMethod(method.getName());

		if (candidate == null)
			return false;

		/* Can't exactly override (same signature) in the same class, so if the name exists already, it's an error */
		if (methods.containsKey(method.getName()))
			return true;

		/* At this point the method was found in a parent class.
		 * Compare signature to see if we're overloading or not */
		if (!method.sameTypeAs(candidate))
			return true;

		List<Symbol> methodParams = method.getParameters();
		List<Symbol> candidateParams = candidate.getParameters();

		if (methodParams.size() != candidateParams.size())
			return true;

		for (int i = 0; i < methodParams.size(); ++i) {
			if (!methodParams.get(i).sameTypeAs(candidateParams.get(i)))
				return true;
		}

		// Functions are the same, which means overriding
		return false;
	}

	public boolean isSubclassOf(ClassSymbol ancestor) {
		if (parent == null)
			return false;
		else if (parent.equals(ancestor))
			return true;
		else
			return parent.isSubclassOf(ancestor);
	}
}
