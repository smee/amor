package org.infai.amor.backend.internal;

import org.infai.amor.backend.ModelLocation;
import org.infai.amor.backend.Revision;

public interface InternalRevision extends Revision{

    /**
     * @param modelPath
     * @return
     */
    ModelLocation getModelLocation(final String modelPath);

    void setCommitMessage(final String message);

    /**
     * @param currentTimeMillis
     */
    void setTimestamp(long currentTimeMillis);
    void setUser(final String username);

    /**
     * For internal usage only, not part of the external interface! Add references to added model nodes.
     * 
     * @param loc
     */
    void touchedModel(final ModelLocation loc);
}