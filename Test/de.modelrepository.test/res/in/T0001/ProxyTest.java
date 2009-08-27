package de.modelrepository.test.testcases;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;


public class ProxyTest {
	public static void main(String[] args) {
		ResourceSet set = new ResourceSetImpl();
		Resource res = set.getResources().get(0);
	}
}
