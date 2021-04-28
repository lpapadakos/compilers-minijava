package symbol_table;

import java.util.*;
import symbol_table.*;

public class SymbolTable {
	// Use LinkedHashMap to get nice indexing
	private Map<String, Symbol> classes = new LinkedHashMap<>();
	// TODO: offsets or sth

	public Type getType(String name) {
		if (classes.containsKey(name)) {
			return Type.CLASS;
		}
	}

	public Type getType(String className, String methodName, String name) {
		//Class symbol, search method if not null, e.t.c.
	}

	//subtypes, make offset things happen
}
