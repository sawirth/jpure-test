public class Constructor_6 {
    static String field;

    public static void create() {
	if(field != null) {
	    new Constructor_6();
	    field.toString();
	}
    }

    public Constructor_6() {
	field = null;
    }
}