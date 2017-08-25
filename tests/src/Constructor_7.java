public class Constructor_7 {
    static String field;

    public static void create() {
	if(field != null) {
	    new Constructor_7();
	    field.toString();
	}
    }

    public String myField;

    public Constructor_7() {
	myField = null;
    }
}