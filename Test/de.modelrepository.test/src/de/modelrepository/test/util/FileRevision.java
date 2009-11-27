package de.modelrepository.test.util;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.ArrayList;
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

	public ObjectId getObjectId() {
		return objectId;
	}

	public List<String> getBranches() {
		return branches;
	}

	public String getFileContent() throws IOException {
		ObjectDatabase db = repo.getObjectDatabase();
		byte[] buffer = db.openObject(new WindowCursor(), objectId).getBytes();
		RawCharSequence seq = new RawCharSequence(buffer, 0, buffer.length);
		return seq.toString();
	}
	
	public Date getCommitTime() {
		return commit.getCommitterIdent().getWhen();
	}
	
	public RevCommit getRevCommit() {
		return commit;
	}

	public boolean isFork() {
		return isFork;
	}

	public void setFork(boolean isFork) {
		this.isFork = isFork;
	}

	public boolean isMerge() {
		return isMerge;
	}

	public void setMerge(boolean isMerge) {
		this.isMerge = isMerge;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(FileRevision rev) {
		return getCommitTime().compareTo(rev.getCommitTime());
	}
	
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
