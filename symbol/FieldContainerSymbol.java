package symbol;

import java.util.*;

/* Abstract class to cover constructs that can contain fields (e.g. Class, Method)*/
public abstract class FieldContainerSymbol extends Symbol {
	private final Map<String, Symbol> fields = new LinkedHashMap<>();

	public FieldContainerSymbol(String type, String name) {
		super(type, name);
	}

	public void addField(Symbol field) {
		fields.put(field.getName(), field);
	}

	@Override
	public int getSize() {
		/* Class and Method declarations are pointers in this case */
		return 8;
	}

	public Symbol getField(String name) {
		return fields.get(name);
	}

	public Collection<Symbol> getFields() {
		return fields.values();
	}

	public boolean hasField(String name) {
		return (getField(name) != null);
	}

	/* hasFieldLocally() is used to ensure name uniqueness withing the same class.
	 * subclasses can actually shadow parent fields */
	public boolean hasFieldLocally(String name) {
		return fields.containsKey(name);
	}
}
