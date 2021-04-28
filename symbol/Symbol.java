package symbol;

enum Type {
	VOID, BOOLEAN, INT, INT_ARRAY, STRING_ARRAY, CLASS;

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

			case "class":
				return CLASS;

			default:
				throw new TypeCheckException('"' + type + "\" is not a valid type for MiniJava!");
		}
	}
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

	//TODO: hmm Maybe instance of or sth else?
	public boolean hasField(String name) {
		return false;
	}

	public void addField(Symbol field) {

	}
}
