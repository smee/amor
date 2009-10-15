package de.modelrepository.test.testcases;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.URIish;
import org.junit.After;
import org.junit.Test;

import de.modelrepository.test.git.GitCloneOperation;

public class T0003_GitClone {
	GitCloneOperation cloneOperation;
	
	@After
	public void after() {
		cloneOperation = null;
	}
	
	@Test
	public void test01() {
		try {
			URIish uri = new URIish("git://github.com/voldemort/voldemort.git");
			File workDir = new File("res/out/T0003/01/voldemort");
			String branch = Constants.R_HEADS + Constants.MASTER;
			String remoteName = "origin";
			
			cloneOperation = new GitCloneOperation(uri, workDir, branch, remoteName);
			cloneOperation.cloneRepository();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
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
}
