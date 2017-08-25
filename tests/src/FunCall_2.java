public class FunCall_2 {
    public String f(String x) {
	return x;
    }

    public void g(String y) {
	y = f(y);
	h(y);
    }

    public void h(String z) {
	z.toString();
    }
}
