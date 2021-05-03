package symbol;
//TODO package all the things, might help with import?

import java.util.*;

public class ClassSymbol extends FieldContainerSymbol {
	private ClassSymbol parent;           /* MiniJava: Single Inheritance */
	private Map<String, MethodSymbol> methods = new LinkedHashMap<>();
	private int lastFieldOffset = 0;
	private int lastMethodOffset = 0;

	// ClassExtendsDeclaration
	public ClassSymbol(String name, ClassSymbol parent) throws Exception {
		super("class", name);
		this.parent = parent;

		if (parent != null) {
			/* Resume counters from parent class */
			lastFieldOffset = parent.getLastFieldOffset();
			lastMethodOffset = parent.getLastMethodOffset();
		}
	}

	// ClassDeclaration
	public ClassSymbol(String name) throws Exception {
		this(name, null);
	}

	@Override
	public void addField(Symbol field) {
		field.setOffset(lastFieldOffset);
		lastFieldOffset += field.getSize();

		super.addField(field);
	}

	public void addMethod(MethodSymbol method) {
		method.setOffset(lastMethodOffset);
		lastMethodOffset += method.getSize();

		methods.put(method.getName(), method);
	}

	/* These 3 recurse up the inheritance tree to locate a field or function */
	@Override
	public Symbol getField(String name) {
		Symbol field = super.getField(name);

		if (field != null)
			return field;
		else if (parent == null)
			return null;
		else
			return parent.getField(name);
	}

	public MethodSymbol getMethod(String name) {
		MethodSymbol method = methods.get(name);

		if (method != null)
			return method;
		else if (parent == null)
			return null;
		else
			return parent.getMethod(name);
	}

	public Collection<MethodSymbol> getMethods() {
		return methods.values();
	}

	public boolean hasMethod(String name) {
		return (getMethod(name) != null);
	}

	public boolean hasMethodLocally(String name) {
		return methods.containsKey(name);
	}

	public boolean isSubclassOf(ClassSymbol ancestor) {
		if (parent == null)
			return false;
		else if (parent.equals(ancestor))
			return true;
		else
			return parent.isSubclassOf(ancestor);
	}

	public int getLastFieldOffset() {
		return lastFieldOffset;
	}

	public int getLastMethodOffset() {
		return lastMethodOffset;
	}
}
