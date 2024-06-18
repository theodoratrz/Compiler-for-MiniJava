package SymbolTable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import ClassInfo.*;

// helping class that stores all the variables  in a scope(used for symbol table
public class Scopes {

    public final Map<String, Variable> variables;

    public Scopes()
    {
        variables = new HashMap<>();
    }

    public boolean insertVariable(Variable v)
    {
        if(variables.containsKey(v.getName()))
            return false;
        variables.put(v.getName(), v);
        return true;
    }

    public Variable getVariable(String name)
    {
        return variables.get(name);
    }

}