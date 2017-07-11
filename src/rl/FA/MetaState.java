package rl.FA;

import java.util.List;

import burlap.mdp.core.state.State;

public class MetaState implements State {
    private String statename;
	public MetaState() {
		// TODO Auto-generated constructor stub
	}

	public MetaState(String sname) {
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
