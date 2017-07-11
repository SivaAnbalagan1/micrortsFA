package ai.metagame;

import java.util.List;
import java.util.Random;

import rl.models.common.ScriptActionTypes;

import burlap.behavior.policy.Policy;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.debugtools.RandomFactory;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.UniversalActionType;
import burlap.mdp.core.state.State;

public class RandomPolicy implements Policy {

	List<UniversalActionType> actions;
	UniversalActionType randomAction;
	protected QLearning qLearner;
	protected Random rand = RandomFactory.getMapped(0);

	public RandomPolicy(QLearning learner) {
		this.qLearner = learner;		
	}

	@Override
	public Action action(State s) {		
		// Returns a random action 
		this.actions = ScriptActionTypes.getLearnerActionTypes();
		this.randomAction = actions.get(this.rand.nextInt(this.actions.size()));
		
		return this.randomAction.associatedAction(null);
	}

	@Override
	public double actionProb(State s, Action a) {
		return (1.0 / this.actions.size());
	}

	@Override
	public boolean definedFor(State s) {
		return true;
	}

}
