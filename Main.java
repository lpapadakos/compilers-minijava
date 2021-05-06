import java.io.FileInputStream;

import syntaxtree.*;
import symbol.*;

public class Main {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("You need to pass arguments like so: <inputFile1> <inputfile2> ... <inputFileN>");
			System.exit(1);
		}

		for (String filename: args) {
			/* Pretty-print file basename */
			String basename = filename.substring(filename.lastIndexOf('/') + 1);
			System.out.println(basename);
			System.out.println(String.format("%080d", 0).replace('0', '-'));

			try (FileInputStream input = new FileInputStream(filename)) {
				/* Parsing: Make AST */
				Goal root = new MiniJavaParser(input).Goal();

				/* Semantic Checking Phase 1: Populate Symbol Table */
				SymbolTable symbols = new SymbolTable();

				SymbolVisitor firstPhase = new SymbolVisitor(symbols);
				root.accept(firstPhase, null);

				/* Semantic Checking Phase 2: Type checking, using Symbol Table */
				TypeCheckVisitor secondPhase = new TypeCheckVisitor(symbols);
				root.accept(secondPhase, null);

				/* Print offsets */
				symbols.printOffsets();
			} catch (Exception e) {
				//DEBUG whole stacktrace
				//e.printStackTrace();

				System.err.println("\033[1;31mERROR\033[0m: " + e.getMessage());
			}

			System.out.println();
		}
	}
}
