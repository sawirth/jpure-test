import java.util.*;

public class FunCall_6 {
    public void f(String x) {
	g(x);
    }

    public List<String> g(String y) {
	ArrayList<String> x = new ArrayList<String>();
	x.add(y.toString());
	return x;	
    }
}
