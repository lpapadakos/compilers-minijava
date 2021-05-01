package symbol;

import java.util.*;

public class SymbolTable {
	// Use LinkedHashMap to get nice indexing
	private Map<String, ClassSymbol> classes = new LinkedHashMap<>();
	// TODO: offsets

	public void addClass(ClassSymbol c) {
		classes.put(c.getName(), c);
	}

	public boolean hasClass(String name) {
		return (getClassSymbol(name) != null);
	}

	public ClassSymbol getClassSymbol(String name) {
		return classes.get(name);
	}

	public boolean isValidType(String type) {
		return (Symbol.hasBasicType(type) || hasClass(type));
	}

	public boolean isSubclass(String c, String ancestor) {
		if (!hasClass(c) || !hasClass(ancestor))
			return false;

		return getClassSymbol(c).isSubclassOf(getClassSymbol(ancestor));
	}

	public boolean typesMatch(String derived, String base) {
		if (derived.equals(base))
			return isValidType(derived);  /* Basic types here too */
		else
			return isSubclass(derived, base);
	}

	public String getFieldType(String className, String methodName, String name) {
		ClassSymbol c = getClassSymbol(className);
		if (c == null)
			return null;

		MethodSymbol method = c.getMethod(methodName);
		if (method == null)
			return null;

		/* Use nearest definition of symbol, since local variables
		 * shadow class fields */
		//TODO what if using parent's field in own method
		Symbol target = method.getField(name);
		if (target == null)
			target = c.getField(name);

		if (target == null)
			return null;
		else
			return target.getType();
	}

	//TODO make offset things happen (on demand.. we'll see)
}
