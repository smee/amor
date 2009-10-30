/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.epatch.applier.ApplyStrategy;
import org.eclipse.emf.compare.epatch.applier.CopyingEpatchApplier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.infai.amor.backend.ChangedModel;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.Revision.ChangeType;
import org.infai.amor.backend.exception.TransactionException;
import org.infai.amor.backend.internal.impl.ModelImpl;
import org.infai.amor.backend.internal.impl.NeoRevision;
import org.infai.amor.backend.storage.Storage;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 * Store models as xml documents without messing with their internal structures.
 * 
 * @author sdienst
 * 
 */
public class FileBlobStorage implements Storage {

    private final File storageDir;
    private ResourceSetImpl resourceSet;
    private Collection<FileModelLocation> addedModelUris;
    /**
     * order them by last modified timestamp
     */
    final static Ordering<File> revisionsFolderOrder = Ordering.from(new Comparator<File>() {
        @Override
        public int compare(final File o1, final File o2) {
            final long time1 = o1.lastModified();
            final long time2 = o2.lastModified();
            // during unit tests the timestamp might be the same, then sort by revision (name of the directory)
            if (time1 == time2) {
                final long rev1 = Long.parseLong(o1.getName());
                final long rev2 = Long.parseLong(o2.getName());
                return new Long(rev1).compareTo(rev2);
            } else {
                return new Long(time1).compareTo(time2);
            }
        }
    });

    /**
     * @param modelPath
     * @param includeFilename
     * @return
     */
    private static String createModelSpecificPath(final IPath modelPath) {
        if (modelPath != null && !modelPath.isAbsolute()) {
            final int numSegments = modelPath.segmentCount();

            final StringBuilder sb = new StringBuilder();
            // ignore filename
            for (int i = 0; i < numSegments - 1; i++) {
                sb.append(File.separatorChar).append(modelPath.segment(i));
            }

            return sb.toString();
        } else {
            throw new IllegalArgumentException("The given path must be relative for storing a model, was absolute: " + modelPath);
        }
    }

    /**
     * @param text
     * @return
     */
    public static String md5(final String text) {
        try {
            MessageDigest md;
            md = MessageDigest.getInstance("MD5");
            byte[] sha1hash = new byte[40];
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            sha1hash = md.digest();
            final StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < sha1hash.length; i++) {
                hexString.append(Integer.toHexString(0xFF & sha1hash[i]));
            }
            return hexString.toString();
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param storageDir
     * @param branch
     */
    public FileBlobStorage(final File storageDir, final String branchname) {
        this.storageDir = new File(storageDir, branchname);
        /*
         * TODO create Map<nsuri, most recent metamodel>, needed to be able to load model instances
         */
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkin(org.infai.amor.backend.ChangedModel, org.eclipse.emf.common.util.URI,
     * long)
     */
    @Override
    public void checkin(final ChangedModel model, final URI externalUri, final long revisionId) throws IOException {
        // we ignore dependant models altogether
        final ResourceSet inputRS = findMostRecentModelFor(model.getPath(), revisionId);
        // apply the model patch
        final CopyingEpatchApplier epatchApplier = new CopyingEpatchApplier(ApplyStrategy.LEFT_TO_RIGHT, model.getDiffModel(), inputRS);
        epatchApplier.apply();
        // store changed models
        final ResourceSet outputResourceSet = epatchApplier.getOutputResourceSet();
        for (final Resource res : outputResourceSet.getResources()) {
            // create the real storage uri, as the patch sets only a name
            res.setURI(createStorageUriFor(model.getPath(), revisionId, true));
            res.save(null);
        }
        addedModelUris.add(new FileModelLocation(externalUri, createModelSpecificPath(model.getPath()), ChangeType.CHANGED));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkin(org.infai.amor.backend.Model, org.eclipse.emf.common.util.URI, long)
     */
    @Override
    public void checkin(final Model model, final URI externalUri, final long revisionId) throws IOException {
        final URI storagePath = createStorageUriFor(model.getPersistencePath(), revisionId, true);
        final Resource resource = resourceSet.createResource(storagePath);
        resource.getContents().add(model.getContent());
        resource.save(null);

        addedModelUris.add(new FileModelLocation(externalUri, createModelSpecificPath(model.getPersistencePath()), ChangeType.ADDED));

        if(model.getContent() instanceof EPackage){
            // write a file containing the mapping of revision number to
            // this epackage name
            writeM2TagFile(revisionId, storagePath, ((EPackage) model.getContent()).getNsURI());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkout(org.eclipse.emf.common.util.URI)
     */
    @Override
    public Model checkout(final IPath path, final long revisionId) throws IOException {
        final ResourceSet rs = new ResourceSetImpl();
        final URI modelStorageUri = createStorageUriFor(path, revisionId, true);
        // first load the metamodel to prevent exception
        final String m2Uri = getM2Uri(new File(modelStorageUri.toFileString()));
        if (m2Uri != null) {
            final URI m2StorageUri = findMostRecentM2ByNamespace(revisionId, m2Uri);
            if (m2StorageUri != null) {
                // this is a m1 model, let's load the corresponding m2 model first
                final Resource m2resource = rs.createResource(m2StorageUri);
                m2resource.load(null);
                rs.getPackageRegistry().put(m2Uri, m2resource.getContents().get(0));
            }
        }
        // load the model
        final Resource resource = rs.createResource(modelStorageUri);
        resource.load(null);
        return new ModelImpl(resource.getContents().get(0), path);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#commit(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void commit(final CommitTransaction tr, final Revision rev) throws TransactionException {
        // nothing to do
        resourceSet = null;
        if (rev instanceof NeoRevision) {
            final NeoRevision revision = (NeoRevision) rev;
            for (final FileModelLocation fml : addedModelUris) {
                revision.touchedModel(fml);
            }
        } else {
            throw new TransactionException("Internal error: Do not know how to commit to revision of type " + rev.getClass());
        }
    }

    /**
     * @param modelPath
     * @param tr
     * @return
     */
    protected URI createStorageUriFor(final IPath modelPath, final long revisionId, final boolean includeFilename) {
        String dirName = Long.toString(revisionId);
        // if there is a model path, use its relative directory part
        if (modelPath != null) {
            dirName = dirName + "/" + createModelSpecificPath(modelPath);
        }
        File dir = new File(storageDir, dirName);
        dir.mkdirs();
        if (includeFilename && modelPath != null) {
            dir = new File(dir, modelPath.lastSegment());
        }
        // create uri for this new path
        final URI fileUri = URI.createURI(dir.toURI().toString());
        return fileUri;
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.storage.Storage#delete(org.eclipse.core.runtime.IPath, long)
     */
    @Override
    public void delete(final IPath modelPath, final URI externalUri,final long revisionId) throws IOException {
        addedModelUris.add(new FileModelLocation(externalUri, createModelSpecificPath(modelPath), ChangeType.DELETED));
        // TODO make sure to signal an error, if anyone checks in another model that depends on this deleted model
        // throw new UnsupportedOperationException("not implemented");
    }

    /**
     * @param dir
     */
    private void deleteRecurively(final File dir) {
        if (dir.isDirectory()) {
            for (final File file : dir.listFiles()) {
                deleteRecurively(file);
            }
        }
        dir.delete();

    }

    /**
     * @param revisionId
     * @param m2Namespace
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private URI findMostRecentM2ByNamespace(final long revisionId, final String m2Namespace) throws FileNotFoundException, IOException {
        // TODO add the tagfile contents to FileModelLocation#customProperties instead of writing them to the filesystem
        // find all tag files
        final List<File> allTagFiles = Lists.newArrayList(storageDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".tagfile");
            }
        }));
        // sort by timestamp descending
        Collections.sort(allTagFiles, new Comparator<File>() {
            @Override
            public int compare(final File f1, final File f2) {
                return new Long(f2.lastModified()).compareTo(new Long(f1.lastModified()));
            }
        });
        for (final File tagFile : allTagFiles) {
            final BufferedReader br = new BufferedReader(new FileReader(tagFile));
            final Scanner sc = new Scanner(br.readLine());
            final long revId = sc.nextLong();
            // ignore all newer revisions
            if (revId > revisionId) {
                continue;
            }else{
                final String nsUri = sc.next();
                if (nsUri.equals(m2Namespace)) {
                    // found our most recent m2 model, return it's location
                    return URI.createURI(sc.next());
                }
            }
        }
        return null;
    }

    /**
     * Create a new resourceset that contains the newest instance of the model specified by the given relative path
     * 
     * @param path
     * @param revisionId
     * @return
     * @throws IOException
     */
    protected ResourceSet findMostRecentModelFor(final IPath path, final long revisionId) throws IOException {
        final String modelSpecificPath = createModelSpecificPath(path) + File.separatorChar + path.lastSegment();
        // find all revisions with id <= revisionId
        final ArrayList<File> allRevDirs = Lists.newArrayList(Arrays.asList(storageDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File path) {
                return path.isDirectory() && Long.parseLong(path.getName()) <= revisionId;
            }
        })));

        // find the newest revision
        final File newestRevisionDir = revisionsFolderOrder.max(Iterables.filter(allRevDirs, new Predicate<File>() {
            @Override
            public boolean apply(final File revDir) {
                final File f = new File(revDir, modelSpecificPath);
                return f.exists();
            }
        }));
        // first, load the most recent metamodel in case this is a M1 model
        Resource m2Resource = null;
        final String nsUri = getM2Uri(new File(newestRevisionDir, modelSpecificPath));
        if (nsUri != null && nsUri.length() > 0) {
            // find most recent m2 model with the given package name
            final URI m2Uri = findMostRecentM2ByNamespace(revisionId,nsUri);
            m2Resource = resourceSet.getResource(m2Uri, true);
            m2Resource.load(null);
            resourceSet.getPackageRegistry().put(nsUri, m2Resource.getContents().get(0));
        }
        // load the newest model version
        final Resource res = resourceSet.getResource(createStorageUriFor(path, Long.parseLong(newestRevisionDir.getName()), true), true);
        res.load(null);

        if (m2Resource != null) {
            // if we loaded the m2 model,remove it to work around a bug in epatch
            resourceSet.getResources().remove(m2Resource);
        }
        return resourceSet;
    }

    /**
     * Try to find the package name. Returns null if the file is no m2 emf model.
     * 
     * @param file
     *            file containing an emf model xml
     * @return nsUri or null
     * @throws
     */
    String getM2Uri(final File file){
        try {
            // extract namespace uri if this is a m2 model
            final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(false);
            final DocumentBuilder builder = domFactory.newDocumentBuilder();
            final Document doc = builder.parse(file);

            // final String nsUri = XPathFactory.newInstance().newXPath().evaluate("/EPackage/@nsURI", doc);
            // // is this a m2 model?
            // if (nsUri != null && nsUri.length() > 0) {
            // return nsUri;
            // } else {
            // m1 model
            final String rootNodeName = doc.getFirstChild().getNodeName();
            if (rootNodeName.contains(":")) {
                final String attrName = "xmlns:" + rootNodeName.substring(0, rootNodeName.indexOf(':'));
                final Node attribute = doc.getFirstChild().getAttributes().getNamedItem(attrName);
                final String m2uri = attribute.getNodeValue();
                if (!m2uri.equals("http://www.eclipse.org/emf/2002/Ecore")) {
                    return m2uri;
                }
            }
            return null;
            // }
        } catch (final ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        } catch (final SAXException e) {
            e.printStackTrace();
            return null;
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#rollback(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void rollback(final CommitTransaction tr) {
        final URI fileURI = createStorageUriFor(null, tr.getRevisionId(), false);
        // delete the directory of this revision
        try {
            final File dir = new File(new java.net.URI(fileURI.toString()));
            deleteRecurively(dir);
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#startTransaction(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void startTransaction(final CommitTransaction tr) {
        resourceSet = new ResourceSetImpl();
        addedModelUris = Lists.newArrayList();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#view(org.eclipse.emf.common.util.URI)
     */
    @Override
    public EObject view(final IPath path, final long revisionId) throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Write a unique file containing the revision id, epackge namespace uri and filesystem location of a m2 model.
     * 
     * @param revisionId
     * @param storagePath
     * @param pckg
     * @throws IOException
     */
    private void writeM2TagFile(final long revisionId, final URI storagePath, final String nsURI) throws IOException {
        final File f = new File(this.storageDir, md5(Long.toString(System.nanoTime())) + ".tagfile");
        final BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        bw.write(String.format("%d\t%s\t%s", revisionId, nsURI, storagePath));
        bw.close();
    }

}
