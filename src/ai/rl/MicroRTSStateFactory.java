package ai.rl;

import rl.WorldFactory;
import rl.models.aggregatediff.AggregateDiffState;
import rl.models.common.MicroRTSState;
import rl.models.stages.GameStage;
import rts.GameState;

public class MicroRTSStateFactory {
	
	/**
	 * Returns a MicroRTSState object given a string with its name and an underlying state
	 * @param name can be the class name, the class simple name or a constant defined in {@link WorldFactory}
	 * @param underlying
	 * @return
	 */
	public static MicroRTSState fromString(String name, GameState underlying){
		if(name.equalsIgnoreCase(GameStage.class.getName()) || 
				name.equalsIgnoreCase(GameStage.class.getSimpleName()) || 
				name.equalsIgnoreCase(WorldFactory.STAGES)){
			return new GameStage(underlying);
		}
		else if(name.equalsIgnoreCase(AggregateDiffState.class.getName()) || 
				name.equalsIgnoreCase(AggregateDiffState.class.getSimpleName()) || 
				name.equalsIgnoreCase(WorldFactory.AGGREGATE_DIFF)){
			return new AggregateDiffState(underlying);
		}
		
		throw new RuntimeException("Unrecognized state class name: " + name);
	}
}
