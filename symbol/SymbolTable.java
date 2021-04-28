package symbol;

import java.util.*;

public class SymbolTable {
	// Use LinkedHashMap to get nice indexing
	private Map<String, Symbol> classes = new LinkedHashMap<>();
	// TODO: offsets or sth

	public boolean hasClass(String name) {
		return classes.containsKey(name);
	}

	public Type getType(String className, String methodName, String name) {
		//TODO Class symbol, search method if not null, e.t.c.
		return null;
	}

	public void addClass(ClassSymbol c) {
		classes.put(c.getName(), c);
	}

	//TODO subtypes, make offset things happen
}
