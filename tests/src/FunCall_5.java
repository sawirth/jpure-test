public class FunCall_5 {
    public String f() {
	return "Hello World";
    }

    public static class Inner extends FunCall_5 {
    }

    public static void main(String[] args) {
	FunCall_5.Inner ti = new FunCall_5.Inner();
	System.out.println(ti.f().toString());
    }
}