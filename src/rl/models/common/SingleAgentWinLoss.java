package rl.models.common;

import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.model.RewardFunction;
import rts.GameState;

public class SingleAgentWinLoss implements RewardFunction {

	@Override
	/**
	 * Returns the reward from the point of view of player 1
	 */
	public double reward(State s, Action a, State sprime) {
		MicroRTSState resultingState = (MicroRTSState) sprime;
		GameState underlyingState = resultingState.getUnderlyingState();
		
		if (! underlyingState.gameover()) {
			return 0.;
		}
		
		else if (underlyingState.winner() == -1) { //DRAW
			return 0;
		}
		else if (underlyingState.winner() == 0) { //victory of first player
			return 1;
		}
		else { //victory of 2nd player
			return -1;
		}
	}

}
