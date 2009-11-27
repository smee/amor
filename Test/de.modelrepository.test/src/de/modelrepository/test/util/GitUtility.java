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
	public static List<String> getAllBranchNames(Repository repo) {
		return new ArrayList<String>(repo.getAllRefs().keySet());
	}
	
	public static String loadBranch(String refName, Repository repo) throws IOException {
		Commit commit = repo.mapCommit(refName);
		Tree tree = commit.getTree();
		GitIndex index = repo.getIndex();
		WorkDirCheckout co = new WorkDirCheckout(repo, repo.getWorkDir(), index, tree);
		index.write();
		repo.writeSymref(Constants.HEAD, refName);
		return refName;
	}
	
	public static ArrayList<RevCommit> getAllParents(RevCommit c) {
		ArrayList<RevCommit> parents = new ArrayList<RevCommit>();
		for (RevCommit parent : c.getParents()) {
			parents.add(parent);
			parents.addAll(getAllParents(parent));
		}
		return parents;
	}
	
	public static ArrayList<RevCommit> getDirectChildren(RevCommit c, ArrayList<FileRevision> allRevisions) {
		ArrayList<RevCommit> children = new ArrayList<RevCommit>();
		for (FileRevision candidate : allRevisions) {
			for (RevCommit parent : candidate.getRevCommit().getParents()) {
				if(parent.getId().getName().equals(c.getId().getName()))
					children.add(candidate.getRevCommit());
			}
		}
		if(children.size() > 0)
			return children;
		return null;
	}
	
	public static ArrayList<RevCommit> getChildrenRecoursive(RevCommit c, ArrayList<FileRevision> allRevisions) {
		ArrayList<RevCommit> children = new ArrayList<RevCommit>();
		ArrayList<RevCommit> directChildren = getDirectChildren(c, allRevisions);
		if(directChildren != null) {
			children.addAll(directChildren);
			for (RevCommit child : directChildren) {
				children.addAll(getChildrenRecoursive(child, allRevisions));
			}
		}
		return children;
	}
	
	public static ArrayList<ArrayList<RevCommit>> getAllChildren(RevCommit c, ArrayList<FileRevision> allRevisions) {
		ArrayList<ArrayList<RevCommit>> children = new ArrayList<ArrayList<RevCommit>>();
		
		for (RevCommit directChild : getDirectChildren(c, allRevisions)) {
			ArrayList<RevCommit> childPath = new ArrayList<RevCommit>();
			childPath.add(directChild);
			childPath.addAll(getChildrenRecoursive(directChild, allRevisions));
			children.add(childPath);
		}
		return children;
	}
}
