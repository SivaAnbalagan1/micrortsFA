package rl.functionapprox.linearq;

import java.util.List;

import burlap.mdp.core.state.State;

public class LinearQState implements State {
    private String statename;
	public LinearQState() {
		// TODO Auto-generated constructor stub
	}

	public LinearQState(String sname) {
	//dummy- cannot define all states...
     statename = sname;
	}

	@Override
	public List<Object> variableKeys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object get(Object variableKey) {
	//dummy	
		return statename;
	}

	@Override
	public State copy() {
		// TODO Auto-generated method stub
		return null;
	}

}
