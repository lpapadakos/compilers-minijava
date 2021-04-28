package symbol_table;

enum Type {
	INT, BOOLEAN, INT_ARRAY, CLASS;
}

public class Symbol {
	private String name;
	private Type type;

	public Symbol(String name, String type) throws Exception {
		setName(name);
		setType(type);
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setType(String type) throws Exception {
		switch(type) {
			case "int":
				this.type = Type.INT;
				break;

			case "boolean":
				this.type = Type.BOOLEAN;
				break;

			case "int[]":
				this.type = Type.INT_ARRAY;
				break;

			case "class":
				this.type = Type.CLASS;
				break;

			default:
				//TODO: Special Exception type?
				throw new Exception('"' + type + "\" is not a valid type for MiniJava!");
		}
	}

}
