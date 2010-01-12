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

import static org.infai.amor.test.ModelUtil.readInputModel;
import static org.infai.amor.test.ModelUtil.readInputModels;
import static org.junit.Assert.assertEquals;

import java.util.*;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.infai.amor.backend.impl.experiments.Graph;
import org.infai.amor.backend.impl.experiments.Tarjan;
import org.infai.amor.backend.impl.experiments.Graph.Node;
import org.infai.amor.backend.util.EcoreModelHelper;
import org.junit.Test;

import com.google.common.base.Joiner;
import com.google.common.collect.*;

/**
 * @author sdienst
 *
 */
public class ModelDependencyLoopTest {

    /**
     * @param nextDep
     * @return
     */
    private String dot(final String s) {
        return "\""+s+"\"";
    }

    /**
     * @param refs
     */
    private void dumpDotFile(final Multimap<String, String> refs) {
        System.out.println("digraph deps {\n" + "label=\"Hello.java.xmi\"");
        for(final String node: refs.keySet()){
            final Collection<String> deps = refs.get(node);
            if (deps.isEmpty()) {
                System.out.println(node);
            } else {
                System.out.print(node + " -> { ");
                System.out.println(Joiner.on(" ").join(deps) + "}");
            }
        }
        System.out.println("}");
    }

    /**
     * @param refs
     * @return
     */
    private List<String> findCycles(final Multimap<String, String> refs, final String startNode) {
        final Graph<String> graph = new Graph<String>();
        graph.addNode(startNode);
        for (final String node : refs.keySet()) {
            graph.addNode(node);
            for (final String succ : refs.get(node)) {
                graph.addNode(succ);
            }
        }
        for (final String node : refs.keySet()) {
            for (final String succ : refs.get(node)) {
                graph.addPredecessor(succ, node);
            }
        }
        assert graph.isIndependent(startNode);
        // search for cycles starting from every graph node
        for (final Node<String> node : graph.getNodes()) {
            final Tarjan<String> tarjan = new Tarjan<String>(graph, startNode);
            final List<String> cycle = tarjan.getCycle();
            if (cycle.size() > 1) {
                return cycle;
            }
        }
        return Collections.EMPTY_LIST;
    }

    @Test
    public void shouldFindNoLoopInTestData() throws Exception {
        final ResourceSet rs = new ResourceSetImpl();
        readInputModels("testmodels/02/primitive_types.ecore", rs);
        readInputModels("testmodels/02/java.ecore", rs);

        final Multimap<String, String> refs = ArrayListMultimap.create();
        final LinkedList<String> stack = new LinkedList<String>();
        final Set<String> done = Sets.newHashSet();
        URI startUri = null;

        stack.push("testmodels/02/Hello.java.xmi");

        while (!stack.isEmpty()) {
            final String fileUri = stack.pop();
            done.add(fileUri);

            final EObject input = readInputModel(fileUri, rs);

            if (startUri == null) {
                startUri = input.eResource().getURI().trimSegments(2);
            }

            final URI inputUri = input.eResource().getURI();
            final Set<URI> referencedModels = EcoreModelHelper.findReferencedModels(input, inputUri);
            for(final URI exturi:referencedModels){
                final String nextDep = exturi.resolve(inputUri).deresolve(startUri).toString();
                refs.put(dot(fileUri), dot(nextDep));
                if (!done.contains(nextDep)) {
                    stack.push(nextDep);
                }
            }
        }
        // write dot file
        //        dumpDotFile(refs);
        assertEquals(0, findCycles(refs, "testmodels/02/Hello.java.xmi").size());

    }
}
