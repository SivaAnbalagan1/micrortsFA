package rl.models.common;

import burlap.mdp.stochasticgames.model.JointRewardFunction;

/**
 * Encapsulates a useful method to return a Joint Reward Fucntion given its name
 * Please do not confuse with the factory design pattern
 * @author anderson
 *
 */
public class MicroRTSRewardFactory {
	
	public static final String WIN_LOSS = "winloss";
	public static final String SIMPLE_WEIGHTED = "simpleweighted";
	public static final String SIMPLE_WEIGHTED_TERMINAL = "simpleweightedterminal";
	
	public static final String SINGLE_AGENT_WIN_LOSS = "singleagent-winloss";
	public static final String SINGLE_AGENT_WEIGHTED = "singleagent-simpleweighted";
	

	/**
	 * Returns a JointRewardFunction given its name
	 * @param functionName
	 * @return
	 */
	public static JointRewardFunction getRewardFunction(String functionName){
		
		if(functionName.equalsIgnoreCase(WIN_LOSS)){
			return new WinLossRewardFunction();
		}
		
		else if (functionName.equalsIgnoreCase(SIMPLE_WEIGHTED)){
			return new SimpleWeightedFeatures();
		}
		
		else if (functionName.equalsIgnoreCase(SIMPLE_WEIGHTED_TERMINAL)){
			return new SimpleWeightedFeaturesTerminal();
		}
		/*
		else if (functionName.equalsIgnoreCase(SINGLE_AGENT_WIN_LOSS)){
			
		}
		
		else if (functionName.equalsIgnoreCase(SINGLE_AGENT_WEIGHTED)){
			
		}*/
		
		
		// should not get here, throws an exception
		throw new RuntimeException("Unrecognized reward function name: " + functionName);
	}

}
