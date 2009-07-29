package org.infai.amor.backend.neo;

import java.util.Dictionary;
import java.util.Hashtable;

import org.neo4j.api.core.NeoService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ManagedService;

/**
 * @author sdienst
 * 
 */
public class Activator implements BundleActivator {
    private ConfigurableNeoService managedNeo;

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext bc) throws Exception {
        final Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("service.pid", NeoService.class.getName());

        managedNeo = new ConfigurableNeoService(bc);
        bc.registerService(new String[] { ManagedService.class.getName(), NeoService.class.getName() }, managedNeo, props);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext bc) throws Exception {
        managedNeo.shutdown();
    }

}
