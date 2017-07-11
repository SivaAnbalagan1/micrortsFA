package rl.FA;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.functionapproximation.sparse.SparseStateActionFeatures;
import burlap.behavior.functionapproximation.sparse.StateFeature;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import rl.models.stages.GameStages;

public class MetaStateActionFeatures implements SparseStateActionFeatures {
	public List<StateFeature> FeatureList;
	public static int DiffMeasureCount = 7;//as defined in AggregateDiffState Class
	public MetaStateActionFeatures() {
		// TODO Auto-generated constructor stub
	}
	public MetaStateActionFeatures(double [] featureValue){
	//epsilon greedy class uses these features values.
		FeatureList = new ArrayList<StateFeature>(); 
		//id for workers-1,light-2,ranged-3,heavy-4,bases-5,barracks-6,resources-7
		for(int i=0;i<DiffMeasureCount;i++)
			FeatureList.add(new StateFeature(i+1,featureValue[i]));
		//gamestage is added as feature list as well.
		for(int i=7;i<GameStages.values().length;i++)
			FeatureList.add(new StateFeature(i+1,featureValue[i]));
	}
	@Override
	public List<StateFeature> features(State s, Action a) {
		//Feature list and value from game are same irrespective of action
		return new ArrayList<StateFeature>(FeatureList);
	}

	@Override
	public SparseStateActionFeatures copy() {
		return this.copy();
	}

	@Override
	public int numFeatures() {
		return FeatureList.size();
	}

}
