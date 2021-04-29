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
		return classes.containsKey(name);
	}

	public ClassSymbol getClassSymbol(String name) {
		return classes.get(name);
	}

	public boolean isSubclass(String c, String ancestor) {
		if (!hasClass(c) || !hasClass(ancestor))
			return false;

		return getClassSymbol(c).isSubclassOf(getClassSymbol(ancestor));
	}

	//TODO subtypes, make offset things happen (on demand.. we'll see)
	/* public String getType(String className, String methodName, String name) {
		//TODO Class symbol, search method if not null, e.t.c.
		return null;
	} */
}
