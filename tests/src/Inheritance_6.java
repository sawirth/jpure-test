public class Inheritance_6<E> {
    void f(E x) {
    }

    public static class Inner extends Inheritance_6<String> {
	void f(String x) {
	    x.toString();
	}
    }
}