package ClassInfo;
// implementation of a variable: stores the type and the name
public class Variable {
    public final String name;
    public final String type;
    public int offset;  // each variable knows its offset
    public String irType;   // map variables types to ir types
    public String registerName; // each variable knows its register

    public Variable(String n, String t)
    {
        name = n;
        type = t;
        offset = 0;
        irType = getIR(t);
        registerName = null;
    }

    public Variable(String n, String t, int offset,String register)
    {
        name = n;
        type = t;
        this.offset = offset;
        irType = getIR(t);
        registerName = register;
    }
    public String getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    public static boolean isPrimitive(String s) {
        return (s.equals("int") || s.equals("int[]") || s.equals("boolean") || s.equals("boolean[]"));
    }

    public static String getIR(String type){
        return switch (type) {
            case "int" -> "i32";
            case "int[]" -> "i32*";
            case "boolean" -> "i1";
            case "boolean[]" -> "i32*";
            default -> "i8*";
        };
    }
}