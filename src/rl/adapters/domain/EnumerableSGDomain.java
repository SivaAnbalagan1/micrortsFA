package rl.adapters.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.SGDomain;

public abstract class EnumerableSGDomain extends SGDomain {

	/**
	 * Maps a name to a {@link State} object
	 */
	Map<String, State> nameToState = new HashMap<>();
	
	public EnumerableSGDomain() {
		nameToState = null;
	}
	
	/**
	 * Returns a list with ALL states in this domain
	 * @return
	 */
	public abstract List<? extends State> enumerate();
	
	/**
	 * Returns a map from state names to {@link State} objects
	 * @return
	 */
	public Map<String, State> namesToStates(){
		// creates and fills the map if it has not been initialized
		if(nameToState == null) {
			nameToState = new HashMap<>();
			
			for(State s : enumerate()){
				nameToState.put(s.toString(), s);
			}
		}
		
		return nameToState;
	}

}
