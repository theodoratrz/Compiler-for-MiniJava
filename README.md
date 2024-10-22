# MiniJava to LLVM-IR Compiler

## Written in Java, it accepts a MiniJava file, performs Semantic checking and generates an equivalent LLVM-IR file that can be compiled to an executable binary by Clang.
### This was a Spring 2021 semester project for the University of Athens Compilers course.

### Compilation:
- Run `make` for compiling all files
- Run `make clean` for cleaning all generated files

### Execution: 
- Run `java Main <file1> <file2>... ` to run for multiple files

### Features:
- The program the input files for semantic errors.
If an error is found, an exception is raised, and it appears in the terminal the line that was 
problematic, followed by the error. If no errors are found,
the program prints the offsets for all the variables and methods for each class.
- We keep a map that includes all the classes(and their information). We pass this map to all visitors. At the end, we
use it to set the offsets of all classes,methods,variables and print them.
- We use a virtual table object to map Methods with their names. When a class has a super class, it stores the superclass methods first, then its own.

### Visitors:
In this exercise we were asked to use the Visitor Pattern.
We use 4 visitors in this implementation:
- ClassNameVisitor: Find and stores all classes' declarations and arguments, existing in the file.
- MethodNameVisitor: Find and stores all class variables, methods' declarations and methods' arguments, existing in the file.
- MethodsBody: the variables and statements of the methods and the type checking, using all the stored information at this point.
- IRGenerator: It generates the llvm-ir file

