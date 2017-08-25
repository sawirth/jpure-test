import java.util.*;

public class FunCall_10 {
    public String str;

    public void f(String x) {
	g(x);
    }

    public void g(String y) {
	FunCall_10 x = new FunCall_10();
	x.str = "Hello World";
    }
}