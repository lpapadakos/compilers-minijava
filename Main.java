import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import syntaxtree.*;
public class Main {
	public static void main(String[] args) throws Exception {
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

				// TODO: Dunno what's happening
				SemanticVisitor v = new SemanticVisitor();
				root.accept(v, "");
			} catch (Exception e) {
				e.printStackTrace();
			}

			System.out.println();
		}
	}
}
