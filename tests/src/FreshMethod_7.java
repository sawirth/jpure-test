public class FreshMethod_7 {
    FreshMethod_7 link;

    static FreshMethod_7 terminator = new FreshMethod_7();

    static FreshMethod_7 create() {
	FreshMethod_7 r = new FreshMethod_7();
	r.link = terminator;
	return r;
    }
}
