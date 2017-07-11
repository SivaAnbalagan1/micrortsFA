package rl.FA;

import burlap.mdp.core.action.Action;

public class MetaAction implements Action {
	private String actName; 
	MetaAction(String actionName){
		actName = actionName;
	}

	@Override
	public String actionName() {
		return actName;
	}

	@Override
	public Action copy() {
		return this.copy();
	}

}
