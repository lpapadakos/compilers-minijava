package symbol_table;

import java.util.*;

public class ClassSymbol extends Symbol {
	private String parent;
	private Map<String, Symbol> fields = new LinkedHashMap<>();
	private Map<String, MethodSymbol> methods = new LinkedHashMap<>();

	// ClassExtendsDeclaration
	public ClassSymbol(String name, String parent) throws Exception {
		// TODO maybe type null for non basic types?
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
}
