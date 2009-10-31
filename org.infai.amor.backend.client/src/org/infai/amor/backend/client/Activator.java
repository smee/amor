package org.infai.amor.backend.client;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.infai.amor.backend.Repository;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

    private static Activator instance;
    private Repository repository;

    public static Activator getInstance() {
        return instance;
    }
    /**
     * @return the repository
     */
    public Repository getRepository() {
        return repository;
    }
    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception {
        instance = this;
        final ServiceTracker repoTracker = new ServiceTracker(context, Repository.class.getName(), null) {
            /*
             * (non-Javadoc)
             * 
             * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
             */
            @Override
            public Object addingService(final ServiceReference reference) {
                repository = (Repository) super.addingService(reference);
                return repository;
            }
        };
        repoTracker.open();
        repository = (Repository) repoTracker.getService();

        context.registerService(CommandProvider.class.getName(), new AmorCommands(), null);
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) throws Exception {
        instance = null;
        repository = null;
    }

}
