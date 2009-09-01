package org.infai.amor.backend.internal.impl;

import java.util.Iterator;

/**
 * A helper for making large result sets iterable.
 * 
 * @author Peter H&auml;nsgen
 */
public abstract class WrapperIterable<WRAPPER, INNER> implements Iterable<WRAPPER> {
    class NeoRelationshipIterator implements Iterator<WRAPPER> {
        private final Iterator<INNER> it;

        public NeoRelationshipIterator(final Iterator<INNER> it) {
            this.it = it;
        }

        public boolean hasNext() {
            return it.hasNext();
        }

        public WRAPPER next() {
            return narrow(it.next());
        }

        public void remove() {
            it.remove();
        }
    }

    /**
     * The relationships.
     */
    private final Iterable<INNER> rels;

    /**
     * The constructor.
     */
    public WrapperIterable(final Iterable<INNER> rels) {
        this.rels = rels;
    }

    /**
     * Creates an iterator.
     */
    public Iterator<WRAPPER> iterator() {
        return new NeoRelationshipIterator(rels.iterator());
    }

    /**
     * @param r
     * @return
     */
    public abstract WRAPPER narrow(INNER r);
}
