public class FunCall_4 {
    public void f(String... args) {
	for(String s : args) {
	    System.out.println(s);
	}
    }

    public void g(String x, String y) {
	f(x,y);
    }
}