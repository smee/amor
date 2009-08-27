package de.modelrepository.test.testcases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.diff.metamodel.DiffElement;
import org.eclipse.emf.compare.diff.metamodel.DiffGroup;
import org.eclipse.emf.compare.diff.metamodel.DiffModel;
import org.eclipse.emf.compare.diff.metamodel.DifferenceKind;
import org.eclipse.emf.compare.diff.metamodel.UpdateAttribute;
import org.eclipse.emf.compare.util.ModelUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.modelrepository.test.JavaToEMFParser;
import de.modelrepository.test.ModelComparator;

public class T0002_ModelComparison {
	ModelComparator comparator;
	JavaToEMFParser parser;
	
	@Before
	public void before() {
		comparator = new ModelComparator();
		parser = new JavaToEMFParser();
	}
	
	@After
	public void after() {
		comparator = null;
		parser = null;
	}
	
	@Test
	/*
	 * Tests the comparison of 2 simple models given as xmi files.
	 * This test checks for number of changes and the associated difference-types.
	 */
	public void test01() {
		String f1Name = "Hello.java.xmi";
		String f2Name = "Hello_modified.java.xmi";
		File f1 = new File("res/in/T0002/" + f1Name);
		File f2 = new File("res/in/T0002/" + f2Name);
		assertTrue("The file \"res/in/T0002/" + f1Name + "\" which shall be parsed does not exist.", f1.exists());
		assertTrue("The file \"res/in/T0002/" + f2Name + "\" which shall be parsed does not exist.", f2.exists());
		
		try {
			DiffModel diff = comparator.compare(f1, f2);
			EList<DiffElement> differences = diff.getOwnedElements();
			DiffGroup g0 = (DiffGroup) differences.get(0);
			assertEquals("The number of changes differs from the expected one.", 2, g0.getSubchanges());
			Vector<DiffElement> changes = getLastObjects(g0);
			assertTrue("A wrong difference type was detected.", changes.get(0) instanceof UpdateAttribute && changes.get(0).getKind().equals(DifferenceKind.CHANGE));
			assertTrue("A wrong difference type was detected.", changes.get(1) instanceof UpdateAttribute && changes.get(0).getKind().equals(DifferenceKind.CHANGE));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	/*
	 * Tests the comparison of 2 simple models (as in-memory EObjects).
	 * This test checks for number of changes and the associated difference-types.
	 */
	public void test02() {
		ResourceSet set = new ResourceSetImpl();
		String f1Name = "Hello.java.xmi";
		String f2Name = "Hello_modified.java.xmi";
		File f1 = new File("res/in/T0002/" + f1Name);
		File f2 = new File("res/in/T0002/" + f2Name);
		assertTrue("The file \"res/in/T0002/" + f1Name + "\" which shall be parsed does not exist.", f1.exists());
		assertTrue("The file \"res/in/T0002/" + f2Name + "\" which shall be parsed does not exist.", f2.exists());
		
		try {
			EObject model1 = ModelUtils.load(f1, set);
			EObject model2 = ModelUtils.load(f2, set);
			DiffModel diff = comparator.compare(model1, model2);
			EList<DiffElement> differences = diff.getOwnedElements();
			DiffGroup g0 = (DiffGroup) differences.get(0);
			assertEquals("The number of changes differs from the expected one.", 2, g0.getSubchanges());
			Vector<DiffElement> changes = getLastObjects(g0);
			assertTrue("A wrong difference type was detected.", changes.get(0) instanceof UpdateAttribute && changes.get(0).getKind().equals(DifferenceKind.CHANGE));
			assertTrue("A wrong difference type was detected.", changes.get(1) instanceof UpdateAttribute && changes.get(0).getKind().equals(DifferenceKind.CHANGE));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Vector<DiffElement> getLastObjects(DiffGroup group) {
		Vector<DiffElement> result = new Vector<DiffElement>();
		for (DiffElement e : group.getSubDiffElements()) {
			if(e instanceof DiffGroup) {
				result.addAll(getLastObjects((DiffGroup) e));
			}else {
				result.add(e);
			}
		}
		return result;
	}
}
