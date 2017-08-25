package jpure;

import jkit.java.tree.Type;
import jkit.jil.util.Tag;

public class Label {
	
	public final static Label FRESH = new Fresh();	
	public final static Label IMPURE = new Impure();
	public final static Label UNKNOWN = new Unknown();
	public final static Label RECEIVER = new Receiver();
	
	public final static Parameter parameters[] = {
		new Parameter(0),
		new Parameter(1),
		new Parameter(2),
		new Parameter(3),
		new Parameter(4),
		new Parameter(5),
		new Parameter(6),
		new Parameter(7),
		new Parameter(8),
		new Parameter(9),
		new Parameter(10),
		new Parameter(11),
		new Parameter(12),
		new Parameter(13),
		new Parameter(14),
		new Parameter(15),
		new Parameter(16),
		new Parameter(17),
		new Parameter(18),
		new Parameter(19),
		new Parameter(20),
		new Parameter(21),
		new Parameter(22),
		new Parameter(22)
	};
	
	public static class Parameter extends Label {
		public final int index;
		
		private Parameter(int index) {
			this.index = index;
		}
		
		public boolean equals(Object o) {
			if(o instanceof Parameter) {
				Parameter p = (Parameter) o;
				return p.index == index;
			}
			return false;
		}
		
		public int hashCode() {
			return index;
		}
	}	
	
	public static class NonLocalField extends Label {
		public final Tag.Field tag;		
		
		public NonLocalField(Tag.Field tag) {
			this.tag = tag;
		}
		
		public boolean equals(Object o) {
			if(o instanceof NonLocalField) {
				NonLocalField p = (NonLocalField) o;
				return tag.equals(p.tag);
			}
			return false;
		}
		
		public int hashCode() {
			return tag.hashCode();
		}
	}	
	
	public static class Receiver extends Label {
		public boolean equals(Object o) {
			return o instanceof Receiver;
		}
		public int hashCode() {
			return 0;
		}
	}			
	
	public static class Fresh extends Label {
		public boolean equals(Object o) {
			return o instanceof Fresh;
		}
		public int hashCode() {
			return 0;
		}
	}			
		
	public static class Impure extends Label {
		public boolean equals(Object o) {
			return o instanceof Impure;
		}
		public int hashCode() {
			return 0;
		}
	}		
	
	private static class Unknown extends Label {
		public boolean equals(Object o) {
			return o instanceof Unknown;
		}
		public int hashCode() {
			return 0;
		}
	}	
}
