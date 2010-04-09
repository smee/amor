package org.infai.amor.backend.resources;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.xmi.XMLHelper;
import org.eclipse.emf.ecore.xmi.XMLLoad;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

public class AmorResourceImpl extends XMIResourceImpl{

	public static boolean hasGMFPackage(final EObject object){
		boolean correctPackage = false;
		if(object instanceof EClass){
			final EClass eClazz = (EClass) object;
			final EPackage ePackage = eClazz.getEPackage();
			if(ePackage != null && ePackage.getNsURI().startsWith(
					"http://www.eclipse.org/gmf/runtime/")){
				correctPackage = true;
			}
		}
		return correctPackage;
	}
	
	public AmorResourceImpl(){
		super();
	}
	
	public AmorResourceImpl(final URI uri){
		super(uri);
	}
	
	@Override
    protected XMLHelper createXMLHelper(){
		return new AmorXMIHelperImpl(this);
	}
	
	@Override
    protected XMLLoad createXMLLoad(){
		return new AmorXMILoadImpl(createXMLHelper());
	}
	
}
