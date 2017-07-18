package rl.functionapprox.linearq;

import burlap.mdp.core.state.State;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;

public class LinearQHashStateFactory implements HashableStateFactory {

	public LinearQHashStateFactory() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public HashableState hashState(State s) {
		LinearQHashState lq = new LinearQHashState();
		// TODO Auto-generated method stub
		return lq;
	}

}
