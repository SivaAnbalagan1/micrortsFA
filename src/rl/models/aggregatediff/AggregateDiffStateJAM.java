package rl.models.aggregatediff;

import java.util.Map;

import ai.core.AI;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.model.JointModel;
import rl.RLParamNames;
import rl.RLParameters;
import rl.models.stages.GameStages;
import rts.GameState;
import rts.PlayerAction;

public class AggregateDiffStateJAM implements JointModel {
	
	Map<String, AI> actions;
	
	private int maxCycles;
	
	public AggregateDiffStateJAM(Map<String, AI> actions) {
		maxCycles = (int) RLParameters.getInstance().getParameter(RLParamNames.GAME_DURATION);
		this.actions = actions;
	}

	@Override
	public State sample(State s, JointAction ja) {
		//TODO = this seems to be common to all jointActionModels of Aggregate States 
		AggregateDiffState currentState = (AggregateDiffState)s;
		
		GameState gameState = currentState.getUnderlyingState().clone();
		
		boolean gameOver = false;
		boolean changedStage = false;	//stores whether game has moved to a different AggregateDiffState
		
		// instantiates the AIs that players selected (clones the objects)
		AI ai1 = actions.get(ja.action(0).actionName()).clone();
		AI ai2 = actions.get(ja.action(1).actionName()).clone();

		// advance game until next state is reached or game finishes
		do {
			PlayerAction pa1 = null, pa2 = null;
			try {
				pa1 = ai1.getAction(0, gameState);
				pa2 = ai2.getAction(1, gameState);
				
			} catch (Exception e) {
				System.err.println("An error happened when getting action for a player :(");
				e.printStackTrace();
			}
			
			gameState.issueSafe(pa1);
			gameState.issueSafe(pa2);

			// simulate:
			gameOver = gameState.cycle();
			
			//checks whether any state variable has changed
			changedStage = ! currentState.equals(new AggregateDiffState(gameState)); 
				
		} while (!gameOver && !changedStage && gameState.getTime() < maxCycles);
		
		//returns the new State associated with current underlying game state
		AggregateDiffState newState = new AggregateDiffState(gameState);
		
		//timeout is not checked inside GameStage constructor, set finished here
		if(gameState.getTime() >= maxCycles){
			newState.setStage(GameStages.FINISHED);
		}
		return newState; 
	}

}
