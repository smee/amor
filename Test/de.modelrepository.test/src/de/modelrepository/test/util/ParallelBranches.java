package de.modelrepository.test.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

//nur Zweige, die von fork bis merge vollständig sind
public class ParallelBranches {
	private ArrayList<ArrayList<ObjectId>> forkToMergeIds = new ArrayList<ArrayList<ObjectId>>();
	private ObjectId mergeId;
	private ObjectId forkId;
	
	private FileRevision mergeFileRev;
	private FileRevision forkFileRev;
	private ArrayList<ArrayList<FileRevision>> forkToMergeRevisions = new ArrayList<ArrayList<FileRevision>>();
	private ArrayList<FileRevision> allRevisions;
	
	public ParallelBranches(RevCommit mergeNode, ArrayList<RevCommit> forks, ArrayList<FileRevision> fileRevisions) {
		mergeId = mergeNode.getId();
		allRevisions = fileRevisions;
		searchFork(mergeNode, forks);
		searchRevisions();
	}

	private void searchFork(RevCommit mergeNode, ArrayList<RevCommit> forks) {
		RevCommit fork = null;
		if(forks.size() == 1) {
			fork = forks.get(0);
		}else {
			ArrayList<RevCommit> possibleForks = new ArrayList<RevCommit>();
			for (RevCommit forkCandidate : forks) {
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
			
			if(possibleForks.size() == 1) {
				fork = possibleForks.get(0);
			}else if(possibleForks.size() > 1) {
				RevCommit oldest = possibleForks.get(0);
				for (RevCommit candidate : possibleForks) {
					if(candidate.getCommitTime() < oldest.getCommitTime())
						oldest = candidate;
				}
				fork = oldest;
			}
		}
		
		forkId = fork.getId();
		setIdsBetween(fork, mergeNode);
	}
	
	private void setIdsBetween(RevCommit fork, RevCommit merge) {
		for (ArrayList<RevCommit> childPath : GitUtility.getAllChildren(fork, allRevisions)) {
			Collections.sort(childPath, new Comparator<RevCommit>() {
				public int compare(RevCommit o1, RevCommit o2) {
					return o1.getCommitTime() - o2.getCommitTime();
				}
			});
			int mergeIndex = childPath.indexOf(merge.getId());
			List<RevCommit> l = childPath.subList(0, mergeIndex);
			forkToMergeIds.add(new ArrayList<ObjectId>(l));
		}
	}
	
	private void searchRevisions() {
		for (FileRevision fileRev : allRevisions) {
			ObjectId id = fileRev.getObjectId();
			if(id.getName().equals(mergeId.getName()))
				mergeFileRev = fileRev;
			else if(id.getName().equals(forkId.getName()))
				forkFileRev = fileRev;
		}
		
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

	public ArrayList<ArrayList<FileRevision>> getRevisonsFromForkToMerge() {
		return forkToMergeRevisions;
	}

	public FileRevision getMergeRevision() {
		return mergeFileRev;
	}

	public FileRevision getForkRevision() {
		return forkFileRev;
	}
}
