/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.internal.storage.neo;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.ChangedModel;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Model;
import org.infai.amor.backend.Revision;
import org.infai.amor.backend.exception.TransactionException;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.internal.impl.ModelImpl;
import org.infai.amor.backend.internal.impl.NeoRelationshipType;
import org.infai.amor.backend.internal.impl.NeoRevision;
import org.infai.amor.backend.storage.Storage;
import org.neo4j.api.core.Node;

/**
 * @author sdienst
 * 
 */
public class NeoBlobStorage implements Storage {
    private final static Logger logger = Logger.getLogger(NeoBlobStorage.class.getName());
    private final NeoProvider np;
    private final Branch branch;
    private Set<Node> addedModelNodes;

    public NeoBlobStorage(final NeoProvider np, final Branch branch) {
        this.np = np;
        this.branch = branch;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkin(org.infai.amor.backend.ChangedModel,
     * org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void checkin(final ChangedModel model, final CommitTransaction tr) throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkin(org.infai.amor.backend.Model, org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void checkin(final Model model, final CommitTransaction tr) throws IOException {
        // store all eobjects/epackages
        // TODO remember the stored models for associating them with the revision on commit
        logger.finer("----------1-Storing contents----------");
        final NeoMappingDispatcher disp1 = new NeoMappingDispatcher(np);
        disp1.dispatch(model.getContent());
        for (final TreeIterator<EObject> it = model.getContent().eAllContents(); it.hasNext();) {
            final EObject eo = it.next();
            disp1.dispatch(eo);
        }
        logger.finer("----------2-Storing metadata----------");
        final NeoMetadataDispatcher disp2 = new NeoMetadataDispatcher(np);
        // reuse the eobject->neo4j node map
        disp2.setRegistry(disp1.getRegistry());

        // store all additional references and meta relationships
        disp2.dispatch(model.getContent());
        for (final TreeIterator<EObject> it = model.getContent().eAllContents(); it.hasNext();) {
            final EObject eo = it.next();
            disp2.dispatch(eo);
        }
        // remember new model node
        this.addedModelNodes.add((Node) disp2.getRegistry().get(model.getContent()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#checkout(org.eclipse.core.runtime.IPath, long)
     */
    @Override
    public Model checkout(final IPath path, final long revisionId) throws IOException {
        final NeoRestorer restorer = new NeoRestorer(np);
        return new ModelImpl(restorer.load("http://filesystem/"), path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#commit(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void commit(final CommitTransaction tr, final Revision rev) throws TransactionException {
        if (rev instanceof NeoRevision) {
            final NeoRevision revision = (NeoRevision) rev;
            for (final Node modelNode : addedModelNodes) {
                revision.getNode().createRelationshipTo(modelNode, NeoRelationshipType.getRelationshipType("added"));
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#rollback(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void rollback(final CommitTransaction tr) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.exception.TransactionListener#startTransaction(org.infai.amor.backend.CommitTransaction)
     */
    @Override
    public void startTransaction(final CommitTransaction tr) {
        this.addedModelNodes = new HashSet<Node>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.infai.amor.backend.storage.Storage#view(org.eclipse.core.runtime.IPath, long)
     */
    @Override
    public EObject view(final IPath path, final long revisionId) throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }

}
