package rl.adapters.learners;

import burlap.mdp.stochasticgames.agent.SGAgent;

public interface PersistentLearner extends SGAgent{

	/**
	 * Saves its learned action-values to file in specified path
	 * @param path 
	 */
	public void saveKnowledge(String path);
	
	/**
	 * Loads knowledge from file in specified path
	 * @param path
	 */
	public void loadKnowledge(String path);
}
