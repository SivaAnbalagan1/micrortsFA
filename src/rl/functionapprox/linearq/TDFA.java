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
	double EPSILON,error;//1.0 always explore and 0.0 always exploit
	double discountFactor = 0.9,curLR,curEpsilon,epsilonDecay,learningRate;
	LearningRateExpDecay expDecayLRate; 
	int time = 0;
	int featuresize;
	Action actionEpsilon,prevactionEpsilon,dummy,prevActionQ;
	String prevAction;
	List<StateFeature> curFeature,prevFeature,gradient;
	List<double[]> weightList; double[] valueStore;
	LearningRateExpDecay ExpDLearningRate;
	LearningRateExpDecay ExpDEpsilon;
	Map<String, LearningRateExpDecay> actionLR;
	Map<String, Double> actionCurLR;
	Map<String, ArrayList<Double>> predError;
	Map<String, Map<String,ArrayList<Double>>> SGD;
	Map<String, ArrayList<Double>> weightchange;
	double [] weighttemp = new double [featuresize];
	LinearQ LQ;
	String [] aiNamesCopy;
	double convergenceValue;
	Map<String, String> convergence;
	String actiontemp;
	public TDFA(String [] aiNames,double [] featureValue,double [] weights
			,LearningRateExpDecay lr, double epi, double epiDecay){
	    LQ = new LinearQ(aiNames,weights,featureValue,featureValue.length);
		featuresize = featureValue.length;
		weightChange = new double[featuresize];
		actionLR =  new HashMap();actionCurLR =  new HashMap();
		predError = new HashMap();aiNamesCopy = aiNames.clone();
		weightchange = new HashMap();convergence = new HashMap();
		SGD = new HashMap();
		expDecayLRate = lr;
//		curLR = lr; //set initial learning rate
		curEpsilon =epi;
		epsilonDecay = epiDecay;
		//adding initial learning rate for all actions
		for(String action : aiNames){
			LearningRateExpDecay lrdummy = new LearningRateExpDecay(0.1,0.1);
			lrdummy = expDecayLRate; 
			actionLR.put(action, lrdummy);
			actionCurLR.put(action,lrdummy.getinitialRate());
			predError.put(action, new ArrayList<Double>());
		}
		LinearQHashState sdummy = new LinearQHashState();
		LinearQHashStateFactory hashDummy = new LinearQHashStateFactory();

		ExpDEpsilon = new LearningRateExpDecay(curEpsilon,epsilonDecay,0.000001);
		//constant Epsilon unless decayed between episodes.
		curEpsilon = ExpDEpsilon.nextLRVal(curEpsilon);
	
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
		if(prevAction != null)
				//&& convergence.get(prevAction) != null) //prevFeature = new ArrayList<StateFeature>(curFeature);
		{ 
		//	reward = calcReward(featValue,prevfeatValue);
			reward = 0;//no reward shaping
		//	learningRate = timeInverseLR.pollLearningRate(
		//			time,s, prevactionEpsilon);
		//	learningRate = ExpDLearningRate.nextLRVal(curLR);
			learningRate = actionLR.get(prevAction).nextLRVal(actionCurLR.get(prevAction));
		//	learningRate = curLR;
			actionCurLR.put(prevAction, learningRate);
			error = qtplus1 - qt;
			predError.get(prevAction).add(error);
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
		double threshold = 0.000001,change;
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
		 for(i=0;i<featuresize;i++){
			 newWeight = prevWeight[i] - weightChange[i];
	//		 double mse = Math.sqrt(newWeight - prevWeight[i])/2;
	//		 MSE.get(prevAction).get(Integer.toString(i)).add(mse);
			 change = Math.abs(newWeight - prevWeight[i]);
			 if( change > threshold ){
				LQ.LVFA.setParameter(idx[i],newWeight);
		//		SGD.get(prevAction).get(Integer.toString(i)).add(newWeight);
				prevWeight[i]=newWeight;				
			}
		 }
		 
	}
	/*private double calcReward(double[] features,double[] prevfeat){
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
	}*/
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
		for(String action : aiNamesCopy){
			actionCurLR.put(action, lr);
		}
	}
	public void decayEpslion(){
		curEpsilon = ExpDEpsilon.nextLRVal(curEpsilon);
	}
	public void setEpslion(double epsilon){
		curEpsilon = epsilon;
	}
	public Map<String, ArrayList<Double>> getPredError(){
		return predError;
	}
	public Map<String, Map<String,ArrayList<Double>>> getSGD(){
		return SGD;
	}
	public int[] getIdx(String action){
		return LQ.actionFeatureidx.get(action);
	}
	private void print(String act, double[] weight){
		System.out.println(String.format(
				"\t<a name='%s' value='%s' />\n",
				act,
				Arrays.toString(weight)));
	}
	
}
