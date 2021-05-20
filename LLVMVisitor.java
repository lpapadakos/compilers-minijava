import java.io.BufferedWriter;

import syntaxtree.*;
import visitor.*;
import symbol.*;

public class LLVMVisitor {
	private SymbolTable symbols;
	private BufferedWriter ll;

	public LLVMVisitor(SymbolTable symbols, BufferedWriter ll) {
		this.symbols = symbols;
		this.ll = ll;
	}

	private void emit(String line) throws Exception {
		ll.write(line);
		ll.newLine();
	}
}
