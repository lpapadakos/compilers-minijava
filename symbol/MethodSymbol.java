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

	public Symbol getParameter(String name) {
		return params.get(name);
	}

	public List<Symbol> getParameters() {
		return new ArrayList<>(params.values());
	}

	public boolean hasParameter(String name) {
		return (getParameter(name) != null);
	}

	/* Compare signatures of 2 functions */
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof MethodSymbol))
			return false;

		MethodSymbol candidate = (MethodSymbol) o;

		/* At this point the method was found in a parent class.
		 * Compare signature to see if we're overloading or not */
		if (!sameTypeAs(candidate))
			return false;

		if (!getName().equals(candidate.getName()))
			return false;


		List<Symbol> methodParams = getParameters();
		List<Symbol> candidateParams = candidate.getParameters();

		if (methodParams.size() != candidateParams.size())
			return false;

		for (int i = 0; i < methodParams.size(); ++i) {
			if (!methodParams.get(i).sameTypeAs(candidateParams.get(i)))
				return false;
		}

		// Functions are the same, which means overriding (if in the same class)
		return true;
	}
}
