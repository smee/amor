package de.modelrepository.test.testcases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.util.*;

import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

import de.modelrepository.test.git.GitFileHistory;
import de.modelrepository.test.util.FileRevision;
import de.modelrepository.test.util.ParallelBranches;

public class T0004_GitFileRevisions {
    @Test
    public void test01() {
        try {
            Repository repo = new Repository(new File("res/in/T0004/voldemort/.git"));
            String testFile = "src/java/voldemort/server/VoldemortConfig.java";
            GitFileHistory fh = new GitFileHistory(testFile, repo);
            List<ParallelBranches> pB = fh.getParallelBranches();
            assertEquals("There is a different number of file revisions. should be 41!", fh.getAllFileRevisions().size(), 41);
            assertEquals("The number of parallel running branches which were merged should be 2!", pB.size(), 2);

            if(pB.size() == 2) {
                ArrayList<String> forks = new ArrayList<String>();
                forks.add(pB.get(0).getForkRevision().getObjectId().getName());
                forks.add(pB.get(1).getForkRevision().getObjectId().getName());

                ArrayList<String> merges = new ArrayList<String>();
                merges.add(pB.get(0).getMergeRevision().getObjectId().getName());
                merges.add(pB.get(1).getMergeRevision().getObjectId().getName());

                assertTrue(forks.contains("497e662f14dc64295b77942e299958868a737ac6"));
                assertTrue(merges.contains("0de3d054a796123dbefe8bc4ee91188a849cafc2"));
                assertTrue(merges.contains("7fa3e0678e7576387051d4e344e35645033a8b53"));

                ParallelBranches pb1 = null;
                ParallelBranches pb2 = null;
                if(merges.get(0).equals("0de3d054a796123dbefe8bc4ee91188a849cafc2")) {
                    pb1 = pB.get(0);
                    pb2 = pB.get(1);
                }else {
                    pb1 = pB.get(1);
                    pb2 = pB.get(0);
                }

                ArrayList<Integer> pb1Count = new ArrayList<Integer>();
                ArrayList<Integer> pb2Count = new ArrayList<Integer>();
                pb1Count.add(pb1.getRevisonsFromForkToMerge().get(0).size());
                pb1Count.add(pb1.getRevisonsFromForkToMerge().get(1).size());
                pb2Count.add(pb2.getRevisonsFromForkToMerge().get(0).size());
                pb2Count.add(pb2.getRevisonsFromForkToMerge().get(1).size());

                assertTrue(pb1Count.get(0) == 3 && pb1Count.get(1) == 3);
                assertTrue((pb2Count.get(0) == 1 && pb2Count.get(1) == 2) || (pb2Count.get(0) == 2 && pb2Count.get(1) == 1));
            }

            //			for (ParallelBranches b : fh.getParallelBranches()) {
            //				System.out.println("-----BRANCH-----");
            //				System.out.println(b.getForkRevision().getCommitTime());
            //				System.out.println(b.getMergeRevision().getCommitTime());
            //				for (ArrayList<FileRevision> l : b.getRevisonsFromForkToMerge()) {
            //					System.out.println("<<<Branch>>>");
            //					for (FileRevision fileRevision : l) {
            //						System.out.println(fileRevision.getCommitTime());
            //					}
            //					System.out.println("-----------------------------------------");
            //				}
            //				System.out.println("=============================================");
            //			}
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Not all outgoing branches of the fork will be merged again.
     * Check that only the two branches occur in the revision list which will be merged again.
     */
    @Test
    public void test02() {
        try {
            Repository repo = new Repository(new File("res/in/T0004/voldemort/.git"));
            String testFile = "src/java/voldemort/store/readonly/ReadOnlyStorageEngine.java";
            GitFileHistory fh = new GitFileHistory(testFile, repo);
            List<ParallelBranches> pB = fh.getParallelBranches();
            assertEquals("The number of parallel running branches which were merged should be 1!", pB.size(), 1);

            if(pB.size() == 1) {
                ParallelBranches pb1 = pB.get(0);
                assertEquals(pb1.getForkRevision().getObjectId().getName(), "607c3cc823185e6d5b9d885921b98a897a5e0f50");
                assertEquals(pb1.getMergeRevision().getObjectId().getName(), "b2daa523a27ec0e977cc1e9ee81e971909852ba0");
                assertEquals(pb1.getRevisonsFromForkToMerge().size(), 2);

                if(pb1.getRevisonsFromForkToMerge().size() == 2) {
                    assertEquals(pb1.getRevisonsFromForkToMerge().get(0).size(), 1);
                    assertEquals(pb1.getRevisonsFromForkToMerge().get(1).size(), 1);
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Check the object retrieval from the blob for several revision-ids.
     */
    @Test
    public void test03() {
        String graphId0 = "768509bbdd869dc8408541277ae609d6fab5984c";
        String graphId1 = "c56cdc6b57d3541c688f1f9b11daef19457d887c";
        String graphId2 = "8ea5a2d1c5d2352183c6af0ab1c6d39fb1b9b05e";
        try {
            Repository repo = new Repository(new File("res/in/T0003/.git"));
            String graphFile = "src/de/asv/graph/Graph.java";
            GitFileHistory fh = new GitFileHistory(graphFile, repo);
            for(Iterator<FileRevision> i=fh.getAllFileRevisions().iterator(); i.hasNext(); ) {
                boolean isTestRevision = false;
                FileRevision rev = i.next();
                File contentFile = null;
                if(rev.getObjectId().getName().equals(graphId0)) {
                    contentFile = new File("res/in/T0004/03/Graph01");
                    isTestRevision = true;
                }
                else if(rev.getObjectId().getName().equals(graphId1)) {
                    contentFile = new File("res/in/T0004/03/Graph02");
                    isTestRevision = true;
                }
                else if(rev.getObjectId().getName().equals(graphId2)) {
                    contentFile = new File("res/in/T0004/03/Graph03");
                    isTestRevision = true;
                }

                if(isTestRevision) {
                    String content = rev.getFileContent();
                    String testContent = "";
                    BufferedReader in = new BufferedReader(new FileReader(contentFile));
                    String line = "";
                    while((line=in.readLine()) != null) {
                        testContent+=line+"\n";
                    }
                    assertEquals("An error occured during reading the content of a file.", testContent, content);
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
