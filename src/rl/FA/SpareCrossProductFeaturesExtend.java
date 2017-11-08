package rl.FA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import burlap.behavior.functionapproximation.sparse.SparseCrossProductFeatures;
import burlap.behavior.functionapproximation.sparse.SparseStateFeatures;
import burlap.behavior.functionapproximation.sparse.StateFeature;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;

public class SpareCrossProductFeaturesExtend extends SparseCrossProductFeatures {
	public Map<Integer, Integer> featureWeightidx = new HashMap();
	public Map<String,Map<Integer,Integer>> actionFeature = new HashMap();

	public SpareCrossProductFeaturesExtend(SparseStateFeatures sFeatures) {
		super(sFeatures);		
		// TODO Auto-generated constructor stub
	}

	public SpareCrossProductFeaturesExtend(SparseStateFeatures sFeatures, Map<Action, FeaturesMap> actionFeatures,
			int nextFeatureId) {
		super(sFeatures, actionFeatures, nextFeatureId);
		// TODO Auto-generated constructor stub
	}
	@Override
	public List<StateFeature> features(State s, Action a) {
		List<StateFeature> sfs = super.sFeatures.features(s);
		List safs = new ArrayList(sfs.size());
		for (StateFeature sf : sfs) {
			StateFeature saf = new StateFeature(actionFeature(a, sf.id), sf.value);
			safs.add(saf);
			featureWeightidx.put(sf.id, saf.id);
			actionFeature.put(a.actionName(),featureWeightidx);
		}
		return safs;
	
	}

}
