package rl.functionapprox.linearq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
	double EPSILON = 0.1,error;//1.0 always explore and 0.0 always exploit
	double learningRate = 0.8,discountFactor = 0.9,curLR,curEpsilon,epsilonDecay;
	int time = 0;
	int featuresize;
	Action actionEpsilon,prevactionEpsilon,dummy,prevActionQ;
	String prevAction;
	List<StateFeature> curFeature,prevFeature,gradient;
	List<double[]> weightList; double[] valueStore;
//	Map<String,int []> actionFeatureidx;
//	Map<String,double []> actionWeights;
	LearningRateTimeInverse timeInverseLR,timeInverseExplore;
	LearningRateExpDecay ExpDLearningRate;
	LearningRateExpDecay ExpDEpsilon;
	Map<String, LearningRateExpDecay> actionLR;
	Map<String, Double> actionCurLR;
	Map<String, ArrayList<Double>> predError;
	double [] weighttemp = new double [featuresize];
	LinearQ LQ;
	String [] aiNamesref;
	
	public TDFA(AI [] ai, String [] aiNames,double [] featureValue,double [] weights
			,double lr, double epi, double epiDecay){
	    LQ = new LinearQ(aiNames,weights,featureValue,featureValue.length);
		featuresize = featureValue.length;
		weightChange = new double[featuresize];
		actionLR =  new HashMap();actionCurLR =  new HashMap();
		predError = new HashMap();aiNamesref = aiNames.clone();
//		curLR = lr; //set initial learning rate
		curEpsilon =epi;
		epsilonDecay = epiDecay;
//		EPSILON=0.1;
		//adding initial learning rate for all actions
		for(String action : aiNames){
			actionLR.put(action, new LearningRateExpDecay(lr,0.99907939,0.000001));
			actionCurLR.put(action, lr);
			predError.put(action, new ArrayList<Double>());
		}
	//	actionCurLR.put("RangedRush", 0.0001);
		LinearQHashState sdummy = new LinearQHashState();
		LinearQHashStateFactory hashDummy = new LinearQHashStateFactory();
//		ExpDLearningRate = new LearningRateExpDecay(.00001,0.99907939,0.00000001);
		ExpDEpsilon = new LearningRateExpDecay(curEpsilon,epsilonDecay,0.000001);
		//constant learning rate unless decayed between episodes.
		curEpsilon = ExpDEpsilon.nextLRVal(curEpsilon);
	//	timeInverseLR = new LearningRateTimeInverse(1.0,0.99907939,0.01,hashDummy,true);
	//	timeInverseExplore = new LearningRateTimeInverse(1,0.0,0.0001,hashDummy,false);
		
	}
	
	public String getAction(double [] featValue){
		//differentiating gives just feature values.
		//getting previous feature values. deltaw q(s,a,q)
		gradient= LQ.LVFA.getFeatures();

		LQ.updatefeatureLVFA(featValue);
		//New Feature values plugged-in and weight estimates from previous iteration
		//determines the best action using epsilon greedy.
		//EpsilonGreedy calls qvalues to calculate q-value for all actions
		// and returns the epsilon greedy action.
//		time = (int)featValue[featValue.length-1];
//		EPSILON = timeInverseExplore.pollLearningRate(
//				time,s,prevactionEpsilon);
//		curEpsilon = ExpDEpsilon.nextLRVal(curEpsilon);
//		System.out.println("curEpsilon " + curEpsilon);
		epslionGreedy = new EpsilonGreedy(LQ.qValues,curEpsilon);
		actionEpsilon = epslionGreedy.action(s);//get epsilon greedy action.
		//qtplus1 = LQ.qValues.qValue(s, actionEpsilon);
		qtplus1 = LQ.qValues.qValue(s, prevactionEpsilon);
		//start TD from 2nd iteration.
		if(prevAction != null) //prevFeature = new ArrayList<StateFeature>(curFeature);
		{ 
		//	reward = calcReward(featValue,prevfeatValue);
			reward = 0;//no reward shaping
		//	learningRate = timeInverseLR.pollLearningRate(
		//			time,s, prevactionEpsilon);
		//	learningRate = ExpDLearningRate.nextLRVal(curLR);
			learningRate = actionLR.get(prevAction).nextLRVal(actionCurLR.get(prevAction));
			actionCurLR.put(prevAction, learningRate);
			error = discountFactor * qtplus1 - qt;
			predError.get(prevAction).add(error);
		//	System.out.println("Action " + prevAction + "LR " + learningRate);
		//	curLR = learningRate;
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
		double threshold = 0.0001,change;
		int i;int idx[];double[] prevWeight = new double[featuresize]; double newWeight;
		i=0;
		//previous action features.
		for(StateFeature sf: gradient){
			weightChange[i] = learningRate * 
				(reward + (discountFactor * qtplus1) - qt) * sf.value;
			 //differentiation of qt is its value. X(s)
			i++;
		}
		i=0;
		idx = LQ.actionFeatureidx.get(prevAction);
		for(i=0;i<idx.length;i++)
			prevWeight[i]=LQ.LVFA.getParameter(idx[i]);
		
//		prevWeight = LQ.actionWeights.get(prevAction);
//		System.out.println("Before");
//		print(prevAction,prevWeight);
//		print(prevAction,LQ.actionWeights.get(prevAction));
		 for(i=0;i<featuresize;i++){
			 newWeight = prevWeight[i] - weightChange[i];
			 change = Math.abs(newWeight - prevWeight[i]);
			//if(newWeight > prevWeight[i]){//stopping at first minimum
			 if( change > threshold ){
				LQ.LVFA.setParameter(idx[i],newWeight);
				prevWeight[i]=newWeight;				
			}
		 }
//		 LQ.actionWeights.put(prevAction, prevWeight);//feature based weight update
//		System.out.println("After");
// 		print(prevAction,prevWeight);
//		print(prevAction,LQ.actionWeights.get(prevAction));
		 
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
	public Map<String,double []> actionWeightret(){
		return LQ.actionWeightret();
	}
	public String[] actionNames(){
		return LQ.actionNames();
	}
	public double getLearninRate(){
		return curLR;
	}
	public void setLearninRate(double lr){
		for(String action : aiNamesref){
			actionCurLR.put(action, lr);
		}
	}
	public void decayEpslion(){
		curEpsilon = ExpDEpsilon.nextLRVal(curEpsilon);
//		System.out.println("Epsilon " + curEpsilon);
	}
	public void setEpslion(double epsilon){
		curEpsilon = epsilon;
//		System.out.println("Epsilon " + curEpsilon);
	}
	public Map<String, ArrayList<Double>> getPredError(){
		return predError;
	}
	private void print(String act, double[] weight){
		System.out.println(String.format(
				"\t<a name='%s' value='%s' />\n",
				act,
				Arrays.toString(weight)));
	}
	

	
}
