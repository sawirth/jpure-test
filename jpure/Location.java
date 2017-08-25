package jpure;

import jkit.jil.tree.Type;
import jkit.jil.util.Types;
import jkit.jil.util.Exprs;

/**
 * A Location represents an abstract location somewhere in the system.
 * 
 * @author djp
 * 
 */
public abstract class Location {
	public static abstract class Local extends Location {}
	
	/**
	 * A VarLocation represents a local variable location.
	 * 
	 * @author djp	 
	 */
	public static class Var extends Local {
		public final String name;
		
		public Var(String n) {
			this.name = n;
		}
		
		public boolean equals(Object o) {
			if(o instanceof Var) {
				Var v = (Var) o;
				return v.name.equals(name);
			}
			return false;
		}
		
		public int hashCode() {
			return name.hashCode();
		}
		
		public String toString() {
			return name;
		}
	}
		
	public static class Field extends Local {
		public final Exprs.Equiv field; 
		
		public Field(Exprs.Equiv field) {
			this.field = field;			
		}
		
		public boolean equals(Object o) {
			if(o instanceof Field) {
				Field v = (Field) o;
				return v.field.equals(field);
			}
			return false;
		}
		
		public int hashCode() {
			return field.hashCode();
		}
		
		public String toString() {
			return field.toString();
		}
	}
		
	
	public static class Param extends Location {
		public final Type.Reference owner;
		public final Type.Function funtype;
		public final String name;
		public final int index;
		
		public Param(Type.Reference owner, Type.Function funtype,
				String name, int idx) {
			if(name.equals("this") || name.equals("super")) {
				this.name = ((Type.Clazz)owner).lastComponent().first();
			} else {
				this.name = name;
			}
			this.owner = (Type.Reference) Types.stripGenerics(owner);
			this.funtype = Types.stripGenerics(funtype);
			this.index = idx;
		}
		
		public boolean equals(Object o) {
			if(o instanceof Param) {
				Param v = (Param) o;
				return v.name.equals(name) && owner.equals(v.owner)
						&& funtype.equals(v.funtype) && index == v.index;
			}
			return false;
		}
		
		public int hashCode() {
			return name.hashCode() ^ owner.hashCode();
		}
		
		public String toString() {
			return owner + "." + name + ":" + funtype + "$" + index;
		}
	}
	
	public static class Return extends Param {
		
		public Return(Type.Reference owner, Type.Function funtype,
				String name) {
			super(owner,funtype,name,-1);
		}
		
		public String toString() {
			return owner + "." + name + ":" + funtype + "$";
		}
	}
	
	/*
	 * This represents a freshly allocated location
	 */
	public static class New extends Location {		
		public New() {			
		}
		
		public boolean equals(Object o) {			
			return o instanceof New;
		}
		
		public int hashCode() {
			return 123;
		}
		
		public String toString() {
			return "new";
		}
	}
	
	/**
	 * This represents an element of an Array
	 * @author djp	 
	 */
	public static class Array extends Location {
		public final Location target;
		public Array(Location target) {
			this.target = target;
		}
		
		public boolean equals(Object o) {
			if(o instanceof Array) {
				Array v = (Array) o;
				return v.target.equals(target);
			}
			return false;
		}
		
		public int hashCode() {
			return target.hashCode();
		}
		
		public String toString() {
			return target + "[?]";
		}
	}
}
