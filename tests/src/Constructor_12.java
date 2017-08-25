public class Constructor_12 {
    Constructor_12 link;
    
    public Constructor_12(Constructor_12 link) {
	this.link = link;
    }

    public void local() {
	link = null;
    }

    public void link(Constructor_12 next) {
	Constructor_12 x = new Constructor_12(next);
	x.link.local();
    }	
}
