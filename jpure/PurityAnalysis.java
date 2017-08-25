package jpure;

import java.util.*;
import jkit.jil.util.*;
import jkit.jil.tree.*;

public interface PurityAnalysis {
	public Set<Label> methodLabels(Tag.Method method);	
	public boolean isFieldLocal(Tag.Field field);
	public void apply(List<JilClass> classes);
	public void addWatchMethod(Type.Clazz owner, String name) throws ClassNotFoundException;
}
