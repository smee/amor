package de.modelrepository.test.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.RawCharSequence;

public class FileRevision implements Comparable<FileRevision>{
	private ObjectId objectId;
	private List<String> branches;
	private RevCommit commit;
	private Repository repo;
	private String path;
	private List<RevCommit> children;
	
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
		RevWalk walk = new RevWalk(repo);
		Tree tree = commit.asCommit(walk).getTree();
		TreeEntry entry = tree.findBlobMember(path);
		byte[] data = repo.openBlob(entry.getId()).getBytes();
		RawCharSequence seq = new RawCharSequence(data, 0, data.length);
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
		if(children != null) {
			return children.size()>1;
		}
		return false;
	}

	/**
	 * @return true if the commit is a merge.
	 */
	public boolean isMerge() {
		return commit.getParentCount()>1;
	}
	
	/**
	 * Compares this revision with <code>rev</code> using the commit time.
	 * @param rev the FileRevision to compare with.
	 */
	@Override
	public int compareTo(FileRevision rev) {
		return getCommitTime().compareTo(rev.getCommitTime());
	}
	
	/**
	 * @param forkedChildren the forkedChildren to set
	 */
	public void setChildren(List<RevCommit> children) {
		this.children = children;
	}
	
	/**
	 * @return the forkedChildren
	 */
	public List<RevCommit> getChildren() {
		return children;
	}

	/**
	 * @return the repository which contains this commit.
	 */
	public Repository getRepository() {
		return repo;
	}
	
	/**
	 * @return the relative path of the source file which was used to create the Revision.
	 */
	public String getSourceFileRelativePath() {
		return path;
	}
	
	public void setBranches(List<String> b) {
		branches = b;
	}
}
