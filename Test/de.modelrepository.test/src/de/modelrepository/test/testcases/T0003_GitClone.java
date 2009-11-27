package de.modelrepository.test.testcases;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.URIish;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

import de.modelrepository.test.git.GitCloneOperation;
import de.modelrepository.test.util.FileUtility;

public class T0003_GitClone {
	GitCloneOperation cloneOperation;
	
	@AfterClass
	public static void tearDown() {
		File repo = new File("res/out/T0003");
		if(repo.exists())
			FileUtility.delete(repo);
	}
	
	@After
	public void after() {
		cloneOperation = null;
	}
	
	@Test
	public void test01() {
		try {
//			URIish uri = new URIish("git://github.com/voldemort/voldemort.git");
//			File workDir = new File("res/out/T0003/01/voldemort");
//			URIish uri = new URIish("git://github.com/RJ/irccat.git");
//			File workDir = new File("res/out/T0003/02/irccat");
//			URIish uri = new URIish("git://github.com/richhickey/clojure.git");
//			File workDir = new File("res/out/T0003/03/clojure");
			URIish uri = new URIish(new File("res/in/T0003/.git").toURI().getPath());
			File workDir = new File("res/out/T0003");
			
			String branch = Constants.R_HEADS + Constants.MASTER;
			String remoteName = "origin";
			
			cloneOperation = new GitCloneOperation(uri, workDir, branch, remoteName);
			cloneOperation.cloneRepository();
			
			assertTrue("A file does not exist.", new File(workDir, "Graph/src/de/asv/graph/Graph.java").exists());
			assertTrue("A file does not exist.", new File(workDir, "Graph/src/de/asv/graph/Node.java").exists());
			assertTrue("A file does not exist.", new File(workDir, "Graph/src/de/asv/graph/Edge.java").exists());
			assertTrue("A file does not exist.", new File(workDir, "Graph/src/de/asv/graph/GraphItem.java").exists());
			assertTrue("A file does not exist.", new File(workDir, "Graph/src/de/asv/graph/Constants.java").exists());
			assertTrue("A file does not exist.", new File(workDir, "Graph/src/de/asv/graph/RedundandDataException.java").exists());
			assertTrue("A file does not exist.", new File(workDir, "Graph/src/de/asv/graph/RedundandItemException.java").exists());
			assertTrue("A file does not exist.", new File(workDir, "Graph/src/de/asv/graph/parser/GraphParser.java").exists());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
