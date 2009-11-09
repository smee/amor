/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.client;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Repository;
import org.infai.amor.backend.Response;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.Revision.ChangeType;
import org.infai.amor.backend.responses.CommitSuccessResponse;

/**
 * @author sdienst
 *
 */
public class AmorCommands implements CommandProvider {

    private CommitTransaction transaction;
    URI currentUri = getRepoUri();
    private File crntDir;

    public AmorCommands() throws IOException {
        crntDir = new File(".").getCanonicalFile();
    }
    public void _aborttransaction(final CommandInterpreter ci){
        if(transaction!=null){
            getRepo().rollbackTransaction(transaction);
            transaction = null;
            ci.println("Transaction aborted.");
        } else {
            ci.println("No running transaction found.");
        }
    }

    public void _add(final CommandInterpreter ci) throws IOException {
        final String path = ci.nextArgument();
        final File modelfile = new File(crntDir, path);
        if (!modelfile.exists()) {
            ci.println("Please specify an existing model file!");
            return;
        }
        final ResourceSet rs = new ResourceSetImpl();
        final Resource resource = rs.getResource(URI.createFileURI(modelfile.getAbsolutePath()), true);
        resource.load(null);
        final Model m = new Model() {

            @Override
            public List<EObject> getContent() {
                return resource.getContents();
            }

            @Override
            public IPath getPersistencePath() {
                return new Path(path);
            }
        };
        assert transaction != null;
        final Response checkin = getRepo().checkin(m, transaction);
        ci.println(checkin.getMessage().getContent());
    }

    public void _cd(final CommandInterpreter ci){
        final String arg = ci.nextArgument();
        if (arg == null) {
            currentUri = getRepoUri();
        } else if (arg.trim().equals("..")) {
            currentUri = currentUri.trimSegments(1);
        } else {
            currentUri = currentUri.appendSegments(arg.split("/"));
        }
    }

    public void _committransaction(final CommandInterpreter ci) {
        if (transaction == null) {
            ci.println("There is no running transaction, can't commit...");
        } else {
            final String commitmessage = ci.nextArgument();
            final String username = ci.nextArgument();
            if (commitmessage == null || username == null) {
                ci.println("Please provide a commitmessage and a username!");
            } else {
                transaction.setCommitMessage(commitmessage);
                transaction.setUser(username);

                final Response response = getRepo().commitTransaction(transaction);

                if (!(response instanceof CommitSuccessResponse)) {
                    ci.println("Error on commit: " + response.getMessage().getContent());
                } else {
                    ci.println("Successfully commited " + response.getURI());
                    transaction = null;
                }
            }
        }

    }

    public void _delete(final CommandInterpreter ci) throws IOException {
        final String path = ci.nextArgument();

        final Response response = getRepo().deleteModel(new Path(path), this.transaction);
        ci.println(response.getMessage().getContent());
    }

    public void _getbranches(final CommandInterpreter ci) throws MalformedURIException {
        for (final Branch branch : getRepo().getBranches(getRepoUri())) {
            ci.println(branch.getName());
        }
    }

    public void _lcd(final CommandInterpreter ci) throws IOException {
        final String arg = ci.nextArgument();
        if (arg == null) {
            crntDir = new File(".").getCanonicalFile();
        } else if (arg.trim().equals("..")) {
            crntDir = new File(crntDir.getParent());
        } else {
            crntDir = new File(crntDir, arg);
        }
    }

    public void _lls(final CommandInterpreter ci){
        for (final File file : crntDir.listFiles()) {
            if (file.isDirectory()) {
                ci.print("[dir]  ");
            } else {
                ci.print("[file] ");
            }
            ci.println(file.getName());
        }

    }

    public void _lpwd(final CommandInterpreter ci){
        ci.println(crntDir.getAbsolutePath());
    }

    public void _ls(final CommandInterpreter ci) throws MalformedURIException {
        final String flag = ci.nextArgument();
        if (flag != null && flag.trim().equals("-l")) {
            // assume we are staring at a revision, let's show the details!
            final Revision revision = getRepo().getRevision(currentUri);
            ci.println(dumpTouchedModels(revision, Revision.ChangeType.ADDED));
            ci.println(dumpTouchedModels(revision, Revision.ChangeType.CHANGED));
            ci.println(dumpTouchedModels(revision, Revision.ChangeType.DELETED));
        } else {
            for (final URI uri : getRepo().getActiveContents(currentUri)) {
                ci.println(uri);
            }
        }
    }

    public void _newbranch(final CommandInterpreter ci) throws MalformedURIException {
        final String branchname = ci.nextArgument();
        final String revId = ci.nextArgument();
        if (branchname == null) {
            ci.println("please specify a valid branchname!");
            return;
        }
        Long revisionId = null;
        if (revId != null) {
            try {
                revisionId = new Long(revId);
            } catch (final NumberFormatException e) {
                ci.println("please specify a valid revisionId!");
                return;

            }
        }
        final Branch branch = getRepo().createBranch(null, branchname);
        // TODO use revision to branch from there
        // if (revisionId == null) {
        // branch = getRepo().createBranch(null, branchname);
        // } else {
        // final Revision revision = getRepo().getRevision(getRepoUri().appendSegments(new String[] { branchname, revId }));
        // branch = getRepo().createBranch(revision, branchname);
        // }
        ci.println("Successfully created branch '" + branchname + "'");
    }

    public void _pwd(final CommandInterpreter ci) {
        ci.println(currentUri);
    }

    public void _starttransaction(final CommandInterpreter ci) throws MalformedURIException {
        if (transaction != null) {
            ci.println(String.format("Already in transaction on branch '%s'", transaction.getBranch().getName()));
            return;
        }
        final String branchname = ci.nextArgument();
        if (branchname == null) {
            ci.println("Please specify the name of the branch this transaction should operate on!");
        } else {
            transaction = getRepo().startCommitTransaction(getRepo().getBranch(getRepoUri().appendSegment(branchname)));
        }
    }

    /**
     * @param revision
     * @param added
     * @return
     */
    private String dumpTouchedModels(final Revision revision, final ChangeType ct) {
        final StringBuilder sb = new StringBuilder(ct.name().toUpperCase());
        sb.append(":\n");
        for(final URI uri: revision.getModelReferences(ct)) {
            sb.append(uri).append("\n");
        }
        return sb.toString();
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.osgi.framework.console.CommandProvider#getHelp()
     */
    @Override
    public String getHelp() {
        final String[][] commands = new String[][]{
            {"---AMOR Repository Commands---"},
            { "Checkin:" },
            {"starttransaction <branchname>", "needed before invoking any other amor command!"},
            {"newbranch <branchname> <revisionid>","create a new branch starting from a revision"},
            {"getbranches","print names of all known branches"},
            {"add <relative path to model>","add a modelfile"},
            { "delete <relative path to model>", "delete a persisted model" },
            {"committransaction <username> <message>","commit all actions done during the current transaction"},
            {"aborttransaction","rollback all actions done during the current transaction"},
            {"Navigation:"},
            { "pwd","show the current amor uri we are looking at" },
            { "ls","show the current amor repository contents using the uri show by 'pwd'" },
            { "cd <string>","append a string to the current amor uri, use '..' to remove the last uri segment, call without parameter to change uri back to the default" },
            { "Lokale Navigation (zum Finden von lokalen Modellen)" },
            { "lpwd","show the local file path we are in" },
            { "lls","show the contents of the local path" },
            { "lcd <string>","change the local directory" },
            { "" } };
        final StringBuilder sb = new StringBuilder();
        for (final String[] command : commands) {
            if (command.length == 2) {
                sb.append(String.format("\t%-30s - %s\n", command[0], command[1]));
            } else {
                sb.append(String.format("%s\n", command[0]));
            }
        }
        return sb.toString();
    }

    private Repository getRepo(){
        return Activator.getInstance().getRepository();
    }

    /**
     * @return
     */
    private URI getRepoUri() {
        return URI.createURI("amor://localhost/repo");
    }

    /**
     * @param s
     * @return
     */
    private boolean isNumber(final String s){
        for (int i = 0; i < s.length(); i++) {
            if(!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

}
