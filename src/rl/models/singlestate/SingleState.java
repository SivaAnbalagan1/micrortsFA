package rl.models.singlestate;

import java.util.ArrayList;
import java.util.List;

import burlap.mdp.core.state.State;

import rl.models.stages.GameStage;
import rl.models.stages.GameStages;
import rts.GameState;

/**
 * Provides a one-shot interaction with the environment. Thus, the game stage is either
 * {@link GameStages#OPENING} or {@link GameStages#FINISHED} 
 * @author anderson
 *
 */
public class SingleState extends GameStage {
	
	//GameStages stage;
	
	public SingleState() {
		super();
		stage = GameStages.OPENING;	
	}
	
	public SingleState(GameState gameState) {
		super(gameState);
		//TODO check if underlying state is 'gameOver' and set to FINISHED
		stage = GameStages.OPENING;		
	}

	public static GameStages frameToStage(int frameNumber){
		return GameStages.OPENING; //all frames belong to the same state
	}	

	public static List<GameStage> allStates(){
		List<GameStage> states = new ArrayList<>();
		
		states.add(new SingleState());
		
		return states;
	}
	
	@Override
	public State copy() {
		SingleState theCopy = new SingleState(getUnderlyingState().clone());
		if(this.stage == GameStages.FINISHED){
			theCopy.setStage(GameStages.FINISHED);
		}
		return theCopy;
	}
}
