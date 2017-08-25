//This file is part of the Java Compiler Kit (JKit)

//The Java Compiler Kit is free software; you can 
//redistribute it and/or modify it under the terms of the 
//GNU General Public License as published by the Free Software 
//Foundation; either version 2 of the License, or (at your 
//option) any later version.

//The Java Compiler Kit is distributed in the hope
//that it will be useful, but WITHOUT ANY WARRANTY; without 
//even the implied warranty of MERCHANTABILITY or FITNESS FOR 
//A PARTICULAR PURPOSE.  See the GNU General Public License 
//for more details.

//You should have received a copy of the GNU General Public 
//License along with the Java Compiler Kit; if not, 
//write to the Free Software Foundation, Inc., 59 Temple Place, 
//Suite 330, Boston, MA  02111-1307  USA

//(C) David James Pearce, 2009. 

package jpure;

import java.io.*;
import java.util.*;

import jkit.util.*;
import jkit.compiler.SyntacticElement;
import jkit.compiler.ClassLoader;
import jkit.compiler.SyntaxError;
import jkit.java.*;
import jkit.java.stages.TypeSystem;
import jkit.jil.tree.*;
import jkit.util.graph.*;
import jkit.jil.util.*;
import jkit.jil.ipa.StaticDependenceGraph;
import jkit.jil.ipa.StaticDependenceGraph.*;
import jpure.annotations.Pure;

import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;


/**
 * The main class provides the entry point for the JKit compiler. It is
 * responsible for parsing command-line parameters, configuring and executing
 * the pipeline and determining the name of the output file.
 * 
 * @author djp
 * 
 */
public class Main {

	public static final int MAJOR_VERSION = 0;
	public static final int MINOR_VERSION = 2;
	public static final int MINOR_REVISION = 0;
			
	/**
	 * Main method provides command-line processing capability.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		String[] a = {
				"-cp", "C:\\Users\\Sandro\\Downloads\\Junk\\jpure",
				"C:\\Users\\Sandro\\Downloads\\Junk\\jpure\\Main.java",
				"C:\\Users\\Sandro\\Downloads\\Junk\\jpure\\Product.java"
		};

		if(!new Main().compile(a)) {
			System.exit(1);
		} else {
			System.exit(0);
		}
	}		
	
	public boolean compile(String[] args) {						
		ArrayList<String> classPath = null;
		ArrayList<String> bootClassPath = null;
		ArrayList<String> sourcePath = null;
		ArrayList<Pair<Type.Clazz,String>> watchlist = new ArrayList();
		String outputDirectory = null;		
		boolean verbose = false;	
		boolean base = false;
		
		if (args.length == 0) {
			// no command-line arguments provided
			usage();
			System.exit(0);
		}
				
		// ======================================================
		// ======== First, parse command-line arguments ========
		// ======================================================

		int fileArgsBegin = 0;
		for (int i = 0; i != args.length; ++i) {
			if (args[i].startsWith("-")) {
				String arg = args[i];
				if(arg.equals("-help")) {
					usage();
					System.exit(0);
				} else if (arg.equals("-version")) {
					System.out.println("JPure (Java Purity Inference Tool) version " + MAJOR_VERSION + "."
							+ MINOR_VERSION + "." + MINOR_REVISION);
				} else if (arg.equals("-verbose")) {
					verbose = true;
				} else if (arg.equals("-cp") || arg.equals("-classpath")) {
					classPath = new ArrayList<String>();
					// split classpath along appropriate separator
					Collections.addAll(classPath, args[++i]
					                                   .split(File.pathSeparator));
				} else if (arg.equals("-sourcepath")) {
					sourcePath = new ArrayList<String>();
					// split classpath along appropriate separator
					Collections.addAll(sourcePath, args[++i]
					                                   .split(File.pathSeparator));
				} else if (arg.equals("-bootclasspath")) {
					bootClassPath = new ArrayList<String>();
					// split classpath along appropriate separator
					Collections.addAll(bootClassPath, args[++i]
					                                       .split(File.pathSeparator));
				} else if (arg.equals("-watch")) {
					String n = args[++i];
					int index = n.indexOf(":");
					if(index < 0) {
						System.err.println("invalid watch method: " + n);
						System.exit(1);
					}
					String name = n.substring(index+1,n.length());
					int dot = n.lastIndexOf('.');
					String pkg = dot == -1 ? "" : n.substring(0,dot);
					String cname = n.substring(dot+1,index);					
					watchlist.add(new Pair(new Type.Clazz(pkg,cname),name));					
				} else if (arg.equals("-d")) {
					outputDirectory = args[++i];					
				} else if (arg.equals("-base")) {
					base = true;
				} else {				
					throw new RuntimeException("Unknown option: " + args[i]);
				}

				fileArgsBegin = i + 1;
			}
		}

		PrintStream verbOutput = null;
		
		if(verbose) {
			verbOutput = System.err;
		}
		
		// ======================================================
		// =========== Second, setup classpath properly ==========
		// ======================================================

		if (classPath == null) {
			classPath = ClassLoader.buildClassPath();
		}
		if (bootClassPath == null) {
			bootClassPath = ClassLoader.buildBootClassPath();
		}
		if(sourcePath == null) {
			sourcePath = new ArrayList<String>();
		}

		classPath.addAll(bootClassPath);				
		
		try {
			JavaCompiler compiler = new JavaCompiler(sourcePath, classPath, verbOutput) {				
				public void writeOutputFile(String baseName, JilClass clazz, File rootdir)
						throws IOException {
					// don't do anything here
				}
				
				public void addBypassMethods() {
					// don't want these.
				}
			};
			
			
			HashMap<String,List<JilClass>> files = new HashMap();
			ArrayList<File> srcfiles = new ArrayList();
			for(int i=fileArgsBegin;i!=args.length;++i) {
				srcfiles.add(new File(args[i]));
			}

			List<JilClass> classes = compiler.compile(srcfiles);
			
			for(JilClass jc : classes) {
				String srcfile = jc.sourceFile();
				List<JilClass> cs = files.get(srcfile);
				if(cs == null) {
					cs = new ArrayList();
					files.put(srcfile,cs);
				}
				cs.add(jc);
			}				
			
			long time = System.currentTimeMillis();
			StaticDependenceGraph sdg = new StaticDependenceGraph(compiler.getClassLoader());									
			sdg.apply(classes);		
							
			Graph<Tag.Method,Invocation> cg = sdg.callGraph();
			Graph<Tag,FieldAccess> rfg = sdg.fieldReads();
			Graph<Tag,FieldAccess> wfg = sdg.fieldWrites();				
			System.out.println("Constructed static dependence graph ... ("
					+ cg.size() + " call edges, " + rfg.size()
					+ " read edges, " + wfg.size() + " write edges) ["
					+ (System.currentTimeMillis() - time) + "ms]");

			HashMap<String,List<Insert>> inserts = new HashMap();
			
			computeImportInserts(files.keySet(),inserts);
			
			time = System.currentTimeMillis();
			HashMap<String,InferenceStats> stats = doInference(classes,files,inserts,watchlist,sdg,compiler,base);
			
			System.out.println("Performed Inference ... " + (System.currentTimeMillis() - time) + "ms]");
			
			printStats(stats);
			
			boolean errorVal = true;
			
			if(outputDirectory != null) {
				File f = new File(outputDirectory);
				if(!f.exists()) {
					System.out.println("Output directory does not exist: " + outputDirectory);
					f.mkdirs();
					System.out.println("Created output directory: " + outputDirectory);
				}
			} else {
				System.out.println("No output directory given, renaming output files to avoid collision.");
			}
									
			int nfailures = writeOutputFiles(files.keySet(),outputDirectory,inserts);
			
			if(nfailures > 0) {
				System.out.println("There were " + nfailures + " insert failures.");
			}
						
			return errorVal;
			
		} catch (SyntaxError e) {			
			if(e.fileName() != null) {
				jkit.JKitC.outputSourceError(e.fileName(), e.line(), e.column(), e.width(), e
						.getMessage());
			} else {
				System.err.println("Error: " + e.getMessage());
			}
			if (verbose) {
				e.printStackTrace(System.err);
			}
			return false;
		} catch(Exception e) {
			System.err.println("Error: " + e.getMessage());
			if (verbose) {
				e.printStackTrace(System.err);
			}
			return false;
		}		
	}
	
	/**
	 * Print out information regarding command-line arguments
	 * 
	 */
	public void usage() {
		String[][] info = {
				{"version", "Print version information"},
				{"verbose",
				"Print information about what the compiler is doing"},
				{"classpath <path>", "Specific where to find user class files"},
				{"cp <path>", "Specific where to find user class files"},
				{"bootclasspath <path>",
				"Specific where to find system class files"},
				{"jil","output jil intermediate representation"},
				{"bytecode","output bytecode in textual format"}};
		System.out.println("Usage: jkit <options> <source-files>");
		System.out.println("Options:");

		// first, work out gap information
		int gap = 0;

		for (String[] p : info) {
			gap = Math.max(gap, p[0].length() + 5);
		}

		// now, print the information
		for (String[] p : info) {
			System.out.print("  -" + p[0]);
			int rest = gap - p[0].length();
			for (int i = 0; i != rest; ++i) {
				System.out.print(" ");
			}
			System.out.println(p[1]);
		}
	}	
	
	public void computeImportInserts(Set<String> srcfiles,
			HashMap<String, List<Insert>> inserts) throws IOException {
		for(String filename : srcfiles) { 
			int line = findPackageDecl(filename);
			List<Insert> is = inserts.get(filename);
			if(is == null) {
				is = new ArrayList<Insert>();
				inserts.put(filename,is);
			}
			is.add(new Insert("import jpure.annotations.*;\n",new SourceLocation(line,0)));
		}
	}

	public int findPackageDecl(String filename) throws IOException {
		LineNumberReader in = new LineNumberReader(new FileReader(filename));		
		String line = "";
		while (line != null) {
			line = in.readLine();
			if(line == null) { break; }
			int i = 0;
			while(i < line.length()) {
				if(!Character.isWhitespace(line.charAt(i))) {
					break;
				}
				i = i + 1;
			}
			if(line.startsWith("package",i)) {				
				in.close();
				return in.getLineNumber() + 1;
			}
		}
		in.close();
		return 1; // there is no package declaration
	}
	
	private final static class InferenceStats {
		int nmethods; // total number of non-synthetic methods
		int nlocals; // number methods with one or more local parameters
		int npures; // number methods which are pure (either marked pure, or
					// marked fresh with nothing else)
		int nfresh; // number methods marked fresh
		int nprimret; // number methods primitive return
		int nprimparam; // number  methods only primitive parameters
		
		public void add(InferenceStats stats) {
			nmethods += stats.nmethods;
			nlocals += stats.nlocals;
			npures += stats.npures;
			nfresh += stats.nfresh;
			nprimret += stats.nprimret;
			nprimparam += stats.nprimparam;
		}
	}
	
	public static void printStats(HashMap<String,InferenceStats> stats) {
		System.out.println("pkg\t#Methods\t#Pure\t#Local\t#Fresh");
		InferenceStats total = new InferenceStats();
		for(Map.Entry<String,InferenceStats> e : stats.entrySet()) {
			InferenceStats s = e.getValue();
			printStatLine(e.getKey(),s);
			total.add(s);
		}
		System.out.println("==================================================");
		printStatLine("Total",total);		
	}
	
	public static void printStatLine(String label, InferenceStats s) {
		double purep = (s.npures*100.0) / s.nmethods;
		double localp = (s.nlocals * 100.0) / (s.nmethods - s.nprimparam);
		double freshp = (s.nfresh * 100.0) / (s.nmethods - s.nprimret);
		System.out.print(label + " & " + s.nmethods + " & ");
		System.out.print(s.npures + " & (" + round(purep) + "\\%) & ");
		System.out.print(s.nlocals + " / " + (s.nmethods - s.nprimparam) + " & (" + round(localp) + "\\%) & ");
		System.out.print(s.nfresh + " / " + (s.nmethods - s.nprimret) + " & (" + round(freshp) + "\\%) & ");
		System.out.println();
	}
	
	public static double round(double d) {
		int i = (int) (d * 10);
		d = i;
		return d / 10;
	}
	
	public HashMap<String,InferenceStats> doInference(
			List<JilClass> classes,
			HashMap<String, List<JilClass>> files,
			HashMap<String, List<Insert>> inserts,
			List<Pair<Type.Clazz,String>> watchlist,
			StaticDependenceGraph sdg,
			JavaCompiler compiler, boolean base) throws ClassNotFoundException {
		
		HashMap<String,InferenceStats> stats = new HashMap<String,InferenceStats>();
		ClassLoader loader = compiler.getClassLoader();
		
		PurityAnalysis pi;
		
		if(base) {
			pi = new BaseAnalysis(sdg.callGraph(),new TypeSystem(), loader);
		} else {
			pi = new ExtendedAnalysis(sdg.callGraph(),new TypeSystem(), loader);
		}
		
		for(Pair<Type.Clazz,String> w : watchlist) {
			pi.addWatchMethod(w.first(),w.second());
		}
		
		pi.apply(classes);
		
		for (Map.Entry<String, List<JilClass>> file : files.entrySet()) {
			List<JilClass> jilclasses = file.getValue();

			for (JilClass jclass : jilclasses) {
				InferenceStats p = computeInserts(file.getKey(), jclass, inserts, pi);
				addStats(jclass.type().pkg(),p,stats);				
			}
		}
		
		return stats;
	}
	
	public static void addStats(String pkg, InferenceStats ns, HashMap<String,InferenceStats> stats) {
		InferenceStats s = stats.get(pkg);
		if(s == null) {
			stats.put(pkg,ns);			
		} else {
			s.add(ns);
		}
	}
	
	public static InferenceStats computeInserts(String filename, JilClass jclass,
			HashMap<String, List<Insert>> insertmap, PurityAnalysis pi) {
		
		List<Insert> inserts = insertmap.get(filename);
		if(inserts == null) {
			inserts = new ArrayList<Insert>();
			insertmap.put(filename,inserts);
		}

		InferenceStats stats = new InferenceStats();
		
		for (JilMethod m : jclass.methods()) {
			Tag.Method node = Tag.create(jclass,m.name(),m.type());
			
			boolean isConstructor = node.name().equals(jclass.name());
			
			if(!m.isSynthetic()) {
				Set<Label> labels = pi.methodLabels(node);								
							
				stats.nmethods ++;
				
				if(!(m.type().returnType() instanceof Type.Reference)) {
					stats.nprimret++;
					labels.remove(Label.FRESH);
				}
				
				boolean hasRefParam=false;
				for(Type t : m.type().parameterTypes()) {
					if(t instanceof Type.Reference) {
						hasRefParam=true;
					}
				}
				
				if(!hasRefParam) {
					stats.nprimparam++;
				}
				
				// First, do receiver labels
				if(labels.contains(Label.IMPURE)) {					
					continue; // nothing doing here
				}
								
				boolean hasFresh = false;
				boolean hasLocalParam=false;
				boolean hasPure = false;
				if (labels.contains(Label.FRESH)) {
						addAnnotation(m, jclass, "Fresh", inserts);
						hasFresh = true;
				} else if (labels.size() == 0
						|| (isConstructor && labels.size() == 1 && labels
								.contains(Label.RECEIVER))) {
					addAnnotation(m,jclass,"Pure",inserts);
					hasPure=true;
				} else if (labels.contains(Label.RECEIVER)){
					hasLocalParam = true;
					addAnnotation(m,jclass,"Local",inserts);	
				}
				
				// Second, do parameters
				int idx=0;
				
				for(JilMethod.JilParameter p : m.parameters()) {
					if(labels.contains(Label.parameters[idx])) {
						hasLocalParam = true;
						addAnnotation(p,jclass,"Local",inserts);
					}
					idx = idx + 1;
				}
				
				if(hasLocalParam) {
					stats.nlocals ++;
				}
				
				if(hasFresh) {
					stats.nfresh++;
				}
				
				if(hasPure || (hasFresh && !hasLocalParam)) {
					stats.npures++;
				}								
			}
		}				
		
		for(JilField f : jclass.fields()) {
			Tag.Field node = Tag.create(jclass,f.name());
			
			if (!f.isStatic() && f.type() instanceof Type.Reference
					&& pi.isFieldLocal(node)) {
				addAnnotation(f, jclass, "Local", inserts);
			}
		}
		
		return stats;
	}
	
	public static void addAnnotation(SyntacticElement m, JilClass jclass, String annotation, List<Insert> inserts) {
		// Second, try and compute the insert
		SourceLocation loc = m.attribute(SourceLocation.class);
		String name = null;
		if(m instanceof JilMethod) {
			name = ((JilMethod)m).name();
		} else if(m instanceof JilField) {
			name = ((JilField)m).name();
		}
		if(loc == null) {
			System.err
			.println("*** WARNING: unable to apply patch --- missing line number info: "
					+ jclass.type()
					+ "."
					+ name
			);
		} else if(loc.line() < 1) {
			System.err
			.println("*** WARNING: unable to apply patch --- invalid line number info (" + loc  + "): "
					+ jclass.type()
					+ "."
					+ name
			);
		} else {			
			inserts.add(new Insert("@" + annotation + " ",  loc));			
		}						
	}

	public static class Insert implements Comparable<Insert> {
		public SourceLocation loc;
		public String text;
		
		public Insert(String text, SourceLocation loc) {
			this.text = text;
			this.loc = loc;
		}
		
		public int compareTo(Insert i) {
			if(loc.line() < i.loc.line()) {
				return -1;
			} else if(loc.line() > i.loc.line()) {
				return 1;
			} else if(loc.column() < i.loc.column()){
				return -1;
			} else if(loc.column() > i.loc.column()){
				return 1;
			}
			return 0;
		}
		
		public String toString() {
			return text + ":" + loc;
		}
	}
	
	public static int writeOutputFiles(Set<String> srcfiles,
			String outputDir, HashMap<String,List<Insert>> insertmap) throws IOException {
		int nfailures = 0;
		for(String filename : srcfiles) {			
			List<Insert> inserts = insertmap.get(filename);			
			nfailures += writeOutputFile(filename,outputDir,inserts);
		}
		return nfailures;
	}
	
	public static int writeOutputFile(String filename, String outputDir, List<Insert> inserts) throws FileNotFoundException,IOException {
		int nfailures = 0;
		Collections.sort(inserts);
		String outname = filename;
		
		if(outputDir == null) {			
			outname = filename.substring(0,filename.length() - 5); // strip .java
			outname = outname + ".jout";
		} else {
			outname = outputDir + File.separatorChar + outname;
		}
		
		File pf = new File(outname).getParentFile();
		if(pf != null) {
			pf.mkdirs();
		}
		
		System.out.println("Writing: " + outname + " ... (" + inserts.size() + " inserts)");				
		LineNumberReader in = new LineNumberReader(new FileReader(filename));
		Writer out = new FileWriter(outname);
		String line = "";
		
		while(inserts.get(0).loc.line() < 1) {
			System.out.println("*** WARNING: failed writing insert in " + outname
					+ " (" + inserts.get(0) + ")");
			inserts.remove(0);
			nfailures++;
		}
		
		while (line != null) {
			line = in.readLine();
			int lineno = in.getLineNumber();	
			
			int offset = 0;			
			while(inserts.size() > 0 && inserts.get(0).loc.line() == lineno) {				
				Insert i = inserts.get(0);		
				int ipos = i.loc.column() + offset;
				String before = line.substring(0,ipos);
				String after = line.substring(ipos);
				line = before + i.text + after;
				offset = offset + i.text.length();
				inserts.remove(0);
			}
			
			if(line != null) {
				out.write(line + "\n");
			}
		}		
		in.close();
		out.close();
		return nfailures;
	}	
}
