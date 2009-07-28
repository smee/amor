package org.infai.amor.backend.impl;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.infai.amor.backend.storage.Storage;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.infai.amor.backend.impl";

    // The shared instance
    private static Activator plugin;

    private Storage storage;

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

        // initialize the storage adapter
        final IExtensionPoint ep =
            Platform.getExtensionRegistry().getExtensionPoint(getBundle().getSymbolicName(), "org.infai.amor.backend.storage");

        final IConfigurationElement[] ces = ep.getConfigurationElements();
        for (final IConfigurationElement ce : ces) {
            final String elementName = ce.getName();
            if ("storage".equals(elementName)) {
                final String className = ce.getAttribute("implementation");

                // FIXME make backend selectable in properties?
                storage = getStorage(ce, className);

                break;
            }
        }
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

}
