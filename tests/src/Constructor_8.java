public class Constructor_8 {
    static String field;

    public static void create() {
	if(field != null) {
	    new Constructor_8();
	    field.toString();
	}
    }

    public Constructor_8() {
    }
}