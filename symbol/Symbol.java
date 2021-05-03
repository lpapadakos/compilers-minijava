package symbol;

public class Symbol {
	private String type;
	private String name;
	private int offset;

	public Symbol(String type, String name) {
		this.type = type;
		this.name = name;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public int getSize() {
		if (this instanceof FieldContainerSymbol) /* Class and Method declarations */
			return 8;
		else if (type.equals("boolean"))
			return 1;
		else if (type.equals("int"))
			return 4;
		else
 			return 8;              /* int[], Instances of classes */
	}

	public int getOffset() {
		return offset;
	}

	public boolean hasType(String type) {
		return this.type.equals(type);
	}

	public boolean sameTypeAs(Symbol s) {
		return hasType(s.getType());
	}

	public static boolean isBasicType(String type) {
		switch(type) {
			case "boolean":
			case "int":
			case "int[]":
				return true;

			default:
				return false;
		}
	}
}
