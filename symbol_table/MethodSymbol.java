package symbol_table;

import java.util.*;

public class MethodSymbol extends Symbol {
	private Map<String, Symbol> params = new LinkedHashMap<>();
	private Map<String, Symbol> localVars = new LinkedHashMap<>();

	public MethodSymbol(String returnType, String name) throws Exception {
		super(returnType, name);
	}

	public void addParameter(Symbol param) {
		params.put(param.getName(), param);
	}

	public void addLocalVariable(Symbol var) {
		localVars.put(var.getName(), var);
	}

	// TODO: Getters on demand
}
