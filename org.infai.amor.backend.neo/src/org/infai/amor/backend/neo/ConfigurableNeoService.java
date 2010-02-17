package org.infai.amor.backend.neo;

import java.io.Serializable;
import java.util.Dictionary;
import java.util.Map;
import java.util.logging.Logger;

import org.neo4j.graphdb.*;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * Configurable {@link GraphDatabaseService}, can change it's storage directory on the fly (but without copying contents!)
 * 
 * @author sdienst
 */
public final class ConfigurableNeoService implements ManagedService, GraphDatabaseService {
    // TODO use tinkerpop/blueprint graph abstraction
    private final Logger log = Logger.getLogger(ConfigurableNeoService.class.getName());

    private EmbeddedGraphDatabase neo;
    private String currentStoreDir = "storage";

    public ConfigurableNeoService(final BundleContext bc) {
        final String storageDirectory = bc.getProperty(NeoConfigurationConstants.STORAGE_DIRECTORY);
        if (storageDirectory != null) {
            currentStoreDir = storageDirectory;
        }

        neo = new EmbeddedGraphDatabase(currentStoreDir);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.neo4j.api.core.GraphDatabaseService#beginTx()
     */
    public Transaction beginTx() {
        return neo.beginTx();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.neo4j.api.core.GraphDatabaseService#createNode()
     */
    public Node createNode() {
        return neo.createNode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.neo4j.api.core.GraphDatabaseService#enableRemoteShell()
     */
    public boolean enableRemoteShell() {
        return neo.enableRemoteShell();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.neo4j.api.core.GraphDatabaseService#enableRemoteShell(java.util.Map)
     */
    public boolean enableRemoteShell(final Map<String, Serializable> initialProperties) {
        return neo.enableRemoteShell(initialProperties);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.neo4j.api.core.GraphDatabaseService#getAllNodes()
     */
    public Iterable<Node> getAllNodes() {
        return neo.getAllNodes();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.neo4j.api.core.GraphDatabaseService#getNodeById(long)
     */
    public Node getNodeById(final long id) {
        return neo.getNodeById(id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.neo4j.api.core.GraphDatabaseService#getReferenceNode()
     */
    public Node getReferenceNode() {
        return neo.getReferenceNode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.neo4j.api.core.GraphDatabaseService#getRelationshipById(long)
     */
    public Relationship getRelationshipById(final long id) {
        return neo.getRelationshipById(id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.neo4j.api.core.GraphDatabaseService#getRelationshipTypes()
     */
    public Iterable<RelationshipType> getRelationshipTypes() {
        return neo.getRelationshipTypes();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.neo4j.api.core.GraphDatabaseService#shutdown()
     */
    public void shutdown() {
        log.info("shutdown");
        if (neo != null) {
            neo.shutdown();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
     */
    @SuppressWarnings("unchecked")
    public void updated(final Dictionary properties) throws ConfigurationException {

        if (properties != null) {
            final String newStorageDir = (String) properties.get(NeoConfigurationConstants.STORAGE_DIRECTORY);
            if (newStorageDir != null) {
                if (!currentStoreDir.equals(newStorageDir)) {
                    log.info(String.format("Updating storage directory from '%s' to '%s'", currentStoreDir, newStorageDir));

                    currentStoreDir = newStorageDir;
                    neo.shutdown();
                    neo = new EmbeddedGraphDatabase(newStorageDir);
                }
            }
        }
    }

}
