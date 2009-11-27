package de.modelrepository.test.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import de.modelrepository.test.util.FileRevision;
import de.modelrepository.test.util.FileUtility;
import de.modelrepository.test.util.GitUtility;
import de.modelrepository.test.util.ParallelBranches;

public class GitFileHistory {
	private Repository repo;
	private RevWalk walk;
	private String fileRelativePath;
	private ArrayList<FileRevision> fileRevisions = new ArrayList<FileRevision>();
	private String currentBranch;
	private ArrayList<ObjectId> ids = new ArrayList<ObjectId>();
	private ArrayList<ParallelBranches> parallelBranches = new ArrayList<ParallelBranches>();
	//TODO testrepository und testklassen erstellen.
	
	
	/**
	 * Creates a new history for the given file. All Revisions of this file and additional information will be available.
	 * @param originalFile the file for which the history is needed.
	 * @param repo the git-repository that contains the file.
	 * @throws IOException If any Exceptions occur during the history creation the creation of the GitFileHistory object will be aborted.
	 */
	public GitFileHistory(File originalFile, Repository repo) throws IOException {
		this.repo = repo;
		fileRelativePath = FileUtility.getRelativePath(originalFile.getAbsoluteFile(), this.repo.getWorkDir()).replace(File.separator, "/");
		walk = buildWalk();
		fileRevisions = buildRevisions();
		parallelBranches = buildParallelBranches();
		this.repo.close();
	}
	
	/**
	 * @return a ArrayList that contains all Revisions for this file ordered by their commit time (ascending).
	 */
	public ArrayList<FileRevision> getAllFileRevisions() throws IOException {
		return fileRevisions;
	}
	
	
	/**
	 * @return a ArrayList that contains branches of parallel development.
	 */
	public ArrayList<ParallelBranches> getParallelBranches() {
		return parallelBranches;
	}

	private RevWalk buildWalk() {
		RevWalk walk = new RevWalk(repo);
		walk.sort(RevSort.COMMIT_TIME_DESC, true);
		walk.sort(RevSort.BOUNDARY, true);
		walk.setTreeFilter(AndTreeFilter.create(PathFilterGroup.createFromStrings(Collections.singleton(fileRelativePath)), TreeFilter.ANY_DIFF));
		return walk;
	}

	private ArrayList<FileRevision> buildRevisions() throws IOException {
		ArrayList<FileRevision> result = new ArrayList<FileRevision>();
		
		for (Ref ref : repo.getAllRefs().values())
			walk.markStart(walk.parseCommit(ref.getObjectId()));

		for (RevCommit revCommit : walk) {
			TreeWalk fileWalker = TreeWalk.forPath(repo, fileRelativePath, revCommit.getTree());
			result.add(new FileRevision(fileRelativePath, revCommit, repo));
		}
		Collections.sort(fileRevisions);
		return result;
	}

	private ArrayList<RevCommit> getMergeCommits() {
		ArrayList<RevCommit> merges = new ArrayList<RevCommit>();
		for (FileRevision fileRev : fileRevisions) {
			if(fileRev.getRevCommit().getParentCount() > 1) {
				fileRev.setMerge(true);
				merges.add(fileRev.getRevCommit());
			}
		}
		return merges;
	}

	private ArrayList<RevCommit> getForkCommits() {
		ArrayList<RevCommit> forks = new ArrayList<RevCommit>();
		
		for (FileRevision fileRev : fileRevisions) {
			List<RevCommit> children = GitUtility.getDirectChildren(fileRev.getRevCommit(), fileRevisions);
			if(children != null && children.size() > 1)
				forks.add(fileRev.getRevCommit());
		}
		return forks;
	}
	
	private ArrayList<ParallelBranches> buildParallelBranches() {
		ArrayList<RevCommit> merges = getMergeCommits();
		ArrayList<ParallelBranches> result = new ArrayList<ParallelBranches>();
		if(merges.size() != 0) {
			ArrayList<RevCommit> forks = getForkCommits();
			for (RevCommit merge : merges) {
				result.add(new ParallelBranches(merge, forks, fileRevisions));
			}
		}
		return result;
	}
}
