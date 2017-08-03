package rl.functionapprox.linearq;

import java.util.ArrayList;
import java.util.Arrays;
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
//			System.out.println(" Update");
//			print(qa.actionName(),weighttemp);
			actionWeights.put(qa.actionName(), weighttemp);
		}
	}
	public void createQvalues(){
		qValues = new LinearQvalues(LVFA,linearQActions);
	}
	
	public void updatefeatureLVFA(double[] featureValue){
		double[] weights = new double[weightlength];
//    	System.out.println("Before update");
//		for(int i=0;i<weightlength;i++)weights[i] = LVFA.getParameter(i);
		weights = getWeights(); //get weights from LVFA
//		print(weights);
		featureValues(featureValue); //recreate LVFA with new feature values.
//		System.out.println("After update");
        updateWeights(weights);
//		for(LinearQAction qa : linearQActions)	print(qa.actionName(),actionWeights.get(qa.actionName()));
//		for(int i=0;i<weightlength;i++)weights[i] = LVFA.getParameter(i); 
//		print(weights);
		createQvalues();
	}
	public double[] getWeights(){
		double[] weights = new double[weightlength];
		for(int i=0;i<weights.length;i++) weights[i]=LVFA.getParameter(i);
		return weights;
	}

	public void updateWeights(double [] weights){
		double dummy;int [] idx;int step;
		step = 0; 
		for(LinearQAction qa : linearQActions)dummy = LVFA.evaluate(s, qa);
		for(LinearQAction qa : linearQActions){
			idx = actionFeatureidx.get(qa.actionName());
			for(int i=0;i<idx.length;i++){
				LVFA.setParameter(idx[i],weights[idx[i]]);
			    weighttemp[i] = weights[idx[i]];
			}
//			actionWeights.put(qa.actionName(), weighttemp);
		}	
/*		for(LinearQAction qa : linearQActions){
			//System.out.println("zero weight check" + dummy);
			idx = LVFA.getidx(qa);
//			System.out.println("After ids");
			print(qa.actionName(),idx);
            Arrays.fill(weighttemp, 0);
			for(int i=0;i<idx.length;i++){
				LVFA.setParameter(idx[i],weights[idx[i]]);
				weighttemp[i] = weights[idx[i]];
//				LVFA.setParameter(idx[i],weights[i+step]);
//				weighttemp[i] = weights[i+step];
			}
			step=step+featureSize;
//			System.out.println(" Update");
			print(qa.actionName(),weighttemp);
			actionWeights.put(qa.actionName(), weighttemp);
//			print(qa.actionName(),weighttemp);

		}*/
	}
	public Map<String,double []> actionWeightret(){
		int [] idx; double [] weights = new double[weightlength];
		double [] weightstemp = new double[featureSize];
		actionWeights.clear();
		
		for(int i=0;i<weights.length;i++) weights[i]=LVFA.getParameter(i);
	      for(LinearQAction qa : linearQActions){
			idx = actionFeatureidx.get(qa.actionName());
			for(int i=0;i<idx.length;i++)weightstemp[i] = weights[idx[i]];
			actionWeights.put(qa.actionName(), weightstemp.clone());
		}
	  	
		return actionWeights;
	}
	public String[] actionNames(){
		String[] actname = new String[linearQActions.size()];
		int i=0;
		for(LinearQAction qa : linearQActions){
            actname[i] = qa.actionName();i++;}
		return actname;
	}
	private void print(String act, int[] weight){
		System.out.println(String.format(
				"\t<a name='%s' value='%s' />\n",
				act,
				Arrays.toString(weight)));
	}
	private void print(String act, double[] weight){
		System.out.println(String.format(
				"\t<a name='%s' value='%s' />\n",
				act,
				Arrays.toString(weight)));
	}

	private void print(double[] weight){
		System.out.println(String.format(
				"\t< value='%s' />\n",
				Arrays.toString(weight)));
	}
}
