import java.util.*;

public class FunCall_12 {
    public void f(String x) {
	g(x);
    }

    public void g(String y) {
	String[] xs = new String[10];
	xs[h()] = "Hello";	
    }

    public int h() {
	System.out.println("i'm impure");
	return 0;
    }
}