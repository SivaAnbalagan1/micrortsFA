package rl.models.common;

import ai.evaluation.SimpleSqrtEvaluationFunction;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.model.JointRewardFunction;
import rts.GameState;

/**
 * This joint reward class gives a reward based on
 * {@link SimpleSqrtEvaluationFunction#evaluate} for all states.
 * 
 * @author anderson
 */
public class SimpleWeightedFeatures implements JointRewardFunction {

	protected SimpleSqrtEvaluationFunction gameEvalFunction;
	
	public SimpleWeightedFeatures() {
		gameEvalFunction = new SimpleSqrtEvaluationFunction();
	}

	@Override
	/**
	 * Reward is {@link SimpleSqrtEvaluationFunction#evaluate} for all states.
	 * This evaluation function is the player score minus its opponent's.
	 * The score is a weighted average of some entities a player has
	 */
	public double[] reward(State s, JointAction ja, State sp) {
		MicroRTSState resultingState = (MicroRTSState) sp;
		GameState underlyingState = resultingState.getUnderlyingState();
		
		return reward(underlyingState);
		
	}

	/**
	 * Returns an array with 2 rewards. 
	 * First component is evaluation with first player as max; 
	 * Second component is evaluation with 2nd player as max
	 * @param underlyingState
	 * @return
	 */
	protected double[] reward(GameState underlyingState) {
		double[] rewards = new double[2];
		rewards[0] = gameEvalFunction.evaluate(0, 1, underlyingState);
		rewards[1] = gameEvalFunction.evaluate(1, 0, underlyingState);
		return rewards;
	}


}
