package de.modelrepository.test.testcases;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

import de.modelrepository.test.FileVersionList;
import de.modelrepository.test.git.GitFileHistory;
import de.modelrepository.test.util.VersionObject;

public class T0005_Epatches {
	@Test
	public void test01() {
		try {
			Repository repo = new Repository(new File("res/in/T0005/.git"));
			File testFile = new File("res/in/T0005/Graph/src/de/asv/graph/Graph.java");
			GitFileHistory fh = new GitFileHistory(testFile, repo);
			FileVersionList list = new FileVersionList(fh);
			for (VersionObject vo : list) {
				System.out.println(vo.getCommitTime());
				System.out.println(vo.getBranch());
				System.out.println(vo.getContent());
				System.out.println(vo.getPatches());
				System.out.println();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
