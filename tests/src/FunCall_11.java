import java.util.*;

public class FunCall_11 {
    public String str;

    public void f(String x) {
	g(x);
    }

    public void g(String y) {
	ArrayList<String> x = new ArrayList<String>();
	List<String> a = x;
	a.add("HELLO");
    }
}