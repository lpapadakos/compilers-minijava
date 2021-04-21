all:
	java -jar jtb132di.jar -te minijava.jj
	java -jar javacc5.jar minijava-jtb.jj
	javac Main.java

clean:
	rm -rf syntaxtree/ visitor/ *~ *.class JavaCharStream.java  Mini*.java ParseException.java Token*.java minijava-jtb.jj

.PHONY: all clean
