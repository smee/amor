package de.modelrepository.test.testcases;

import java.io.*;
import java.util.Properties;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.epatch.Epatch;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

import de.modelrepository.test.FileVersionList;
import de.modelrepository.test.git.GitFileHistory;
import de.modelrepository.test.util.VersionObject;

public class T0005_Epatches {
    @Test
    public void test01() throws IOException {
        Repository repo = new Repository(new File("res/in/T0005/commons-lang/.git"));
        String relativeSourcePath = "src/java/org/apache/commons/lang/SystemUtils.java";
        GitFileHistory fh = new GitFileHistory(relativeSourcePath, repo);
        FileVersionList list = new FileVersionList(fh);
        int counter = 0;
        for (VersionObject vo : list) {
            System.out.println(vo.getCommitTime());
            System.out.println(vo.getBranch());
            System.out.println(vo.getAuthor().getName());
            System.out.println(vo.getCommitMessage());
            System.out.println(vo.getPatches().size() + " epatches");
            counter = dumpEpatches(relativeSourcePath, vo, counter);
            System.out.println();
        }
    }

    /**
     * @param relativeSourcePath
     * @param vo
     * @throws IOException
     */
    private int dumpEpatches(String relativeSourcePath, VersionObject vo, int counter) throws IOException {
        ResourceSet rs = new ResourceSetImpl();
        assert vo.getPatches().size() <= 1;

        for (Epatch epatch : vo.getPatches().values()) {
            Resource patchResource = rs.createResource(URI.createFileURI(String.format("foo/epatches/%s.%d.xmi", relativeSourcePath, counter)));
            patchResource.getContents().add(epatch);
            patchResource.save(null);

        }
        Resource contentResource = rs.createResource(URI.createFileURI(String.format("foo/models/%s.%d.xmi", relativeSourcePath, counter)));
        contentResource.getContents().add(vo.getContent());
        contentResource.save(null);

        Properties commitInfo = new Properties();
        commitInfo.setProperty("author", vo.getAuthor().toExternalString());
        commitInfo.setProperty("message", vo.getCommitMessage());
        commitInfo.setProperty("branch", vo.getBranch());
        commitInfo.setProperty("timestamp", Long.toString(vo.getCommitTime().getTime()));
        commitInfo.setProperty("id", vo.getRev().getRevCommit().name());
        if (vo.getRev().getRevCommit().getParentCount() > 0) {
            commitInfo.setProperty("parentid", vo.getRev().getRevCommit().getParent(0).name());
        }

        File commitFile = new File(String.format("foo/commits/%s.%d.properties", relativeSourcePath, counter));
        commitFile.getParentFile().mkdirs();

        commitInfo.store(new FileOutputStream(commitFile), "commit " + counter);
        return ++counter;
    }
}
