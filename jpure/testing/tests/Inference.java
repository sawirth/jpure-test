// This file is part of the Java Compiler Kit (JKit)
//
// The Java Compiler Kit is free software; you can 
// redistribute it and/or modify it under the terms of the 
// GNU General Public License as published by the Free Software 
// Foundation; either version 2 of the License, or (at your 
// option) any later version.
//
// The Java Compiler Kit is distributed in the hope
// that it will be useful, but WITHOUT ANY WARRANTY; without 
// even the implied warranty of MERCHANTABILITY or FITNESS FOR 
// A PARTICULAR PURPOSE.  See the GNU General Public License 
// for more details.
//
// You should have received a copy of the GNU General Public 
// License along with the Java Compiler Kit; if not, 
// write to the Free Software Foundation, Inc., 59 Temple Place, 
// Suite 330, Boston, MA  02111-1307  USA
//
// (C) David James Pearce, 2009. 

package jpure.testing.tests;

import jpure.testing.TestHarness;

import org.junit.*;


public class Inference extends TestHarness {
	public Inference() {
		super("tests/src", "java", "tests/output", "jout");
	}
	
	@Test public void Constructor_1() { runTest("Constructor_1"); }
	@Test public void Constructor_2() { runTest("Constructor_2"); }
	@Test public void Constructor_3() { runTest("Constructor_3"); }
	@Test public void Constructor_4() { runTest("Constructor_4"); }
	@Test public void Constructor_5() { runTest("Constructor_5"); }
	@Test public void Constructor_6() { runTest("Constructor_6"); }
	@Test public void Constructor_7() { runTest("Constructor_7"); }
	@Test public void Constructor_8() { runTest("Constructor_8"); }
	@Test public void Constructor_9() { runTest("Constructor_9"); }	
	@Test public void Constructor_10() { runTest("Constructor_10"); }
	@Test public void Constructor_11() { runTest("Constructor_11"); }
	@Test public void Constructor_12() { runTest("Constructor_12"); }
	@Test public void Constructor_13() { runTest("Constructor_13"); }
	
	@Test public void FreshMethod_1() { runTest("FreshMethod_1"); }
	@Test public void FreshMethod_2() { runTest("FreshMethod_2"); }
	@Test public void FreshMethod_3() { runTest("FreshMethod_3"); }
	@Test public void FreshMethod_4() { runTest("FreshMethod_4"); }
	@Test public void FreshMethod_5() { runTest("FreshMethod_5"); }
	@Test public void FreshMethod_6() { runTest("FreshMethod_6"); }
	@Test public void FreshMethod_7() { runTest("FreshMethod_7"); }
	@Test public void FreshMethod_8() { runTest("FreshMethod_8"); }
	@Test public void FreshMethod_9() { runTest("FreshMethod_9"); }
	@Test public void FreshMethod_10() { runTest("FreshMethod_10"); }
	
	@Test public void LocalMethod_1() { runTest("LocalMethod_1"); }
	@Test public void LocalMethod_2() { runTest("LocalMethod_2"); }
	@Test public void LocalMethod_3() { runTest("LocalMethod_3"); }
	@Test public void LocalMethod_4() { runTest("LocalMethod_4"); }
	@Test public void LocalMethod_5() { runTest("LocalMethod_5"); }
	@Test public void LocalMethod_6() { runTest("LocalMethod_6"); }
	
	// Following caused by Iterator.next being non-local
	@Ignore("Known Bug") @Test public void PureMethod_1() { runTest("PureMethod_1"); }
		
	@Test public void FunCall_1() { runTest("FunCall_1"); }
	@Test public void FunCall_2() { runTest("FunCall_2"); }
	@Test public void FunCall_3() { runTest("FunCall_3"); }
	@Test public void FunCall_4() { runTest("FunCall_4"); }
	@Test public void FunCall_5() { runTest("FunCall_5"); }
	// Following caused by ArrayList.add being non-local
	@Ignore("Known Bug") @Test public void FunCall_6() { runTest("FunCall_6"); }
	// Following caused by Iterator.next being non-local
	@Ignore("Known Bug") @Test public void FunCall_7() { runTest("FunCall_7"); }
	@Test public void FunCall_8() { runTest("FunCall_8"); }
	// Following caused by StringBuilder.append being non-local
	@Ignore("Known Bug") @Test public void FunCall_9() { runTest("FunCall_9"); }
	@Test public void FunCall_10() { runTest("FunCall_10"); }
	// Following caused by List.add being non-local
	@Ignore("Known Bug") @Test public void FunCall_11() { runTest("FunCall_11"); }
	@Test public void FunCall_12() { runTest("FunCall_12"); }
	@Test public void FunCall_13() { runTest("FunCall_13"); }
	@Test public void FunCall_14() { runTest("FunCall_14"); }
	
	@Test public void FunCall_CheckFail_1() { runTest("FunCall_CheckFail_1"); }
	
	@Test public void Inheritance_1() { runTest("Inheritance_1"); }
	@Test public void Inheritance_2() { runTest("Inheritance_2"); }
	@Test public void Inheritance_3() { runTest("Inheritance_3"); }
	@Test public void Inheritance_4() { runTest("Inheritance_4"); }
	
	// Following caused by HashMap.clone being non-local
	@Ignore("Known Bug") @Test public void Inheritance_5() { runTest("Inheritance_5"); }
	@Test public void Inheritance_6() { runTest("Inheritance_6"); }	
	@Test public void Inheritance_7() { runTest("Inheritance_7"); }
	@Test public void Inheritance_8() { runTest("Inheritance_8"); }
	@Test public void Inheritance_9() { runTest("Inheritance_9"); }
	
	@Test public void Inner_1() { runTest("Inner_1"); }
	@Test public void Inner_2() { runTest("Inner_2"); }
	
	@Test public void Throw_1() { runTest("Throw_1"); }
	
	// other, larger + harder tests
	@Ignore("Known Bug") @Test public void Tree() { runTest("Tree"); }
	@Ignore("Known Bug") @Test public void Matrix() { runTest("Matrix"); }
	@Ignore("Known Bug") @Test public void IntVec() { runTest("IntVec"); }
	@Ignore("Known Bug") @Test public void BigRational() { runTest("BigRational"); }
}

