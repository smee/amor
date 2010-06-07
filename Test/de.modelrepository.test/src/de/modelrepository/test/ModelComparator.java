package de.modelrepository.test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.eclipse.emf.compare.diff.metamodel.DiffModel;
import org.eclipse.emf.compare.diff.service.DiffService;
import org.eclipse.emf.compare.epatch.Epatch;
import org.eclipse.emf.compare.epatch.diff.DiffEpatchService;
import org.eclipse.emf.compare.match.metamodel.MatchModel;
import org.eclipse.emf.compare.match.service.MatchService;
import org.eclipse.emf.compare.util.ModelUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

public class ModelComparator {
    private MatchModel match;
    private DiffModel diff;
    /**
     * Compares two models, each given as an {@link EObject}.
     * @param model1 first model
     * @param model2 second model
     * @return a {@link DiffModel} which contains all differences between both models.
     */
    public DiffModel compare(EObject model1, EObject model2) throws InterruptedException {
        match = MatchService.doMatch(model1, model2, Collections.<String, Object> emptyMap());
        // diff = DiffService.doDiff(match, false);
        diff = new WorkaroundDiffEngine().doDiff(match, false);
        return diff;
    }

    /**
     * Method compares two emf model files.
     * @param model1 first model
     * @param model2 second model
     * @return a {@link DiffModel} which contains all differences between both models.
     */
    public DiffModel compare(File model1, File model2) throws InterruptedException, IOException {
        if(model1 != null && model2 != null) {
            if(model1.exists() && model1.exists()) {
                ResourceSet rs = new ResourceSetImpl();
                EObject m1 = ModelUtils.load(model1, rs);
                EObject m2 = ModelUtils.load(model2, rs);

                return compare(m1, m2);
            }
        }
        return null;
    }

    /**
     * This method creates an Epatch from the previously parsed model files.
     * @return the created Epatch.
     */
    public Epatch getEpatch() {
        if(match == null || diff == null) {
            return null;
        }
        return DiffEpatchService.createEpatch(match, diff, "patch");
    }

    /**
     * This method compares two resources instead of EObjects.
     * @param r1 the first resource (source)
     * @param r2 the second resource (target)
     * @return the difference model.
     */
    public DiffModel compare(Resource r1, Resource r2) throws InterruptedException {
        match = MatchService.doResourceMatch(r1, r2, null);
        diff = DiffService.doDiff(match, false);
        return diff;
    }
}
