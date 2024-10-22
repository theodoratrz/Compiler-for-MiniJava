package Visitors;
import SymbolTable.*;
import syntaxtree.*;
import visitor.GJDepthFirst;
import ClassInfo.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//generator of .ll files
// the first agrument of the visit functions is String, because we want to keep
// and pass the name of the register of each value. We return null where we don't
// need to know the register.
// the second argument of visit functions is also String,
// because of the identifier's visitor, in order to know what is our identifier(value, name etc)

public class IrGenerator extends GJDepthFirst<String,String>{
    Map<String, ClassInformation> classes;  // all the classes, methods, variables from parsing
    Map<String, ClassInformation> registerForObject;    // A map to store register names and class types
                                                        // it is used for finding the offset in the vtable
    ClassInformation currentclass;  // hold the name of current class. It is used to identify where "this" belongs
    SymbolTable table;  // a stack for local variables. When the method body ends, the stack is cleared
    public FileWriter IROut;    // for writing to .ll files
    int regNum;     // used for generating new registers
    int regLabel;   // used for generating new labels

    public IrGenerator(Map<String, ClassInformation> m, String file) throws IOException {
        classes = m;
        registerForObject = new HashMap<String ,ClassInformation>();
        table = new SymbolTable(m);
        IROut = new FileWriter(file);
        regNum = 0;
        regLabel = 0;
    }

    public String generateRegister(){
        return "%_" + regNum++;
    }
    public String generateLabels(String type){return type + regLabel++;}
    // print to .ll file
    public void print(String str) throws IOException{IROut.write("\t" + str + "\n");}

    // prints all virtual tables
    public void printVTable() throws IOException{
        for(ClassInformation e: classes.values()){
            e.increaseOffset();
            e.createVTable();
            e.printVTable(IROut);
        }
        print("\n");
    }

    // this function prints some essentials lines after the vtable.
    // (throw_oob, print_int and some imports)
    public void printUtils() throws FileNotFoundException, IOException {
        FileReader reader = new FileReader("Visitors/header.ll");
        for(int c = reader.read(); c != -1; c = reader.read())
            IROut.write(c);
        IROut.write("\n");
        reader.close();
    }

    /**
     * Grammar production:
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */
    @Override
    public String visit(Type n, String argu) throws Exception{
        return n.f0.accept(this,null);
    }

    /**
     * Grammar production:
     * f0 -> BooleanArrayType()
     *       | IntegerArrayType()
     */
    @Override
    public String visit(ArrayType n, String argu) throws Exception{
        return n.f0.accept(this,null);
    }

    @Override
    public String visit(BooleanArrayType n , String argu) throws Exception{
        return "boolean[]";
    }

    @Override
    public String visit(IntegerArrayType n , String argu) throws Exception{
        return "int[]";
    }

    @Override
    public String visit(BooleanType n , String argu) throws Exception{
        return "boolean";
    }

    @Override
    public String visit(IntegerType n , String argu) throws Exception{
        return "int";
    }

    @Override
    public String visit(Identifier n, String argu) throws Exception{
        String id = n.f0.toString();
        if(argu != null) {
            Variable v = table.findIdentifier(id);
            if (v != null && argu.equals("rightValue")) {
                String newReg = generateRegister();
                print(newReg + " = load " + v.irType + ", " + v.irType + "* " + v.registerName);
                if (!Variable.isPrimitive(v.type))
                    registerForObject.put(newReg, classes.get(v.type));
                return newReg;
            }
            else if (v != null)
                return v.registerName;
            else{
                Variable var = currentclass.getField(id);
                String reg = generateRegister();
                print(reg + " = getelementptr i8, i8* %this, i32 " + var.offset);
                String t = var.irType;
                String castReg = generateRegister();
                print(castReg + "= bitcast i8* " + reg + " to " + t + "*");
                if (argu.equals("rightValue")) {
                    String f = generateRegister();
                    print(f + " = load " + t + ", " + t + "* " + castReg);
                    if (!Variable.isPrimitive(var.type))
                        registerForObject.put(f, classes.get(var.type));
                    return f;
                }
                return castReg;
            }
        }
        return id;
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
     *       | BracketExpression()
     */
    @Override
    public String visit(PrimaryExpression n, String argu) throws Exception {
        return n.f0.accept(this,argu);
    }

    @Override
    public String visit(TrueLiteral n, String argu) throws Exception{
        return "1";
    }
    @Override
    public String visit(FalseLiteral n, String argu) throws Exception{
        return "0";
    }
    @Override
    public String visit(IntegerLiteral n, String argu) throws Exception{
        return n.f0.toString();
    }

    @Override
    public String visit(ThisExpression n, String argu) throws Exception {
        return "%this";
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(PlusExpression n, String argu) throws Exception{
        String value1 = n.f0.accept(this, "rightValue");
        String value2 = n.f2.accept(this, "rightValue");
        String newReg = generateRegister();
        print(newReg + " = add i32 "+ value1 + ", " + value2);
        return newReg;
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(MinusExpression n, String argu) throws Exception{
        String value1 = n.f0.accept(this, "rightValue");
        String value2 = n.f2.accept(this, "rightValue");
        String newReg = generateRegister();
        print(newReg + " = sub i32 "+ value1 + ", " + value2);
        return newReg;
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(TimesExpression n, String argu) throws Exception{
        String value1 = n.f0.accept(this, "rightValue");
        String value2 = n.f2.accept(this, "rightValue");
        String newReg = generateRegister();
        print(newReg + " = mul i32 "+ value1 + ", " + value2);
        return newReg;
    }

    /**
     * Grammar production:
     * f0 -> "!"
     * f1 -> Clause()
     */
    @Override
    public String visit(NotExpression n, String argu) throws Exception {
        String value1 = n.f1.accept(this, "rightValue");
        String newReg = generateRegister();
        print(newReg + " = icmp eq i1 0, " + value1);
        return newReg;
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(CompareExpression n, String argu) throws Exception {
        String value1 = n.f0.accept(this, "rightValue");
        String value2 = n.f2.accept(this, "rightValue");
        String newReg = generateRegister();
        print(newReg + " = icmp slt i32 "+ value1 + ", " + value2);
        return newReg;
    }

    /**
     * Grammar production:
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    @Override
    public String visit(BracketExpression n, String argu) throws Exception {
        return n.f1.accept(this,argu);
    }

    /**
     * Grammar production:
     * f0 -> BooleanArrayAllocationExpression()
     *       | IntegerArrayAllocationExpression()
     */
    @Override
    public String visit(ArrayAllocationExpression n, String argu) throws Exception {
        return n.f0.accept(this,argu);
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
    public String visit(IntegerArrayAllocationExpression n, String argu) throws Exception {
        String arraySize = n.f3.accept(this, "rightValue");
        String negativeSize = generateRegister();
        print(negativeSize + " = icmp slt i32 " + arraySize + ", 0");
        String invalidSize = generateLabels("Oob");
        String validSize = generateLabels("Oob");
        print("br i1" + negativeSize + ", label %" + invalidSize + ", label %" + validSize);
        print("\n" + invalidSize + ":");
        print("call void @throw_oob()");
        print("br label %" + validSize);
        print("\n" + validSize + ":");
        String size = generateRegister();
        print(size + " = add i32 " + arraySize + ", 1");
        String allocate = generateRegister();
        print(allocate + " = call i8* @calloc(i32 4, i32 " + size +")");
        String start = generateRegister();
        print(start + " = bitcast i8* " + allocate + " to i32* ");
        print("store i32 " + arraySize + ", i32* " + start);
        return start;
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
    public String visit(BooleanArrayAllocationExpression n, String argu) throws Exception {
        String arraySize = n.f3.accept(this, "rightValue");
        String negativeSize = generateRegister();
        print(negativeSize + " = icmp slt i32 " + arraySize + ", 0");
        String invalidSize = generateLabels("Oob");
        String validSize = generateLabels("Oob");
        print("br i1" + negativeSize + ", label %" + invalidSize + ", label %" + validSize);
        print("\n" + invalidSize + ":");
        print("call void @throw_oob()");
        print("br label %" + validSize);
        print("\n" + validSize + ":");
        String size = generateRegister();
        print(size + " = add i32 " + arraySize + ", 1");
        String allocate = generateRegister();
        print(allocate + " = call i8* @calloc(i32 4, i32 " + size +")");
        String start = generateRegister();
        print(start + " = bitcast i8* " + allocate + " to i32* ");
        print("store i32 " + arraySize + ", i32* " + start);
        return start;
    }
    /**
     * Grammar production:
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    @Override
    public String visit(AllocationExpression n, String argu) throws Exception {
        ClassInformation cl = classes.get(n.f1.accept(this,null));
        int tableSize = cl.getVirtualTable().getSum();
        String newReg = generateRegister();
        print(newReg + " = call i8* @calloc(i32 1, i32 " + cl.getFieldOffset() + ")");
        String castReg = generateRegister();
        print(castReg + " = bitcast i8* " + newReg + " to i8***");
        String table = generateRegister();
        print(table + " = getelementptr [" + tableSize + " x i8*], [" + tableSize + " x i8*]* "
                + "@." + cl.name + "_vtable, i32 0, i32 0");
        print("store i8** " + table + ", i8*** " + castReg);
        registerForObject.put(newReg, cl);
        return newReg;
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
    public String visit(PrintStatement n, String argu) throws Exception {
        String reg = n.f2.accept(this,"rightValue");
        print("call void (i32) @print_int(i32 " + reg + ")");
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
    public String visit(IfStatement n, String argu) throws Exception {
        String result = n.f2.accept(this, "rightValue");
        String trueLabel = generateLabels("TrueLabel");
        String falseLabel = generateLabels("FalseLabel");
        String finallyLabel = generateLabels("FinallyLabel");
        print("br i1 " + result + ", label %" + trueLabel + ", label %" + falseLabel);
        print("\n" +trueLabel + ":");
        n.f4.accept(this,null);
        print("br label %" + finallyLabel);
        print("\n" + falseLabel + ":");
        n.f6.accept(this,null);
        print("br label %" + finallyLabel);
        print("\n" + finallyLabel + ":");
        return null;
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
    public String visit(WhileStatement n, String argu) throws Exception {
        String conditionLabel = generateLabels("Condition");
        String bodyLabel = generateLabels("Body");
        String endLabel = generateLabels("End");
        print("br label %" + conditionLabel);
        print("\n" + conditionLabel + ":");
        String cond = n.f2.accept(this, "rightValue");
        print("br i1 " + cond + ", label %" + bodyLabel + ", label %" + endLabel);
        print("\n" + bodyLabel + ":");
        n.f4.accept(this,null);
        print("br label %" + conditionLabel);
        print("\n" + endLabel + ":");
        return null;
    }

    /**
     * Grammar production:
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    @Override
    public String visit(AndExpression n, String argu) throws Exception {
        String result = generateRegister();
        String value1 = n.f0.accept(this,"rightValue");
        String value2Label = generateLabels("Expr2");
        String trueLabel = generateLabels("TrueLabel");
        String falseLabel = generateLabels("FalseLabel");
        String endLabel = generateLabels("EndLabel");
        print("br i1 " + value1 + ", label %" + value2Label + ", label %" + falseLabel);
        print("\n" + value2Label + ":");
        String value2 = n.f2.accept(this, "rightValue");
        print("br i1 " + value2 + ", label %" + trueLabel + ", label %" + falseLabel);
        print("\n" + trueLabel + ":");
        print("br label %" + endLabel);
        print("\n" + falseLabel + ":");
        print("br label %" + endLabel);
        print("\n" + endLabel + ":");
        print(result + " = phi i1 [" + 1 + ", %" + trueLabel + "], [" + 0 + ", %" + falseLabel + "]");
        return  result;
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
     *       | Clause()
     */
    @Override
    public String visit(Expression n, String argu) throws Exception {
        return n.f0.accept(this,argu);
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    @Override
    public String visit(ArrayLength n, String argu) throws Exception {
        String start = n.f0.accept(this, "rightValue");
        String size = generateRegister();
        print(size + " = load i32, i32* " + start);
        return size;
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
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
        String ind = n.f2.accept(this, "rightValue");
        String invalid = generateLabels("Invalid");
        String array = generateLabels("Array");
        String negative = generateRegister();
        print(negative + " = icmp slt i32 " + ind + ", 0");
        print("br i1 " + negative + ", label %" + invalid + ", label %" + array);
        print("\n" + array + ":");
        String add = n.f0.accept(this,"leftValue");
        String start = generateRegister();
        print(start + " = load i32*, i32** " + add);
        String size = generateRegister();
        print(size + " = load i32, i32* " + start);
        String checkIndex = generateRegister();
        print(checkIndex + " = icmp ule i32 " + size + ", " + ind);
        String valid = generateLabels("Valid");
        print("br i1 " + checkIndex + ", label %" + invalid + ", label %" + valid);
        print("\n" + invalid + ":");
        print("call void @throw_oob()");
        print("br label %" + valid);
        print("\n" + valid + ":");
        String real = generateRegister();
        print(real + " = add i32 " + ind + ", 1");
        String add2 = generateRegister();
        print(add2 + " = getelementptr i32, i32* " + start + ", i32 " + real);
        String rvalue = n.f5.accept(this, "rightValue");
        print("store i32 " + rvalue + ", i32* " + add2);
        return null;
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    @Override
    public String visit(ArrayLookup n, String argu) throws Exception {
        String num = n.f2.accept(this, "rightValue");
        String invalid = generateLabels("IndexOutOfBounds");
        String array = generateLabels("Array");
        String negative = generateRegister();   // negative index

        print(negative + " = icmp slt i32 " + num + ", 0"); // if index < 0 throw_oob
        print("br i1 " + negative + ", label %" + invalid + ", label %" + array);
        print("\n" + array + ":");
        String start = n.f0.accept(this, "rightValue");
        String size = generateRegister();
        print(size + " = load i32, i32* " + start); // size of the array is stored in its first cell
                                                    // we need to easily take the size, in order to check for oob
        String check = generateRegister();
        print(check + " = icmp ule i32 " + size + ", " + num);
        String valid = generateLabels("ValidIndex");
        print("br i1 " + check + ", label %" + invalid + ", label %" + valid);
        print("\n" + invalid + ":");
        print("call void @throw_oob()");
        print("br label %" + valid);
        print("\n" + valid + ":");
        String index = generateRegister();
        print(index + " = add i32 " + num + ", 1"); // real index is +1. array[0] is the size
        String getAddress = generateRegister();
        print(getAddress + " = getelementptr i32, i32* " + start + ", i32 " + index);
        String value = generateRegister();
        print(value + " = load i32, i32* " + getAddress);
        return  value;
    }

    /**
     * Grammar production:
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, String argu) throws Exception {
        String type = n.f0.accept(this,null);
        String id = n.f1.accept(this,null);
        return type + " " + id;
    }

    /**
     * Grammar production:
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, String argu) throws Exception {
        String s = n.f0.accept(this,null);
        if(n.f1 != null)
            s += n.f1.accept(this,null);
        return s;
    }

    /**
     * Grammar production:
     * f0 -> ( FormalParameterTerm() )*
     */
    @Override
    public String visit(FormalParameterTail n, String argu) throws Exception {
        String parameters = "";
        for(Node it: n.f0.nodes)
            parameters += "," + it.accept(this,null);
        return parameters;
    }

    /**
     * Grammar production:
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTerm n, String argu) throws Exception {
        return  n.f1.accept(this,null);
    }

    /**
     * Grammar production:
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    @Override
    public String visit(ExpressionList n, String argu) throws Exception {
        String expr = n.f0.accept(this,"rightValue");
        String exprTail = n.f1.accept(this,null);
        return expr + exprTail;
    }

    /**
     * Grammar production:
     * f0 -> ( ExpressionTerm() )*
     */
    @Override
    public String visit(ExpressionTail n, String argu) throws Exception {
        String expr = "";
        for (Node it: n.f0.nodes)
            expr += "," + it.accept(this,null);
        return expr;
    }

    /**
     * Grammar production:
     * f0 -> ","
     * f1 -> Expression()
     */
    @Override
    public String visit(ExpressionTerm n, String argu) throws Exception {
        return n.f1.accept(this, "rightValue");
    }

    /**
     * Grammar production:
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, String argu) throws Exception {
        String type;
        String left = n.f0.accept(this, "leftValue");
        String leftName = n.f0.accept(this,null);
        String right = n.f2.accept(this, "rightValue");
        Variable v = table.findIdentifier(leftName);
        if(v != null)
            type = v.irType;
        else
            type = currentclass.getField(leftName).irType;
        print("store "+ type + " " + right + ", " + type + "* " + left);
        return null;
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
    public String visit(MainClass n, String argu) throws Exception {
        Scopes sc = new Scopes();
        table.TableInsertScope(sc);
        IROut.write("define i32 @main() {\n");
        for(Node it: n.f14.nodes)
            it.accept(this,null);
        for(Node it: n.f15.nodes)
            it.accept(this,null);
        print("ret i32 0");
        IROut.write("}\n");
        registerForObject.clear();
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
    public String visit(VarDeclaration n, String argu) throws Exception {
        String t = n.f0.accept(this,null);
        String name = n.f1.accept(this,null);
        String reg = "%" + name;
        String irType = Variable.getIR(t);
        print(reg + " = alloca " + irType);
        table.TableInsertVariable(new Variable(name,t,0,reg));
        if(!Variable.isPrimitive(t))
            registerForObject.put(reg, classes.get(t));
        return reg;
    }

    /**
     * Grammar production:
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, String argu) throws Exception {
        currentclass = classes.get(n.f1.accept(this,null));
        for(Node it: n.f4.nodes){
            IROut.write("\n");
            it.accept(this,null);
        }
        return null;
    }

    /**
     * Grammar production:
     * f0 -> NotExpression()
     *       | PrimaryExpression()
     */
    @Override
    public String visit(Clause n, String argu) throws Exception {
        return n.f0.accept(this,argu);
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
    public String visit(Statement n, String argu) throws Exception {
        return n.f0.accept(this,argu);
    }

    /**
     * Grammar production:
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
    public String visit(MethodDeclaration n, String argu) throws Exception {
        Scopes sc = new Scopes();
        table.TableInsertScope(sc);
        String t = Variable.getIR(n.f1.accept(this,null));
        String name = n.f2.accept(this,null);
        IROut.write("define " + t + " @" + currentclass.name + "." + name); // method's signature
        IROut.write("(i8* %this ");     // "this" must be the first in parameter list to be printed
        // print the rest of parameter list
        if(n.f4.present()){
            String[] args = n.f4.accept(this,null).split("\\s*,\\s*");  // split parameters
            List<Variable> argList = new ArrayList<Variable>(); // store variables here
            for(String str: args){
                IROut.write(", ");
                String[] s = str.split("\\s* \\s*");    // split type from name
                argList.add(new Variable(s[1],s[0]));
                IROut.write(Variable.getIR(s[0]) + " %." + s[1]);
            }
            IROut.write(") {\n");
            for(Variable v: argList){
                // allocate space and store the value of the parameter
                v.registerName = "%" + v.name;
                print(v.registerName + " = alloca " + v.irType);    // we have mapped the variable's type to an irType
                print("store " + v.irType + " %." + v.name + ", " + v.irType + "* " + v.registerName);
                table.TableInsertVariable(v);
            }
        }
        else
            IROut.write(") {\n");
        for(Node it: n.f7.nodes)
            it.accept(this,null);
        for(Node it: n.f8.nodes)
            it.accept(this,null);
        String ret = n.f10.accept(this,"rightValue");
        print("ret " + t + " " + ret);
        IROut.write("}\n");
        table.TableRemoveScope();   // pop scope
        registerForObject.clear();  // clear map
        return null;
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
    @Override
    public String visit(MessageSend n, String argu) throws Exception {
        String str = n.f0.accept(this, "rightValue");
        String method = n.f2.accept(this,null);
        ClassInformation tempClass;
        if(str.equals("%this"))
            tempClass = currentclass;
        else
            tempClass = registerForObject.get(str); //get class type from the map, depending on the current register

        Method m = tempClass.getVTableMethod(method);   // find the method in the virtual table.
                                                        // we just need the offset.
        String irType = Variable.getIR(m.returnType);
        String s = generateRegister();
        print(s + " = bitcast i8* " + str + " to i8***");

        String t = generateRegister();  //vtable start
        print(t + " = load i8**, i8*** " + s);
        String methPos = generateRegister();
        print(methPos + " = getelementptr i8*, i8** " + t + ", i32 " + m.offset/8);   //get method's pos

        String methAdd = generateRegister();
        print(methAdd + " = load i8*, i8** " + methPos);    // method's address
        String sign = m.getSignature();
        String a = generateRegister();
        print(a + " = bitcast i8* " + methAdd + " to " + sign + " * ");
        String ret = generateRegister();
        String callMeth = ret + " = call " + irType + " " + a + "(i8* " + str;
        // arguments
        if(n.f4.present()){
            String[] args = n.f4.accept(this,null).split("\\s*,\\s*");
            for(int i = 0; i < args.length; i++)
                callMeth = callMeth + ", "+Variable.getIR(m.argsTypes[i]) + " " + args[i];
        }
        callMeth += ")";
        print(callMeth);
        if(!Variable.isPrimitive((m.returnType)))
            registerForObject.put(ret, classes.get(m.returnType));  // we want objects not primitive types
        return ret;
    }

    /**
     * Grammar production:
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
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
        currentclass = classes.get(n.f1.accept(this,null));
        for(Node it: n.f6.nodes){
            IROut.write("\n");
            it.accept(this,null);
        }
        return null;
    }
}


