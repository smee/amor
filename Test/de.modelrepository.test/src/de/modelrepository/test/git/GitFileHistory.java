package de.modelrepository.test.git;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.WindowCursor;
import org.eclipse.jgit.lib.WorkDirCheckout;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.RawCharSequence;

import de.modelrepository.test.util.FileUtility;
import de.modelrepository.test.util.Tupel;

public class GitFileHistory {
	private Repository repo;
	private RevWalk walk;
	private String fileRelativePath;
	private Vector<Tupel<ObjectId,String>> fileRevisionIds = new Vector<Tupel<ObjectId,String>>();
	private String currentBranch;
	
	
	public GitFileHistory(File originalFile, Repository repo) throws IOException {
		this.repo = repo;
		fileRelativePath = FileUtility.getRelativePath(originalFile.getAbsoluteFile(), this.repo.getWorkDir()).replace(File.separator, "/");
		loadBranch("refs/heads/master");
		for (String refName : getAllBranchNames()) {
			if(refName.equals("HEAD"))
				continue;
			loadBranch(refName);
			walk = buildWalk();
			fileRevisionIds.addAll(buildRevisions());
		}
		loadBranch("refs/heads/master");
		this.repo.close();
	}
	
	private RevWalk buildWalk() {
		RevWalk walk = new RevWalk(repo);
		walk.sort(RevSort.COMMIT_TIME_DESC, true);
		walk.sort(RevSort.BOUNDARY, true);
		walk.setTreeFilter(AndTreeFilter.create(PathFilterGroup.createFromStrings(Collections.singleton(fileRelativePath)), TreeFilter.ANY_DIFF));
		return walk;
	}
	
	private Vector<Tupel<ObjectId,String>> buildRevisions() throws IOException {
		Vector<Tupel<ObjectId,String>> result = new Vector<Tupel<ObjectId,String>>();
		AnyObjectId headID = repo.resolve(Constants.HEAD);
		if(headID == null) {
			System.err.println("NULL - headID");
			return null;
		}
		
		walk.markStart(walk.parseCommit(headID));
		
		for (RevCommit revCommit : walk) {
			TreeWalk fileWalker = TreeWalk.forPath(repo, fileRelativePath, revCommit.asCommit(walk).getTreeId());
			if(fileWalker != null)
				result.add(new Tupel<ObjectId, String>(fileWalker.getObjectId(0),currentBranch));
		}

		return result;
	}
	
	public Vector<Tupel<String,String>> getFileRevisions() throws IOException {
		Vector<Tupel<String,String>> result = new Vector<Tupel<String,String>>();
		ObjectDatabase db = repo.getObjectDatabase();
		for (Tupel<ObjectId,String> t : fileRevisionIds) {
			byte[] buffer = db.openObject(new WindowCursor(), t.getO1()).getBytes();
			RawCharSequence seq = new RawCharSequence(buffer, 0, buffer.length);
			result.add(new Tupel<String, String>(seq.toString(), t.getO2()));
		}
		return result;
	}
	
	private List<String> getAllBranchNames() {
		return new ArrayList<String>(repo.getAllRefs().keySet());
	}
	
	private void loadBranch(String refName) throws IOException {
		Commit commit = repo.mapCommit(refName);
		Tree tree = commit.getTree();
		GitIndex index = repo.getIndex();
		WorkDirCheckout co = new WorkDirCheckout(repo, repo.getWorkDir(), index, tree);
		index.write();
		repo.writeSymref(Constants.HEAD, refName);
		currentBranch = refName;
	}
	
	public static void main(String[] args) {
		try {
//			Repository repo = new Repository(new File("res/out/T0003/01/voldemort/.git"));
//			FileIterator iterator = new FileIterator(new File("res/out/T0003/01/voldemort"), new FileFilter() {
//				public boolean accept(File pathname) {
//					return pathname.isDirectory() || pathname.getAbsolutePath().endsWith(".java");
//				}
//			});
//			int max = 0;
//			File maxFile = null;
//			for (File file : iterator) {
//				GitFileHistory fileHistory = new GitFileHistory(file, repo);
//				Vector<String> revisions = fileHistory.getFileRevisions();
//				if(revisions.size() >= max) {
//					max = revisions.size();
//					maxFile = file;
//				}
//			}
//			
//			System.out.println(maxFile);
//			System.out.println(max);
//			
			GitFileHistory fileHistory = new GitFileHistory(new File("res/out/T0003/01/voldemort/src/java/voldemort/store/routed/RoutedStore.java"), new Repository(new File("res/out/T0003/01/voldemort/.git")));
			Vector<Tupel<String,String>> fileRevisions = fileHistory.getFileRevisions();
			
			int i=0;
			for (Tupel<String,String> t : fileRevisions) {
				File path = new File("D:/test/"+t.getO2());
				if(!path.exists())
					path.mkdirs();
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(path, "RoutedStore"+ ++i + ".java")));
				bw.write(t.getO1());
				bw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
