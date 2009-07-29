package org.infai.amor.backend.impl;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.infai.amor.backend.Repository;
import org.infai.amor.backend.internal.NeoProvider;
import org.infai.amor.backend.internal.impl.NeoBranchFactory;
import org.infai.amor.backend.storage.Storage;
import org.neo4j.api.core.NeoService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin implements ServiceTrackerCustomizer, NeoProvider {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.infai.amor.backend.impl";

    // The shared instance
    private static Activator plugin;

    private Storage storage;

    private BundleContext context;

    private NeoService neoService;

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

    /**
     * @return the storage
     */
    public Storage getStorage() {
        return storage;
    }

    /**
     * @return
     */
    public NeoService getNeo() {
        return neoService;
    }

    /**
     * Creates storage instance for the given class name.
     */
    @SuppressWarnings("unchecked")
    private Storage getStorage(final IConfigurationElement ce, final String className) {
        try {
            final Class<Storage> clazz = Platform.getBundle(ce.getNamespaceIdentifier()).loadClass(className);
            return clazz.newInstance();
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Setter for tests.
     * 
     * @param strg
     */
    protected void setStorage(final Storage strg) {
        this.storage = strg;
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

        // TODO use servicetracker for storage implementations?
        // initialize the storage adapter
        final IExtensionPoint ep = Platform.getExtensionRegistry().getExtensionPoint(getBundle().getSymbolicName(), "org.infai.amor.backend.storage");
        if (ep != null) {
            final IConfigurationElement[] ces = ep.getConfigurationElements();
            for (final IConfigurationElement ce : ces) {
                final String elementName = ce.getName();
                if ("storage".equals(elementName)) {
                    final String className = ce.getAttribute("implementation");

                    // FIXME make backend selectable in properties?
                    // TODO use a storage factory instead
                    storage = getStorage(ce, className);

                    break;
                }
            }
        }
        // let's ask for neoservice implementations
        final ServiceTracker st = new ServiceTracker(context, NeoService.class.getName(), this);
        st.open();
        // FIXME instantiate completely
        final Repository repo = new RepositoryImpl(null, new NeoBranchFactory(this), null, null);
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
        super.stop(context);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    @Override
    public Object addingService(final ServiceReference reference) {
        this.neoService = (NeoService) context.getService(reference);
        return reference;
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
        if (service == this.neoService) {
            context.ungetService(reference);
        }
        this.neoService = null;
    }

}
