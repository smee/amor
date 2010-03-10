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

import java.io.*;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.infai.amor.backend.api.SimpleRepository;

/**
 * @author sdienst
 *
 */
public class AmorCommands implements CommandProvider {

    private long txId = -1;
    URI currentUri = getRepoUri();
    String branchname = null;
    private File crntDir;

    private static String readModel(final File file) {
        final StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    public AmorCommands() throws IOException {
        crntDir = new File(".").getCanonicalFile();
    }

    public void _aborttransaction(final CommandInterpreter ci) throws Exception {
        if (txId <= 0) {
            getRepo().rollbackTransaction(txId);
            txId = -1;
            this.branchname = null;
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
        assert txId > 0;
        final List<String> missing = getRepo().checkin(readModel(modelfile), path, txId);
        ci.println(missing);
    }

    public void _addpatch(final CommandInterpreter ci) throws IOException {
        final String path = ci.nextArgument();
        final File modelfile = new File(crntDir, path);
        if (!modelfile.exists()) {
            ci.println("Please specify an existing model file!");
            return;
        }
        final String patchPath = ci.nextArgument();
        if (patchPath == null) {
            ci.println("Please specify the path to a serialized epatch!");
            return;
        }

        assert txId > 0;
        getRepo().checkinPatch(readModel(new File(crntDir, patchPath)), path, txId);
        ci.println("Success.");
    }

    public void _amorhelp(final CommandInterpreter ci){
        ci.println(getHelp());
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

    public void _checkout(final CommandInterpreter ci) throws IOException {
        final String filename = ci.nextArgument();
        if (filename == null) {
            ci.println("Please specify a valid filename as parameter for checkout.");
            return;
        }
        String branchToUse = branchname;
        if (branchToUse == null) {
            if (currentUri.segmentCount() > 1) {
                branchToUse = currentUri.segment(1);
            } else {
                ci.println("There is no current branch, please select one via \"cd\".");
                return;
            }
        }
        long revisionId = -1;
        if (currentUri.segmentCount() > 2) {
            revisionId = new Long(currentUri.segment(2));
        } else {
            ci.println("There is no current revision, please select one via \"cd\".");
            return;
        }
        final String modelContents = getRepo().checkout(branchToUse, revisionId, filename);
        // recreate folder structure

        final File file = new File(crntDir.getAbsolutePath(), filename);
        file.getParentFile().mkdirs();
        // store the model locally
        final BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        bw.write(modelContents);
        bw.close();
        ci.println("Saved model to " + file.getAbsolutePath());
    }

    public void _committransaction(final CommandInterpreter ci) throws Exception {
        if (txId <= 0) {
            ci.println("There is no running transaction, can't commit...");
        } else {
            final String commitmessage = ci.nextArgument();
            final String username = ci.nextArgument();
            if (commitmessage == null || username == null) {
                ci.println("Please provide a commitmessage and a username!");
            } else {
                final long revisionId = getRepo().commitTransaction(txId, username, commitmessage);

                ci.println("Successfully commited revision " + revisionId);
                txId = -1;
                this.branchname = null;
            }
        }

    }

    public void _delete(final CommandInterpreter ci) throws IOException {
        final String path = ci.nextArgument();

        getRepo().delete(txId, path);
        ci.println("Successfully deleted " + path + ". Hopefully...");
    }

    public void _getbranches(final CommandInterpreter ci) {
        for (final String branchname : getRepo().getBranches()) {
            ci.println(branchname);
        }
    }

    public void _lcd(final CommandInterpreter ci) throws IOException {
        final String arg = ci.nextArgument();
        File newDir = null;
        if (arg == null) {
            newDir = new File(".").getCanonicalFile();
        } else if (arg.trim().equals("..")) {
            newDir = new File(crntDir.getParent());
        } else {
            newDir = new File(crntDir, arg).getCanonicalFile();
        }
        if (!newDir.isDirectory()) {
            ci.println(newDir+" is no valid directory!");
        } else {
            crntDir = newDir;
        }
        System.out.println("Currently: " + crntDir);
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

    public void _ls(final CommandInterpreter ci) {
        final String flag = ci.nextArgument();
        if (flag != null && flag.trim().equals("-l")) {
            // assume we are staring at a revision, let's show the details!
            // final Revision revision = getRepo().getRevision(currentUri);
            // ci.println(dumpTouchedModels(revision, Revision.ChangeType.ADDED));
            // ci.println(dumpTouchedModels(revision, Revision.ChangeType.CHANGED));
            // ci.println(dumpTouchedModels(revision, Revision.ChangeType.DELETED));
        } else {
            for (final String uri : getRepo().getActiveContents(currentUri.toString())) {
                ci.println(uri.substring(uri.lastIndexOf('/')));
            }
        }
    }

    public void _newbranch(final CommandInterpreter ci) throws Exception {
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
        // TODO use revision to branch from there
        getRepo().createBranch(branchname, null, -1);

        ci.println("Successfully created branch '" + branchname + "'");
    }

    public void _pwd(final CommandInterpreter ci) {
        ci.println(currentUri);
    }

    public void _starttransaction(final CommandInterpreter ci) {
        if (txId >= 0) {
            ci.println("Already in transaction!");
            return;
        }
        final String branchname = ci.nextArgument();
        if (branchname == null) {
            ci.println("Please specify the name of the branch this transaction should operate on!");
        } else {
            txId = getRepo().startTransaction(branchname);
            this.branchname = branchname;
        }
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
            { "addpatch <relative path to model> <relative path to epatch>", "add a changed model" },
            { "delete <relative path to model>", "delete a persisted model" },
            {"committransaction <username> <message>","commit all actions done during the current transaction"},
            {"aborttransaction","rollback all actions done during the current transaction"},
            { "Checkout:" },
            { "checkout <pathelement>", "checkout out the specified model relative to the current directory" },
            {"Navigation:"},
            { "pwd","show the current amor uri we are looking at" },
            { "ls","show the current amor repository contents using the uri show by 'pwd'" },
            { "cd <string>","append a string to the current amor uri, use '..' to remove the last uri segment, call without parameter to change uri back to the default" },
            { "Lokale Navigation (zum Finden von lokalen Modellen)" },
            { "lpwd","show the local file path we are in" },
            { "lls","show the contents of the local path" },
            { "lcd <string>","change the local directory" },
            { "Misc:" }, { "amorhelp", "show these help messages" },
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


    private SimpleRepository getRepo() {
        return Activator.getInstance().getRepository();
    }

    /**
     * @return
     */
    private URI getRepoUri() {
        return URI.createURI("amor://localhost/repo");
    }
}
