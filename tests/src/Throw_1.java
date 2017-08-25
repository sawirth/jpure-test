public class Throw_1 {
    public void g() {
	
    }

    public void f() {
	try {
	    g();
	} catch(NullPointerException e) {
	    throw e;
	}
    }
}
