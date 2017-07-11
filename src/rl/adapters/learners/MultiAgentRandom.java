package rl.adapters.learners;

import java.util.List;
import java.util.Random;

import rl.models.common.ScriptActionTypes;
import burlap.behavior.learningrate.LearningRate;
import burlap.behavior.stochasticgames.madynamicprogramming.SGBackupOperator;
import burlap.behavior.valuefunction.QFunction;
import burlap.debugtools.RandomFactory;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.UniversalActionType;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.SGDomain;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.statehashing.HashableStateFactory;

public class MultiAgentRandom extends PersistentMultiAgentQLearning {
	
	List<UniversalActionType> actions;
	UniversalActionType randomAction;
	protected Random rand = RandomFactory.getMapped(0);

	public MultiAgentRandom(SGDomain d, double discount, double learningRate,
			HashableStateFactory hashFactory, double qInit,
			SGBackupOperator backupOperator,
			boolean queryOtherAgentsForTheirQValues, String agentName,
			SGAgentType agentType) {
		super(d, discount, learningRate, hashFactory, qInit, backupOperator,
				queryOtherAgentsForTheirQValues, agentName, agentType);
	}

	public MultiAgentRandom(SGDomain d, double discount,
			LearningRate learningRate, HashableStateFactory hashFactory,
			QFunction qInit, SGBackupOperator backupOperator,
			boolean queryOtherAgentsForTheirQValues, String agentName,
			SGAgentType agentType) {
		super(d, discount, learningRate, hashFactory, qInit, backupOperator,
				queryOtherAgentsForTheirQValues, agentName, agentType);
	}

	@Override
	public Action action(State s) {
		// Returns a random action 
		this.actions = ScriptActionTypes.getLearnerActionTypes();
		this.randomAction = actions.get(this.rand.nextInt(this.actions.size()));
				
		return this.randomAction.associatedAction(null);
	}
	
}
