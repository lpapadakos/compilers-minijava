package symbol;
//TODO pacage all the things, might help with import?

import java.util.*;

public class ClassSymbol extends Symbol {
	private String parent;
	private Map<String, Symbol> fields = new LinkedHashMap<>();
	private Map<String, MethodSymbol> methods = new LinkedHashMap<>();

	// ClassExtendsDeclaration
	public ClassSymbol(String name, String parent) throws Exception {
		// TODO maybe type null for non basic types?
		// TODO inline initializtion vs here
		super("class", name);
		this.parent = parent;
	}

	// ClassDeclaration
	public ClassSymbol(String name) throws Exception{
		this(name, null);
	}

	public void addMethod(MethodSymbol method) {
		methods.put(method.getName(), method);
	}

	public void addField(Symbol field) {
		fields.put(field.getName(), field);
	}

	public boolean hasMethod(String name) {
		return methods.containsKey(name);
	}

	//TODO: Change to get == null?
	public boolean hasField(String name) {
		return fields.containsKey(name);
	}
}
