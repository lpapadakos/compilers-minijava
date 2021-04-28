import java.io.FileInputStream;

import syntaxtree.*;
import symbol_table.*;
import mj_visitor.*;

public class Main {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("You need to pass arguments like so: <inputFile1> <inputfile2> ... <inputFileN>");
			System.exit(1);
		}

		for (String filename: args) {
			String basename = filename.substring(filename.lastIndexOf('/') + 1);
			System.out.println(String.format("%-80s", basename).replace(' ', '=').replaceFirst("=", " "));

			try (FileInputStream input = new FileInputStream(filename)) {
				Goal root = new MiniJavaParser(input).Goal();

				//DEBUG
				System.out.println("Apparently, this program was parsed correctly... Huh.");

				SymbolTable sTable = new SymbolTable();

				//TypeNameVisitor firstPhase = new TypeNameVisitor(sTable);
				//root.accept(firstPhase, null);
			} catch (Exception e) {
				e.printStackTrace();
			}

			System.out.println();
		}
	}
}
