public class Inner_2 {
    private String field;

    public class Inner {
	public void g() {	       
	    if(field != null) {
		field.toString();
	    }
	}
    }
}
