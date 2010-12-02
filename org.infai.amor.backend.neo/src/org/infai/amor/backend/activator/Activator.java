package org.infai.amor.backend.activator;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author sdienst
 * 
 */
public class Activator implements BundleActivator {

    private EmbeddedGraphDatabase neo;

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext bc) throws Exception {
        bc.registerService(GraphDatabaseService.class.getName(), neo = new EmbeddedGraphDatabase("storage"), null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext bc) throws Exception {
        neo.shutdown();
    }

}
