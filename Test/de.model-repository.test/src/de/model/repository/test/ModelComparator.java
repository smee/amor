package de.model.repository.test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.eclipse.emf.compare.diff.metamodel.DiffModel;
import org.eclipse.emf.compare.diff.service.DiffService;
import org.eclipse.emf.compare.match.metamodel.MatchModel;
import org.eclipse.emf.compare.match.service.MatchService;
import org.eclipse.emf.compare.util.ModelUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

public class ModelComparator {
	//TODO weitere Vergleiche für Listen von Modellen, dann die ähnlichsten ausgeben, ...
	public DiffModel compare(EObject model1, EObject model2) throws InterruptedException {
		MatchModel match = MatchService.doMatch(model1, model2, Collections.<String, Object> emptyMap());
		return DiffService.doDiff(match, false);
	}
	
	public DiffModel compare(File model1, File model2) throws InterruptedException, IOException {
		if(model1 != null && model2 != null) {
			if(model1.exists() && model1.exists()) {
				ResourceSet rs = new ResourceSetImpl();
				EObject m1 = ModelUtils.load(model1, rs);
				EObject m2 = ModelUtils.load(model2, rs);
				
				MatchModel match = MatchService.doMatch(m1, m2, Collections.<String, Object> emptyMap());
				return DiffService.doDiff(match, false);
			}
		}
		return null;
	}
//	
//	public static void main(String[] args) {
//		ModelComparator comparator = new ModelComparator();
//		ResourceSet rs = new ResourceSetImpl();
//		try {
//			EObject model1 = ModelUtils.load(new File("out/Hello.java.xmi"), rs);
//			EObject model2 = ModelUtils.load(new File("out/Hello2.java.xmi"), rs);
//			System.out.println(comparator.compare(new File("out/Hello.java.xmi"), new File("out/Hello2.java.xmi")).getOwnedElements().size());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
}
