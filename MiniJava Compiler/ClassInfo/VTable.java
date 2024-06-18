package ClassInfo;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandleProxies;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;
public class VTable {
    private Map<String, Method> methodNames;

    public VTable(){
        methodNames = new LinkedHashMap<String,Method>();
    }

    public Method getMethodName(String name){
        return this.methodNames.get(name);
    }

    public void printTable(FileWriter f, String className) throws IOException{
        Iterator<Method> iterator = methodNames.values().iterator();
        if(iterator.hasNext()){
            Method m = iterator.next();
            f.write("i8* bitcast (");
            m.printSignature(f);
            f.write(" " + "@" + className + "." + m.name + " to i8*)");
            while (iterator.hasNext()){
                f.write(", ");
                m = iterator.next();
                f.write("i8* bitcast (");
                m.printSignature(f);
                f.write(" "+ "@" + className + "." + m.name + " to i8*)");
            }
        }
    }
    public int getSum(){return methodNames.size();}
    public void insertMethod(Method m){
        Method current = methodNames.get(m.name);
        if(current != null)
            m.setOffset(current.getOffset());
        methodNames.put(m.name, m);
    }


}
