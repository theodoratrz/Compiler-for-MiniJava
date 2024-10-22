package Visitors;
import SymbolTable.*;
import syntaxtree.*;
import visitor.GJDepthFirst;
import ClassInfo.*;
import java.util.Map;

public class IrGenerate extends GJDepthFirst<String,String>{
    private Map<String, ClassInformation> classes;
    private Map<String,ClassInformation> registers;
    private ClassInformation currentclass;
    SymbolTable table;
}
