package de.modelrepository.test.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

public class ParallelBranches {
	private ArrayList<ArrayList<ObjectId>> forkToMergeIds = new ArrayList<ArrayList<ObjectId>>();
	private ObjectId mergeId;
	private ObjectId forkId;
	
	private FileRevision mergeFileRev;
	private FileRevision forkFileRev;
	private ArrayList<ArrayList<FileRevision>> forkToMergeRevisions = new ArrayList<ArrayList<FileRevision>>();
	private ArrayList<FileRevision> allRevisions;
	
	/**
	 * Construct a ParallelBranches object which contains parallel running branches.<br>
	 * PrallelBranches only contain such branches which are forked and merged again.
	 * @param mergeNode the merge of the branches.
	 * @param forks all forks of the file.
	 * @param fileRevisions all revisions of the file.
	 */
	public ParallelBranches(RevCommit mergeNode, ArrayList<RevCommit> forks, ArrayList<FileRevision> fileRevisions) {
		mergeId = mergeNode.getId();
		allRevisions = fileRevisions;
		searchFork(mergeNode, forks);
		searchRevisions();
	}

	/*
	 * Method searches the right fork for the merge commit.
	 */
	private void searchFork(RevCommit mergeNode, ArrayList<RevCommit> forks) {
		RevCommit fork = null;
		//if there is only one fork it is the right one for the merge.
		if(forks.size() == 1) {
			fork = forks.get(0);
		}else {
			ArrayList<RevCommit> possibleForks = new ArrayList<RevCommit>();
			//search possible forks for the merge from all forks
			for (RevCommit forkCandidate : forks) {
				//search in all children of a fork
				//look if the merge occurs in every child path of the fork.
				ArrayList<ArrayList<RevCommit>> children = GitUtility.getAllChildren(forkCandidate, allRevisions);
				boolean inAllPaths = true;
				for (ArrayList<RevCommit> childPath : children) {
					if(!childPath.contains(mergeNode.getId())) {
						inAllPaths = false;
						break;
					}
				}
				if(inAllPaths)
					possibleForks.add(forkCandidate);
			}
			
			//if there is just one fork remaining use this one.
			if(possibleForks.size() == 1) {
				fork = possibleForks.get(0);
			//if there are multiple forks possible there may be nested parallel running branches.
			//So just take the oldest fork
			}else if(possibleForks.size() > 1) {
				RevCommit oldest = possibleForks.get(0);
				for (RevCommit candidate : possibleForks) {
					if(candidate.getCommitTime() < oldest.getCommitTime())
						oldest = candidate;
				}
				fork = oldest;
			}
		}
		
		//now set the fork and all commits between fork and merge
		forkId = fork.getId();
		setIdsBetween(fork, mergeNode);
	}
	
	/*
	 * Mehtod searches all commits between the fork and the merge commits.
	 */
	private void setIdsBetween(RevCommit fork, RevCommit merge) {
		//get all children of the fork and
		//for each child path sort the path by date
		for (ArrayList<RevCommit> childPath : GitUtility.getAllChildren(fork, allRevisions)) {
			Collections.sort(childPath, new Comparator<RevCommit>() {
				public int compare(RevCommit o1, RevCommit o2) {
					return o1.getCommitTime() - o2.getCommitTime();
				}
			});
			//then search the index of the merge commit and get a sublist till the merge.
			int mergeIndex = childPath.indexOf(merge.getId());
			List<RevCommit> l = childPath.subList(0, mergeIndex);
			forkToMergeIds.add(new ArrayList<ObjectId>(l));
		}
	}
	
	/*
	 * helper method to search the FileRevision objects for all commit ids calculated before.
	 */
	private void searchRevisions() {
		//search fork and merge revisions
		for (FileRevision fileRev : allRevisions) {
			ObjectId id = fileRev.getObjectId();
			if(id.getName().equals(mergeId.getName()))
				mergeFileRev = fileRev;
			else if(id.getName().equals(forkId.getName()))
				forkFileRev = fileRev;
		}
		
		//search all other revisions
		for (ArrayList<ObjectId> branchIds : forkToMergeIds) {
			ArrayList<FileRevision> branchRevisions = new ArrayList<FileRevision>();
			for (ObjectId branchId : branchIds) {
				for (FileRevision rev : allRevisions) {
					if(branchId.getName().equals(rev.getObjectId().getName()))
						branchRevisions.add(rev);
				}
			}
			forkToMergeRevisions.add(branchRevisions);
		}
	}

	/**
	 * @return an {@link ArrayList} containing another list for each child of the fork which contains all commits of this path till the merge commit.
	 */
	public ArrayList<ArrayList<FileRevision>> getRevisonsFromForkToMerge() {
		return forkToMergeRevisions;
	}
	
	/**
	 * @return the merge commit.
	 */
	public FileRevision getMergeRevision() {
		return mergeFileRev;
	}

	/**
	 * @return the fork commit
	 */
	public FileRevision getForkRevision() {
		return forkFileRev;
	}
}
