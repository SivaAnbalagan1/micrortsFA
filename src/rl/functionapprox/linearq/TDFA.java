package rl.functionapprox.linearq;

import java.util.List;

import java.util.Map;

import ai.core.AI;
import burlap.behavior.functionapproximation.sparse.StateFeature;
import burlap.behavior.policy.EpsilonGreedy;
import burlap.mdp.core.action.*;
import burlap.behavior.learningrate.SoftTimeInverseDecayLR;
import burlap.behavior.learningrate.ExponentialDecayLR;

public class TDFA {
	double reward;
	EpsilonGreedy epslionGreedy;
	LinearQState s;
		
	double [] weightChange,prevweightChange,prevfeatValue;
	double qt,qtplus1;//ToDo - change to list for multi-step 
	double EPSILON = 0.5;//1.0 always explore and 0.0 always exploit
	double learningRate = 1.0,discountFactor = 0.9,curLR;
	int time = 0;
	int featuresize;
	Action actionEpsilon,prevactionEpsilon,dummy;
	String prevAction;
	List<StateFeature> curFeature,prevFeature,gradient;
	List<double[]> weightList; double[] valueStore;
	Map<String,int []> actionFeatureidx;
	Map<String,double []> actionWeights;
	LearningRateTimeInverse timeInverseLR,timeInverseExplore;
	LearningRateExpDecay ExpDLearningRate;
	double [] weighttemp = new double [featuresize];
	LinearQ LQ;
	
	public TDFA(AI [] ai, String [] aiNames,double [] featureValue,double [] weights
			){
	    LQ = new LinearQ(aiNames,weights,featureValue,featureValue.length);
		featuresize = featureValue.length;
		weightChange = new double[featuresize];
		curLR = 1.0; //set initial learning rate
		LinearQHashState sdummy = new LinearQHashState();
		LinearQHashStateFactory hashDummy = new LinearQHashStateFactory();
		ExpDLearningRate = new LearningRateExpDecay(1.0,0.99907939,0.01); 
		
	//	timeInverseLR = new LearningRateTimeInverse(1.0,0.99907939,0.01,hashDummy,true);
	//	timeInverseExplore = new LearningRateTimeInverse(1,0.0,0.0001,hashDummy,false);
		
	}
	
	public String getAction(double [] featValue){

		gradient= LQ.LVFA.getFeatures();//differentiating gives just feature values.

		LQ.updatefeatureLVFA(featValue);
		//New Feature values plugged-in and weight estimates from previous iteration
		//determines the best action using epsilon greedy.
		//EpsilonGreedy calls qvalues to calculate q-value for all actions
		// and returns the epsilon greedy action.
//		time = (int)featValue[featValue.length-1];
//		EPSILON = timeInverseExplore.pollLearningRate(
//				time,s,prevactionEpsilon);
		epslionGreedy = new EpsilonGreedy(LQ.qValues,EPSILON);
		actionEpsilon = epslionGreedy.action(s);//get epsilon greedy action.
		qtplus1 = LQ.qValues.qValue(s, actionEpsilon);
		//start TD from 2nd iteration.
		if(prevAction != null) //prevFeature = new ArrayList<StateFeature>(curFeature);
		{ 
			reward = calcReward(featValue,prevfeatValue);
		//	learningRate = timeInverseLR.pollLearningRate(
		//			time,s, prevactionEpsilon);
			learningRate = ExpDLearningRate.nextLRVal(curLR);
			curLR = learningRate;
        	calcTDWeight();
		}
    	qt = qtplus1;
		//prevFeature = sfList;
    	prevactionEpsilon = actionEpsilon;
    	prevAction = actionEpsilon.actionName();
    	prevfeatValue = featValue;
        return actionEpsilon.actionName();//implement the best action in microRTS.		
	}
	private void calcTDWeight(){
		int i;int idx[];double[] prevWeight; double newWeight;
		i=0;
		
		for(StateFeature sf: gradient){
			weightChange[i] = learningRate * 
				 (reward + (discountFactor * qtplus1) - qt) * sf.value;
			i++;
		}
		i=0;
		idx = LQ.actionFeatureidx.get(prevAction);
		prevWeight = LQ.actionWeights.get(prevAction);
		 for(i=0;i<featuresize;i++){
			 newWeight = prevWeight[i] - weightChange[i]; 
			if(newWeight < prevWeight[i]){//stopping at first minimum
				LQ.LVFA.setParameter(idx[i],newWeight);
				prevWeight[i]=newWeight;				
			}
		 }
		 LQ.actionWeights.put(prevAction, prevWeight);//feature based weight update
	}
	private double calcReward(double[] features,double[] prevfeat){
		double oppResCur,oppResPrev,playerResCur,playerResPrev,rateOpp,ratePlayer,r;
		oppResCur=0;oppResPrev=0;playerResCur=0;playerResPrev=0;r=0;
		for(int i=features.length/2;i<features.length;i++)
			oppResCur = oppResCur + features[i];
		for(int i=features.length/2;i<features.length;i++)
			oppResPrev = oppResPrev + features[i];
		rateOpp = oppResPrev/oppResCur;  
		
		for(int i=0;i<features.length/2;i++)
			playerResCur = playerResCur + features[i];
		for(int i=0;i<features.length/2;i++)
			playerResPrev = playerResPrev + features[i];
		
		ratePlayer = playerResPrev/playerResCur;
		
		r = rateOpp*4 + ratePlayer*-3;
		return r;
	}
	
}
