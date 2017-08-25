package jpure;

import jkit.jil.dfa.*;
import java.util.*;

public final class FreshFlowSet implements FlowSet, Cloneable {
	private final HashMap<String,Set<Label>> mapping;
	
	public FreshFlowSet() {
		mapping = new HashMap<String,Set<Label>>();
	}
	
	public void clear(String var) {
		mapping.put(var, new HashSet<Label>());
	}
	
	public void set(String var, Collection<Label> levels) {
		mapping.put(var, new HashSet<Label>(levels));
	}
	
	public void add(String var, Label label) {
		Set<Label> levels = mapping.get(var);
		if(levels == null) {
			levels = new HashSet<Label>();
			mapping.put(var,levels);
		}		
		levels.add(label);
	}
	
	public Set<Label> get(String var) {
		return mapping.get(var);
	}
	
	public Object clone() {
		FreshFlowSet r = new FreshFlowSet();
		r.mapping.putAll(mapping);
		return r;
	}
	
	public Set<String> keySet() {
		return mapping.keySet();
	}
	
	public FlowSet join(FlowSet _f) {
		FreshFlowSet f = (FreshFlowSet) _f;
		FreshFlowSet r = new FreshFlowSet();
		boolean changed = false;
		
		for(Map.Entry<String,Set<Label>> e : mapping.entrySet()) {
			String key = e.getKey();
			Set<Label> mi = e.getValue();
			Set<Label> fi = f.get(key);
			if(fi != null && mi != null) {
				HashSet<Label> s = new HashSet<Label>(mi);
				s.addAll(fi);
				r.mapping.put(key, s);
				changed |= !s.equals(mi);
			} else {
				changed = true;
			}
		}
		
		if(changed) {
			return r;
		} else {
			return this;
		}
	}
	
	public String toString() {
		return "#" + mapping.toString();
	}
}
