import syntaxtree.*;
import Visitors.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
//        if(args.length != 1){
//            System.err.println("Usage: java Main <inputFile>");
//            System.exit(1);
//        }

        for (String arg: args) {
            FileInputStream fis = null;
            try{
                System.out.println("----------------- Processing '" + arg + "' -----------------");

                fis = new FileInputStream(arg);
                MiniJavaParser parser = new MiniJavaParser(fis);

                Goal root = parser.Goal();

                System.err.println("Program parsed successfully.");

                ClassNameVisitor eval = new ClassNameVisitor();
                root.accept(eval, null);
                MethodNameVisitor methEval = new MethodNameVisitor(eval.classes);
                root.accept(methEval, null);
                MethodsBody bodyEval = new MethodsBody(methEval.classes);
                root.accept(bodyEval, null);
                bodyEval.printOffsets();

                String outputFile = arg.replace(".java", ".ll");
                IrGenerator ir = new IrGenerator(methEval.classes,outputFile);
                ir.printVTable();
                ir.printUtils();
                root.accept(ir, null);
                ir.IROut.close();

                // Done.
                System.out.println("LLVM IR file: '" + outputFile + "' has been produced.");
            }
            catch(ParseException ex){
                System.out.println(ex.getMessage());
            }
            catch(FileNotFoundException ex){
                System.err.println(ex.getMessage());
            }
            catch (SemanticException ex)
            {
                System.err.println(ex.getMessage());
            }
            finally{
                try{
                    if(fis != null) fis.close();
                }
                catch(IOException ex){
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}
