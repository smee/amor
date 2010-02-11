package de.modelrepository.test.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.WorkDirCheckout;
import org.eclipse.jgit.revwalk.RevCommit;

public class GitUtility {
	/**
	 * @param repo
	 *            the repository to analyze.
	 * @return returns a {@link List} with the names of all branches of the
	 *         repository.
	 */
	public static ArrayList<String> getAllBranchNames(Repository repo) {
		return new ArrayList<String>(repo.getAllRefs().keySet());
	}

	/**
	 * Method loads a specific branch into the repository's work dir.
	 * 
	 * @param refName
	 *            the name of the branch, e.g. <code>refs/remotes/x</code>.
	 * @param repo
	 *            the repository containing the branch.
	 * @return the name of the loaded branch.
	 */
	public static String loadBranch(String refName, Repository repo)
			throws IOException {
		Commit commit = repo.mapCommit(refName);
		Tree tree = commit.getTree();
		GitIndex index = repo.getIndex();
		WorkDirCheckout co = new WorkDirCheckout(repo, repo.getWorkDir(),
				index, tree);
		index.write();
		repo.writeSymref(Constants.HEAD, refName);
		return refName;
	}

	/**
	 * Method returns all parent commits for a given commit.<br>
	 * This method differs from {@link RevCommit#getParents()} since it resolves
	 * all parents recursively up to the initial commit.
	 * 
	 * @param c
	 *            the commit whose parents are searched.
	 * @return an {@link ArrayList} containing all Commits preceding
	 *         <code>c</code>.
	 */
	public static ArrayList<RevCommit> getAllParents(RevCommit c) {
		ArrayList<RevCommit> parents = new ArrayList<RevCommit>();
		for (RevCommit parent : c.getParents()) {
			parents.add(parent);
			parents.addAll(getAllParents(parent));
		}
		return parents;
	}

	/**
	 * This method resolves the direct children for a given commit.<br>
	 * This means that there is no recursion and that for each child the none
	 * recursive method {@link RevCommit#getParents()} will contain
	 * <code>c</code>.
	 * 
	 * @param c
	 *            the commit whose children are searched.
	 * @param allRevisions
	 *            a List containing all revisions of this file within the
	 *            repository.
	 * @return an {@link ArrayList} containing all Commits succeeding
	 *         <code>c</code>, <code>null</code> if there are no children.
	 */
	public static ArrayList<RevCommit> getDirectChildren(RevCommit c,
			ArrayList<FileRevision> allRevisions) {
		ArrayList<RevCommit> children = new ArrayList<RevCommit>();
		// iterate over all revisions and add those to the child list which
		// contain c in their parent list.
		for (FileRevision candidate : allRevisions) {
			for (RevCommit parent : candidate.getRevCommit().getParents()) {
				if (parent.getId().getName().equals(c.getId().getName()))
					children.add(candidate.getRevCommit());
			}
		}
		if (children.size() > 0)
			return children;
		return null;
	}

	/*
	 * Helper method for #getAllChildren. This method implements the recursion
	 * which fills the list of children.
	 */
	private static ArrayList<RevCommit> getChildrenRecursive(RevCommit c,
			ArrayList<FileRevision> allRevisions) {
		ArrayList<RevCommit> children = new ArrayList<RevCommit>();
		ArrayList<RevCommit> directChildren = getDirectChildren(c, allRevisions);
		if (directChildren != null) {
			children.addAll(directChildren);
			for (RevCommit child : directChildren) {
				children.addAll(getChildrenRecursive(child, allRevisions));
			}
		}
		return children;
	}

	/**
	 * Method returns all child commits for a given commit.<br>
	 * This method differs from {@link #getDirectChildren} since it resolves all
	 * children recursively up to the latest commit.
	 * 
	 * @param c
	 *            the commit whose children are searched.
	 * @param allRevisions
	 *            a List containing all revisions of this file within the
	 *            repository.
	 * @return an {@link ArrayList} which contains all children of the commit.<br>
	 *         Therefore for each direct child commit (branch) the list contains
	 *         another list which contains all its commits till the latest one.<br>
	 *         if the branches are merged later there will be duplicated
	 *         commits, e.g. if the latest commit merges all branches together
	 *         all lists will contain the latest commit.
	 */
	public static ArrayList<ArrayList<RevCommit>> getAllChildren(RevCommit c,
			ArrayList<FileRevision> allRevisions) {
		ArrayList<ArrayList<RevCommit>> children = new ArrayList<ArrayList<RevCommit>>();

		// for each direct child of the commit a list will be created, the child
		// will be added to this list
		// and all its children will be added to this list as well.
		ArrayList<RevCommit> directChildren = getDirectChildren(c, allRevisions);
		if (directChildren != null) {
			for (RevCommit directChild : directChildren) {
				ArrayList<RevCommit> childPath = new ArrayList<RevCommit>();
				childPath.add(directChild);
				childPath
						.addAll(getChildrenRecursive(directChild, allRevisions));
				children.add(childPath);
			}
		}

		return children;
	}

	/**
	 * This method checks out a specific revision by resetting the repository to
	 * the state where the given ref was commited.
	 * 
	 * @param repo
	 *            the repository to reset.
	 * @param refName
	 *            the name of the ref which gives the time of the commit. (e.g.
	 *            ObjectId.getName())
	 */
	public static void checkoutRevision(Repository repo, String refName) {
		ResetOperation rO = new ResetOperation(repo, refName,
				ResetOperation.ResetType.HARD);
		try {
			rO.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
