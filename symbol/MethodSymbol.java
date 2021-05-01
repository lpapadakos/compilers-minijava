package symbol;

import java.util.*;

public class MethodSymbol extends FieldContainerSymbol {
	private Map<String, Symbol> params = new LinkedHashMap<>();

	public MethodSymbol(String returnType, String name) {
		super(returnType, name);
	}

	public void addParameter(Symbol param) {
		params.put(param.getName(), param);
	}

	public List<Symbol> getParameters() {
		return new ArrayList<>(params.values());
	}

	public boolean hasParameter(String name) {
		return params.containsKey(name);
	}
}
