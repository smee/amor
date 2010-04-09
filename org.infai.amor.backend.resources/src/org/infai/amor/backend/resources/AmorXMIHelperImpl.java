package org.infai.amor.backend.resources;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIHelperImpl;

public class AmorXMIHelperImpl extends XMIHelperImpl {

	public AmorXMIHelperImpl() {
	}

	public AmorXMIHelperImpl(final XMLResource resource) {
		super(resource);
	}

	@SuppressWarnings("serial")
	protected Object createListValue(final EDataType dataType, final String value){
		final List<String> pointList = new ArrayList<String>(1){
			
			@Override
            public String toString(){
				String toString = super.toString();
				toString = toString.substring(1, toString.length() - 1);
				return toString;
			}
		};
		pointList.add(value);
		return pointList;
	}
	
	@Override
	public EObject createObject(final EFactory eFactory, final EClassifier type){
	    // TODO remove EModelElement from supertypes, results in NPE when instantiating this class
//        if(type instanceof EClass){
//            EClass eclass=(EClass) type;
//            eclass.getEAllSuperTypes().remove(EcorePackage.eINSTANCE.getEModelElement());
//        }
        return super.createObject(eFactory, type);
	    
	}
	
	@Override
	public void setValue(final EObject object, final EStructuralFeature feature,
			final Object value, final int position) {
		if(object.eClass().getName().equals("RelativeBendpoints") && 
				feature instanceof EAttribute && 
				feature.getName().equals("points") &&
				AmorResourceImpl.hasGMFPackage(object.eClass())){
			final EDataType eClassifier = (EDataType) feature.getEType();
			object.eSet(feature, createListValue(eClassifier, (String) value));
		}
		else{
			super.setValue(object, feature, value, position);
		}
	}
}
