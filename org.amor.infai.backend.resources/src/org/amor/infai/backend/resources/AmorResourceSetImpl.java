package org.amor.infai.backend.resources;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

public class AmorResourceSetImpl extends ResourceSetImpl{

	public Resource createResource(URI uri, String contentType){
		final Resource.Factory resourceFactory = new AmorResourceFactoryImpl();
		Resource resource = resourceFactory.createResource(uri);
		getResources().add(resource);
		
		return resource;
	}
}
