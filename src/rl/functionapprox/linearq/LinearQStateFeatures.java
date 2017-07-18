package rl.functionapprox.linearq;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.functionapproximation.sparse.SparseStateFeatures;
import burlap.behavior.functionapproximation.sparse.StateFeature;
import burlap.mdp.core.state.State;
import rl.models.aggregatediff.AggregateDiffState;
import rl.models.stages.GameStages;

public class LinearQStateFeatures implements SparseStateFeatures {
	public List<StateFeature> FeatureList;
	public int featureCount;
	public LinearQStateFeatures(double [] featureValue,int fCount){
		int i=0;
		featureCount = fCount;
	//epsilon greedy class uses these features values.
		FeatureList = new ArrayList<StateFeature>(); 
		for(i=0;i<featureCount;i++)
			FeatureList.add(new StateFeature(i,featureValue[i]));
	}

	@Override
	public List<StateFeature> features(State s) {
		//Feature list remains same for all states.
		return new ArrayList<StateFeature>(FeatureList);
	}

	@Override
	public SparseStateFeatures copy() {
		return this.copy();
	}

	@Override
	public int numFeatures() {
		return FeatureList.size();
	}

}
