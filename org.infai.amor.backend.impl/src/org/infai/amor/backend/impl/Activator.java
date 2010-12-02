package org.infai.amor.backend.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.runtime.Plugin;
import org.infai.amor.backend.*;
import org.infai.amor.backend.api.SimpleRepository;
import org.infai.amor.backend.exception.TransactionException;
import org.infai.amor.backend.internal.TransactionManager;
import org.infai.amor.backend.internal.impl.*;
import org.infai.amor.backend.neo.NeoProvider;
import org.infai.amor.backend.storage.Storage;
import org.infai.amor.backend.storage.StorageFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import ch.ethz.iks.r_osgi.RemoteOSGiService;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author sdienst
 */
public class Activator extends Plugin implements ServiceTrackerCustomizer, NeoProvider {

    /**
     * @author sdienst
     * 
     */
    private final class DelegatingStorageFactory implements StorageFactory {
        @Override
        public void commit(final CommitTransaction tr) throws TransactionException {
            if (getStorageFactory() != null) {
                getStorageFactory().commit(tr);
            }
        }

        // fetch the storage service lazily
        @Override
        public Storage getStorage(final Branch branch) {
            if (getStorageFactory() == null) {
                return null;
            } else {
                return getStorageFactory().getStorage(branch);
            }
        }

        /* (non-Javadoc)
         * @see org.infai.amor.backend.storage.StorageFactory#getStorage(org.infai.amor.backend.CommitTransaction)
         */
        @Override
        public Storage getStorage(final CommitTransaction tr) {
            if (getStorageFactory() == null) {
                return null;
            }else{
                return  getStorageFactory().getStorage(tr);
            }
        }

        @Override
        public void rollback(final CommitTransaction tr) {
            if (getStorageFactory() != null) {
                getStorageFactory().rollback(tr);
            }
        }

        @Override
        public void startTransaction(final CommitTransaction tr) {
            if (getStorageFactory() != null) {
                getStorageFactory().startTransaction(tr);
            }

        }
    }

    // The plug-in ID
    public static final String PLUGIN_ID = "org.infai.amor.backend.impl";

    // The shared instance
    private static Activator plugin;

    private BundleContext context;

    private GraphDatabaseService neoService;

    private StorageFactory storageFactory;

    private NeoTransactionAwareSimpleRepository simplerepo;

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
        // System.out.println("Got new service of type " + service.getClass().getSimpleName());
        if (service instanceof GraphDatabaseService) {
            this.neoService = (GraphDatabaseService) service;
            if (neoService instanceof EmbeddedGraphDatabase) {
                this.simplerepo.setTransactionManager(((EmbeddedGraphDatabase) neoService).getConfig().getTxModule().getTxManager());
            } else {
                throw new AssertionError("GraphDatabaseService must be of type EmbeddedGraphDatabase.");
            }
        } else if (service instanceof StorageFactory) {
            this.setStorageFactory((StorageFactory) service);
        }
        return reference;
    }

    /**
     * @return
     */
    public GraphDatabaseService getNeo() {
        return neoService;
    }

    /**
     * @return the storageFactory
     */
    private StorageFactory getStorageFactory() {
        return storageFactory;
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
        if (service == this.neoService || service == this.getStorageFactory()) {
            context.ungetService(reference);
        }
        this.neoService = null;
    }

    /**
     * @param storageFactory the storageFactory to set
     */
    private void setStorageFactory(final StorageFactory storageFactory) {
        this.storageFactory = storageFactory;
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

        // instantiate our repository
        // TODO make settings configurable
        final UriHandlerImpl uriHandler = new UriHandlerImpl("localhost", "repo");
        final NeoBranchFactory branchFactory = new NeoBranchFactory(this);
        final TransactionManager trman = new TransactionManagerImpl(uriHandler, this, branchFactory);
        final DelegatingStorageFactory sf = new DelegatingStorageFactory();
        trman.addTransactionListener(sf);
        final Repository repo = new RepositoryImpl(sf, branchFactory, uriHandler, trman);
        // XXX FIXME trman should not be needed, handle read transaction within domain objects!
        simplerepo = new NeoTransactionAwareSimpleRepository(new SimpleRepositoryImpl(repo, uriHandler, trman), null);
        final ServiceTracker stSF = new ServiceTracker(context, StorageFactory.class.getName(), this);
        stSF.open();


        final ServiceTracker st = new ServiceTracker(context, GraphDatabaseService.class.getName(), this);
        st.open();

        // register repository service
        final Dictionary properties = new Hashtable();
        properties.put(RemoteOSGiService.R_OSGi_REGISTRATION, Boolean.TRUE);
        context.registerService(SimpleRepository.class.getName(), simplerepo, properties);
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
        setStorageFactory(null);
        super.stop(context);
    }

}
