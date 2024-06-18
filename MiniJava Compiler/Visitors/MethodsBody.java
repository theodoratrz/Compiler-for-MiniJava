package Visitors;
import ClassInfo.ClassInformation;
import ClassInfo.Method;
import ClassInfo.Variable;
import SymbolTable.*;
import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.Map;
//visitor of methods' variables and statements
//also checks for scoping(using symbol table)
//and type checking the expressions, statements
// we changed the second argument of visit functions from void to int
// because of the identifier's visitor. we want to check if a variable was previously
//declared. If it was we return its type.

public class MethodsBody extends GJDepthFirst<String, Integer> {

    Map<String, ClassInformation> classes;
    ClassInformation currentclass;
    SymbolTable table;

    public MethodsBody(Map<String, ClassInformation> m)
    {
        classes = m;
        table = new SymbolTable(m);
    }

    /**
     * Grammar production:
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
    public String visit(MainClass n, Integer argu) throws Exception {
        String[] temp;
        String classname = n.f1.accept(this, null);
        currentclass = classes.get(classname);
        Scopes sc = new Scopes();
        this.table.TableInsertScope(sc);

        String s = n.f11.accept(this,null);
        Variable var = new Variable(s, "String[]");
        this.table.TableInsertVariable(var);

        for(Node node : n.f14.nodes)
        {
            String s1 = node.accept(this, null);
            temp = s1.split(" ");
            Variable v = new Variable(temp[1], temp[0]);
            table.TableInsertVariable(v);
            currentclass.getVariables().put(v.getName(),v);
        }

        for(Node node : n.f15.nodes)
            node.accept(this,null);
        table.TableRemoveScope();
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
    public String visit(ClassDeclaration n, Integer argu) throws Exception {

        String[] temp;
        String classname = n.f1.accept(this, null);
        currentclass = classes.get(classname);
        Scopes sc = new Scopes();
        table.TableInsertScope(sc);

        for(Node node : n.f3.nodes)
        {
            String s = node.accept(this, null);
            temp = s.split(" ");
            Variable v = new Variable(temp[1], temp[0]);
            table.TableInsertVariable(v);
            currentclass.getVariables().put(v.getName(),v);
        }

        for(Node node : n.f4.nodes)
            node.accept(this,null);

        table.TableRemoveScope();
        return null;
    }

    /**
     * Grammar production:
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public String visit(VarDeclaration n, Integer argu) throws Exception {
        String varName = n.f1.accept(this, null);
        String type = n.f0.accept(this, null);

        if(!Variable.isPrimitive(type) && (this.classes.get(type) == null))
            throw new SemanticException("line " + n.f2.beginLine + ": Variable "+ varName + " is invalid type " + type);
        return type + " " + varName;
    }

    public String visit(Block n, Integer argu) throws Exception{
        for(Node node: n.f1.nodes)
            node.accept(this,null);
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
    public String visit(ClassExtendsDeclaration n, Integer argu) throws Exception {
        String classname = n.f1.accept(this, null);
        String superclass = n.f3.accept(this,null);
        currentclass = classes.get(classname);
        String[] temp;
        Scopes sc = new Scopes();
        table.insertSuper(currentclass.getSuperClass());
        table.TableInsertScope(sc);

        for(Node node : n.f5.nodes)
        {
            String s = node.accept(this, null);
            temp = s.split(" ");
            Variable v = new Variable(temp[1], temp[0]);
            table.TableInsertVariable(v);
            currentclass.getVariables().put(v.getName(),v);
        }

        for(Node node : n.f6.nodes)
            node.accept(this,null);

        table.removeSuper(currentclass);

        return null;
    }

    /**
     * Grammar production:
     * f0 -> Block()
     *       | AssignmentStatement()
     *       | ArrayAssignmentStatement()
     *       | IfStatement()
     *       | WhileStatement()
     *       | PrintStatement()
     */
    @Override
    public String visit(Statement n, Integer argu) throws Exception {
        return n.f0.accept(this,null);
    }

    /**
     * Grammar production:
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    @Override
    public String visit(WhileStatement n, Integer argu) throws Exception {
        String expr = n.f2.accept(this,null);
        if(!expr.equals("boolean"))
            throw new SemanticException("line " + n.f0.beginLine + ": type mismatch");
        n.f4.accept(this,null);
        return null;
    }
    /**
     * Grammar production:
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    @Override
    public String visit(IfStatement n, Integer argu) throws Exception {
        String name = n.f2.accept(this,null);
        if(!name.equals("boolean"))
            throw new SemanticException("line " + n.f0.beginLine + ": Type mismatch");
        n.f4.accept(this,null);
        n.f6.accept(this,null);
        return null;

    }

    /**
     * Grammar production:
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    @Override
    public String visit(PrintStatement n, Integer argu) throws Exception {
        String s = n.f2.accept(this,null);
        if(!s.equals("int"))
            throw new SemanticException("line "+ n.f1.beginLine +": Type mismatch, must be integer");
        return null;
    }

    /**
     * Grammar production:
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    @Override
    public String visit(ArrayAssignmentStatement n, Integer argu) throws Exception {
        String s = n.f0.accept(this,1);
        String s1 = n.f2.accept(this,null);
        if(!s1.equals("int"))
            throw new SemanticException("line "+ n.f3.beginLine +": must be integer");
        String s2 = n.f5.accept(this,null);
        if(s.equals("int[]")) {
            if(!s2.equals("int"))
                throw new SemanticException("line "+ n.f6.beginLine +": must be integer");

            return "int";
        }
        else if(s.equals("boolean[]")) {
            if(!s2.equals("boolean"))
                throw new SemanticException("line "+ n.f6.beginLine +": must be boolean");

            return "boolean";
        }
        else {
            throw new SemanticException("line " + n.f1.beginLine + ": must be an array");
        }
    }

    /**
     * Grammar production:
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, Integer argu) throws Exception {
        String var = n.f0.accept(this,1);
        String expr = n.f2.accept(this,null);
        if(var.equals(expr))
            return var;
        if(!Variable.isPrimitive(var)){
            ClassInformation expr_class = this.classes.get(expr);
            if( expr_class != null && expr_class.findSuperClass(var))
                return var;
        }
        throw new SemanticException("line " + n.f1.beginLine + ": Wrong types assignment");

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
    public String visit(MethodDeclaration n, Integer argu) throws Exception {
        String[] temp;
        Scopes sc = new Scopes();
        table.TableInsertScope(sc);

        String argumentList = n.f4.present() ? n.f4.accept(this, null) : "";
        if(!argumentList.equals(""))
        {
            temp = argumentList.split(",");
            for (String s : temp) {
                String[] temp2 = s.split(" ");
                Variable v = new Variable(temp2[1], temp2[0]);
                table.TableInsertVariable(v);
            }
        }

        String myType = n.f1.accept(this, null);
        String myName = n.f2.accept(this, null);
        for(Node node : n.f7.nodes)
        {
            String s = node.accept(this, null);
            temp = s.split(" ");
            Variable v = new Variable(temp[1], temp[0]);
            table.TableInsertVariable(v);
        }

        for(Node node : n.f8.nodes)
            node.accept(this,null);

        String returnType = n.f10.accept(this,null);
        if(!myType.equals(returnType))
        {
            if((!returnType.equals("int")) && (!returnType.equals("boolean")) && (!returnType.equals("int[]")))
            {
                ClassInformation tempClass = classes.get(returnType);
                if(tempClass == null)
                    throw new SemanticException("line " + n.f0.beginLine + ": Return value doesn't exist");
                if(!tempClass.findSuperClass(returnType))
                    throw new SemanticException("line " + n.f0.beginLine + ": Return value doesn't exist");
            }
            else
                throw new SemanticException("line " + n.f0.beginLine + ": Return value doesn't match return type");

        }

        table.TableRemoveScope();
        return null;
    }


    /**
     * Grammar production:
     * f0 -> AndExpression()
     *       | CompareExpression()
     *       | PlusExpression()
     *       | MinusExpression()
     *       | TimesExpression()
     *       | ArrayLookup()
     *       | ArrayLength()
     *       | MessageSend()
     *       | PrimaryExpression()
     */
    @Override
    public String visit(Expression n, Integer argu) throws Exception {
        return n.f0.accept(this,null);
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(PlusExpression n, Integer argu) throws Exception {
        String s = n.f0.accept(this,null);
        if(!s.equals("int"))
            throw new SemanticException("line " + n.f1.beginLine + ": Expecting int");
        s = n.f2.accept(this,null);
        if(!s.equals("int"))
            throw new SemanticException("line " + n.f1.beginLine + ": Expecting int");
        return "int";
    }


    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(MinusExpression n, Integer argu) throws Exception {
        String s = n.f0.accept(this,null);
        if(!s.equals("int"))
            throw new SemanticException("line " + n.f1.beginLine + ": Expecting int");
        s = n.f2.accept(this,null);
        if(!s.equals("int"))
            throw new SemanticException("line " + n.f1.beginLine + ": Expecting int");
        return "int";
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */

    @Override
    public String visit(TimesExpression n, Integer argu) throws Exception {
        String s = n.f0.accept(this,null);
        if(!s.equals("int"))
            throw new SemanticException("line " + n.f1.beginLine + ": Expecting int");
        s = n.f2.accept(this,null);
        if(!s.equals("int"))
            throw new SemanticException("line " + n.f1.beginLine + ": Expecting int");
        return "int";
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    @Override
    public String visit(ArrayLength n, Integer argu) throws Exception {
        String s = n.f0.accept(this,null);
        if( !s.equals("int[]") && !s.equals("boolean[]") )
            throw new SemanticException("line " + n.f1.beginLine+ ": Expecting array type");
        return "int";
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    @Override
    public String visit(ArrayLookup n, Integer argu) throws Exception {
        String s = n.f2.accept(this,null);
        if(!s.equals("int"))
            throw new SemanticException("line " + n.f1.beginLine + ": Expecting int");
        s = n.f0.accept(this,null);
        if(s.equals("int[]")) {
            return "int";
        }
        else if(s.equals("boolean[]")) {
            return "boolean";
        }
        throw new SemanticException("line " + n.f1.beginLine + ": Expecting array type");
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "&&"
     * f2 -> PrimaryExpression()
     */

    @Override
    public String visit(AndExpression n, Integer argu) throws Exception {
        String leftExpr = n.f0.accept(this,null);
        String rightExpr = n.f2.accept(this,null);

        if(!(leftExpr.equals("boolean") && rightExpr.equals("boolean")))
            throw new SemanticException("line " + n.f1.beginLine + ": '&&' undefined for argument types" + leftExpr + "," + rightExpr);
        return "boolean";
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    @Override   // fix this!
    public String visit(MessageSend n, Integer argu) throws Exception {

        String s = n.f0.accept(this,null);
        if((!s.equals("int")) && (!s.equals("boolean")) && (!s.equals("int[]")))
        {
            if(!classes.containsKey(s))
                throw new SemanticException("line " + n.f1.beginLine + ": Couldn't find symbol");
        }
        else
            throw new SemanticException("line " + n.f1.beginLine + ": Cannot use primitive type");
        ClassInformation temp = classes.get(s);

        String s1 = n.f2.accept(this,null);
        Method meth = temp.getMethod(s1);
        if(meth == null)
            throw new SemanticException("line " + n.f1.beginLine + ": Method not found");


        String argumentList;
        String [] array;
        if(n.f4.present())
        {
            argumentList = n.f4.accept(this, null);
            array = argumentList.split(" ");
        }
        else
        {
            array = new String[0];
        }

        if(!meth.isArgument(array, classes))
            throw new SemanticException("line " + n.f1.beginLine + ": wrong arguments" );
        return meth.getReturnType();
    }

    /**
     * Grammar production:
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    @Override
    public String visit(ExpressionList n, Integer argu) throws Exception {
        String myType = n.f0.accept(this,null);
        String s = n.f1.accept(this,null);
        return myType + s;
    }

    /**
     * Grammar production:
     * f0 -> ( ExpressionTerm() )*
     */
    @Override
    public String visit(ExpressionTail n, Integer argu) throws Exception {
        StringBuilder s = new StringBuilder();
        for(Node node : n.f0.nodes)
        {
            s.append(" ").append(node.accept(this, null));
        }
        return s.toString();
    }

    /**
     * Grammar production:
     * f0 -> ","
     * f1 -> Expression()
     */
    @Override
    public String visit(ExpressionTerm n, Integer argu) throws Exception {
        return n.f1.accept(this,null);
    }

    /**
     * Grammar production:
     * f0 -> IntegerLiteral()
     *       | TrueLiteral()
     *       | FalseLiteral()
     *       | Identifier()
     *       | ThisExpression()
     *       | ArrayAllocationExpression()
     *       | AllocationExpression()
     *       | NotExpression()
     *       | BracketExpression()
     */
    @Override
    public String visit(PrimaryExpression n, Integer argu) throws Exception {
        return n.f0.accept(this,1);
    }


    /**
     * Grammar production:
     * f0 -> <INTEGER_LITERAL>
     */
    @Override
    public String visit(IntegerLiteral n, Integer argu) throws Exception {
        return "int";
    }

    /**
     * Grammar production:
     * f0 -> "true"
     */
    @Override
    public String visit(TrueLiteral n, Integer argu) throws Exception {
        return "boolean";
    }

    /**
     * Grammar production:
     * f0 -> "false"
     */
    @Override
    public String visit(FalseLiteral n, Integer argu) throws Exception {
        return "boolean";
    }

    /**
     * Grammar production:
     * f0 -> "this"
     */
    @Override
    public String visit(ThisExpression n, Integer argu) throws Exception {

        return currentclass.getName();
    }


    /**
     * Grammar production:
     * f0 -> BooleanArrayAllocationExpression()
     *       | IntegerArrayAllocationExpression()
     */
    @Override
    public String visit(ArrayAllocationExpression n, Integer argu) throws Exception {
        return n.f0.accept(this, null);
    }

    /**
     * Grammar production:
     * f0 -> "new"
     * f1 -> "boolean"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */

    @Override
    public String visit(BooleanArrayAllocationExpression n, Integer argu) throws Exception {
        String s = n.f3.accept(this,null);
        if(!s.equals("int"))
            throw new SemanticException("line " + n.f1.beginLine + ": expression must be integer");
        return "boolean[]";
    }

    /**
     * Grammar production:
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(IntegerArrayAllocationExpression n, Integer argu) throws Exception {
        String s = n.f3.accept(this,null);
        if(!s.equals("int"))
            throw new SemanticException("line " + n.f1.beginLine + ": expression must be integer");
        return "int[]";
    }
    /**
     * Grammar production:
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    @Override
    public String visit(AllocationExpression n, Integer argu) throws Exception {
        String s = n.f1.accept(this,null);
        if(this.classes.get(s) == null)
            throw new SemanticException("line "+ n.f0.beginLine +": this class doesn't exist");
        return s;
    }

    /**
     * Grammar production:
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    @Override
    public String visit(BracketExpression n, Integer argu) throws Exception {
        return n.f1.accept(this,null);
    }

    /**
     * Grammar production:
     * f0 -> "!"
     * f1 -> PrimaryExpression()
     */
    @Override
    public String visit(NotExpression n, Integer argu) throws Exception {
        String s = n.f1.accept(this,null);
        if(!s.equals("boolean"))
            throw new SemanticException("line "+ n.f0.beginLine +": Required boolean");
        return "boolean";
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(CompareExpression n, Integer argu) throws Exception{
        String leftExpr = n.f0.accept(this,null);
        String righExpr = n.f2.accept(this,null);
        if( !(leftExpr.equals("int") && righExpr.equals("int")))
            throw new SemanticException("line " + n.f1.beginLine + ": operator < undefined for these types");
        return "boolean";
    }
    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, Integer argu) throws Exception {
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
    public String visit(FormalParameterTerm n, Integer argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, Integer argu) throws Exception {
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
    public String visit(FormalParameter n, Integer argu) throws Exception{
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this,null);
        return type + " " + name;
    }

    /**
     * Grammar production:
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */
    @Override
    public String visit(Type n, Integer argu) throws Exception{
        return n.f0.accept(this, null);
    }

    /**
     * Grammar production:
     * f0 -> ClassDeclaration()
     *       | ClassExtendsDeclaration()
     */
    @Override
    public String visit(TypeDeclaration n, Integer argu) throws Exception{
        return n.f0.accept(this, null);
    }
    /**
     * Grammar production:
     * f0 -> BooleanArrayType()
     *       | IntegerArrayType()
     */
    @Override
    public String visit(ArrayType n, Integer argu) throws Exception{
        return n.f0.accept(this, null);
    }

    @Override
    public String visit(BooleanArrayType n, Integer argu) {
        return "boolean[]";
    }

    @Override
    public String visit(IntegerArrayType n, Integer argu) {
        return "int[]";
    }

    /**
     * Grammar production:
     * f0 -> "boolean"
     */
    public String visit(BooleanType n, Integer argu) {
        return "boolean";
    }

    /**
     * Grammar production:
     * f0 -> "int"
     */
    public String visit(IntegerType n, Integer argu) {
        return "int";
    }

    /**
     * Grammar production:
     * f0 -> NotExpression()
     *       | PrimaryExpression()
     */
    @Override
    public String visit(Clause n, Integer argu) throws Exception {
        return n.f0.accept(this,null);
    }
    /**
     * Grammar production:
     * f0 -> <IDENTIFIER>
     */
    @Override
    public String visit(Identifier n, Integer argu) throws Exception{

        String s = n.f0.toString();
        if(argu == null)
            return s;
        Variable v = this.table.findIdentifier(s);
        if(v == null)
            throw new SemanticException("line "+ n.f0.beginLine +": Variable not found");
        return v.type;
    }

    public void printOffsets()
    {
        for(Map.Entry<String,ClassInformation> entry : classes.entrySet())
        {
            entry.getValue().OffSetTableCreate();
            System.out.println("---" + entry.getKey() + "---");
            entry.getValue().printOffsets();
            System.out.println("");
        }

    }
}

