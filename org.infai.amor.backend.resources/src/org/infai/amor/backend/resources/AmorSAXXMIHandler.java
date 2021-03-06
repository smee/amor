package org.infai.amor.backend.resources;

import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.xmi.XMLHelper;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.SAXXMIHandler;

public class AmorSAXXMIHandler extends SAXXMIHandler {

	public AmorSAXXMIHandler(XMLResource xmiResource, XMLHelper helper,
			Map<?, ?> options) {
		super(xmiResource, helper, options);
	}

	@Override
	public void setFeatureValue(EObject object, EStructuralFeature feature,
			Object value, int position) {
		renameGMFReferences(object, feature, value);
		super.setFeatureValue(object, feature, value, position);
	}
    /**
     * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=159226
     * children is persistedChildren edges is persistedEdges
     * relativeBendpoints do not exist at all in notation.ecore.... (NotationPackage uses custom serialization code for
     * bendpoints, not generated...)
     */
	protected void renameGMFReferences(EObject object,
			EStructuralFeature feature, Object value) {
		if (AmorResourceImpl.hasGMFPackage(object) && value instanceof EReference) {
			final EReference reference = (EReference) value;
			if (reference.getName().equals("persistedChildren")) {
				reference.setName("children");
			} else if (reference.getName().equals("persistedEdges")) {
				reference.setName("edges");
			}
		}
	}
}
