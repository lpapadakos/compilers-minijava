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

	// /* This version of hasMethod() compares signatures as well */
	// public boolean hasMethod(MethodSymbol candidate) {
	// 	if (candidate == null)
	// 		return false;

	// 	/* This depends on the recursive definition of getMethod() */
	// 	MethodSymbol existing = getMethod(candidate.getName());

	// 	if (existing == null)
	// 		return false;

	// 	return existing.equals(candidate);
	// }

	/* isOverload() is used only during the initial filling of the Symbol Table,
	 * to determine if function overload is occurring (illegal in MiniJava) */
	public boolean isOverload(MethodSymbol candidate) {
		if (candidate == null)
			return false;

		MethodSymbol existing = getMethod(candidate.getName());

		if (existing == null)
			return false;

		/* Can't exactly override (same signature) in the same class, so if the name exists already, it's an error */
		if (methods.containsKey(existing.getName()))
			return true;

		/* We know the names are the same.
		 * If the signature doesn't match, overloading is occurring */
		return !existing.equals(candidate);
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
