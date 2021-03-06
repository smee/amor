package de.modelrepository.test.git;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.treewalk.filter.*;

import de.modelrepository.test.util.*;

public class GitFileHistory {
    private Repository repo;
    private RevWalk walk;
    private String fileRelativePath;
    private ArrayList<FileRevision> fileRevisions = new ArrayList<FileRevision>();
    private String currentBranch;
    private ArrayList<ObjectId> ids = new ArrayList<ObjectId>();
    private ArrayList<ParallelBranches> parallelBranches = new ArrayList<ParallelBranches>();
    private Hashtable<String, List<String>> branchCache = new Hashtable<String, List<String>>();

    /**
     * Creates a new history for the given file. All Revisions of this file and additional information will be available.
     * 
     * @param originalFile
     *            the file for which the history is needed, relative to the git workdir
     * @param repo
     *            the git-repository that contains the file.
     * @throws IOException
     *             If any Exceptions occur during the history creation the creation of the GitFileHistory object will be aborted.
     */
    public GitFileHistory(String originalFile, Repository repo) throws IOException {
        this.repo = repo;
        fileRelativePath = originalFile;
        walk = buildWalk();
        fileRevisions = buildRevisions();
        buildBranchCache();
        parallelBranches = buildParallelBranches();
        this.repo.close();
    }

    /**
     * Creates a new history for the given file. All Revisions of this file and additional information will be available.
     * 
     * @param absoluteFile
     *            the absolute file for which the history is needed
     * @param repo
     *            the git-repository that contains the file.
     * @throws IOException
     *             If any Exceptions occur during the history creation the creation of the GitFileHistory object will be aborted.
     */
    public GitFileHistory(File absoluteFile, Repository repo) throws IOException {
        this(FileUtility.getRelativePath(absoluteFile.getAbsoluteFile(), repo.getWorkDir()).replace(File.separator, "/"), repo);
    }

    /**
     * @param relativeSourceFile
     * @param gitDirectory
     * @throws IOException
     */
    public GitFileHistory(String relativeSourceFile, File gitDirectory) throws IOException {
        this(relativeSourceFile, new Repository(gitDirectory));
    }

    /**
     * @return an ArrayList that contains all Revisions for this file ordered by their commit time (ascending).
     */
    public ArrayList<FileRevision> getAllFileRevisions() {
        return fileRevisions;
    }


    /**
     * @return an ArrayList that contains branches of parallel development.<br>
     * Each {@link ParallelBranches} object does only contain parallel running branches which are merged again.
     */
    public ArrayList<ParallelBranches> getParallelBranches() {
        return parallelBranches;
    }

    /*
     * builds up a revision walk to walk through the repository for determining all file revisions.
     */
    private RevWalk buildWalk() {
        RevWalk walk = new RevWalk(repo);
        walk.sort(RevSort.COMMIT_TIME_DESC, true);
        walk.sort(RevSort.BOUNDARY, true);
        walk.setTreeFilter(AndTreeFilter.create(PathFilterGroup.createFromStrings(Collections.singleton(fileRelativePath)), TreeFilter.ANY_DIFF));
        return walk;
    }

    /*
     * Mehtod builds up an ArrayList which contains all revisions for the file.
     */
    private ArrayList<FileRevision> buildRevisions() throws IOException {
        ArrayList<FileRevision> result = new ArrayList<FileRevision>();

        //for gitk --all behaviour mark each branch as a start point for the revision walk.
        for (Ref ref : repo.getAllRefs().values()) {
            walk.markStart(walk.parseCommit(ref.getObjectId()));
        }

        //build a FileRevision for each RevCommit in the walk
        for (RevCommit revCommit : walk) {
            result.add(new FileRevision(fileRelativePath, revCommit, repo));
        }
        //finally sort the list
        Collections.sort(fileRevisions);
        return result;
    }

    private void buildBranchCache() throws MissingObjectException, IncorrectObjectTypeException, IOException {
        for (Ref ref : repo.getAllRefs().values()) {
            RevWalk walk = new RevWalk(repo);
            walk.sort(RevSort.COMMIT_TIME_DESC, true);
            walk.sort(RevSort.BOUNDARY, true);
            walk.setTreeFilter(AndTreeFilter.create(PathFilterGroup.createFromStrings(Collections.singleton(fileRelativePath)), TreeFilter.ANY_DIFF));
            walk.markStart(walk.parseCommit(ref.getObjectId()));
            for (RevCommit revCommit : walk) {
                List<String> branches;
                if(branchCache.containsKey(revCommit.getId().getName())) {
                    branches = branchCache.get(revCommit.getId().getName());
                }else {
                    branches = new ArrayList<String>();
                }
                branches.add(ref.getName());
                branchCache.put(revCommit.getId().getName(), branches);
            }
        }

        for(FileRevision rev : fileRevisions) {
            rev.setBranches(branchCache.get(rev.getObjectId().getName()));
            List<RevCommit> children = GitUtility.getDirectChildren(rev.getRevCommit(), fileRevisions);
            rev.setChildren(children);
        }
    }

    /*
     * searches all commits of the file within the repository which are merging commits.
     * merge commits have multiple parents.
     */
    private ArrayList<RevCommit> getMergeCommits() {
        ArrayList<RevCommit> merges = new ArrayList<RevCommit>();
        for (FileRevision fileRev : fileRevisions) {
            if(fileRev.getRevCommit().getParentCount() > 1) {
                merges.add(fileRev.getRevCommit());
            }
        }
        return merges;
    }

    /*
     * searches all commits of the file within the repository which are forking commits.
     * fork commits have multiple children.
     */
    private ArrayList<RevCommit> getForkCommits() {
        ArrayList<RevCommit> forks = new ArrayList<RevCommit>();

        for (FileRevision fileRev : fileRevisions) {
            List<RevCommit> children = GitUtility.getDirectChildren(fileRev.getRevCommit(), fileRevisions);
            if(children != null) {
                fileRev.setChildren(children);
                if(children.size()>1) {
                    forks.add(fileRev.getRevCommit());
                }
            }
        }
        return forks;
    }

    /*
     * builds up an ArrayList which contains parallel running branches.
     */
    private ArrayList<ParallelBranches> buildParallelBranches() {
        //get all merges
        ArrayList<RevCommit> merges = getMergeCommits();
        ArrayList<ParallelBranches> result = new ArrayList<ParallelBranches>();
        //build a ParallelBranches object for each merge commit
        if(merges.size() != 0) {
            ArrayList<RevCommit> forks = getForkCommits();
            for (RevCommit merge : merges) {
                result.add(new ParallelBranches(merge, forks, fileRevisions));
            }
        }
        return result;
    }
}
