package org.infai.amor.backend.resources;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIHelperImpl;

public class AmorXMIHelperImpl extends XMIHelperImpl {

	public AmorXMIHelperImpl() {
	}

	public AmorXMIHelperImpl(XMLResource resource) {
		super(resource);
	}

	@Override
	public void setValue(EObject object, EStructuralFeature feature,
			Object value, int position) {
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
	
	@SuppressWarnings("serial")
	protected Object createListValue(EDataType dataType, String value){
		List<String> pointList = new ArrayList<String>(1){
			
			public String toString(){
				String toString = super.toString();
				toString = toString.substring(1, toString.length() - 1);
				return toString;
			}
		};
		pointList.add(value);
		return pointList;
	}
}
