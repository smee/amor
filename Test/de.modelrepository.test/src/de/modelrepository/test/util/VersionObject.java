package de.modelrepository.test.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.epatch.Epatch;
import org.eclipse.emf.compare.epatch.applier.ApplyStrategy;
import org.eclipse.emf.compare.epatch.applier.CopyingEpatchApplier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;

import de.modelrepository.test.FileVersionList;
import de.modelrepository.test.JavaToEMFParser;
import de.modelrepository.test.ModelComparator;
import de.modelrepository.test.git.GitFileHistory;

public class VersionObject {
	private Date commitTime;
	private PersonIdent author;
	private PersonIdent committer;
	private String branch;
	/*
	 * This hashtable consists of the VersionObjects of parent commits as the
	 * keys. The values are the Epatches from the parsed content of this version
	 * to the parent version.
	 */
	private Hashtable<VersionObject, Epatch> parentsAndPatches;
	private ResourceSet rs;
	private EObject content;
	private FileRevision rev;

	/**
	 * Creates a new VersionObject for the given revision of a java file.
	 * 
	 * @param rev
	 *            the revision which contains the commit, the content of the
	 *            file, ...
	 * @param parents
	 *            a list of the parents for this VersionObject (needed for
	 *            creation of the patches, ...)
	 * @param siblingsForFork
	 *            a list of VersionObjects of already processed commits which
	 *            are the siblings of this commit (which have the same parent)
	 */
	public VersionObject(FileRevision rev, List<VersionObject> parents,
			List<VersionObject> siblingsForFork) {
		this.rev = rev;
		commitTime = rev.getCommitTime();
		author = rev.getRevCommit().getAuthorIdent();
		committer = rev.getRevCommit().getCommitterIdent();
		branch = resolveBranch(rev, parents, siblingsForFork);
		try {
			content = parseContent(rev);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			createPatches(parents);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Parses the content of the commit and the dependent classes into a
	 * ResourceSet and returns an EObject which contains the content of the
	 * file. This method works directly on the repository directory and checks
	 * out the needed versions of the dependent classes.
	 */
	private EObject parseContent(FileRevision rev) throws IOException {
		EObject o = null;
		System.out.println("checking out");
		GitUtility.checkoutRevision(rev.getRepository(), rev.getRevCommit()
				.getName());
		System.out.println("parsing");
		JavaToEMFParser parser = new JavaToEMFParser();
		File f = new File(rev.getRepository().getWorkDir(), rev
				.getSourceFileRelativePath());

		rs = parser.parseJavaFile(f, null);
		if(rs.getResources().size() > 0 && rs.getResources().get(0).getContents().size() > 0) {
			o = rs.getResources().get(0).getContents().get(0);
		}
		System.out.println("done");
		return o;
	}

	/*
	 * This method creates Epatches from the parsed content to each parent
	 * version.
	 */
	private void createPatches(List<VersionObject> parents)
			throws InterruptedException {
		parentsAndPatches = new Hashtable<VersionObject, Epatch>();
		if (parents != null && content != null) {
			for (VersionObject p : parents) {
				EObject parent = p.getContent();
				ModelComparator comparator = new ModelComparator();
				comparator.compare(content, parent);
				parentsAndPatches.put(p, comparator.getEpatch());
			}
		}
	}

	/*
	 * This method tries to resolve a branch for this version of the file so
	 * that the original commit tree can be restored.
	 */
	private String resolveBranch(FileRevision rev, List<VersionObject> parents,
			List<VersionObject> siblings) {
		List<String> branches = rev.getBranches();
		if (branches.size() == 1)
			return branches.get(0);
		if (parents.size() == 0) {
			return branches.get(0);
		} else if (parents.size() == 1) {
			FileRevision parent = parents.get(0).getRev();
			if (parent.isFork()) {
				if (siblings.size() == 0)
					return parents.get(0).getBranch();
				else {
					for (String branch : branches) {
						boolean used = false;
						for (Iterator i = siblings.iterator(); i.hasNext();) {
							VersionObject sibling = (VersionObject) i.next();
							if (branch.equals(sibling.getBranch())
									&& branch.endsWith(sibling.getBranch())
									&& sibling.getBranch().endsWith(branch))
								used = true;
						}
						if (!used)
							return branch;
					}
				}
				return parents.get(0).getBranch();
			} else
				return parents.get(0).getBranch();
		} else {
			return parents.get(0).getBranch();
		}
	}

	/**
	 * @return the commitTime of this version.
	 */
	public Date getCommitTime() {
		return commitTime;
	}

	/**
	 * @return the author of this version.
	 */
	public PersonIdent getAuthor() {
		return author;
	}

	/**
	 * @return the committer of this version.
	 */
	public PersonIdent getCommitter() {
		return committer;
	}

	/**
	 * @return the branch the branch in which this version is located.
	 */
	public String getBranch() {
		return branch;
	}

	/**
	 * @return the content of this version of the class.
	 */
	public EObject getContent() {
		return content;
	}

	/**
	 * @return the file revision which contains further information (the content
	 *         of the file as text, ...)
	 */
	public FileRevision getRev() {
		return rev;
	}

	/**
	 * @return the patches from this revision to the parent ones.
	 */
	public Hashtable<VersionObject, Epatch> getPatches() {
		return parentsAndPatches;
	}

	/**
	 * @return the ResourceSet which contains the EObject of the current version
	 *         of the class as well as the parsed dependent classes.
	 */
	public ResourceSet getResourceSet() {
		return rs;
	}
}
