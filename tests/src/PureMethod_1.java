import java.util.*;

public class PureMethod_1 {
    public ArrayList<String> array = new ArrayList<String>();
    
    public boolean contains(String x) {
	for(String s : array) {
	    if(s == x) {
		return true;
	    }
	}
	return false;
    }
}