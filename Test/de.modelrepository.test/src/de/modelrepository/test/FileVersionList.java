package de.modelrepository.test;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;

import de.modelrepository.test.git.GitFileHistory;
import de.modelrepository.test.util.FileRevision;
import de.modelrepository.test.util.VersionObject;

/**
 * This class is a list of VersionObjects which contain EObjects, Epatches, ...
 * Using this list the user can restore the commit tree for checking it into another repository.
 */
public class FileVersionList implements Iterable<VersionObject> {
	private GitFileHistory fh;
	private List<VersionObject> versions;
	
	/**
	 * Creates a new List for the given FileHistory.
	 * @param fh the history of a java source file.
	 */
	public FileVersionList(GitFileHistory fh) {
		this.fh = fh;
		this.versions = new LinkedList<VersionObject>();
		try {
			createVersions();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createVersions() throws IOException {
		List<FileRevision> revs = fh.getAllFileRevisions();
		for (int i = revs.size() - 1; i >= 0; i--) {
			FileRevision rev = revs.get(i);
			List<VersionObject> parentVersions = new LinkedList<VersionObject>();
			List<VersionObject> siblingVersions = new LinkedList<VersionObject>();
			RevCommit[] parents = rev.getRevCommit().getParents();
			for (RevCommit parent : parents) {
				for (VersionObject o : versions) {
					if (o.getRev().getRevCommit().equals(parent)) {
						parentVersions.add(o);
					}
					RevCommit[] p = o.getRev().getRevCommit().getParents();
					for (RevCommit x : p) {
						if (x.equals(parent) && !(o.getRev().equals(rev)))
							siblingVersions.add(o);
					}
				}
			}
			VersionObject vo = new VersionObject(rev, parentVersions,
					siblingVersions);
			versions.add(vo);
		}
	}

	@Override
	public Iterator<VersionObject> iterator() {
		return versions.iterator();
	}

}
