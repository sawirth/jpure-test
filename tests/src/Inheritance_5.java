import java.util.*;

public class Inheritance_5 {
    public static abstract class Parent {
	abstract public void f(String x, HashMap<String,String> lmap);
    }

    public static class Child extends Parent {
	public void f(String x, HashMap<String,String> lmap) {
	    lmap.clone();	    
	}
    }

    public static void main(String x) {
	Parent p = new Child();
	p.f(x, new HashMap());
    }
}
