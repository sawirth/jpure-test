import java.util.*;

public class FunCall_13 {
    private ArrayList<String> global;

    public FunCall_13(ArrayList<String> global) {
	this.global = global;
    }
    
    public void broken() {
	global.add("i'm impure");
    }

    public static FunCall_13 create(ArrayList<String> ls) {
	FunCall_13 fc = new FunCall_13(ls);
	fc.broken(); // should make whole method impure
	return fc;
    }

    public static void main(String[] args) {
	ArrayList<String> ls = new ArrayList<String>();
	System.out.println(ls);
	create(ls);
	System.out.println(ls);
    }
}