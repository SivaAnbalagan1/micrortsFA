package rl.models.aggregate;

import java.util.Map;

import ai.core.AI;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.model.JointModel;
import rts.GameState;
import rts.PlayerAction;

public class AggregateStateJAM implements JointModel {


	Map<String, AI> actions;
	
	public static final int MAXCYCLES = 3000;
	
	public AggregateStateJAM(Map<String, AI> actions) {
		this.actions = actions;
	}

	@Override
	public State sample(State s, JointAction ja) {
		AggregateState currentState = (AggregateState)s;
		
		GameState gameState = currentState.getUnderlyingState().clone();
		
		boolean gameOver = false;
		boolean changedStage = false;	//stores whether game has moved to a different AggregateState
		
		// instantiates the AIs that players selected (clones the objects)
		AI ai1 = actions.get(ja.action(0).actionName()).clone();
		AI ai2 = actions.get(ja.action(1).actionName()).clone();

		// advance game until next stage is reached or game finishes
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
			changedStage = ! currentState.equals(new AggregateState(gameState)); 
				
		} while (!gameOver && !changedStage && gameState.getTime() < MAXCYCLES);
		
		//returns the new State associated with current underlying game state
		return new AggregateState(gameState); 
	}

}
