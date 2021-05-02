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
		if (type.equals("boolean"))
			return 1;
		else if (type.equals("int"))
			return 4;
		else
			return 8;                 /* Other types are pointers */
	}

	public int getOffset() {
		return offset;
	}

	public boolean hasType(String type) {
		return getType().equals(type);
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
