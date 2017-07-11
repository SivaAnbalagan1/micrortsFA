package rl.models.common;

import ai.evaluation.SimpleSqrtEvaluationFunction;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import rts.GameState;

/**
 * This joint reward class gives zero rewards for intermediate states and 
 * {@link SimpleSqrtEvaluationFunction#evaluate} for terminal states.
 * 
 * @author anderson
 */
public class SimpleWeightedFeaturesTerminal extends SimpleWeightedFeatures {

	@Override
	/**
	 * Reward is zero for intermediate states, and
	 * {@link SimpleSqrtEvaluationFunction#evaluate} for terminal states.
	 * This evaluation function is the player score minus its opponent's.
	 * The score is a weighted average of some entities a player has
	 */
	public double[] reward(State s, JointAction ja, State sp) {
		MicroRTSState resultingState = (MicroRTSState) sp;
		GameState underlyingState = resultingState.getUnderlyingState();
		
		// tests for game over (reward is zero for intermediate states)
		if (! underlyingState.gameover()) {
			return new double[]{0, 0};
		}
		
		return reward(underlyingState);
		
	}

}
