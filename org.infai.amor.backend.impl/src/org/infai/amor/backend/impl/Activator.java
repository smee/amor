package org.infai.amor.backend.impl;

import org.eclipse.core.runtime.Plugin;
import org.infai.amor.backend.Branch;
import org.infai.amor.backend.Repository;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.internal.impl.NeoBranchFactory;
import org.infai.amor.backend.internal.impl.TransactionManagerImpl;
import org.infai.amor.backend.internal.impl.UriHandlerImpl;
import org.infai.amor.backend.storage.Storage;
import org.infai.amor.backend.storage.StorageFactory;
import org.neo4j.api.core.NeoService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author sdienst
 */
public class Activator extends Plugin implements ServiceTrackerCustomizer, NeoProvider {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.infai.amor.backend.impl";

    // The shared instance
    private static Activator plugin;

    private BundleContext context;

    private NeoService neoService;

    private StorageFactory storageFactory;

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * The constructor
     */
    public Activator() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    @Override
    public Object addingService(final ServiceReference reference) {
        final Object service = context.getService(reference);
        if (service instanceof NeoService) {
            this.neoService = (NeoService) service;
        } else if (service instanceof StorageFactory) {
            this.storageFactory = (StorageFactory) service;
        }
        return reference;
    }

    /**
     * @return
     */
    public NeoService getNeo() {
        return neoService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    @Override
    public void modifiedService(final ServiceReference reference, final Object service) {
        // nothing todo, probably changed its storage directory
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    @Override
    public void removedService(final ServiceReference reference, final Object service) {
        if (service == this.neoService || service == this.storageFactory) {
            context.ungetService(reference);
        }
        this.neoService = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        this.context = context;

        // let's ask for neoservice implementations
        final ServiceTracker st = new ServiceTracker(context, NeoService.class.getName(), this);
        st.open();
        final ServiceTracker stSF = new ServiceTracker(context, ServiceFactory.class.getName(), this);
        stSF.open();
        // instantiate our repository
        final UriHandlerImpl uriHandler = new UriHandlerImpl();
        final Repository repo = new RepositoryImpl(new StorageFactory() {
            // fetch the storage service lazily
            @Override
            public Storage getStorage(final Branch branch) {
                if (storageFactory == null) {
                    return null;
                } else {
                    return storageFactory.getStorage(branch);
                }
            }
        }, new NeoBranchFactory(this), uriHandler, new TransactionManagerImpl(uriHandler, this));
        // register repository osgi service
        context.registerService(Repository.class.getName(), repo, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        plugin = null;
        neoService = null;
        storageFactory = null;
        super.stop(context);
    }

}
