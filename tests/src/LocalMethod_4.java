public class LocalMethod_4 {
    int x;

    public LocalMethod_4(int x) {
	this.x = x;
    }

    public void f() {
	new LocalMethod_4(1);
    }
}