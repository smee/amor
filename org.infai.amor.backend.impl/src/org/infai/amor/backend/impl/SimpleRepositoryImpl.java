/*******************************************************************************
 * Copyright (c) 2009 InfAI.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.infai.amor.backend.impl;

import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Iterables.transform;

import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.URI.MalformedURIException;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.CommitTransaction;
import org.infai.amor.backend.Repository;
import org.infai.amor.backend.SimpleRepository;
import org.infai.amor.backend.internal.UriHandler;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

/**
 * @author sdienst
 *
 */
public class SimpleRepositoryImpl implements SimpleRepository {
    Repository repo;
    UriHandler uh;
    Map<Long, CommitTransaction> transactionMap = Maps.newHashMap();


    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#getBranches()
     */
    @Override
    public String[] getBranches(final String uri) {
        try {
            return toArray(transform(repo.getBranches(URI.createURI(uri)), new Function<Branch, String>() {

                @Override
                public String apply(final Branch branch) {
                    return branch.getName();
                }
            }), String.class);
        } catch (final MalformedURIException e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    /* (non-Javadoc)
     * @see org.infai.amor.backend.SimpleRepository#startTransaction(java.lang.String)
     */
    @Override
    public long startTransaction(final String branchname) {
        try {
            final CommitTransaction tr = repo.startCommitTransaction(repo.getBranch(uh.createUriFor(branchname)));
            transactionMap.put(tr.getRevisionId(), tr);
            return tr.getRevisionId();
        } catch (final MalformedURIException e) {
            throw new RuntimeException(e);
        }
    }

}
