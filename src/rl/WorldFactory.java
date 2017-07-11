package rl;

import java.io.IOException;

import org.jdom.JDOMException;

import burlap.mdp.core.TerminalFunction;
import burlap.mdp.stochasticgames.model.JointRewardFunction;
import burlap.mdp.stochasticgames.world.World;
import rl.models.aggregate.AggregateStateDomain;
import rl.models.aggregatediff.AggregateDifferencesDomain;
import rl.models.common.MicroRTSTerminalFunction;
import rl.models.common.WinLossRewardFunction;
import rl.models.singleagent.SingleAgentDomain;
import rl.models.singlestate.SingleStateDomain;
import rl.models.stages.GameStagesDomain;

/**
 * Utility class to load world models from their names
 * @author anderson
 *
 */
public class WorldFactory {

	
	public final static String STAGES = "stages";
	public final static String AGGREGATE = "aggregate";
	public final static String AGGREGATE_DIFF = "aggregatediff";
	public final static String SINGLE_STATE = "singlestate";
	public final static String SINGLE_AGENT = "singleagent";
	
	/**
	 * Returns a World given its name. Uses default JointRewardFunction and TerminalFunction
	 * @param modelName
	 * @return
	 */
	public static World fromString(String modelName){
		return fromString(modelName, new WinLossRewardFunction(), new MicroRTSTerminalFunction());
	}
	
	/**
	 * Returns a World given its name. Uses default TerminalFunction 
	 * @param model
	 * @param rwdFunc
	 * @return
	 */
	public static World fromString(String model,  JointRewardFunction rwdFunc){
		return fromString(model, rwdFunc, new MicroRTSTerminalFunction());
	}
	
	/**
	 * Returns a World given its name. Uses the provided reward and terminal functions
	 * @param model
	 * @param rwdFunc
	 * @param terminalFunc
	 * @return
	 */
	public static World fromString(String model, JointRewardFunction rwdFunc, TerminalFunction terminalFunc){
		if(model.equalsIgnoreCase(STAGES)){
			return stages(rwdFunc, terminalFunc);
		}
		else if(model.equalsIgnoreCase(AGGREGATE)){
			return aggregateStateFeatures(rwdFunc, terminalFunc);
		}
		else if(model.equalsIgnoreCase(AGGREGATE_DIFF)){
			return aggregateDiffStateFeatures(rwdFunc, terminalFunc);
		} 
		else if(model.equalsIgnoreCase(SINGLE_STATE)){
			return singleState(rwdFunc, terminalFunc);
		}
		else if(model.equalsIgnoreCase(SINGLE_AGENT)){
			return singleAgent(rwdFunc, terminalFunc);
		}
		
		throw new RuntimeException("Unrecognized world name: " + model);
	}
	
	private static World aggregateDiffStateFeatures(JointRewardFunction rwdFunc, TerminalFunction terminalFunc) {
		AggregateDifferencesDomain aggrDiffDomain = null;
		try {
			aggrDiffDomain = new AggregateDifferencesDomain();
		} catch (JDOMException | IOException e) {
			System.err.println("An error happened! Will exit...");
			e.printStackTrace();
			System.exit(0);
		}
		
		World w = new World(aggrDiffDomain, rwdFunc, terminalFunc, aggrDiffDomain.getInitialState());
		return w;
	}

	/**
	 * In this abstraction, microRTS game states are differentiated
	 * only by the game stage they represent (e.g. early, mid, late)
	 * Reward and terminal functions are default: {@link WinLossRewardFunction} and
	 * {@link MicroRTSTerminalFunction}
	 * TODO remove this method
	 * @return {@link World}
	 */
	public static World stages() {
		return stages(new WinLossRewardFunction(), new MicroRTSTerminalFunction());
	}
	
	public static World stages(JointRewardFunction rwdFunc, TerminalFunction terminalFunc) {
		GameStagesDomain stagesDomain = null;
		try {
			stagesDomain = new GameStagesDomain();
		} catch (JDOMException|IOException e) {
			e.printStackTrace();
		}

		World w = new World(stagesDomain, rwdFunc, terminalFunc, stagesDomain.getInitialState());
		return w;
	}
	
	/**
	 * In this abstraction, microRTS states are differentiated by
	 * quantities of entities (few, fair or many)
	 * Reward and terminal functions are default: {@link WinLossRewardFunction} and
	 * {@link MicroRTSTerminalFunction}
	 * TODO remove this method
	 * @return
	 */
	public static World aggregateStateFeatures(){
		return aggregateStateFeatures(new WinLossRewardFunction(), new MicroRTSTerminalFunction());
	}
	
	public static World aggregateStateFeatures(JointRewardFunction rwdFunc, TerminalFunction terminalFunc) {
		AggregateStateDomain aggrDomain = null;
		try {
			aggrDomain = new AggregateStateDomain();
		} catch (JDOMException | IOException e) {
			System.err.println("An error happened! Will exit...");
			e.printStackTrace();
			System.exit(0);
		}
		
		World w = new World(aggrDomain, rwdFunc, terminalFunc, aggrDomain.getInitialState());
		return w;
	}
	
	public static World singleState(JointRewardFunction rwdFunc, TerminalFunction terminalFunc) {
		SingleStateDomain singleStateDomain = null;
		try {
			singleStateDomain = new SingleStateDomain();
		} catch (JDOMException|IOException e) {
			e.printStackTrace();
		}

		World w = new World(singleStateDomain, rwdFunc, terminalFunc, singleStateDomain.getInitialState());
		return w;
	}
	
	public static World singleAgent(JointRewardFunction rwdFunc, TerminalFunction terminalFunc) {
		SingleAgentDomain singleAgentDomain = null;
		try {
			singleAgentDomain = new SingleAgentDomain();
		} catch (JDOMException|IOException e) {
			e.printStackTrace();
		}

		World w = new World(singleAgentDomain, rwdFunc, terminalFunc, singleAgentDomain.getInitialState());
		return w;
	}

}
