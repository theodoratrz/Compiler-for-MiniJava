package ClassInfo;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

// a representation of a class
public class ClassInformation {

    public final String name;
    public final Map<String, Variable> variables; // the variables of the class
    public final Map<String, Method> methods;    // the methods of a class
    public final ClassInformation superClass;    // if the class extends another class
    private OffSetTable table;              // helping class to print the offsets
    int methodOffset;
    int variableOffset;
    private VTable virtualTable;
    public ClassInformation(String n)
    {
        name = n;
        variables = new LinkedHashMap<>();  // we use LInked hash map in order to keep the insertion order
        methods = new LinkedHashMap<>();
        superClass = null;
        virtualTable = new VTable();
    }

    // create the object if a super class exists
    public ClassInformation(String n,ClassInformation sup)
    {
        name = n;
        variables = new LinkedHashMap<>();
        methods = new LinkedHashMap<>();
        superClass = sup;
        virtualTable = new VTable();
    }

    public Map<String, Method> getMethods()
    {
        return methods;
    }

    public Map<String, Variable> getVariables()
    {
        return variables;
    }

    // get the method with name "name".
    // if the method doesn't exist in this, search in super classes(overriding)
    // if it doesn't exist anywhere, return null
    public Method getMethod(String name)
    {
        if(methods.containsKey(name))
            return methods.get(name);
        else
        {
            if(superClass != null)
                return superClass.getMethod(name);
        }
        return null;
    }

    public ClassInformation getSuperClass() {
        return superClass;
    }

    public String getName()
    {
        return name;
    }

    // find the super class with name "name"
    // search recursively to all upper classes
    public boolean findSuperClass(String name)
    {
        if(superClass != null)
        {
            if(this.superClass.getName().equals(name))
                return true;
            return this.superClass.findSuperClass(name);
        }
        return false;
    }

    public int getFieldOffset() {
        return table.currentVariable;
    }

    // create the class of offsets
    // firstly we have to add the offsets of the super classes
    public void OffSetTableCreate()
    {
        // if the table has been previously created, return
        // (many classes may have the same super class)
        if(table != null)
            return;
        table = new OffSetTable();

        if(superClass != null)
        {
            // store the offsets of the super classes(recursively)
            if(superClass.table == null)
                superClass.OffSetTableCreate();
            //update the offset index every time
            table.currentVariable = superClass.table.currentVariable;
            table.currentMethod = superClass.table.currentMethod;
        }

        int offset;
        int off;
        for(Map.Entry<String,Variable> entry: variables.entrySet())
        {
            Variable v = entry.getValue();
            if(v.getType().equals("int"))
                offset = 4;
            else if(v.getType().equals("boolean"))
                offset = 1;
            else
                offset = 8;

            table.vars.put(v.getName(), table.currentVariable);
            v.offset = table.currentVariable;
            table.currentVariable += offset;
        }

        for(Map.Entry<String,Method> entry: methods.entrySet())
        {
            Method v = entry.getValue();
            if(superClass == null || (!(superClass.getMethods().containsKey(v.getName()))))
            {
                table.meths.put(v.getName(),table.currentMethod);
                v.offset = table.currentMethod;
                table.currentMethod += 8;
            }
        }
    }

    public Method getMethod(String name, int isOffset){
        Method temp = methods.get(name);
        if(methods.get(name) == null && superClass!=null)
            return superClass.getMethod(name,1);
        return temp;
    }

    // function to print every class' offsets(first variables, then methods)
    public void printOffsets()
    {
        System.out.println("---Variables---");
        for(Map.Entry<String,Integer> entry : table.vars.entrySet())
            System.out.println((name + "." + entry.getKey() + ": " + entry.getValue()));

        System.out.println("---Methods---");
        for(Map.Entry<String,Integer> entry : table.meths.entrySet())
            System.out.println((name + "." + entry.getKey() + ": " + entry.getValue()));

    }

    public Method getVTableMethod(String m){
        return virtualTable.getMethodName(m);
    }

    public Variable getField(String name){
        Variable temp = variables.get(name);
        if(temp == null){
            if(this.superClass != null)
                return this.superClass.getField(name);
        }
        return temp;
    }

    public VTable getVirtualTable(){return virtualTable;}

    public void createVTable(){
        if(superClass != null)
            superClass.createVTable();
        for(Map.Entry<String,Method> e: methods.entrySet())
            virtualTable.insertMethod(e.getValue());
    }

    public void printVTable(FileWriter f) throws IOException{
        f.write("@." + name + "_vtable = global [" + virtualTable.getSum() + " x i8*] [");
        virtualTable.printTable(f, name);
        f.write("]\n");
    }

    public void  increaseOffset(){
        for(Map.Entry<String,Variable> e: variables.entrySet())
            e.getValue().offset += 8;
        variableOffset += 8;
    }


}

class OffSetTable
{
    public Map<String, Integer> vars; // store every variable with its offset
    public Map<String, Integer> meths; // store every method with its offset
    public int currentVariable; // counter of offsets for variables
    public int currentMethod; // counter of offsets for variables

    public OffSetTable()
    {
        vars = new LinkedHashMap<>();
        meths = new LinkedHashMap<>();
        currentVariable = 0;
        currentMethod = 0;
    }

}