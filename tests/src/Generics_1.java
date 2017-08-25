public class Generics_1<T> {
    private T item;

    public Generics_1(T i) {
	item = i;
    }

    public T get() {
	return item;
    }

    public static void main(String[] args) {
	Generics_1<String> g1 = new Generics_1<String>(null);
	Generics_1<String> g2 = new Generics_1<String>("Hello World");
	System.out.println(g2.get().toString());
    }
}
