package rl.models.common;

import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.state.State;
import rl.RLParamNames;
import rl.RLParameters;
import rts.GameState;

public class MicroRTSTerminalFunction implements TerminalFunction {
	
	int timeLimit;
	
	public MicroRTSTerminalFunction() {
		// retrieves timeLimit from parameters 
		timeLimit = (int) RLParameters.getInstance().getParameter(RLParamNames.GAME_DURATION);
	}

	@Override
	/**
	 * A MicroRTS game finishes when one player loses (gameover) or 
	 * when it runs out of time (getTime > timeLimit)
	 */
	public boolean isTerminal(State s) {
		MicroRTSState state = (MicroRTSState) s;
		GameState underlying = state.getUnderlyingState();
		
		return underlying.getTime() >= timeLimit || underlying.gameover();
	}

}
