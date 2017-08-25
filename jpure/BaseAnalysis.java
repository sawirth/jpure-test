package jpure;

import static jkit.compiler.SyntaxError.syntax_error;

import java.util.*;

import jkit.util.*;
import jkit.util.graph.*;
import jkit.bytecode.ClassFile;
import jkit.compiler.ClassLoader;
import jkit.compiler.*;
import static jkit.compiler.SyntaxError.*;
import jkit.java.stages.TypeSystem;
import jkit.jil.tree.*;
import jkit.jil.util.*;
import jkit.jil.ipa.StaticDependenceGraph.*;

public class BaseAnalysis implements PurityAnalysis {
	private final ClassLoader loader;
	private final TypeSystem types;
	private final Graph<Tag.Method,Invocation> callGraph;
	
	// The domain is useful since it allows us to determine the set
	// of methods which the inference is running over.
	private final HashSet<Tag.Method> domain = new HashSet();
	private final HashSet<Tag.Method> worklist = new HashSet();				
	private final HashSet<Tag.Method> impureMethods = new HashSet();
	
	private final int PURE = 0;
	private final int IMPURE = Integer.MAX_VALUE;
	
	// The watchlist is useful for debugging methods which you're trying to
	// figure out why they're not pure.
	private final HashSet<Pair<Type.Clazz,String>> watchlist = new HashSet();
	
	public BaseAnalysis(Graph<Tag.Method, Invocation> callGraph,
			TypeSystem types, ClassLoader loader) {
		this.callGraph = callGraph;
		this.loader = loader;
		this.types = types;						
	}
	
	public void addWatchMethod(Type.Clazz owner, String name) throws ClassNotFoundException {
		Clazz l = loader.loadClass(owner);		
		watchlist.add(new Pair(Types.stripGenerics(l.type()), name));		
	}
	
	public Set<Label> methodLabels(Tag.Method n) {
		Set<Label> labels = new HashSet<Label>();
		if(impureMethods.contains(n)) {
			labels.add(Label.IMPURE);
		} 
		return labels;
	}
	
	public boolean isFieldLocal(Tag.Field f) {
		return false;
	}
	
	public int methodFreshLevel(Tag.Method n) {
		return 0;
	}
		
	public void apply(List<JilClass> classes) {
		worklist.clear();		
		impureMethods.clear();
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
						int mode = infer(m,n); 
						
						if (mode == IMPURE
								&& !impureMethods.contains(n)) {
							forceMethodNonLocal(n);
						}
					}				
				} catch(SyntaxError ex) {
					if(ex.fileName() == null) {
						throw new SyntaxError(ex.getMessage(), fileName(n.owner(),
							classes), ex.line(), ex.column());
					} else {
						throw ex;
					}
				}
			}
		} catch(ClassNotFoundException e) {
			throw new RuntimeException("Problem in NonNullTypeInference!");
		}
	}
	    
    public void forceMethodNonLocal(Tag.Method n) throws ClassNotFoundException {
    	if (watchlist.contains(new Pair(n.owner(), n.name()))) {
			System.out.println("*** " + n
					+ " FORCED NON-LOCAL");
		}	
		impureMethods.add(n);
		// First, add my direct predecessors back to the worklist, since
		// these may be affected by my change of status.
		for(Invocation e : callGraph.to(n)) {				
			Tag.Method p = e.first();
			if(!impureMethods.contains(p)) {
				worklist.add(p);
			}
		}
		// Second, account for contra-variance of parameters. This
		// is done by traversing the hierarchy to find methods which are
		// overridden by this.						

		List<Triple<Clazz, Clazz.Method, Type.Function>> overrides = types
		.listOverrides((Type.Clazz) n.owner(), n.name(),
				n.type(), loader);

		for(Triple<Clazz,Clazz.Method,Type.Function> or : overrides) {
			Tag.Method orNode = Tag.create(or.first(), n
					.name(), or.second().type());

			if(!impureMethods.contains(orNode)) {

				if (watchlist.contains(new Pair(orNode
						.owner(), orNode.name()))) {
					System.out.println("*** " + orNode
							+ " FORCED NON-LOCAL BY " + n);
				}
				
				impureMethods.add(orNode);
				// Also, add my direct predecessors.
				for(Invocation e : callGraph.to(orNode)) {					
					worklist.add(e.first());
				}	
			}
		}	
	}
    
	public int infer(JilMethod method, Tag.Method myNode) throws ClassNotFoundException {		
		List<JilStmt> body = method.body();
		
		// first, initialise label map
		int purity = PURE;
		for(JilStmt s : body) {			
			purity = Math.max(purity, infer(s, myNode));			 			
		}
						
		return purity;
	}
	
	protected int infer(JilStmt stmt, Tag.Method myNode) {
		if(stmt instanceof JilStmt.IfGoto) {
			return infer((JilStmt.IfGoto)stmt,myNode);					
		} else if(stmt instanceof JilStmt.Switch) {
			return infer((JilStmt.Switch)stmt,myNode);					
		} else if(stmt instanceof JilStmt.Assign) {
			return infer((JilStmt.Assign)stmt,myNode);					
		} else if(stmt instanceof JilExpr.Invoke) {
			return infer((JilExpr.Invoke)stmt,myNode);										
		} else if(stmt instanceof JilExpr.New) {
			return infer((JilExpr.New) stmt,myNode);						
		} else if(stmt instanceof JilStmt.Return) {
			return infer((JilStmt.Return) stmt,myNode);
		} else if(stmt instanceof JilStmt.Throw) {
			return infer((JilStmt.Throw) stmt,myNode);
		} else if(stmt instanceof JilStmt.Nop) {		
			return PURE;
		} else if(stmt instanceof JilStmt.Label) {		
			return PURE;
		} else if(stmt instanceof JilStmt.Goto) {		
			return PURE;
		} else if(stmt instanceof JilStmt.Lock) {		
			return infer((JilStmt.Lock) stmt, myNode);
		} else if(stmt instanceof JilStmt.Unlock) {		
			return infer((JilStmt.Unlock) stmt, myNode);
		} else {
			syntax_error("unknown statement encountered (" + stmt.getClass().getName() + ")",stmt);
			return IMPURE; // unknown
		}		
	}
	
	protected int infer(JilStmt.IfGoto stmt, Tag.Method myNode) {		
		return infer(stmt.condition(),myNode);			
	}
	
	protected int infer(JilStmt.Switch stmt, Tag.Method myNode) {		
		return infer(stmt.condition(),myNode);			
	}
	
	protected int infer(JilStmt.Assign stmt, Tag.Method myNode) {
		JilExpr lhs = stmt.lhs();
		
		int maxLevel = Math.max(infer(lhs, myNode),infer(stmt.rhs(), myNode));;
		
		if(lhs instanceof JilExpr.Deref) {
			JilExpr.Deref df = (JilExpr.Deref) lhs;			
			return IMPURE;			
		} else if(lhs instanceof JilExpr.ArrayIndex) {
			return IMPURE;			
		} else {		
			return maxLevel;
		}
	}
	
	protected int infer(JilStmt.Return stmt, Tag.Method myNode) {
		if(stmt.expr() != null) {
			return infer(stmt.expr(),myNode);
		}	
		return PURE;
	}
	
	protected int infer(JilStmt.Throw stmt, Tag.Method myNode) {		
		return infer(stmt.expr(),myNode);			
	}
	
	protected int infer(JilStmt.Lock stmt, Tag.Method myNode) {		
		return infer(stmt.expr(),myNode);			
	}
	
	protected int infer(JilStmt.Unlock stmt, Tag.Method myNode) {		
		return infer(stmt.expr(),myNode);			
	}
	
	protected int infer(JilExpr expr, Tag.Method myNode) {
		if(expr instanceof JilExpr.ArrayIndex) {
			return infer((JilExpr.ArrayIndex) expr, myNode);
		} else if(expr instanceof JilExpr.BinOp) {		
			return infer((JilExpr.BinOp) expr, myNode);
		} else if(expr instanceof JilExpr.UnOp) {		
			return infer((JilExpr.UnOp) expr, myNode);								
		} else if(expr instanceof JilExpr.Cast) {
			return infer((JilExpr.Cast) expr, myNode);			 			
		}  else if(expr instanceof JilExpr.Convert) {
			return infer((JilExpr.Convert) expr, myNode);			 			
		} else if(expr instanceof JilExpr.ClassVariable) {
			return infer((JilExpr.ClassVariable) expr, myNode);			 			
		} else if(expr instanceof JilExpr.Deref) {
			return infer((JilExpr.Deref) expr, myNode);			 							
		} else if(expr instanceof JilExpr.Variable) {
			return infer((JilExpr.Variable) expr, myNode);
		} else if(expr instanceof JilExpr.InstanceOf) {
			return infer((JilExpr.InstanceOf) expr, myNode);
		} else if(expr instanceof JilExpr.Invoke) {
			return infer((JilExpr.Invoke) expr, myNode);
		} else if(expr instanceof JilExpr.New) {
			return infer((JilExpr.New) expr, myNode);
		} else if(expr instanceof JilExpr.Value) {
			return infer((JilExpr.Value) expr, myNode);
		} else {
			syntax_error("Unknown expression encountered", expr);
			return IMPURE; // unreachable
		}
	}
	
	public int infer(JilExpr.ArrayIndex expr, Tag.Method myNode) { 		
		return Math.max(infer(expr.target(), myNode),infer(expr.index(), myNode));
	}	
	
	public int infer(JilExpr.BinOp expr, Tag.Method myNode) {
		return Math.max(infer(expr.lhs(), myNode),infer(expr.rhs(), myNode));
	}
	public int infer(JilExpr.UnOp expr, Tag.Method myNode) { 		
		return infer(expr.expr(), myNode); 
	}
	public int infer(JilExpr.Cast expr, Tag.Method myNode) { 
		return infer(expr.expr(), myNode);		
	}
	public int infer(JilExpr.Convert expr, Tag.Method myNode) { 
		return infer(expr.expr(), myNode);		
	}
	public int infer(JilExpr.ClassVariable expr, Tag.Method myNode) { 		
		// do nothing!
		return PURE;
	}
	public int infer(JilExpr.Deref expr, Tag.Method myNode) { 		
		return infer(expr.target(), myNode);						
	}	
	public int infer(JilExpr.Variable expr, Tag.Method myNode) { 
		// do nothing!
		return PURE;
	}
	public int infer(JilExpr.InstanceOf expr, Tag.Method myNode) { 		
		return infer(expr.lhs(), myNode);
	}
	public int infer(JilExpr.Invoke expr, Tag.Method myNode) { 				
		JilExpr target = expr.target();
		
		int rval = infer(target, myNode);
		for(JilExpr e : expr.parameters()) {
			rval = Math.max(infer(e, myNode),rval);
		}		
		
		try {			
			Tag.Method targetNode = Tag.create((Type.Reference) target.type(),
					expr.name(), expr.funType(), loader);

			if (!domain.contains(targetNode)) {
				Pair<Clazz, Clazz.Method> rt = loader.determineMethod(
						(Type.Reference) target.type(), expr.name(), expr
								.funType());
				if(hasAnnotation(rt.second(),"Pure")) {
					return Math.max(rval,PURE);
				} else {
					return IMPURE;
				}
			} else if (impureMethods.contains(targetNode)) {
				return IMPURE;
			} else {
				return rval;
			}
		} catch(MethodNotFoundException mnfe) {
			System.out.println("*** MY NODE: " + myNode);
			internal_error(expr,mnfe);
			return IMPURE; // unreachable code!
		} catch(ClassNotFoundException cnfe) {
			System.out.println("*** MY NODE: " + myNode);
			internal_error(expr,cnfe);
			return IMPURE; // unreachable code!
		}
	}

	public int infer(JilExpr.New expr, Tag.Method myNode) { 		
		int r = PURE;
		for(JilExpr e : expr.parameters()) {
			r = Math.max(r,infer(e, myNode));
		}		
						
		Type.Reference tr = expr.type();
		if(!(tr instanceof Type.Clazz)) {
			return PURE;
		}
		
		try {
			Type.Clazz owner = (Type.Clazz) tr;
			String name = owner.lastComponent().first(); 

			Tag.Method targetNode = Tag.create(owner,
					owner.lastComponent().first(), expr.funType(), loader);

			if (!domain.contains(targetNode)) {
				Pair<Clazz, Clazz.Method> rt = loader.determineMethod(
						owner, name, expr
						.funType());
				if((hasAnnotation(rt.second(),"Pure"))) {
					return PURE;
				} else {
					return IMPURE;
				}
			} else if (impureMethods.contains(targetNode)) {
				return IMPURE;
			} 
		} catch(MethodNotFoundException mnfe) {
			System.out.println("*** MY NODE: " + myNode);
			internal_error(expr,mnfe);
			return IMPURE; // unreachable code!
		} catch(ClassNotFoundException cnfe) {
			System.out.println("*** MY NODE: " + myNode);
			internal_error(expr,cnfe);
			return IMPURE; // unreachable code!
		}
		
		return r;
	}
	
	public int infer(JilExpr.Value expr, Tag.Method myNode) { 		
		if(expr instanceof JilExpr.Array) {
			JilExpr.Array ae = (JilExpr.Array) expr;
			int r = PURE;
			for(JilExpr v : ae.values()) {
				r = Math.max(r,infer(v, myNode));
			}
			return r;
		}
		return PURE;
	}		
	
	
	public static boolean hasAnnotation(Clazz.Method method, String annotation) {
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
		return false;
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
}

