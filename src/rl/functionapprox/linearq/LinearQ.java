package rl.functionapprox.linearq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import burlap.behavior.functionapproximation.sparse.StateFeature;
import rl.functionapprox.linearq.LinearQAction;
import rl.functionapprox.linearq.LinearQState;
import rl.functionapprox.linearq.LinearQStateFeatures;
import rl.functionapprox.linearq.LinearQvalues;
import rl.functionapprox.linearq.LinearVFAExtend;

public class LinearQ {
	double reward;
	LinearVFAExtend LVFA;
	LinearQStateFeatures Features;
	LinearQState s;
	LinearQvalues qValues;
			
	int weightlength;
	int featureSize;///***
	double [] weightChange,prevweightChange;
	List<LinearQAction> linearQActions = null;
	String prevAction;
	List<StateFeature> curFeature,prevFeature,gradient;
	List<double[]> weightList; double[] valueStore;
	Map<String,int []> actionFeatureidx;
	Map<String,double []> actionWeights;
	double [] weighttemp;
	public LinearQ(String [] actionNames,double [] weights,double []featValues,int featSize){
		s = new LinearQState("dummy");
		featureSize = featSize;
		weighttemp = new double [featureSize];
		weightChange = new double[featureSize];//weight update for each feature in each action
		prevweightChange = new double[featureSize];
		actionFeatureidx = new HashMap();
		actionWeights = new HashMap();
		createActions(actionNames);//create action. just once.
		//Each Action corresponds to different linear equation,
		//weight maintained for them separately.
		setupLVFA(featValues, weights,actionNames);		
	}
	public void createActions(String [] actionNames){
		linearQActions = new ArrayList<LinearQAction>();
		for(String name : actionNames)linearQActions.add(new LinearQAction(name));
	}
	public void setupLVFA(double[] featureValue,double[] weights,String [] aiNames){
		weightlength = linearQActions.size() * featureSize;
		featureValues(featureValue);//create Feature set with its values.
		setWeights(weights);//setup the feature weight.
		createQvalues();//create q-value class.Epsilon greedy will use this.
	}
	public void featureValues(double [] featureValue){
		Features = new LinearQStateFeatures(featureValue, featureSize);
		LVFA = new LinearVFAExtend(Features,0);
		LVFA.setFeatureSize(featureSize);
	}
	public void setWeights(double[] weights){
		double dummy,weight;int [] idx;int step;
		step = 0;
		//actions are added in LVFA as they are issued.
		//hence actions are issued as dummy for addition and their Ids are noted.
		for(LinearQAction qa : linearQActions){
			dummy = LVFA.evaluate(s, qa);
			idx = LVFA.getidx(qa);			
			actionFeatureidx.put(qa.actionName(), idx.clone());
			for(int i=0;i<idx.length;i++){
				weight = weights[i+step];
				LVFA.setParameter(idx[i],weight);
				weighttemp[i] = weight;
				}
			step=step+featureSize;
			actionWeights.put(qa.actionName(), weighttemp);
		}
	}
	public void createQvalues(){
		qValues = new LinearQvalues(LVFA,linearQActions);
	}
	
	public void updatefeatureLVFA(double[] featureValue){
		double [] weights;
		weights = getWeights(); //get weights from LVFA
		featureValues(featureValue); //recreate LVFA with new feature values.
        updateWeights(weights);
		createQvalues();
	}
	public double[] getWeights(){
		double[] weights = new double[weightlength];
		for(int i=0;i<weights.length;i++) {weights[i]=LVFA.getParameter(i);
		 }
		return weights;
	}

	public void updateWeights(double[] weights){
		double dummy;int [] idx;int step;
		step = 0; 
		for(LinearQAction qa : linearQActions){
			dummy = LVFA.evaluate(s, qa);
			//System.out.println("zero weight check" + dummy);
			idx = actionFeatureidx.get(qa.actionName());
			for(int i=0;i<idx.length;i++){
				LVFA.setParameter(idx[i],weights[i+step]);
				weighttemp[i] = weights[i+step];
			}
			step=step+featureSize;
			actionWeights.put(qa.actionName(), weighttemp);
		}
	}
	
}
