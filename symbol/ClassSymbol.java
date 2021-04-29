package symbol;
//TODO package all the things, might help with import?

import java.util.*;

public class ClassSymbol extends Symbol {
	private ClassSymbol parent;           /* MiniJava: Single Inheritance */
	private Map<String, Symbol> fields = new LinkedHashMap<>();
	private Map<String, MethodSymbol> methods = new LinkedHashMap<>();

	// ClassExtendsDeclaration
	public ClassSymbol(String name, ClassSymbol parent) throws Exception {
		// TODO maybe type null for non basic types?
		// TODO inline initialization vs here
		super("class", name);
		this.parent = parent;
	}

	// ClassDeclaration
	public ClassSymbol(String name) throws Exception{
		this(name, null);
	}

	public void addField(Symbol field) {
		fields.put(field.getName(), field);
	}

	public void addMethod(MethodSymbol method) {
		methods.put(method.getName(), method);
	}

	//TODO: Change to get == null?
	public boolean hasField(String name) {
		return fields.containsKey(name);
	}

	public boolean hasMethod(String name) {
		return methods.containsKey(name);
	}

	public MethodSymbol getMethod(String name) {
		return methods.get(name);
	}

	public boolean isOverload(MethodSymbol method) {
		/* Can't exactly override (same signature) in the same class, so if the name exists already, it's an error */
		if (hasMethod(method.getName()))
			return true;
		else if (parent == null) /* Defined for the first time in this class */
			return false;
		else
			return parent.isOverloadParent(method); /* Look at parent to see if we're overloading or not */
	}

	private boolean isOverloadParent(MethodSymbol method) {
		if (!hasMethod(method.getName())) {
			if (parent == null)
				return false;
			else
				return parent.isOverloadParent(method);
		}

		/* At this point, some ancestor class has defined the function by name.
		 * If the signature matches, we're overriding in the subclass (All OK)
		 */
		MethodSymbol candidate = getMethod(method.getName());

		if (!(method.getType().equals(candidate.getType())))
			return true;

		List<Symbol> methodParams = method.getParameters();
		List<Symbol> candidateParams = candidate.getParameters();

		if (methodParams.size() != candidateParams.size())
			return true;

		for (int i = 0; i < methodParams.size(); ++i) {
			//TODO: do parameter names matter for overloading? probably not
			if (methodParams.get(i).getType() != candidateParams.get(i).getType())
				return true;
		}

		// Functions are the same, which means overriding
		return false;
	}

	public boolean isSubclassOf(ClassSymbol ancestor) {
		if (parent == null)
			return false;
		else if (parent.equals(ancestor))
			return true;
		else
			return parent.isSubclassOf(ancestor);
	}
}
