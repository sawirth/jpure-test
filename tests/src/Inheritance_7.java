public class Inheritance_7 {
    public String f() {
	return "Hello World";
    }

    public static class Inner extends Inheritance_7 {
	public String f() {
	    return null;
	}	
    }
}