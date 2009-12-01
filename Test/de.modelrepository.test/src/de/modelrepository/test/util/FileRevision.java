package de.modelrepository.test.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.WindowCursor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.RawCharSequence;

public class FileRevision implements Comparable<FileRevision>{
	private ObjectId objectId;
	private ArrayList<String> branches;
	private RevCommit commit;
	private Repository repo;
	private boolean isFork = false;
	private boolean isMerge = false;
	private String path;
	
	/**
	 * Create a new FileRevision object which will contain useful informations.
	 * @param path the path of the file relative to the repository's work dir.
	 * @param commit the commit of the file revision.
	 * @param repo the repository which contains the file.
	 */
	public FileRevision(String path, RevCommit commit, Repository repo) {
		this.objectId = commit.getId();
		this.path = path;
		this.commit = commit;
		this.repo = repo;
		try {
			this.branches = searchBranches();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return the id of the revision.
	 */
	public ObjectId getObjectId() {
		return objectId;
	}

	/**
	 * @return a {@link List} containing the names of all branches which contain the commit. 
	 */
	public List<String> getBranches() {
		return branches;
	}

	/**
	 * @return the content of the file as a String.<br>
	 * The content is retrieved from the object database using the id.
	 */
	public String getFileContent() throws IOException {
		ObjectDatabase db = repo.getObjectDatabase();
		byte[] buffer = db.openObject(new WindowCursor(), objectId).getBytes();
		RawCharSequence seq = new RawCharSequence(buffer, 0, buffer.length);
		return seq.toString();
	}
	
	/**
	 * @return the date and time of the commit.
	 */
	public Date getCommitTime() {
		return commit.getCommitterIdent().getWhen();
	}
	
	/**
	 * @return the commit for the file revision.
	 */
	public RevCommit getRevCommit() {
		return commit;
	}

	/**
	 * @return true if the commit is a fork.
	 */
	public boolean isFork() {
		return isFork;
	}

	/**
	 * @param isFork whether the commit is a fork.
	 */
	public void setFork(boolean isFork) {
		this.isFork = isFork;
	}

	/**
	 * @return true if the commit is a merge.
	 */
	public boolean isMerge() {
		return isMerge;
	}
	
	/**
	 * @param isMerge whether the commit is a merge.
	 */
	public void setMerge(boolean isMerge) {
		this.isMerge = isMerge;
	}

	/**
	 * Compares this revision with <code>rev</code> using the commit time.
	 * @param rev the FileRevision to compare with.
	 */
	@Override
	public int compareTo(FileRevision rev) {
		return getCommitTime().compareTo(rev.getCommitTime());
	}
	
	/*
	 * helper method for searching all branches which contain the commit.
	 */
	private ArrayList<String> searchBranches() throws IOException {
		ArrayList<String> branches = new ArrayList<String>();
		
		for (Ref ref : repo.getAllRefs().values()) {
			RevWalk walk = new RevWalk(repo);
			walk.sort(RevSort.COMMIT_TIME_DESC, true);
			walk.sort(RevSort.BOUNDARY, true);
			walk.setTreeFilter(AndTreeFilter.create(PathFilterGroup.createFromStrings(Collections.singleton(path)), TreeFilter.ANY_DIFF));
			walk.markStart(walk.parseCommit(ref.getObjectId()));
			for (RevCommit revCommit : walk) {
				if(commit.getId().getName().equals(revCommit.getId().getName()))
					branches.add(ref.getName());
			}
		}
		
		return branches;
	}
}
