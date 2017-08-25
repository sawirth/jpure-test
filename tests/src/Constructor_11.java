public class Constructor_11 {
    private String[] array;

    public Constructor_11(String x, String[] array) {
	this.array = new String[1];
	this.array[0] = x;
	f(array);
    }

    void f(String[] array) {
	this.array = array;
    }
}
