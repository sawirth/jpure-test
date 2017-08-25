public class IntVec {
    private int data[];
    private int length = 0;

    public IntVec(int size) { data = new int[size]; }    

    public int size()  { return length; }

    public boolean add(int v) {
	if(length < data.length) {
	    data[length]=v;	
	    length = length + 1;
	    return true;
	} else {
	    return false;
	}
    } 

    public void addAll(IntVec src) {
	for(int i=0;i!=src.size();++i) {
	    add(src.get(i));
	}
    }

    public boolean add(int idx, int v) {
	if(length < data.length) {
	    // Copy everything past idx down one spot
	    System.arraycopy(data,idx,data,idx+1,(length-idx-1));
	    // now do the assignment
	    data[idx]=v;

	    return true;
	} else {
	    return false;
	}
    }

    public boolean remove(int v) {
	// search through array looking for v.
	for(int i=0;i!=length;++i) {
	    if(data[i] == v) {
		// found it, so copy everything above down one spot
		System.arraycopy(data,i+1,data,i,(length-i-1));
		return true;
	    }
	}
	return false;
    }

    public int get(int idx)  {
	return data[idx];
    }

    public int indexOf(int v)  {
	for(int i=0;i!=length;++i) {
	    if(data[i] == v) {
		return i;
	    }
	}
	return -1;
    }

    public String toString() {
	String r = "[";
	boolean firstTime=true;
	for(int v : data) {
	    if(!firstTime) {
		r += ", ";
	    }
	    firstTime=false;
	    r += v;
	}
	return r + "]";
    }

    public static void main(String[] args) {
	IntVec iv1 = new IntVec(6);
	IntVec iv2 = new IntVec(3);
	iv1.add(1);
	iv1.add(2);
	iv1.add(3);
	System.out.println("GOT: " + iv1);
	iv2.add(4);
	iv2.add(5);
	iv2.add(6);
	System.out.println("GOT: " + iv2);
	iv1.addAll(iv2);
	System.out.println("GOT: " + iv2);
	System.out.println("GOT: " + iv1);
    }
}
