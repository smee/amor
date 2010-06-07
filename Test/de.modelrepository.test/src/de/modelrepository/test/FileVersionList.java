package de.modelrepository.test;

import java.util.*;

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
    List<FileRevision> revs;
    /**
     * Creates a new List for the given FileHistory.
     * @param fh the history of a java source file.
     */
    public FileVersionList(GitFileHistory fh) {
        this.fh = fh;
        this.revs = fh.getAllFileRevisions();
    }

    @Override
    public Iterator<VersionObject> iterator() {
        return new Iterator<VersionObject>() {
            int idx = revs.size();
            Map<String, VersionObject> commitCache = new HashMap<String, VersionObject>();

            @Override
            public VersionObject next() {
                FileRevision rev = revs.get(--idx);

                RevCommit crntCommit = rev.getRevCommit();

                List<VersionObject> parentVersions = new LinkedList<VersionObject>();
                RevCommit[] parents = rev.getRevCommit().getParents();
                for (RevCommit parent : parents) {
                    VersionObject vo = commitCache.get(parent.name());
                    assert vo != null : "parent commit should have been present by now!";
                    parentVersions.add(vo);
                }
                VersionObject vo = new VersionObject(rev, parentVersions, findSiblingBranches(crntCommit.name(), getParentIds(parentVersions)));
                commitCache.put(crntCommit.name(), vo);
                return vo;
            }

            private List<String> findSiblingBranches(String commitId, Set<String> parentIds) {
                Set<String> branches = new HashSet<String>();
                for (FileRevision rev : revs) {
                    if (!commitId.equals(rev.getRevCommit().name())) {
                        for(RevCommit potentialCommonParent: rev.getRevCommit().getParents()) {
                            if(parentIds.contains(potentialCommonParent.name())){
                                branches.addAll(rev.getBranches());
                            }
                        }
                    }
                }
                return new ArrayList<String>(branches);
            }

            @Override
            public boolean hasNext() {
                return idx > 0;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("not supported");
            }
            /**
             * @param versions
             * @return
             */
            private Set<String> getParentIds(List<VersionObject> versions) {
                Set<String> ids = new HashSet<String>(versions.size());
                for (VersionObject vo : versions) {
                    ids.add(vo.getRev().getRevCommit().name());
                }
                return ids;
            }
        };
    }
}
