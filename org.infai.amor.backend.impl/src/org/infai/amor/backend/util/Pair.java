/**
 * 
 */
package org.infai.amor.backend.util;

/**
 * @author Steffen Dienst
 *
 */
public class Pair<S,T> {
    public final S first;
    public final T second;

    public Pair(S f, T s){
        this.first = f;
        this.second = s;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("<%s:%s>", first.toString(), second.toString());
    }
}
