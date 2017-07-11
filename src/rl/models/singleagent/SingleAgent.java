package rl.models.singleagent;

import rl.models.stages.GameStage;
import rl.models.stages.GameStages;
import rts.GameState;
import burlap.mdp.core.state.State;

public class SingleAgent extends GameStage {

	public SingleAgent() {
		super();
	}

	public SingleAgent(GameState gameState) {
		super(gameState);
	}
	
	@Override
	public State copy() {
		SingleAgent theCopy = new SingleAgent(getUnderlyingState().clone());
		if (this.stage == GameStages.FINISHED) {
			theCopy.setStage(GameStages.FINISHED);
		}
		return theCopy;
	}
}
