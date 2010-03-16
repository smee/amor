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

    // remote service
    private RemoteOSGiService remote;
    private RemoteServiceReference remoteReference;
    private String currentUrl = "r-osgi://localhost:8788";

    public static Activator getInstance() {
        return instance;
    }


    /**
     * Try to connect to another remote osgi container running r-osgi. Try to fetch a reference to a {@link SimpleRepository}
     * implementation.
     * 
     * @param remoteUri
     * @return true if connect and fetch went successfully
     */
    private boolean connect(final URI remoteUri) {
        try {
            remote.connect(remoteUri);
            final RemoteServiceReference[] remoteServiceReferences = remote.getRemoteServiceReferences(remoteUri, SimpleRepository.class.getName(), null);
            if (remoteServiceReferences != null) {
                remoteReference = remoteServiceReferences[0];
                setRepository((SimpleRepository) remote.getRemoteService(remoteReference));
                return true;
            }
        } catch (final RemoteOSGiException e) {
            System.err.println("Warning: Could not access remote AMOR repository!");
        } catch (final IOException e) {
            System.err.println("Warning: Could not access remote AMOR repository!");
        }
        return false;
    }
    /**
     * @param context
     * @throws BundleException
     * @throws IOException
     * @throws RemoteOSGiException
     */
    private void fetchRemoteRepositoryService(final BundleContext context) throws BundleException {
        final Runnable fetchRemoteRepo = new Runnable() {


            @Override
            public void run() {
                // get the RemoteOSGiService
                final ServiceReference sref = context.getServiceReference(RemoteOSGiService.class.getName());
                if (sref != null) {
                    remote = (RemoteOSGiService) context.getService(sref);

                    connect(URI.create(currentUrl));
                } else {
                    System.err.println("Could not find RemoteOSGiService!");
                }

            }
        };
        // fetch reference to remote repository service in the background
        new Thread(fetchRemoteRepo).start();
    }

    /**
     * @return the repository
     */
    public SimpleRepository getRepository() {
        return repository;
    }

    public void setRepository(final SimpleRepository repo) {
        this.repository = repo;
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
                System.out.println("Found new instance of simplerepo!");
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
        remote = null;
    }

    public boolean useRemoteRepositoryAt(final String hostname, final int port) {
        if(repository !=null){
            if(remote != null && remoteReference!=null){
                remote.ungetRemoteService(remoteReference);
                remote.disconnect(URI.create(currentUrl));
                remoteReference=null;
                repository=null;
            }
        }
        currentUrl="r-osgi://"+hostname+":"+port;
        return connect(URI.create(currentUrl));
    }

}
