package symbol;

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

	public void addField(Symbol field) {
		localVars.put(field.getName(), field);
	}

	public boolean hasField(String name) {
		return localVars.containsKey(name);
	}
}
