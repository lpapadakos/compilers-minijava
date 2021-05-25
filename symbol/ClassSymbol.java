package symbol;

import java.util.*;

public class ClassSymbol extends FieldContainerSymbol {
	private ClassSymbol parent;           /* MiniJava: Single Inheritance */
	private final Map<String, MethodSymbol> methods = new LinkedHashMap<>();
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
		/* Essentally, offset is undefined for overrides and static methods */
		if (!method.isStatic() && !method.isOverride()) {
			method.setOffset(lastMethodOffset);
			lastMethodOffset += method.getSize();
		}

		method.setOwner(this);
		methods.put(method.getName(), method);
	}

	/* Size of class is essentially the sum of all its fields
	 (C structs come to mind) */
	@Override
	public int getSize() {
		return lastFieldOffset;
	}

	/* These getters recurse up the inheritance tree to locate a field or function */
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

	//TODO Override getFields with recursive def?

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
		Map<String, MethodSymbol> allMethods = new LinkedHashMap<>();

		putMethods(allMethods);
		return methods.values();
	}

	private void putMethods(Map<String, MethodSymbol> allMethods) {
		if (parent != null)
			parent.putMethods(allMethods);

		/* In HashMaps, put() overrides values for existing keys.
		 * Since overloads don't exist in MiniJava, the functions that
		 * override parent ones will replace them, at the same position in the map. */
		allMethods.putAll(methods);
	}

	public int getMethodsAmount() {
		return lastMethodOffset / 8; // MethodSymbol size
	}

	public int getLastFieldOffset() {
		return lastFieldOffset;
	}

	public int getLastMethodOffset() {
		return lastMethodOffset;
	}

	public boolean hasMethod(String name) {
		return (getMethod(name) != null);
	}

	public boolean isOverload(MethodSymbol candidate) {
		if (candidate == null)
			return false;

		/* Can't exactly override (same signature) in the same class, so if the name exists already, it's an error */
		if (methods.containsKey(candidate.getName()))
			return true;

		/* This recursively looks up the parent chain */
		MethodSymbol existing = getMethod(candidate.getName());

		if (existing == null)
			return false;

		/* At this point the method was found in a parent class of c.
 		 * Compare signature to see if we're overloading or not. */
		if (existing.equals(candidate)) {
			/* candidate is overriding a parent method, borrow offset */
			candidate.setOverride();
			candidate.setOffset(existing.getOffset());

			return false;
		} else {
			return true;
		}
	}

	public boolean isSubclassOf(ClassSymbol ancestor) {
		if (parent == null)
			return false;
		else if (parent.equals(ancestor))
			return true;
		else
			return parent.isSubclassOf(ancestor);
	}

	public void printOffsets() {
		for (Symbol field: getFields())
			System.out.println(getName() + '.' + field.getName() + " : " + field.getOffset());

		for (MethodSymbol method: methods.values()) {
			if (!method.isStatic() && !method.isOverride())
				System.out.println(getName() + '.' + method.getName() + "() : " + method.getOffset());
		}
	}
}
