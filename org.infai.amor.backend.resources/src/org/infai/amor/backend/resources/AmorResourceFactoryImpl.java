package org.infai.amor.backend.resources;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;

public class AmorResourceFactoryImpl extends ResourceFactoryImpl{

	public Resource createResource(URI uri){
		return new AmorResourceImpl(uri);
	}
}
