public class Constructor_13 {
    public String field;

    public static String escaped = "blah";

    public Constructor_13() {
	this(escaped);
    }

    public Constructor_13(String x) {
	field = x;
    }
}
