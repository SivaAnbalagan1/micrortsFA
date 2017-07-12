package rl.FA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.core.AI;
import burlap.behavior.functionapproximation.FunctionGradient;
import burlap.behavior.functionapproximation.sparse.LinearVFA;
import burlap.behavior.functionapproximation.sparse.SparseStateFeatures;
import burlap.behavior.functionapproximation.sparse.StateFeature;
import burlap.behavior.policy.EpsilonGreedy;
import burlap.mdp.core.action.*;
import burlap.mdp.core.state.State;

public class TDlinearFA {
	double reward;
	LinearVFAExtend LVFA;
	MetaStateFeatures Features;
	MetaState s;
	MetaQvalues metaQvalues;
	EpsilonGreedy epslionGreedy;
		
	int weightlength;
	int featuresize = 8;///***
	double [] weightChange,prevweightChange;
	double qt,qtplus1;//ToDo - change to list for multi-step 
	double EPSILON = 0.4;//1.0 always explore and 0.0 always exploit
	double learningRate = 0.1,discountFactor = 0.9;
	double featureValue;
	List<MetaAction> metaActions = null;
	Action actionEpsilon,actionEpsilon1;
	String prevAction;
	List<StateFeature> curFeature,prevFeature,gradient;
	List<double[]> weightList; double[] valueStore;
	Map<String,int []> actionFeatureidx;
	Map<String,double []> actionWeights;
	double [] weighttemp = new double [featuresize];
	public TDlinearFA(AI [] ai, String [] aiNames,double [] weights,double []fValues){
		s = new MetaState("dummy");
		weightChange = new double[featuresize];//weight update for each feature in each action
		prevweightChange = new double[featuresize];
		actionFeatureidx = new HashMap();
		actionWeights = new HashMap();
		createMetaActions(aiNames);//create meta action. just once.
		//meta actions are  playing different AIs
		valueStore = fValues;//8 features.
		setupLVFA(valueStore, weights,aiNames);		
	}
	public void createMetaActions(String [] aiNames){
		metaActions = new ArrayList<MetaAction>();
		for(String name : aiNames)metaActions.add(new MetaAction(name));
	}
	public void setupLVFA(double[] featureValue,double[] weights,String [] aiNames){
		weightlength = metaActions.size() * featuresize;
		featureValues(featureValue);//create Feature set with its values.
		setWeights(weights);//setup the feature weight.
		createmetaQvalues();//create q-value class.Epsilon greedy will use this.
		//weightList = new ArrayList<>();
	}
	public void updatefeatureLVFA(double[] featureValue){
		double [] weights;
		weights = getWeights(); //get weights from LVFA
		featureValues(featureValue); //recreate LVFA with new feature values.
        updateWeights(weights);
		createmetaQvalues();
	/*	System.out.println("Next iteration");
		for(StateFeature sf: LVFA.getFeatures())
			System.out.println(sf.id + " " + sf.value);*/

	}
	public void featureValues(double [] featureValue){
		Features = new MetaStateFeatures(featureValue);
		LVFA = new LinearVFAExtend(Features,0);
	}
	public void setWeights(double[] weights){
		double dummy;int [] idx;int step;
		step = featuresize;
		//actions are added in LVFA as they are issued.
		//hence actions are issued as dummy for addition and their Ids are noted.
		for(MetaAction ma : metaActions){
			dummy = LVFA.evaluate(s, ma);
			idx = LVFA.getidx(ma);			
			actionFeatureidx.put(ma.actionName(), idx.clone());
			for(int i=0;i<idx.length;i++){
				LVFA.setParameter(idx[i],weights[i+step]);
				weighttemp[i] = weights[i+step];
				}
			actionWeights.put(ma.actionName(), weighttemp);
		}
	}
	
	public void updateWeights(double[] weights){
		double dummy;int [] idx = new int [8];int step;
		step = featuresize;
		for(MetaAction ma : metaActions){
			dummy = LVFA.evaluate(s, ma);
			//System.out.println("zero weight check" + dummy);
			idx = actionFeatureidx.get(ma.actionName());
			for(int i=0;i<idx.length;i++){
				LVFA.setParameter(idx[i],weights[i+step]);
				weighttemp[i] = weights[i+step];
			}
			actionWeights.put(ma.actionName(), weighttemp);
		}
	}
	public double[] getWeights(){
		double[] weights = new double[weightlength];
		for(int i=0;i<weights.length;i++) {weights[i]=LVFA.getParameter(i);
		 }
		return weights;
	}
	public void createmetaQvalues(){
		metaQvalues = new MetaQvalues(LVFA,metaActions);
	}
	public String getAction(List<StateFeature> sfList){
		double []featValue = new double[featuresize];
		//set feature values from the game.
		curFeature = sfList;
		for(StateFeature sf: curFeature)featValue[sf.id-1] = sf.value;
		
		gradient= LVFA.getFeatures();//differentiating gives just feature values.

	    updatefeatureLVFA(featValue);
		//New Feature values plugged-in and weight estimates from previous iteration
		//determines the best action using epsilon greedy.
		//EpsilonGreedy calls metaQvalues to calculate q-value for all actions
		// and returns the epsilon greedy action.
		epslionGreedy = new EpsilonGreedy(metaQvalues,EPSILON);
		actionEpsilon = epslionGreedy.action(s);//get epsilon greedy action.
		qtplus1 = metaQvalues.qValue(s, actionEpsilon);
		//start TD from 2nd iteration.
		if(prevFeature != null) //prevFeature = new ArrayList<StateFeature>(curFeature);
		{ 
			reward = calcReward(prevFeature,curFeature);
        	calcTDWeight();
		}
    	qt = qtplus1;
		prevFeature = sfList;
    	prevAction = actionEpsilon.actionName();
        return actionEpsilon.actionName();//implement the best action in microRTS.		
	}
	private void calcTDWeight(){
		int i;int idx[];double[] prevWeight; double newWeight;
		i=0;
		for(StateFeature sf: gradient){
			weightChange[i] = learningRate * 
				 (reward + (discountFactor * qtplus1) - qt) * sf.value;
		//	System.out.println(weightChange[i] + " " + qtplus1 + " " + qt + " " + sf.value);
			i++;
		}
		i=0;
		idx = actionFeatureidx.get(prevAction);
		prevWeight = actionWeights.get(prevAction);
		 for(i=0;i<featuresize;i++){
//			System.out.println(weightChange[i] + " " + prevWeight[i]);
			 newWeight = prevWeight[i] - weightChange[i]; 
			if(newWeight < prevWeight[i]){//stopping at first minimum
				LVFA.setParameter(idx[i],newWeight);
				System.out.println("id: " + idx[i] + " " + newWeight );
				prevWeight[i]=newWeight;				
			}
		 }
		 actionWeights.put(prevAction, prevWeight);//feature based weight update
	}
	private int calcReward(List<StateFeature> prev,List<StateFeature> cur){
		int rtemp=0,r=0;
		for(StateFeature sf : cur){
			  for(StateFeature sf1 : prev){ 
				  if(sf.id==sf1.id){
					  if(sf.value > sf1.value) rtemp = 1; //resource increase compared to previous state
					  else if (sf.value < sf1.value) rtemp = -2;//resource decrease compared to previous state 
					  else rtemp=-1;//no change in resource.
				  }
			  }
			  r=r+rtemp;
		}
		return r;
	}
	public void setInitialFeature(List<StateFeature> sfList){
		prevFeature = sfList;//featureValueUpdate(sfList);
	}
	
}
