public class Constructor_2 {
    public String field;
    
    public Constructor_2() {
	init();
	field = "Hello";
    }

    public void init() {
	System.out.println(field.toString());
    }

    public static void main(String[] args) {
	new Constructor_2();
    }
}
