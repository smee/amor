package org.infai.amor.backend.resources;

import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

public class AmorResourceSetImpl extends ResourceSetImpl{

    public AmorResourceSetImpl() {
        super();
        getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new AmorResourceFactoryImpl());
        // EMF assumes we run in an eclipse environment all the time
        // but there is no way of resolving platform:/plugin uris when being standalone
        getPackageRegistry().put("platform:/plugin/org.eclipse.emf.ecore/model/Ecore.ecore", EcorePackage.eINSTANCE);
        getPackageRegistry().put("http://www.eclipse.org/emf/2002/Ecore", EcorePackage.eINSTANCE);

        // getURIConverter().getURIMap().put(URI.createURI("platform:/plugin/org.eclipse.emf.ecore/model/Ecore.ecore"),
        // URI.createURI("http://www.eclipse.org/emf/2002/Ecore"));
    }
}
