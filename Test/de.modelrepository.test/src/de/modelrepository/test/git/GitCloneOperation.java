package de.modelrepository.test.git;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryConfig;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.WorkDirCheckout;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;

import de.modelrepository.test.util.FileUtility;

public class GitCloneOperation {
	private URIish uri;
	private File workDir;
	private String branch;
	private String remoteName;
	private Repository local;
	private RemoteConfig remoteConfig;
	private FetchResult fetchResult;

	public GitCloneOperation(URIish uri, File workDir, String branch, String remoteName) {
		this.uri = uri;
		this.workDir = workDir;
		this.branch = branch;
		this.remoteName = remoteName;
	}
	
	private void initLocalRepository() throws IOException, URISyntaxException {
		File gitDir = new File(workDir, ".git");
		local = new Repository(gitDir);
		local.create();
		local.writeSymref(Constants.HEAD, branch);
		
		remoteConfig = new RemoteConfig(local.getConfig(), remoteName);
		remoteConfig.addURI(uri);
		
		String destination = Constants.R_REMOTES + remoteConfig.getName();
		RefSpec rs = new RefSpec();
		rs = rs.setForceUpdate(true);
		rs = rs.setSourceDestination(Constants.R_HEADS + "*", destination + "/*");
		
		remoteConfig.addFetchRefSpec(rs);
		local.getConfig().setBoolean("core", null, "bare", false);
		remoteConfig.update(local.getConfig());
		
		local.getConfig().setString(RepositoryConfig.BRANCH_SECTION, branch, "remote", remoteName);
		local.getConfig().setString(RepositoryConfig.BRANCH_SECTION, branch, "merge", branch);
		local.getConfig().save();
	}
	
	private void fetch() throws NotSupportedException, TransportException {
		Transport transport = Transport.open(local, remoteConfig);
		try {
			fetchResult = transport.fetch(NullProgressMonitor.INSTANCE, null);
		}finally {
			transport.close();
		}
	}
	
	private void checkout() throws IOException {
		Ref head = fetchResult.getAdvertisedRef(branch);
		if(head == null || head.getObjectId() == null)
			return;
		
		GitIndex index = new GitIndex(local);
		Commit mapCommit = local.mapCommit(head.getObjectId());
		Tree tree = mapCommit.getTree();
		
		RefUpdate u = local.updateRef(Constants.HEAD);
		u.setNewObjectId(mapCommit.getCommitId());
		u.forceUpdate();
		
		WorkDirCheckout co = new WorkDirCheckout(local, local.getWorkDir(), index, tree);
		co.checkout();
		index.write();
	}
	
	private void closeLocal() {
		if(local != null) {
			local.close();
			local = null;
		}
	}
	
	public void cloneRepository() throws InvocationTargetException {
		try {
			try {
				initLocalRepository();
				fetch();
				checkout();
			}finally {
				closeLocal();
			}
		}catch (Exception e) {
			FileUtility.delete(workDir);
			throw new InvocationTargetException(e);
		}
	}
}
