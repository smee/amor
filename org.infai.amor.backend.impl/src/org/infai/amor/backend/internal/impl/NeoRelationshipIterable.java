package org.infai.amor.backend.internal.impl;

import java.util.Iterator;

import org.neo4j.api.core.Relationship;

/**
 * A helper for making large result sets iterable.
 * 
 * @author Peter H&auml;nsgen
 */
public abstract class NeoRelationshipIterable<T> implements Iterable<T> {
    /**
     * The relationships.
     */
    private final Iterable<Relationship> rels;

    /**
     * The constructor.
     */
    public NeoRelationshipIterable(final Iterable<Relationship> rels) {
        this.rels = rels;
    }

    /**
     * Creates an iterator.
     */
    public Iterator<T> iterator() {
        return new NeoRelationshipIterator(rels.iterator());
    }

    /**
     * @param r
     * @return
     */
    public abstract T narrow(Relationship r);

    class NeoRelationshipIterator implements Iterator<T> {
        private final Iterator<Relationship> it;

        public NeoRelationshipIterator(final Iterator<Relationship> it) {
            this.it = it;
        }

        public boolean hasNext() {
            return it.hasNext();
        }

        public T next() {
            return narrow(it.next());
        }

        public void remove() {
            it.remove();
        }
    }
}
