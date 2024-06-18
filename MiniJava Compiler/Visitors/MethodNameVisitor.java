package Visitors;
import SymbolTable.*;
import syntaxtree.*;
import visitor.GJDepthFirst;
import ClassInfo.*;
import java.util.Map;

// viitor of class variables and methods declarations
public class MethodNameVisitor extends GJDepthFirst<String, Void> {

    public Map<String, ClassInformation> classes;
    public ClassInformation currentclass;
    SymbolTable table;

    public MethodNameVisitor(Map<String, ClassInformation> m)
    {
        classes = m;
        table = new SymbolTable(m);
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
        currentclass = classes.get(classname);

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
        currentclass = classes.get(classname);
        for(Node node : n.f3.nodes)
            node.accept(this, null);

        for(Node node : n.f4.nodes)
            node.accept(this,null);

        return null;
    }

    /**
     * Grammar production:
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public String visit(VarDeclaration n, Void argu) throws Exception {
        String varName = n.f1.accept(this, null);
        String type = n.f0.accept(this, null);

        if((!type.equals("int")) && (!type.equals("boolean")) && (!type.equals("int[]")))
        {
            if(!classes.containsKey(type))
                throw new SemanticException("line "+ n.f2.beginLine + ": Invalid type");
        }

        Variable var = new Variable(varName, type);
        if(currentclass.getVariables().get(varName) != null)
            throw new SemanticException("line "+ n.f2.beginLine + ": Duplicate variable");
        else
            currentclass.getVariables().put(varName,var);
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
        currentclass = classes.get(classname);
        for(Node node : n.f5.nodes)
            node.accept(this, null);

        for(Node node : n.f6.nodes)
            node.accept(this,null);

        return null;
    }


    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, Void argu) throws Exception {
        Method meth = this.currentclass.getMethod(n.f2.accept(this, null));
        Scopes sc = new Scopes();
        table.TableInsertScope(sc);
        String argumentList = n.f4.present() ? n.f4.accept(this, null) : "";

        String myType = n.f1.accept(this, null);
        String myName = n.f2.accept(this, null);
        if((!myType.equals("int")) && (!myType.equals("boolean")) && (!myType.equals("int[]")))
        {
            if(!classes.containsKey(myType))
                throw new SemanticException("line "+ n.f0.beginLine + ": Invalid type");
        }

        if(currentclass.getMethods().get(myName) != null)
            throw new SemanticException("line "+ n.f0.beginLine + ": Overloading not allowed");
        Method temp = currentclass.getMethod(myName);
        Method met;
        if(!argumentList.equals(""))
            met = new Method(myName, myType, argumentList.split(","));
        else
            met = new Method(myName, myType);
        if(temp != null)
        {
            if(!met.isEqual(temp))
                throw new SemanticException("line "+ n.f0.beginLine + ": Same name but not overriding");
        }

        currentclass.getMethods().put(myName, met);

//        // Analyzing method body statements
//        for (Node node: n.f8.nodes)
//        {
//            node.accept(this, null);
//        }

        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, Void argu) throws Exception {
        String ret = n.f0.accept(this, null);

        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterTerm n, Void argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, Void argu) throws Exception {
        StringBuilder ret = new StringBuilder();
        for ( Node node: n.f0.nodes) {
            ret.append(",").append(node.accept(this, null));
        }

        return ret.toString();
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, Void argu) throws Exception{
        return n.f0.accept(this, null);
    }

    @Override
    public String visit(ArrayType n, Void argu) {
        return "int[]";
    }

    public String visit(BooleanType n, Void argu) {
        return "boolean";
    }

    public String visit(IntegerType n, Void argu) {
        return "int";
    }

    @Override
    public String visit(Identifier n, Void argu) {
        return n.f0.toString();
    }
}