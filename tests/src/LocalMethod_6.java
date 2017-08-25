import java.util.*;

public class LocalMethod_6 {
    public String[] arr;

    public LocalMethod_6() {
	arr = new String[1];
	arr[0] = "Hello";
    }

    public void nonlocal(String[] dummy) {
	arr = dummy;
	arr[0] = "GOTCHA";
    }

    public static void impure(String[] arr) {
	LocalMethod_6 l = new LocalMethod_6();
	l.nonlocal(arr);
    }
    
    public static void main(String[] args) {
	String[] toBreak = new String[1];
	impure(toBreak);
	System.out.println(Arrays.toString(toBreak));
    }
}
