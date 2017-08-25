public class FreshMethod_5 {
    int[] f(int x, int[] xs) {
	int[] a = new int[10];
	if(x > 0) {
	    return a;
	} else {
	    return null;
	}
    }

    int[] g() {
	return f(1,null);
    }
}