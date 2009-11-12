package org.infai.amor.backend.client;

import java.io.IOException;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.infai.amor.backend.Repository;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import ch.ethz.iks.r_osgi.RemoteOSGiException;
import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.URI;

public class Activator implements BundleActivator {

    private static Activator instance;
    private Repository repository;
    private RemoteOSGiService remote;

    public static Activator getInstance() {
        return instance;
    }

    /**
     * @param context
     * @throws BundleException
     * @throws IOException
     * @throws RemoteOSGiException
     */
    private void fetchRemoteRepositoryService(final BundleContext context) throws BundleException, RemoteOSGiException, IOException {
        // get the RemoteOSGiService
        final ServiceReference sref = context.getServiceReference(RemoteOSGiService.class.getName());
        if (sref == null) {
            throw new BundleException("No R-OSGi found");
        }
        remote = (RemoteOSGiService) context.getService(sref);

        // connect
        final URI remoteUri = new URI("r-osgi://localhost:8788");
        remote.connect(remoteUri);
        final RemoteServiceReference[] remoteServiceReferences = remote.getRemoteServiceReferences(remoteUri, Repository.class.getName(), null);
        if (remoteServiceReferences != null) {
            this.repository = (Repository) remote.getRemoteService(remoteServiceReferences[0]);
        }

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

        // fetchRemoteRepositoryService(context);
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
