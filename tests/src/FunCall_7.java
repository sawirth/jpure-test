import java.util.*;

public class FunCall_7 {
    private ArrayList<String> list = new ArrayList<String>();

    public boolean contains(String s) {
	for(String l : list) {
	    if(l.equals(s)) {
		return true;
	    }
	}
	return false;
    }
}
