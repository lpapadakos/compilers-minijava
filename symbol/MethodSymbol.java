package symbol;

import java.util.*;

public class MethodSymbol extends FieldContainerSymbol {
	private boolean override = false;
	private final Map<String, Symbol> params = new LinkedHashMap<>();

	public MethodSymbol(String returnType, String name) {
		super(returnType, name);
	}

	public void setOverride() {
		override = true;
	}

	public void addParameter(Symbol param) {
		params.put(param.getName(), param);
	}

	public boolean isOverride() {
		return override;
	}

	public Symbol getParameter(String name) {
		return params.get(name);
	}

	public List<Symbol> getParameters() {
		return new ArrayList<>(params.values());
	}

	public boolean hasParameter(String name) {
		return (getParameter(name) != null);
	}

	/* Test if functions have the same signature (i.e. override) */
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof MethodSymbol))
			return false;

		MethodSymbol candidate = (MethodSymbol) o;

		if (!sameTypeAs(candidate))
			return false;

		if (!getName().equals(candidate.getName()))
			return false;

		List<Symbol> thisParams = getParameters();
		List<Symbol> candidateParams = candidate.getParameters();

		if (thisParams.size() != candidateParams.size())
			return false;

		for (int i = 0; i < candidateParams.size(); ++i) {
			/* Exactly the same argument types, for override */
			if (!thisParams.get(i).sameTypeAs(candidateParams.get(i)))
				return false;
		}

		// Functions are the same, which means overriding (if in the same class)
		return true;
	}
}
