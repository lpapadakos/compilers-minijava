package symbol;

public class Symbol {
	private String type;
	private String name;

	public Symbol(String type, String name) {
		this.type = type;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public boolean hasType(String type) {
		return getType().equals(type);
	}

	public boolean sameTypeAs(Symbol s) {
		return hasType(s.getType());
	}

	public static boolean hasBasicType(String type) {
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
