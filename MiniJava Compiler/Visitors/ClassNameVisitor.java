package Visitors;
import syntaxtree.*;
import visitor.GJDepthFirst;
import ClassInfo.ClassInformation;
import java.util.HashMap;
import java.util.Map;

// visit only the classes' declarations
public class ClassNameVisitor extends GJDepthFirst<String, Void> {

    public static Map<String, ClassInformation> classes; // we want to pass it as argument to next visitors

    public ClassNameVisitor()
    {
        classes = new HashMap<>();
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, Void argu) throws Exception {
        String classname = n.f1.accept(this, null);
        ClassInformation info = new ClassInformation(classname);
        classes.put(classname, info);
        return null;

    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, Void argu) throws Exception {

        String classname = n.f1.accept(this, null);
        ClassInformation info = new ClassInformation(classname);
        if(classes.containsKey(classname))
            throw new SemanticException("Duplicate Class");
        classes.put(classname, info);
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, Void argu) throws Exception {
        String classname = n.f1.accept(this, null);
        String superclass = n.f3.accept(this,null);

        ClassInformation info = classes.get(superclass);
        ClassInformation subInfo;
        if(info != null)
            subInfo = new ClassInformation(classname, info);
        else
            throw new SemanticException("Super class doesn't exist");
        classes.put(classname,subInfo);
        return null;
    }


    @Override
    public String visit(Identifier n, Void argu) {
        return n.f0.toString();
    }
}