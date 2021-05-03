package symbol;

import java.util.*;

public class SymbolTable {
	// Use LinkedHashMap to get nice indexing
	private Map<String, ClassSymbol> classes = new LinkedHashMap<>();

	public void addClass(ClassSymbol c) {
		classes.put(c.getName(), c);
	}

	public ClassSymbol getClassSymbol(String name) {
		return classes.get(name);
	}

	public boolean hasClass(String name) {
		return (getClassSymbol(name) != null);
	}

	public boolean isValidType(String type) {
		return (Symbol.isBasicType(type) || hasClass(type));
	}

	public boolean isSubclass(String derived, String base) {
		if (!hasClass(derived) || !hasClass(base))
			return false;

		return getClassSymbol(derived).isSubclassOf(getClassSymbol(base));
	}

	public boolean typesMatch(String derived, String base) {
		if (derived.equals(base))
			return isValidType(derived);  /* Basic types here too */
		else
			return isSubclass(derived, base);
	}

	public boolean typesMatch(Symbol derived, Symbol base) {
		return typesMatch(derived.getType(), base.getType());
	}

	public String getFieldType(String className, String methodName, String name) {
		Symbol target = null;

		if (className == null)
			return null;

		ClassSymbol c = getClassSymbol(className);
		if (c == null)
			return null;

		/* Use nearest definition of symbol, since local variables
		 * shadow class fields */
		if (methodName != null) {
			MethodSymbol method = c.getMethod(methodName);
			if (method != null) {
				target = method.getField(name);

				if (target == null)
					target = method.getParameter(name);
			}
		}

		if (target == null)
			target = c.getField(name);

		if (target == null)
			return null;
		else
			return target.getType();
	}

	public boolean compatibleCall(MethodSymbol definition, MethodSymbol call) {
		if (!definition.getName().equals(call.getName()))
			return false;

		List<Symbol> definitionParams = definition.getParameters();
		List<Symbol> callParams = call.getParameters();

		if (definitionParams.size() != callParams.size())
			return false;

		for (int i = 0; i < callParams.size(); ++i) {
			/* Call can pass subtype of argument type */
			if (!typesMatch(callParams.get(i), definitionParams.get(i)))
				return false;
		}

		/* Arguments are used correctly */
		return true;
	}

	public void printOffsets() {
		for (ClassSymbol c: classes.values()) {
			for (Symbol field: c.getFields())
				System.out.println(c.getName() + '.' + field.getName() + " : " + field.getOffset());

			for (MethodSymbol method: c.getMethods()) {
				if (!method.isOverride())
					System.out.println(c.getName() + '.' + method.getName() + " : " + method.getOffset());
			}
		}
	}
}
