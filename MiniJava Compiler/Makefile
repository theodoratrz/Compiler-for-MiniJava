all: compile

compile:
	java -jar jtb132di.jar -te minijava.jj
	java -jar javacc5.jar minijava-jtb.jj
	javac Main.java

clean:
	rm -r visitor
	rm -r syntaxtree
	rm Token*.java
	rm ParseException.java
	rm MiniJavaParser*.java
	rm JavaCharStream.java
	rm minijava-jtb.jj
	rm *.class
	rm Visitors/*.class
	rm ClassInfo/*.class
	rm SymbolTable/*.class