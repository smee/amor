package org.infai.amor.backend.neostorage;

import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.storage.StorageFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator, NeoProvider {

    private GraphDatabaseService neo;

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    /* (non-Javadoc)
     * @see org.infai.amor.backend.internal.NeoProvider#getNeo()
     */
    @Override
    public GraphDatabaseService getNeo() {
        return neo;
    }

    public void start(final BundleContext context) throws Exception {
        final ServiceTracker serviceTracker = new ServiceTracker(context, GraphDatabaseService.class.getName(), null) {
            /* (non-Javadoc)
             * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
             */
            @Override
            public Object addingService(final ServiceReference reference) {
                final GraphDatabaseService ns =  (GraphDatabaseService) super.addingService(reference);
                neo = ns;
                return ns;
            }
        };
        serviceTracker.open();
        this.neo = (GraphDatabaseService) serviceTracker.getService();

        context.registerService(StorageFactory.class.getName(), new NeoBlobStorageFactory(this), null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) throws Exception {
        this.neo = null;
    }

}
