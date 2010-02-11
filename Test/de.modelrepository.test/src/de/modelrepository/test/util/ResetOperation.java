package de.modelrepository.test.util;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefLogWriter;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tag;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.WorkDirCheckout;

/**
 * This class is used to reset the repository to a specific state.
 */
public class ResetOperation {
	/**
	 * the reset type
	 */
	public enum ResetType {
		SOFT,
		MIXED,
		HARD
	}
	private final Repository repository;
	private final String refName;
	private final ResetType type;
	
	private Commit commit;
	private Commit previousCommit;
	private Tree newTree;
	private GitIndex index;
	
	/**
	 * Creates a new reset operation which resets the repository to the given commit.
	 * @param repository the repository to reset.
	 * @param refName the name of the commit or branch to reset to. (e.g. ObjectId.getName())
	 * @param type the type of the reset (soft/hard/mixed)
	 */
	public ResetOperation(Repository repository, String refName, ResetType type) {
		this.repository = repository;
		this.refName = refName;
		this.type = type;
	}
	
	/**
	 * Executes the reset operation. 
	 */
	public void run() throws IOException{
		mapObjects();
		writeRef();
		
		if (type != ResetType.SOFT) {
			if (type == ResetType.MIXED)
				resetIndex();
			else
				readIndex();
			writeIndex();
		}
		
		if (type == ResetType.HARD) {
			checkoutIndex();
		}
		
		if (type != ResetType.SOFT) {
			refreshIndex();
		}
		writeReflogs();
	}
	
	private void mapObjects() throws IOException {
		final ObjectId commitId;
		commitId = repository.resolve(refName);
		try {
			commit = repository.mapCommit(commitId);
		} catch (IOException e) {
			Tag t = repository.mapTag(refName, commitId);
			commit = repository.mapCommit(t.getObjId());
		}
//		previousCommit = repository.mapCommit(repository.resolve(Constants.HEAD));
	}
	
	private void writeRef() throws IOException {
		final RefUpdate ru = repository.updateRef(Constants.HEAD);
		ru.setNewObjectId(commit.getCommitId());
		ru.setRefLogMessage("reset", false);
		if (ru.forceUpdate() == RefUpdate.Result.LOCK_FAILURE)
			throw new IOException("Can't update " + ru.getName());
	}
	
	private void readIndex() throws IOException {
		newTree = commit.getTree();
		index = repository.getIndex();
	}
	
	private void resetIndex() throws IOException {
		newTree = commit.getTree();
		index = repository.getIndex();
		index.readTree(newTree);
	}
	
	private void refreshIndex() throws IOException {
		index.write();
	}
	
	private void writeIndex() throws IOException {
		index.write();
	}
	
	private void checkoutIndex() throws IOException {
		final File parentFile = repository.getWorkDir();
		WorkDirCheckout workDirCheckout = new WorkDirCheckout(repository, parentFile, index, newTree);
		workDirCheckout.setFailOnConflict(false);
		workDirCheckout.checkout();
	}


	private void writeReflog(String reflogRelPath) throws IOException {
		String name = refName;
		if (name.startsWith("refs/heads/"))
			name = name.substring(11);
		if (name.startsWith("refs/remotes/"))
			name = name.substring(13);
		
		String message = "reset --" + type.toString().toLowerCase() + " " + name;
		RefLogWriter.writeReflog(repository, null, commit.getCommitId(), message, reflogRelPath);
	}

	private void writeReflogs() throws IOException {
		writeReflog(Constants.HEAD);
		writeReflog(repository.getFullBranch());
	}
}
