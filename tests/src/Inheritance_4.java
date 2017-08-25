public class Inheritance_4 {
    public static interface Parent {
	void f(String x);
    }

    public static class Child implements Parent {
	public void f(String x) {
	    x.toString();
	}
    }

    public static void main(String x) {
	Parent p = new Child();
	p.f(x);
    }
}
