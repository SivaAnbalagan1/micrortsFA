package rl.functionapprox.linearq;

import burlap.mdp.core.action.Action;

public class LinearQAction implements Action {
	private String actName; 
	LinearQAction(String actionName){
		actName = actionName;
	}

	@Override
	public String actionName() {
		return actName;
	}

	@Override
	public LinearQAction copy() {
		return this.copy();
	}

}
