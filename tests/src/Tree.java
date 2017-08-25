import java.util.*;

public class Tree {
    Tree lhs;
    Tree rhs;
    int data;
    
    public Tree(Tree lhs, Tree rhs, int data) {
	this.lhs = lhs;
	this.rhs = rhs;
	this.data = data;
    }

    public Tree append(Tree tree) {
	if(lhs == null) {
	    return new Tree(tree,rhs,data);
	} else {
	    Tree r = lhs.append(tree);
	    return new Tree(r,rhs,data);
	}
    }

    public int size() {
	if(lhs == null) {
	    if(rhs == null) {
		return 1;
	    } else {
		return 1 + rhs.size();
	    }
	} else if(rhs == null) {
	    return 1 + lhs.size();
	}
	return 1 + lhs.size() + rhs.size();
    }

    private void flattern(int i, int[] rs) {
	rs[i] = data;
	if(lhs == null) {
	    if(rhs == null) {
		return;
	    } else {
		rhs.flattern(i+1,rs);
	    }
	} else if(rhs == null) {
	    lhs.flattern(i+1,rs);
	} else {
	    lhs.flattern(i+1,rs);
	    rhs.flattern(i+1+lhs.size(),rs);
	}
    }

    public int[] flattern() {
	int[] rs = new int[size()];
	flattern(0,rs);
	return rs;
    }

    public static void main(String[] args) {
	Tree t1 = new Tree(null,null,1);
	Tree t2 = new Tree(t1,null,2);
	Tree t3 = new Tree(t1,t2,2);
	Tree t4 = new Tree(t3,t2,2);
	
	System.out.println("GOT: " + Arrays.toString(t4.flattern()));
	System.out.println("SIZE: " + t4.size());	

	Tree t5 = t4.append(t3);
	System.out.println("GOT: " + Arrays.toString(t5.flattern()));
	System.out.println("SIZE: " + t5.size());		
    }
}
