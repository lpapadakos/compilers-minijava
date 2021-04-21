import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import syntaxtree.*;
import visitor.*;

public class Main {
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("You need to pass arguments like so: <inputFile1> <inputfile2> ... <inputFileN>");
			System.exit(1);
		}

		FileInputStream input = null;
		for (String file: args) {
			try {
				String basename = file.substring(file.lastIndexOf('/') + 1);
				System.out.println(String.format("%-80s", basename).replace(' ', '=').replaceFirst("=", " "));

				input = new FileInputStream(file);
				Goal root = new MiniJavaParser(input).Goal();

				System.out.println("Apparently, this program was parsed correctly... Huh.");

				// TODO: this is where visitor stuff would go
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (input != null)
						input.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}

class SemanticVisitor extends GJDepthFirst<String, String> {

}