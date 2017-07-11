package ai.metagame;

import java.util.List;

import burlap.behavior.policy.Policy;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.valuefunction.QValue;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;

/**
 * A policy that never learns, i.e., always returns the same pre-defined behavior
 * @author anderson
 *
 */
public class DummyPolicy implements Policy {
	
	protected String behavior;
	protected QLearning qLearner;
	/**
	 * Creates a DummyPolicy
	 * TODO: remove the need for the QLearning object
	 * @param behavior
	 * @param unitTypeTable
	 * @param learner QLearning instance is needed in order to retrieve actions
	 */
	public DummyPolicy(String behavior, QLearning learner){
		this.behavior = behavior;
		this.qLearner = learner;
	}

	@Override
	public Action action(State s) {
		//traverses the list of qValues to find which action corresponds to pre-defined behavior
		List<QValue> qValues = qLearner.qValues(s);
		
		for (QValue qValue : qValues){
			if(qValue.a.actionName().equals(behavior)){
				return qValue.a;
			}
		}
		//should not get here!
		System.err.println(String.format("Behavior %s not found in state %s!", behavior, s));
		return null;	
	}

	@Override
	public double actionProb(State s, Action a) {
		if(a.actionName().equals(behavior)){
			return 1;
		}
		return 0;
	}

	@Override
	public boolean definedFor(State s) {
		return true;
	}


}
