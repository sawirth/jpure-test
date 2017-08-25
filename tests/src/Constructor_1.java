public class Constructor_1 {
    public String field;
    
    public Constructor_1() {
	
    }

    public String f() {
	return field.toString();
    }

    public static void main(String[] args) {
	System.out.println(new Constructor_1().f());
    }
}
