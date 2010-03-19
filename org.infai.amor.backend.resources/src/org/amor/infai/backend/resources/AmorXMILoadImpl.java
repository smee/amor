package org.amor.infai.backend.resources;

import org.eclipse.emf.ecore.xmi.XMLHelper;
import org.eclipse.emf.ecore.xmi.impl.XMILoadImpl;
import org.xml.sax.helpers.DefaultHandler;

public class AmorXMILoadImpl extends XMILoadImpl {

	public AmorXMILoadImpl(XMLHelper helper) {
		super(helper);
	}

	protected DefaultHandler makeDefaultHandler() {
		return new AmorSAXXMIHandler(this.resource, this.helper, this.options);
	}
}
