package symbol;

public class Symbol {
	private String type;
	private String name;

	public Symbol(String type, String name) {
		this.type = type;
		this.name = name;

		//DEBUG
		System.out.println("New symbol \"" + type + ' ' + name + '"');
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public boolean hasBasicType() {
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
