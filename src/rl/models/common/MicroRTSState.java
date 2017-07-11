package rl.models.common;

import burlap.mdp.core.state.State;
import rts.GameState;

public abstract class MicroRTSState implements State {
	
	protected GameState underlyingState;
	
	public MicroRTSState() {
		// empty constructor (java beans stuff?)
	}
	
	public MicroRTSState(GameState s){
		underlyingState = s;
	}
	
	/**
	 * Returns the underlying microRTS game state
	 * @return
	 */
	public GameState getUnderlyingState(){
		return underlyingState;
	}
	
	/**
	 * Writes all relevant information in a String and returns it
	 * @return
	 */
	public String dump(){
		String dumped = "GameState information:\n";
		dumped += underlyingState.toString();
		dumped += "\n\nPhysicalGameState information:\n";
		dumped += underlyingState.getPhysicalGameState().toString();
		
		return dumped;
	}
}
