all: libs mine

libs:
	java -jar jtb132di.jar -te minijava.jj
	java -jar javacc5.jar minijava-jtb.jj

mine:
	javac Main.java

clean:
	rm -rf syntaxtree/ visitor/ symbol/*.class *.class JavaCharStream.java  Mini*.java ParseException.java Token*.java minijava-jtb.jj

.PHONY: all libs mine clean
