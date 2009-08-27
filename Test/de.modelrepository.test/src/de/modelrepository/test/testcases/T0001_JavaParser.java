package de.modelrepository.test.testcases;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Vector;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.modelrepository.test.JavaToEMFParser;
import de.modelrepository.test.ProxyException;

public class T0001_JavaParser {
	JavaToEMFParser parser;
	
	@BeforeClass
	public static void setUp() {
//		clear output folder before running the tests
		File outFolder = new File("res/out");
		File[] files = outFolder.listFiles();
		if(files.length > 0) {
			for (File file : files) {
				del(file);
			}
		}
	}
	
	/*
	 * deletes the given file or folder by recursively clearing all subfolders
	 */
	public static boolean del(File dir){
		if(dir.isDirectory()) {
			for(File aktFile : dir.listFiles()) {
				del(aktFile);
			}
			if(dir.delete()) return true;
			else return false;
		}else {
			if(dir.delete()) return true;
			else return false;
		}
	}
	
	@Before
	public void before() {
		parser = new JavaToEMFParser();
	}
	
	@After
	public void after() {
		parser = null;
	}
	
	@Test
	/*
	 * Tests the parsing of a simple java class (Hello.java).
	 * This class does not contain any proxies except a native java one (System class).
	 */
	public void test01() {
		String fileName = "Hello.java"; 
		File inFile = new File("res/in/T0001/" + fileName);
		assertTrue("The file \"res/in/T0001/" + fileName + "\" which shall be parsed does not exist.", inFile.exists());
		
		try {
			ResourceSet result = parser.parseJavaFile(inFile, null);
			assertEquals("The file \"" + fileName + "\" is not contained in the ResourceSet.", fileName, result.getResources().get(0).getURI().lastSegment());
		} catch (ProxyException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	/*
	 * Tests the parsing of a simple java class (Hello.java) including the serialization of the resulting EMF model and all referenced models.
	 * This class does not contain any proxies except a native java one (System class).
	 */
	public void test02() {
		String fileName = "Hello.java"; 
		File inFile = new File("res/in/T0001/" + fileName);
		File outFolder = new File("res/out/T0001/02");
		assertTrue("The file \"res/in/T0001/" + fileName + "\" which shall be parsed does not exist.", inFile.exists());
		
		try {
			parser.parseAndSerializeJavaFile(inFile, null, outFolder);
			assertTrue("The parsed java file (" + fileName + ") wasn't serialized correctly.", new File(outFolder, fileName + ".xmi").exists());
			assertTrue("The Java metamodel (java.ecore) wasn't serialized correctly.", new File(outFolder, "java.ecore").exists());
			assertTrue("The Primitive Types metamodel (primitive_types.ecore) wasn't serialized correctly.", new File(outFolder, "primitive_types.ecore").exists());
		} catch (ProxyException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	/*
	 * Tests the parsing of java class (ProxyTest.java) which contains references to other classes which are not contained in Java 5.
	 * The neccesary libraries are added as jar files to the library-vector.
	 */
	public void test03() {
		String fileName = "ProxyTest.java";
		File inFile = new File("res/in/T0001/" + fileName);
		assertTrue("The file \"res/in/T0001/" + fileName + "\" which shall be parsed does not exist.", inFile.exists());
		
		Vector<File> libs = new Vector<File>();
		libs.add(new File("res/in/T0001/lib/org.eclipse.emf.ecore_2.5.0.v200906151043.jar"));
		libs.add(new File("res/in/T0001/lib/org.eclipse.emf.common_2.5.0.v200906151043.jar"));
		
		try {
			ResourceSet result = parser.parseJavaFile(inFile, libs);
			assertEquals("The file \"" + fileName + "\" is not contained in the ResourceSet.", fileName, result.getResources().get(0).getURI().lastSegment());
		} catch (ProxyException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	/*
	 * Tests the parsing of java class (ProxyTest.java) which contains references to other classes which are not contained in Java 5.
	 * The neccesary libraries are added to the library-vector by defining the folder where they lie in.
	 */
	public void test04() {
		String fileName = "ProxyTest.java";
		File inFile = new File("res/in/T0001/" + fileName);
		assertTrue("The file \"res/in/T0001/" + fileName + "\" which shall be parsed does not exist.", inFile.exists());
		
		Vector<File> libs = new Vector<File>();
		libs.add(new File("res/in/T0001/lib"));
		
		try {
			ResourceSet result = parser.parseJavaFile(inFile, libs);
			assertEquals("The file \"" + fileName + "\" is not contained in the ResourceSet.", fileName, result.getResources().get(0).getURI().lastSegment());
		} catch (ProxyException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	/*
	 * Tests the parsing of java class (ProxyTest.java) which contains references to other classes which are not contained in Java 5.
	 * It also test the serialization of the parsed file.
	 * The neccesary libraries are added as jar files to the library-vector.
	 */
	public void test05() {
		String fileName = "ProxyTest.java";
		File inFile = new File("res/in/T0001/" + fileName);
		File outFolder = new File("res/out/T0001/05");
		assertTrue("The file \"res/in/T0001/" + fileName + "\" which shall be parsed does not exist.", inFile.exists());
		
		Vector<File> libs = new Vector<File>();
		libs.add(new File("res/in/T0001/lib/org.eclipse.emf.ecore_2.5.0.v200906151043.jar"));
		libs.add(new File("res/in/T0001/lib/org.eclipse.emf.common_2.5.0.v200906151043.jar"));
		
		try {
			parser.parseAndSerializeJavaFile(inFile, libs, outFolder);
			assertTrue("The parsed java file (" + fileName + ") wasn't serialized correctly.", new File(outFolder, fileName + ".xmi").exists());
			assertTrue("The Java metamodel (java.ecore) wasn't serialized correctly.", new File(outFolder, "java.ecore").exists());
			assertTrue("The Primitive Types metamodel (primitive_types.ecore) wasn't serialized correctly.", new File(outFolder, "primitive_types.ecore").exists());
			//TODO Test der Lib-files (ob serialisiert)
		} catch (ProxyException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	/*
	 * Tests the parsing of java class (ProxyTest.java) which contains references to other classes which are not contained in Java 5.
	 * It also test the serialization of the parsed file.
	 * The neccesary libraries are added to the library-vector by defining the folder where they lie in.
	 */
	public void test06() {
		String fileName = "ProxyTest.java";
		File inFile = new File("res/in/T0001/" + fileName);
		File outFolder = new File("res/out/T0001/05");
		assertTrue("The file \"res/in/T0001/" + fileName + "\" which shall be parsed does not exist.", inFile.exists());
		
		Vector<File> libs = new Vector<File>();
		libs.add(new File("res/in/T0001/lib"));
		
		try {
			parser.parseAndSerializeJavaFile(inFile, libs, outFolder);
			assertTrue("The parsed java file (" + fileName + ") wasn't serialized correctly.", new File(outFolder, fileName + ".xmi").exists());
			assertTrue("The Java metamodel (java.ecore) wasn't serialized correctly.", new File(outFolder, "java.ecore").exists());
			assertTrue("The Primitive Types metamodel (primitive_types.ecore) wasn't serialized correctly.", new File(outFolder, "primitive_types.ecore").exists());
			//TODO Test der Lib-files (ob serialisiert)
		} catch (ProxyException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	/*
	 * Tests the parsing of multiple java classes contained in a folder (res/in/T0001).
	 * The neccesary libraries are added as jar files to the library-vector.
	 */
	public void test11() {
		File inFolder = new File("res/in/T0001/");
		Vector<File> libs = new Vector<File>();
		libs.add(new File("res/in/T0001/lib/org.eclipse.emf.ecore_2.5.0.v200906151043.jar"));
		libs.add(new File("res/in/T0001/lib/org.eclipse.emf.common_2.5.0.v200906151043.jar"));
		try {
			ResourceSet result = parser.parseAllJavaFiles(inFolder, libs);
			assertEquals("The file \"Hello.java\" is not contained in the ResourceSet.", "Hello.java", result.getResources().get(0).getURI().lastSegment());
			assertEquals("The file \"ProxyTest.java\" is not contained in the ResourceSet.", "ProxyTest.java", result.getResources().get(2).getURI().lastSegment());
		} catch (ProxyException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	/*
	 * Tests the parsing of multiple java classes contained in a folder (res/in/T0001).
	 * It also test the serialization of the parsed files.
	 * The neccesary libraries are added as jar files to the library-vector.
	 */
	public void test12() {
		File inFolder = new File("res/in/T0001/");
		File outFolder = new File("res/out/T0001/12");
		Vector<File> libs = new Vector<File>();
		libs.add(new File("res/in/T0001/lib/org.eclipse.emf.ecore_2.5.0.v200906151043.jar"));
		libs.add(new File("res/in/T0001/lib/org.eclipse.emf.common_2.5.0.v200906151043.jar"));
		try {
			parser.parseAndSerializeAllJavaFiles(inFolder, libs, outFolder);
			assertTrue("The parsed java file (Hello.java) wasn't serialized correctly.", new File(outFolder, "Hello.java.xmi").exists());
			assertTrue("The parsed java file (ProxyTest.java) wasn't serialized correctly.", new File(outFolder, "ProxyTest.java.xmi").exists());
			assertTrue("The Java metamodel (java.ecore) wasn't serialized correctly.", new File(outFolder, "java.ecore").exists());
			assertTrue("The Primitive Types metamodel (primitive_types.ecore) wasn't serialized correctly.", new File(outFolder, "primitive_types.ecore").exists());
		} catch (ProxyException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	/*
	 * Tests the parsing of multiple java classes contained in a folder (res/in/T0001).
	 * The neccesary libraries are added to the library-vector by defining the folder where they lie in.
	 */
	public void test13() {
		File inFolder = new File("res/in/T0001/");
		Vector<File> libs = new Vector<File>();
		libs.add(new File("res/in/T0001/lib"));
		try {
			ResourceSet result = parser.parseAllJavaFiles(inFolder, libs);
			assertEquals("The file \"Hello.java\" is not contained in the ResourceSet.", "Hello.java", result.getResources().get(0).getURI().lastSegment());
			assertEquals("The file \"ProxyTest.java\" is not contained in the ResourceSet.", "ProxyTest.java", result.getResources().get(2).getURI().lastSegment());
		} catch (ProxyException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	/*
	 * Tests the parsing of multiple java classes contained in a folder (res/in/T0001).
	 * It also test the serialization of the parsed file.
	 * The neccesary libraries are added to the library-vector by defining the folder where they lie in.
	 */
	public void test14() {
		File inFolder = new File("res/in/T0001/");
		File outFolder = new File("res/out/T0001/14");
		Vector<File> libs = new Vector<File>();
		libs.add(new File("res/in/T0001/lib"));
		try {
			parser.parseAndSerializeAllJavaFiles(inFolder, libs, outFolder);
			assertTrue("The parsed java file (Hello.java) wasn't serialized correctly.", new File(outFolder, "Hello.java.xmi").exists());
			assertTrue("The parsed java file (ProxyTest.java) wasn't serialized correctly.", new File(outFolder, "ProxyTest.java.xmi").exists());
			assertTrue("The Java metamodel (java.ecore) wasn't serialized correctly.", new File(outFolder, "java.ecore").exists());
			assertTrue("The Primitive Types metamodel (primitive_types.ecore) wasn't serialized correctly.", new File(outFolder, "primitive_types.ecore").exists());
		} catch (ProxyException e) {
			e.printStackTrace();
		}
	}
}
