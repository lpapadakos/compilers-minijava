package symbol;

import java.util.*;

public class ClassSymbol extends FieldContainerSymbol {
	private ClassSymbol parent;           /* MiniJava: Single Inheritance */
	private final Map<String, MethodSymbol> methods = new LinkedHashMap<>();
	private int lastFieldOffset = 0;
	private int lastMethodOffset = 0;

	// ClassDeclaration
	public ClassSymbol(String name) throws Exception {
		super("class", name);
	}

	// ClassExtendsDeclaration
	public ClassSymbol(String name, ClassSymbol parent) throws Exception {
		this(name);
		this.parent = parent;

		/* Resume counters from parent class */
		lastFieldOffset = parent.getLastFieldOffset();
		lastMethodOffset = parent.getLastMethodOffset();
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

	@Override
	public Collection<Symbol> getFields() {
		// Optimization: Many classes might not extend some other class. No need to putAll()
		if (parent == null)
			return super.getFields();

		Map<String, Symbol> allFields = new LinkedHashMap<>();

		putFields(allFields);
		return allFields.values();
	}

	private void putFields(Map<String, Symbol> allFields) {
		if (parent != null)
			parent.putFields(allFields);

		/* In HashMaps, put() overrides values for existing keys.
		 * The subclasses fields shadow the parent ones, in the case of a name conflict */
		allFields.putAll(fields);
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
		// Optimization: Many classes might not extend some other class. No need to putAll()
		if (parent == null)
			return methods.values();

		Map<String, MethodSymbol> allMethods = new LinkedHashMap<>();

		putMethods(allMethods);
		return allMethods.values();
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

		/* This recursively looks up the parent chain */
		MethodSymbol existing = getMethod(candidate.getName());

		if (existing == null)
			return false;

		/* Can't exactly override (same signature) in the same class, so if the name exists already, it's an error */
		if (existing.getOwner() == this)
			return true;

		/* At this point the method was found in a parent class.
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
		else if (parent == ancestor)
			return true;
		else
			return parent.isSubclassOf(ancestor);
	}

	public void printOffsets() {
		for (Symbol field: super.getFields())
			System.out.println(getName() + '.' + field.getName() + " : " + field.getOffset());

		for (MethodSymbol method: methods.values()) {
			if (!method.isStatic() && !method.isOverride())
				System.out.println(getName() + '.' + method.getName() + "() : " + method.getOffset());
		}
	}
}
