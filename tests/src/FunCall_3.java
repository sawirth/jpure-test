public class FunCall_3 {
    public void f(String... args) {
	for(String s : args) {
	    System.out.println(s.toString());
	}
    }

    public void g(String x, String y) {
	f(x,y);
    }
}