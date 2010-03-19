package org.infai.amor.backend.resources;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.xmi.XMLHelper;
import org.eclipse.emf.ecore.xmi.XMLLoad;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

public class AmorResourceImpl extends XMIResourceImpl{

	public AmorResourceImpl(){
		super();
	}
	
	public static boolean hasGMFPackage(EObject object){
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
	
	public AmorResourceImpl(URI uri){
		super(uri);
	}
	
	protected XMLHelper createXMLHelper(){
		return new AmorXMIHelperImpl(this);
	}
	
	protected XMLLoad createXMLLoad(){
		return new AmorXMILoadImpl(createXMLHelper());
	}
	
}
