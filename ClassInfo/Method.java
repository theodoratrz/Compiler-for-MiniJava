package ClassInfo;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import Visitors.SemanticException;
public class Method {

    public final String name;
    public final String returnType;
    public final String[] argsTypes; // arguments of the method
    public int offset;  // each method knows its offset

    public Method(String n, String rn)
    {
        name = n;
        returnType = rn;
        argsTypes = new String[0];  // there are no arguments
        offset = 0;
    }

    // create the object with arguments
    public Method(String n, String rn, String [] array)
    {
        name = n;
        returnType = rn;
        argsTypes = array;
    }

    // compare the arguments of 2 methods
    public boolean isEqual(Method m2) {
        if (this.name.equals(m2.name) && this.returnType.equals(m2.returnType))
        {
            if(this.argsTypes.length != m2.argsTypes.length)
                return false;
            for(int i = 0; i < argsTypes.length; i++)
            {
                if( !(this.argsTypes[i].equals(m2.argsTypes[i])) )
                    return false;
            }
            return true;
        }
        return false;
    }

    // function that returns true if the given arguments are valid(the same with argsType[])
    // returns false if the arguments aren't the same or throws exception if they don't exist
    public boolean isArgument(String[] arguments, Map<String, ClassInformation> map) throws SemanticException
    {
        if(argsTypes.length != arguments.length)
            return false;
        for(int i = 0; i < argsTypes.length; i++)
        {
            if(!argsTypes[i].equals(arguments[i]))
            {
                if((!argsTypes[i].equals("int")) && (!argsTypes[i].equals("boolean")) && (!argsTypes[i].equals("int[]")))
                {
                    ClassInformation temp = map.get(arguments[i]);
                    if(temp == null)
                        throw new SemanticException("class doesn't exist ");
                    ClassInformation temp2 = map.get(argsTypes[i]);
                    if(!temp.findSuperClass(temp2.getName()))
                        throw new SemanticException("class doesn't exist");
                }
                else
                    return false;
            }
        }
        return true;
    }

    public String getReturnType()
    {
        return returnType;
    }

    public String getName()
    {
        return name;
    }

    public int getOffset(){return offset;}

    public void setOffset(int newOffset){this.offset = newOffset;}

    public String getSignature(){
        String signature = Variable.getIR(returnType) + "(i8*";
        for (String argsType : argsTypes)
            signature = signature + "," + Variable.getIR(argsType);
        signature = signature + ")";
        return signature;
    }

    public void printSignature(FileWriter f) throws IOException{ f.write(getSignature()+"*");}
}