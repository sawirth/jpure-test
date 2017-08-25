public class Inheritance_8 {
    public String f() {
	return "Hello World";
    }

    public static class Inner extends Inheritance_8 {
	public String f() {
	    return null;
	}	
    }

    public static class Child extends Inner {
	public String f() {
	    return "Hello World";
	}	
    }

    public String g() {
	return f();
    }
}