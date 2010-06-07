package de.modelrepository.test.testcases;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

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
        File testFile = new File("res/in/T0005/commons-lang/" + relativeSourcePath);
        GitFileHistory fh = new GitFileHistory(testFile, repo);
        FileVersionList list = new FileVersionList(fh);
        int counter = 0;
        for (VersionObject vo : list) {
            System.out.println(vo.getCommitTime());
            System.out.println(vo.getBranch());
            System.out.println(vo.getAuthor().getName());
            System.out.println(vo.getCommitMessage());

            // System.out.println(vo.getContent());
            System.out.println(vo.getPatches().size() + " epatches");
            counter = dumpEpatches(relativeSourcePath, vo.getPatches().values(), counter);
            System.out.println();
        }
    }

    /**
     * @param relativeSourcePath
     * @param values
     * @throws IOException
     */
    private int dumpEpatches(String relativeSourcePath, Collection<Epatch> values, int counter) throws IOException {
        ResourceSet rs = new ResourceSetImpl();
        for (Epatch epatch : values) {
            Resource resource = rs.createResource(URI.createFileURI(String.format("foo/epatches/%s.%d.xmi", relativeSourcePath, counter++)));
            resource.getContents().add(epatch);
            resource.save(null);
        }
        return counter;
    }
}
