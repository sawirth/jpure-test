public class Inheritance_1 {
    public static class Parent {
	public void f(String x) {
	}
    }

    public static class Child extends Parent {
	public void f(String x) {
	    x.toString();
	}
    }

    public static void main(String x) {
	Parent p = new Parent();
	p.f(x);
    }
}
