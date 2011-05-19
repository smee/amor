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
import java.text.SimpleDateFormat;
import java.util.*;

import org.eclipse.emf.common.util.URI;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.infai.amor.backend.api.*;

/**
 * @author sdienst
 *
 */
public class AmorCommands implements CommandProvider {
    private long txId = -1;
    URI currentUri = getRepoUri();
    String branchname = null;
    private File crntDir;
    private RemoteAmor remoteamor;
    private SimpleRepository repo;

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
        if (txId > 0) {
            try{
                getRepo().rollbackTransaction(txId);
            } catch (NullPointerException e) {
            }
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
        if(!missing.isEmpty()){
            ci.print("Missing dependencies: ");
            ci.println(missing);
        }else{
            ci.println("OK.");
        }
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
        String argument = ci.nextArgument();
        if (argument == null) {
            currentUri = getRepoUri();
            return;
        }
        try{
            for (String arg : argument.split("/")) {
                if (arg.trim().equals("..")) {
                    currentUri = currentUri.trimSegments(1);
                } else {
                    final URI uri = currentUri.appendSegment(arg);
                    if (!getActiveContents(uri).isEmpty()) {
                        currentUri = uri;
                    } else {
                        ci.println("No such element (Hint: Try 'dir').");
                        break;
                    }
                }
            }
        }finally{
            ci.execute("pwd");
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

                ci.println("Successfully commited revision " + revisionId + " on branch " + branchname);
                txId = -1;
                this.branchname = null;
            }
        }

    }

    public void _connect(final CommandInterpreter ci) throws IOException {
        String hostname = ci.nextArgument();
        if (hostname == null) {
            ci.println("Please specify a valid hostname!");
        }
        int port = 9278;
        if (hostname.contains(":")) {
            final int idx = hostname.indexOf(':');
            port = Integer.parseInt(hostname.substring(idx + 1));
            hostname = hostname.substring(0, idx);
        }

        this.repo = remoteamor.getRepository(hostname, port);
        ci.println(repo!=null ? "Success." : "Could not connect to remote host!");
    }

    public void _delete(final CommandInterpreter ci) throws IOException {
        final String path = ci.nextArgument();

        getRepo().delete(txId, path);
        ci.println("Successfully deleted " + path + ". Hopefully...");
    }

    public void _dir(final CommandInterpreter ci) {
        URI uri = currentUri;
        String parameter = ci.nextArgument();
        if (parameter != null) {
            parameter = parameter.trim();
            if (parameter.equals("-l")) {
                if (currentUri.segmentCount() < 3) {
                    ci.println("Please choose an existing revision via \"cd\".");
                } else {
                    // assume we are staring at a revision, let's show the details!
                    ci.println(getTouchedModels());
                    return;
                }
            } else
                if (parameter.equals("..")) {
                    uri=uri.trimSegments(1);
                }
            uri = uri.appendSegments(parameter.split("/"));
        }
        final Set<String> strings = getActiveContents(uri);
        for (final String s : strings) {
            ci.println(s);
        }
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
            newDir = new File(crntDir.getAbsoluteFile().getParent());
        } else {
            newDir = new File(crntDir, arg).getCanonicalFile();
        }
        if (!newDir.isDirectory()) {
            ci.println(newDir+" is no valid directory!");
        } else {
            crntDir = newDir;
        }
        ci.println("Currently: " + crntDir);
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

    public void _newbranch(final CommandInterpreter ci) throws Exception {
        final String branchname = ci.nextArgument();
        if (branchname == null) {
            ci.println("please specify a valid branchname!");
            return;
        }
        final String oldbranchname = ci.nextArgument();
        long revisionId = -1;
        try {
            revisionId = Long.parseLong(ci.nextArgument());
        } catch (final NumberFormatException e) {
            if (!e.getMessage().equals("null")) {
                ci.println("please specify a valid revisionId!");
                return;
            }
        }
        final SimpleRepository repo = getRepo();
        assert repo != null;
        repo.createBranch(branchname, oldbranchname, revisionId);

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

    /**
     * @param uri
     * @return
     */
    private Set<String> getActiveContents(final URI uri) {
        final Set<String> strings = new TreeSet<String>();
        for (final String contenturi : getRepo().getActiveContents(uri.toString())) {
            strings.add(contenturi.substring(contenturi.lastIndexOf('/') + 1));
        }
        return strings;
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
            {"newbranch <branchname> <parent branchname> <revisionid>","create a new branch starting from a revision"},
            {"getbranches","print names of all known branches"},
            {"add <relative path to model>","add a modelfile"},
            {"addpatch <relative path to model> <relative path to epatch>", "add a changed model" },
            { "delete <relative path to model>", "delete a persisted model" },
            {"committransaction <username> <message>","commit all actions done during the current transaction"},
            {"aborttransaction","rollback all actions done during the current transaction"},

            { "Checkout:" },
            { "checkout <pathelement>", "checkout out the specified model relative to the current directory" },

            {"Navigation:"},
            { "pwd","show the current amor uri we are looking at" },
            { "dir","show the current amor repository contents using the uri show by 'pwd'" },
            { "dir -l","show details for the current revision (as specified via \"cd branch/revision\")" },
            { "cd <string>","append a string to the current amor uri, use '..' to remove the last uri segment, call without parameter to change uri back to the default" },

            { "Lokale Navigation (zum Finden von lokalen Modellen)" },
            { "lpwd","show the local file path we are in" },
            { "lls","show the contents of the local path" },
            { "lcd <string>","change the local directory" },

            { "Misc:" },
            { "amorhelp", "show these help messages" },

            { "Remoteanbindung" },
            { "connect <hostname[:port]>", "change to another remote amor repository" },
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
        final SimpleRepository repo = this.repo;
        if (repo == null) {
            System.err.println("No repository service found! Please connect via \"connect <hostname> <port>\".");
        }
        return repo;
    }
    /**
     * @return
     */
    private URI getRepoUri() {
        return URI.createURI("amor://localhost/repo");
    }

    /**
     * @param deleted
     * @return
     */
    private String getTouchedModels() {
        final StringBuilder sb = new StringBuilder();
        String branch = branchname;
        if (currentUri.segmentCount() >= 2) {
            branch = currentUri.segment(1);
        }

        RevisionInfo revInfo = getRepo().getRevisionInfo(branch, Long.parseLong(currentUri.segment(2)));
        sb.append("User    : ").append(revInfo.getUsername());
        sb.append("\n");
        sb.append("Message : ").append(revInfo.getMessage());
        sb.append("\n");
        sb.append("Time    : ").append(SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG).format(new Date(revInfo.getTimestamp())));
        sb.append("\n");
        sb.append("Added   : ").append(revInfo.getAddedModels());
        sb.append("\n");
        sb.append("Changed : ").append(revInfo.getChangedModels());
        sb.append("\n");
        sb.append("Removed : ").append(revInfo.getRemovedModels());
        sb.append("\n");

        return sb.toString();
    }

    /**
     * @param ra
     */
    public void setRemoteAmor(final RemoteAmor ra){
        this.remoteamor = ra;
    }


    public void shutdown(){
        this.repo = null;
        this.remoteamor = null;
        System.out.println("No remote amor available anymore :(");
    }

    /**
     * Called once on activation of this bundle.
     * 
     * @throws IOException
     */
    public void startup() throws IOException{
        final String defaulthost = "localhost";
        final int defaultport = 8788;
        System.out.format("Trying to connect to remote amor at %s:%d...", defaulthost, defaultport);
        this.txId = -1;
        currentUri=getRepoUri();
        branchname = null;
        crntDir=new File(".");
        try {
            this.repo = remoteamor.getRepository(defaulthost, defaultport);
            System.out.println("Success!");
        } catch (final IOException ioe) {
            System.err.format("Could not find amor repository at %s:%d!", defaulthost, defaultport);
        }
    }
}
