public class Inheritance_2 {
    public static class Parent {
	public String f() {
	    return null;
	}
    }

    public static class Child extends Parent {
	public String f() {
	    return null;
	}
    }

    public static void main() {
	Parent p = new Child();	
	p.f().toString();
    }
}
