package rl.functionapprox.linearq;

import burlap.mdp.core.state.State;
import burlap.statehashing.HashableState;

public class LinearQHashState implements HashableState {

	public LinearQHashState() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public State s() {
		// TODO Auto-generated method stub
		LinearQState s = new LinearQState();
		return s;
	}

}
