package rl.functionapprox.linearq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.behavior.functionapproximation.sparse.LinearVFA;
import burlap.behavior.functionapproximation.sparse.SparseCrossProductFeatures;
import burlap.behavior.functionapproximation.sparse.SparseStateFeatures;
import burlap.behavior.functionapproximation.sparse.StateFeature;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;

public class LinearVFAExtend extends LinearVFA {
	Map<String,int []> actionFeatureidx;
	int featureSize;
	public LinearVFAExtend(SparseStateFeatures sparseStateFeatures) {
		super(sparseStateFeatures);
		actionFeatureidx = new HashMap();
	}

	public LinearVFAExtend(SparseStateFeatures sparseStateFeatures, double defaultWeight) {
		super(sparseStateFeatures, defaultWeight);
		actionFeatureidx = new HashMap();
	}
	@Override
	public double evaluate(State s, Action a) {
		int [] idx;int i=0;
		idx = new int[featureSize];		
		List<StateFeature> features = this.stateActionFeatures.features(s, a);
		double val = 0.0D;
		for (StateFeature sf : features) {
			double prod = sf.value * getWeight(sf.id);
			idx[i] = sf.id;i++;
			val += prod;
		}
		super.currentValue = val;
		super.currentGradient = null;
		super.currentFeatures = features;
		super.lastState = s;
		super.lastAction = a;
		actionFeatureidx.put(a.actionName(), idx.clone());
		return val;
	}
	public int[] getidx(Action a) {
		return actionFeatureidx.get(a.actionName());		
	}
	public void setFeatureSize(int fSize) {
		featureSize = fSize;		
	}
	public List<StateFeature> getFeatures(){
		return super.currentFeatures;
	} 
	public void updateFeatureValue(List<StateFeature> sf){
	///to do	
	}
}
