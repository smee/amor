package org.infai.amor.backend.client;

import java.io.IOException;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.infai.amor.backend.api.SimpleRepository;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

import ch.ethz.iks.r_osgi.*;

public class Activator implements BundleActivator {

    private static Activator instance;
    private SimpleRepository repository;
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
    private void fetchRemoteRepositoryService(final BundleContext context) throws BundleException {
        // get the RemoteOSGiService
        final ServiceReference sref = context.getServiceReference(RemoteOSGiService.class.getName());
        if (sref == null) {
            throw new BundleException("No R-OSGi found");
        }
        remote = (RemoteOSGiService) context.getService(sref);

        // connect
        final URI remoteUri = new URI("r-osgi://localhost:8788");
        try {
            remote.connect(remoteUri);
            final RemoteServiceReference[] remoteServiceReferences = remote.getRemoteServiceReferences(remoteUri, SimpleRepository.class.getName(), null);
            if (remoteServiceReferences != null) {
                this.repository = (SimpleRepository) remote.getRemoteService(remoteServiceReferences[0]);
            }
        } catch (final RemoteOSGiException e) {
            System.err.println("Warning: Could not access remote AMOR repository!");
        } catch (final IOException e) {
            System.err.println("Warning: Could not access remote AMOR repository!");
        }

    }
    /**
     * @return the repository
     */
    public SimpleRepository getRepository() {
        return repository;
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception {
        instance = this;
        final ServiceTracker repoTracker = new ServiceTracker(context, SimpleRepository.class.getName(), null) {
            /*
             * (non-Javadoc)
             * 
             * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
             */
            @Override
            public Object addingService(final ServiceReference reference) {
                repository = (SimpleRepository) super.addingService(reference);
                return repository;
            }
        };
        repoTracker.open();
        repository = (SimpleRepository) repoTracker.getService();

        context.registerService(CommandProvider.class.getName(), new AmorCommands(), null);

        fetchRemoteRepositoryService(context);
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
