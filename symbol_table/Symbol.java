package symbol_table;

enum Type {
	BOOLEAN, INT, INT_ARRAY, STRING_ARRAY, METHOD, CLASS;

	public static Type toEnum(String type) throws Exception {
		switch(type) {
			case "void":
				//tODO: null?
				return VOID;

			case "boolean":
				return BOOLEAN;

			case "int":
				return INT;

			case "int[]":
				return INT_ARRAY;

			case "String[]":
				return STRING_ARRAY;

			case "method":
				return METHOD;

			case "class":
				return CLASS;

			default:
				//TODO: Special Exception type?
				throw new Exception('"' + type + "\" is not a valid type for MiniJava!");
		}
	}

	//TODO basic type check?
}

public class Symbol {
	private Type type;
	private String name;

	public Symbol(String type, String name) throws Exception {
		setName(name);
		setType(type);
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setType(String type) throws Exception {
		this.type = Type.toEnum(type);
	}

}
