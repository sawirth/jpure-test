package jpure;

import static jkit.compiler.SyntaxError.internal_error;
import static jkit.compiler.SyntaxError.syntax_error;

import java.util.*;

import jkit.bytecode.ClassFile;
import jkit.util.*;
import jkit.util.graph.*;
import jkit.compiler.ClassLoader;
import jkit.compiler.*;
import jkit.java.stages.TypeSystem;
import jkit.java.tree.Expr;
import jkit.jil.dfa.*;
import jkit.jil.tree.*;
import jkit.jil.util.*;
import jkit.jil.ipa.StaticDependenceGraph.*;

public class ExtendedAnalysis extends ForwardAnalysis<FreshFlowSet> implements PurityAnalysis {
	private final ClassLoader loader;
	private final TypeSystem types;
	private final Graph<Tag.Method,Invocation> callGraph;
		
	// The domain is useful since it allows us to determine the set
	// of methods which the inference is running over.
	private final HashSet<Tag.Method> domain = new HashSet();
	private final HashSet<Tag.Method> worklist = new HashSet();		
	private final HashMap<Tag.Method, Set<Label>> methodLabels = new HashMap();		
	private final HashMap<Tag.Field, Boolean> fieldLabels = new HashMap();
	
	// The watchlist is useful for debugging methods which you're trying to
	// figure out why they're not pure.
	private final HashSet<Pair<Type.Clazz,String>> watchlist = new HashSet();
	
	private boolean currentMethodIsConstructor;
	private Set<Label> currentMethodLabels;
	
	public ExtendedAnalysis(Graph<Tag.Method, Invocation> callGraph,
			TypeSystem types, ClassLoader loader) {
		this.callGraph = callGraph;
		this.loader = loader;
		this.types = types;						
	}
	
	public void addWatchMethod(Type.Clazz owner, String name) throws ClassNotFoundException {
		Clazz l = loader.loadClass(owner);		
		watchlist.add(new Pair(Types.stripGenerics(l.type()), name));		
	}
	
	public void apply(List<JilClass> classes) {		
		worklist.clear();		
		methodLabels.clear();
		domain.clear();
		
		HashMap<Tag.Method,JilMethod> methodMap = new HashMap();
		
		// First, initialise the worklist
		for(JilClass owner : classes) {			
			for(JilMethod method : owner.methods()) {
				Tag.Method node = Tag.create(owner, method.name(), method
						.type());
				worklist.add(node);
				methodMap.put(node,method);							
				domain.add(node);
			}			
		}
		
		try {
			// Second, iterate until a fixed point is reached
			while(!worklist.isEmpty()) {
				Tag.Method n = worklist.iterator().next();
				try {
					worklist.remove(n);

					JilMethod m = methodMap.get(n);

					if(m != null && m.body() != null) {						 						
						// m may be null if this method is not contained in the initial
						// set of classes considered, or it's an interface or
						// abstract class.
						Set<Label> olevels = new HashSet<Label>(methodLabels(n));	
						Set<Label> nlevels = infer(m,n);																						
						if(!olevels.equals(nlevels)) {
							forceMethod(n,nlevels);
						}
							
					}
				} catch(SyntaxError ex) {
					if(ex.fileName() == null) {
						throw new SyntaxError(ex.getMessage(), fileName(n.owner(),
							classes), ex.line(), ex.column(), 1, ex);
					} else {
						throw ex;
					}
				}
			}
		} catch(ClassNotFoundException e) {
			throw new RuntimeException("Problem in NonNullTypeInference!");
		}
	}
	
	public void forceMethod(Tag.Method method, Set<Label> labels) throws ClassNotFoundException {
		if (watchlist.contains(new Pair(method.owner(), method.name()))) {
			System.out.println("*** " + method
					+ " FORCED");
		}		
		methodLabels.put(method,labels);						
		
		// First, add my direct predecessors back to the worklist, since
		// these may be affected by my change of status.
		for(Invocation e : callGraph.to(method)) {				
			Tag.Method p = e.first();
			worklist.add(p);			
		}
		// Second, account for contra-variance of parameters. This
		// is done by traversing the hierarchy to find methods which are
		// overridden by this.						

		List<Triple<Clazz, Clazz.Method, Type.Function>> overrides = types
		.listOverrides((Type.Clazz) method.owner(), method.name(),
				method.type(), loader);

		for(Triple<Clazz,Clazz.Method,Type.Function> or : overrides) {
			Tag.Method orNode = Tag.create(or.first(), method
					.name(), or.second().type());			
			Set<Label> orLevels = methodLabels(orNode);			
			
			boolean changed = false;
			for(Label l : labels) {
				if (!(l instanceof Label.Fresh)) {
					changed |= orLevels.add(l);
				}
			}
			
			if (!labels.contains(Label.FRESH) && orLevels.contains(Label.FRESH)) {
				orLevels.remove(Label.FRESH);
				changed = true;
			}
			
			if(changed) {			
				if (watchlist.contains(new Pair(orNode
						.owner(), orNode.name()))) {
					System.out.println("*** " + orNode
							+ " FORCED BY " + method);
				}
				// Also, add my direct predecessors.
				for(Invocation e : callGraph.to(orNode)) {					
					worklist.add(e.first());
				}	
			}
		}	
		
		// Do reverse direction for purity
		
		
	}	
	
	public Set<Label> infer(JilMethod method, Tag.Method myNode)
			throws ClassNotFoundException {		
		currentMethodIsConstructor = method.name().equals(((Type.Clazz)myNode.owner()).lastComponent().first());		
		currentMethodLabels = methodLabels(myNode);
		
		FreshFlowSet initStore = new FreshFlowSet();
		List<Type> paramTypes = method.type().parameterTypes();
		List<JilMethod.JilParameter> params = method.parameters();
		for(int i=0;i!=params.size();++i) {
			JilMethod.JilParameter p = params.get(i);
			Type t = paramTypes.get(i);			
			if(t instanceof Type.Reference) {
				initStore.add(p.name(), Label.parameters[i]);
			} else {
				initStore.add(p.name(), Label.FRESH);
			}
		}				
		
		for(Pair<String,Boolean> l : method.localVariables()) {
			initStore.add(l.first(), Label.UNKNOWN);
		}
		
		if(currentMethodIsConstructor) {			
			initStore.add("this", Label.FRESH);
			initStore.add("super", Label.FRESH);
		} else {
			initStore.add("this", Label.RECEIVER);
			initStore.add("super", Label.RECEIVER);
		}
		
		initStore.add("$", Label.FRESH); // could do better probably
		
		start(method,initStore,new FreshFlowSet());
		
		if(currentMethodLabels.contains(Label.IMPURE)) {
			currentMethodLabels.clear();
			currentMethodLabels.add(Label.IMPURE);
		}				
		
		return currentMethodLabels;
	}
	
	public FreshFlowSet transfer(JilStmt stmt, FreshFlowSet in) {
		FreshFlowSet locals = (FreshFlowSet) in.clone();
			
		try {
			if(stmt instanceof JilStmt.Assign) {
				transfer((JilStmt.Assign)stmt,locals);					
			} else if(stmt instanceof JilExpr.Invoke) {
				infer((JilExpr.Invoke)stmt,locals);										
			} else if(stmt instanceof JilExpr.New) {
				infer((JilExpr.New) stmt,locals);						
			} else if(stmt instanceof JilStmt.Return) {
				transfer((JilStmt.Return) stmt,locals);
			} else if(stmt instanceof JilStmt.Throw) {
				transfer((JilStmt.Throw) stmt,locals);
			} else if(stmt instanceof JilStmt.Nop) {		
				
			} else if(stmt instanceof JilStmt.Lock) {		
				transfer((JilStmt.Lock) stmt,locals);
			} else if(stmt instanceof JilStmt.Unlock) {		
				transfer((JilStmt.Unlock) stmt, locals);
			} else {
				syntax_error("unknown statement encountered (" + stmt.getClass().getName() + ")",stmt);
				return null;
			}		
		} catch(ClassNotFoundException e) {
			internal_error(stmt,e);
			return null;
		} catch(MethodNotFoundException e) {
			internal_error(stmt,e);
			return null;
		} catch(Exception e) {						
			internal_error(stmt,e);		
			return null;
		}		
		
		return locals;
	}
	
	public FreshFlowSet transfer(JilExpr expr, FreshFlowSet in) {
		FreshFlowSet locals = (FreshFlowSet) in.clone();
		
		infer(expr,locals);
		
		return locals;
	}
	
	protected void transfer(JilStmt.IfGoto stmt, FreshFlowSet locals) {		
		infer(stmt.condition(), locals);			
	}
	
	protected void transfer(JilStmt.Switch stmt, FreshFlowSet locals) {		
		infer(stmt.condition(), locals);			
	}
	
	protected void transfer(JilStmt.Assign stmt, FreshFlowSet locals)
			throws FieldNotFoundException, ClassNotFoundException {
		JilExpr lhs = stmt.lhs();
		JilExpr rhs = stmt.rhs();

		// First, flattern cast expressions. The reason for this is that if this
        // expression is really just a variable-variable assignment, then I need
        // to know about it.
		if(rhs instanceof JilExpr.Cast) {
			rhs = ((JilExpr.Cast)rhs).expr();
		} else if(rhs instanceof JilExpr.Convert) {
			rhs = ((JilExpr.Convert)rhs).expr();
		}
		
		if (lhs instanceof JilExpr.Variable
				&& rhs instanceof JilExpr.Variable) {
			JilExpr.Variable lhs_v = (JilExpr.Variable) lhs;
			JilExpr.Variable rhs_v = (JilExpr.Variable) rhs;
			locals.set(lhs_v.value(), locals.get(rhs_v.value()));
		} else if (lhs instanceof JilExpr.Variable) {
			JilExpr.Variable lhs_v = (JilExpr.Variable) lhs;			
			locals.set(lhs_v.value(),infer(rhs, locals));							
		} else if(lhs instanceof JilExpr.ArrayIndex) {			
			JilExpr.ArrayIndex ai = (JilExpr.ArrayIndex) lhs;
			Set<Label> lhsLabels = infer(ai.target(), locals);
			infer(ai.index(),locals);
			Set<Label> rhsLabels = infer(rhs, locals);	
			
			if(lhsLabels.contains(Label.UNKNOWN)) {
				currentMethodLabels.add(Label.IMPURE);
			} else if(rhsLabels.size() == 1 && rhsLabels.contains(Label.FRESH)) {
				// OK				
			} else if (ai.target() instanceof JilExpr.Deref) {
				JilExpr.Deref deref = (JilExpr.Deref) ai.target();
				Set<Label> tLabels = infer(deref.target(), locals);
				if(isFieldLocal(deref)) {
					if (currentMethodIsConstructor
							|| (tLabels.size() == 1 && tLabels
									.contains(Label.FRESH))) {
						// NOT OK
						Tag.Field tag = Tag.create((Type.Reference) deref.target().type(),
								deref.name(), loader);
						fieldLabels.put(tag,false);					
					} else {
						currentMethodLabels.add(Label.IMPURE);
					}
				} 				
			}		
			inferLocalParameters(lhsLabels);
		} else {
			JilExpr.Deref deref = (JilExpr.Deref) lhs;
			Set<Label> lhsLabels = infer(deref.target(), locals);			
			Set<Label> rhsLabels = infer(rhs, locals);						
			
			if(lhsLabels.contains(Label.UNKNOWN)) {
				currentMethodLabels.add(Label.IMPURE);
			} else if(isFieldLocal(deref)) {
				if(rhsLabels.size() == 1 && rhsLabels.contains(Label.FRESH)) {
					// OK
				} else if (currentMethodIsConstructor
						|| (lhsLabels.size() == 1 && lhsLabels
								.contains(Label.FRESH))) {
					// NOT OK
					Tag.Field tag = Tag.create((Type.Reference) deref.target().type(),
							deref.name(), loader);
					fieldLabels.put(tag,false);					
				} else {
					currentMethodLabels.add(Label.IMPURE);
				}
			} 				
			
			inferLocalParameters(lhsLabels);
		}
	}
	
	protected void transfer(JilStmt.Return stmt, FreshFlowSet locals) {		
		if(stmt.expr() != null) {
			Set<Label> r = infer(stmt.expr(), locals);			
			if(r.size() == 1 && r.contains(Label.FRESH)) {
				// In this case, all return objects are fresh				
			} else {
				// In this case, some return objects are not fresh
				currentMethodLabels.remove(Label.FRESH);
			}
		} 		
	}
	
	protected void transfer(JilStmt.Throw stmt, FreshFlowSet locals) {		
		infer(stmt.expr(), locals);			
	}
	
	protected void transfer(JilStmt.Lock stmt, FreshFlowSet locals) {		
		infer(stmt.expr(), locals);			
	}
	
	protected void transfer(JilStmt.Unlock stmt, FreshFlowSet locals) {		
		infer(stmt.expr(), locals);			
	}
	
	protected Set<Label> infer(JilExpr expr, FreshFlowSet locals) {
		try {
			if(expr instanceof JilExpr.ArrayIndex) {
				return infer((JilExpr.ArrayIndex) expr, locals);
			} else if(expr instanceof JilExpr.BinOp) {		
				return infer((JilExpr.BinOp) expr, locals);
			} else if(expr instanceof JilExpr.UnOp) {		
				return infer((JilExpr.UnOp) expr, locals);								
			} else if(expr instanceof JilExpr.Cast) {
				return infer((JilExpr.Cast) expr, locals);			 			
			}  else if(expr instanceof JilExpr.Convert) {
				return infer((JilExpr.Convert) expr, locals);			 			
			} else if(expr instanceof JilExpr.ClassVariable) {
				return infer((JilExpr.ClassVariable) expr, locals);			 			
			} else if(expr instanceof JilExpr.Deref) {
				return infer((JilExpr.Deref) expr, locals);			 							
			} else if(expr instanceof JilExpr.Variable) {
				return infer((JilExpr.Variable) expr, locals);
			} else if(expr instanceof JilExpr.InstanceOf) {
				return infer((JilExpr.InstanceOf) expr, locals);
			} else if(expr instanceof JilExpr.Invoke) {
				return infer((JilExpr.Invoke) expr, locals);
			} else if(expr instanceof JilExpr.New) {
				return infer((JilExpr.New) expr, locals);
			} else if(expr instanceof JilExpr.Value) {
				return infer((JilExpr.Value) expr, locals);
			} else {
				syntax_error("Unknown expression encountered", expr);
				return null;
			}
		} catch(ClassNotFoundException e) {
			internal_error(expr,e);
			return null;
		} catch(MethodNotFoundException e) {
			internal_error(expr,e);
			return null;
		} catch(Exception e) {
			internal_error(expr,e);		
			return null;
		}
	}		
	
	public Set<Label> infer(JilExpr.ArrayIndex expr, FreshFlowSet locals) { 		
		infer(expr.target(),locals);
		infer(expr.index(),locals);
		Set<Label> labels = new HashSet<Label>();		
		if(expr.type() instanceof Type.Reference) {
			labels.add(Label.UNKNOWN);
		} else {
			labels.add(Label.FRESH);
		}			
		return labels;
	}	
	public Set<Label> infer(JilExpr.BinOp expr, FreshFlowSet locals) {
		infer(expr.lhs(),locals);
		infer(expr.rhs(),locals);
		Set<Label> labels = new HashSet<Label>();
		labels.add(Label.FRESH);
		return labels; 		
	}
	public Set<Label> infer(JilExpr.UnOp expr, FreshFlowSet locals) { 		
		return infer(expr.expr(), locals); 
	}
	public Set<Label> infer(JilExpr.Cast expr, FreshFlowSet locals) { 
		return infer(expr.expr(), locals);		
	}
	public Set<Label> infer(JilExpr.Convert expr, FreshFlowSet locals) { 
		return infer(expr.expr(), locals);		
	}
	public Set<Label> infer(JilExpr.ClassVariable expr, FreshFlowSet locals) { 		
		// do nothing!
		Set<Label> labels = new HashSet<Label>();
		labels.add(Label.UNKNOWN);		
		return labels;
	}

	public Set<Label> infer(JilExpr.Deref expr, FreshFlowSet locals)
			throws FieldNotFoundException, ClassNotFoundException { 		
		Set<Label> targets = infer(expr.target(), locals);
		Set<Label> labels = new HashSet<Label>();		
		if(expr.type() instanceof Type.Reference) {			
			if(isFieldLocal(expr)) {				
				return targets; 
			} else {
				labels.add(Label.UNKNOWN);
			}
		} else {			
			labels.add(Label.FRESH);			
		}
		return labels;
	}	
	public Set<Label> infer(JilExpr.Variable expr, FreshFlowSet locals) { 
		// do nothing!				
		return locals.get(expr.value());
	}
	public Set<Label> infer(JilExpr.InstanceOf expr, FreshFlowSet locals) { 		
		return infer(expr.lhs(), locals);
	}
	public Set<Label> infer(JilExpr.Invoke expr, FreshFlowSet locals) throws ClassNotFoundException, MethodNotFoundException { 				
		JilExpr target = expr.target();
		
		Set<Label> receiver = infer(target, locals);
		ArrayList<Set<Label>> parameters = new ArrayList();
		
		for(JilExpr e : expr.parameters()) {
			parameters.add(infer(e, locals));
		}		
		
		Set<Label> methodLabels;
		
		Tag.Method targetNode = Tag.create((Type.Reference) target.type(),
				expr.name(), expr.funType(), loader);
		
		if (!domain.contains(targetNode)) {
			Pair<Clazz, Clazz.Method> rt = loader.determineMethod(
					(Type.Reference) target.type(), expr.name(), expr
					.funType());			 
			// TO DO!
			methodLabels = methodLabels(rt.second());
		} else {			
			methodLabels = methodLabels(targetNode);			
		}	
		
		// First, propagate impurity.
		if(methodLabels.contains(Label.IMPURE)){
			currentMethodLabels.add(Label.IMPURE);
		} else {
			// Second, propagate receiver locality
			
			if(methodLabels.contains(Label.RECEIVER)) {
				if(receiver.contains(Label.UNKNOWN)) {
					// this is effectively a dereference
					currentMethodLabels.add(Label.IMPURE);
				} else {					
					for(Label l : receiver) {
						if(!(l == Label.FRESH)) {
							currentMethodLabels.add(l);
						}
					}
				}
			}
			// Third, propagate parameter locality
			int idx = 0;
			for(Set<Label> labels : parameters) {
				if(methodLabels.contains(Label.parameters[idx])) {
					// this means the parameter is marked local in the called
					// method.
					if(labels.contains(Label.UNKNOWN)) {
						// this is effectively a dereference
						currentMethodLabels.add(Label.IMPURE);
					} else {
						for(Label l : receiver) {
							if(!(l == Label.FRESH)) {
								currentMethodLabels.add(l);
							}
						}
					}
				}
				idx = idx + 1;
			}
		}
		
		// Finally, work out return value.
		HashSet<Label> labels = new HashSet<Label>();
		if(methodLabels.contains(Label.FRESH)){
			labels.add(Label.FRESH);
		} else {
			labels.add(Label.UNKNOWN);
		}
		
		return labels;
	}

	public Set<Label> infer(JilExpr.New expr, FreshFlowSet locals)
			throws ClassNotFoundException, MethodNotFoundException { 				
		
		ArrayList<Set<Label>> parameters = new ArrayList();		
		for(JilExpr e : expr.parameters()) {
			parameters.add(infer(e, locals));
		}		
		
		if(expr.type() instanceof Type.Clazz) { 
			Type.Clazz target = (Type.Clazz) expr.type();
			String name = target.lastComponent().first();
			Tag.Method targetNode = Tag.create((Type.Reference) expr.type(),
					name, expr.funType(), loader);

			Set<Label> methodLabels;
			
			if (!domain.contains(targetNode)) {
				Pair<Clazz, Clazz.Method> rt = loader.determineMethod(
						(Type.Reference) target, name, expr
						.funType());								
				// TO DO
				methodLabels = methodLabels(rt.second());
			} else {
				methodLabels = methodLabels(targetNode);					
			}						

			// First, propagate impurity.
			if(methodLabels.contains(Label.IMPURE)){
				currentMethodLabels.add(Label.IMPURE);
			} else {

				// Second, propagate parameter locality
				int idx = 0;
				for(Set<Label> labels : parameters) {
					if(methodLabels.contains(Label.parameters[idx])) {
						// this means the parameter is marked local in the called
						// method.
						if(labels.contains(Label.UNKNOWN)) {
							// this is effectively a dereference
							currentMethodLabels.add(Label.IMPURE);
						} else {
							// COULD BE SLIGHTLY WRONG ?
							labels.remove(Label.FRESH);
							currentMethodLabels.addAll(labels);
						}
					}
					idx = idx + 1;
				}
			}
		}
		
		HashSet<Label> labels = new HashSet<Label>();
		labels.add(Label.FRESH);		
		return labels;					
	}
	
	public Set<Label> infer(JilExpr.Value expr, FreshFlowSet locals) { 		
		if(expr instanceof JilExpr.Array) {
			JilExpr.Array ae = (JilExpr.Array) expr;			
			for(JilExpr v : ae.values()) {
				infer(v, locals);
			}				
		} 
		HashSet<Label> labels = new HashSet<Label>();
		// this could be improved
		labels.add(Label.FRESH);
		return labels;
	}		
	
	public String fileName(Type.Reference r, List<JilClass> classes) {
		for(JilClass c : classes) {
			Type.Clazz tc = Types.stripGenerics(c.type());
			if(tc.equals(r)) {
				return c.sourceFile();
			}
		}
		return null;
	}

	/**
	 * This method updates the currentMethodLabels as a result of a dereference
	 * on the given labels.
	 * 
	 * @param labels
	 */
	public void inferLocalParameters(Set<Label> labels) {
		if(labels.contains(Label.UNKNOWN)){
			currentMethodLabels.add(Label.IMPURE);
		} else {			
			for (Label l : labels) {
				if (l instanceof Label.Parameter || l instanceof Label.Receiver) {
					currentMethodLabels.add(l);
				}
			}
		}
	}
	
	public boolean isFieldLocal(JilExpr.Deref deref)
			throws FieldNotFoundException, ClassNotFoundException {
		Tag.Field tag = Tag.create((Type.Reference) deref.target().type(),
				deref.name(), loader);				
		Boolean local = fieldLabels.get(tag);
		if(local == null) {
			fieldLabels.put(tag, true);
			return true;
		}
		return local;
	}
	
	public boolean isFieldLocal(Tag.Field tag) {
		try {
			Pair<Clazz, Clazz.Field> rt = loader.determineField(tag.owner(),tag.name());	

			Boolean local = fieldLabels.get(tag);
			if (local == null) {
				fieldLabels.put(tag, true);
				return true;
			}
			return local;
		} catch(FieldNotFoundException e) {
			throw new RuntimeException(e);
		} catch(ClassNotFoundException e) {
			throw new RuntimeException(e);	
		}		
	}	
	
	public static class FreshAttr implements jkit.compiler.SyntacticAttribute {
		public int level;
		public FreshAttr(int level) {
			this.level=level;
		}
	}
	
	
	public boolean hasAnnotation(Clazz.Method method, String annotation) {
		if (method instanceof ClassFile.Method) {
			for (Modifier m : method.modifiers()) {
				if (m instanceof Modifier.Annotation) {
					Modifier.Annotation a = (Modifier.Annotation) m;
					Type.Clazz t = a.type();
					if (t.pkg().equals("jpure.annotations")
							&& t.lastComponent().first().equals(annotation)) {
						return true;
					}
				}
			}
		}
		return false;
	}		
	
	public Set<Label> methodLabels(Clazz.Method method) {
		Set<Label> labels = new HashSet<Label>();
		boolean pure = false;
		
		if(hasAnnotation(method,"Pure")) {
			pure = true;
		} else if(hasAnnotation(method,"Fresh")) {
			labels.add(Label.FRESH);
			pure = true;
		} 
		
		if(hasAnnotation(method,"Local")) {
			labels.add(Label.RECEIVER);			
		} else if(!pure) {
			labels.add(Label.IMPURE);
		}
		
		return labels;
	}
	
	public Set<Label> methodLabels(Tag.Method n) {		
		Set<Label> levels = methodLabels.get(n);
		if(levels == null) {
			levels = new HashSet<Label>();
			levels.add(Label.FRESH);
			methodLabels.put(n, levels);
		} 		
		return levels;		
	}					
}
