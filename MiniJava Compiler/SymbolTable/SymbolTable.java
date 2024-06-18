package SymbolTable;
import ClassInfo.*;
import Visitors.SemanticException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

// An implementation of symbol table
public class SymbolTable {

    public final Deque<Scopes> stack;    // we use the symbol table as a stack
    public final Map<String, ClassInformation> classes;

    public SymbolTable(Map<String,ClassInformation> map)
    {
        stack = new ArrayDeque<>();
        classes = map;

    }

    // enter a variable to current scope. if it exists throw exception
    public boolean TableInsertVariable(Variable v) throws Exception {
        if(!stack.getFirst().insertVariable(v)) {
            throw new SemanticException("variable with same name");
        }
        return true;
    }

    // search for an identifier to all scopes
    public Variable findIdentifier(String identifier)
    {
        Scopes prev;
        Iterator<Scopes> it = stack.iterator();
        while(it.hasNext())
        {
            prev = it.next();
            if(prev.getVariable(identifier) != null)
            {
                return prev.getVariable(identifier);
            }
        }
        return null;
    }

    public void TableInsertScope(Scopes s)
    {
        stack.addFirst(s);
    }

    public void TableRemoveScope()
    {
        stack.pop();
    }

    // add the supper classes recursively in the symbol table
    public void insertSuper(ClassInformation name) throws Exception
    {
        Variable prev;
        if(name.getSuperClass() != null)
            insertSuper(name.getSuperClass());

        Scopes s = new Scopes();
        stack.addFirst(s);
        for (Variable variable : name.getVariables().values()) {
            prev = variable;
            TableInsertVariable(prev);
        }
    }

    public Variable currentScopeVar(String name){
        if (stack.size() != 0)
            return stack.peekLast().getVariable(name);
        return null;
    }


    // first remove current scope, then super classes' scopes
    public void removeSuper(ClassInformation name)
    {
        TableRemoveScope();
        if(name.getSuperClass()!=null)
            removeSuper(name.getSuperClass());
    }


}
