/*******************************************************************************
 * Copyright (c) 2010 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.infai.amor.backend.*;
import org.infai.amor.backend.impl.*;
import org.infai.amor.backend.internal.BranchFactory;
import org.infai.amor.backend.internal.UriHandler;
import org.infai.amor.backend.internal.impl.TransactionManagerImpl;
import org.infai.amor.backend.neo.NeoProvider;
import org.infai.amor.backend.storage.StorageFactory;
import org.infai.amor.test.AbstractNeo4JTest;
import org.infai.amor.test.ModelUtil;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * @author sdienst TODO enhance integration tests with concurrent transactions
 */
public class TransactionSuspendResumeTests extends AbstractNeo4JTest {

    private NeoTransactionAwareSimpleRepository repo;
    private SimpleRepositoryImpl simplerepo;
    private TransactionManager txManager;

    @Before
    public void setup() {
        final Mockery context = new Mockery();
        final UriHandler uh = context.mock(UriHandler.class);
        final StorageFactory sf = context.mock(StorageFactory.class);
        final BranchFactory bf = context.mock(BranchFactory.class);
        final InternalRevision internalRev = context.mock(InternalRevision.class);

        final NeoProvider np = new NeoProvider() {
            @Override
            public GraphDatabaseService getNeo() {
                return neoservice;
            }
        };
        context.checking(new Expectations() {
            {
                allowing(uh);
                allowing(bf).createRevision(with(any(Branch.class)), with(any(Long.class)));
                will(returnValue(internalRev));
                allowing(internalRev);
                allowing(bf);
                allowing(sf);
            }
        });
        final Repository repository = new RepositoryImpl(sf, bf, uh, new TransactionManagerImpl(uh, np, bf));
        simplerepo = new SimpleRepositoryImpl(repository, uh);

        txManager = neoservice.getConfig().getTxModule().getTxManager();
        repo = new NeoTransactionAwareSimpleRepository(simplerepo, txManager);
    }

    @Test
    public void shouldResumeTransaction() throws Exception {
        // given
        // when
        final long txId = repo.startTransaction("trunk");
        repo.checkin(ModelUtil.readModel("testmodels/base.ecore"), "base.ecore", txId);
        // then
        // no exceptions should occur
        // resume connection to enable test cleanup
        txManager.resume(repo.txmap.get(txId));
    }

    @Test
    public void shouldStoreTransactionInWrapper() throws Exception {
        // given
        // when
        final long txId = repo.startTransaction("trunk");
        // then
        final Transaction transaction = repo.txmap.get(txId);
        assertNotNull(transaction);
        assertEquals(javax.transaction.Status.STATUS_NO_TRANSACTION, txManager.getStatus());

        // resume connection to enable test cleanup
        txManager.resume(transaction);
    }
}
