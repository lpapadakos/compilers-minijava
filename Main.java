import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.file.*;

import syntaxtree.*;
import symbol.*;

public class Main {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("You need to pass arguments like so: <inputFile1> <inputfile2> ... <inputFileN>");
			System.exit(1);
		}

		String prettyLine = String.format("%080d", 0).replace('0', '-');

		for (String filename: args) {
			/* Pretty-print file basename */
			String basename = filename.substring(filename.lastIndexOf('/') + 1);
			System.out.println(basename);
			System.out.println(prettyLine);

			/* Prepare output filename */
			String outname = filename.replace(".java", ".ll");

			try (FileInputStream input = new FileInputStream(filename);
			     BufferedWriter output = new BufferedWriter(new FileWriter(outname))) {
				/* Parsing: Make AST */
				Goal root = new MiniJavaParser(input).Goal();

				/* Semantic Checking Phase 1: Populate Symbol Table */
				SymbolTable symbols = new SymbolTable();
				root.accept(new SymbolVisitor(symbols), null);

				/* Semantic Checking Phase 2: Type checking, using Symbol Table */
				root.accept(new TypeCheckVisitor(symbols), null);

				//DEBUG offset printing
				//symbols.printOffsets();

				/* LLVM IR Generation */
				root.accept(new LLVMVisitor(symbols, output), null);
				System.out.println("Generated LLVM IR: " + outname);
			} catch (Exception e) {
				//DEBUG whole stacktrace
				//e.printStackTrace();

				System.err.println("\033[1;31m" + e.getClass().getSimpleName() + "\033[0m: " + e.getMessage());
			}

			System.out.println();
		}
	}
}
