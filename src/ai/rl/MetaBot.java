package ai.rl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.abstraction.WorkerRush;
import ai.core.AI;
import ai.core.ParameterSpecification;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.stochasticgames.agents.maql.MultiAgentQLearning;
import burlap.behavior.stochasticgames.madynamicprogramming.backupOperators.MinMaxQ;
import burlap.mdp.core.action.Action;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.model.JointRewardFunction;
import burlap.mdp.stochasticgames.world.World;
import burlap.statehashing.simple.SimpleHashableStateFactory;
import rl.WorldFactory;
import rl.adapters.domain.EnumerableSGDomain;
import rl.adapters.learners.PersistentLearner;
import rl.adapters.learners.PersistentMultiAgentQLearning;
import rl.adapters.learners.SGQLearningAdapter;
import rl.models.aggregatediff.AggregateDifferencesDomain;
import rl.models.common.MicroRTSState;
import rl.models.common.MicroRTSTerminalFunction;
import rl.models.common.ScriptActionTypes;
import rl.models.common.SimpleWeightedFeatures;
import rl.planners.BackwardInduction;
import rts.GameState;
import rts.PlayerAction;

public class MetaBot extends AI {
	
	// parameters
	String learnerType;
	String pathToKnowledge;
	String worldModelName;
	
	/**
	 * The learner whose policy will be used
	 */
	PersistentLearner learner;
	
	Map<String, AI> portfolio;
	
	/**
	 * Current abstract representation of the underlying {@link GameState}
	 */
	MicroRTSState currentState;
	
	/**
	 * Active algorithm
	 */
	AI currentAlgorithm;
	
	/**
	 * Empty constructor, uses {@link SGQLearningAdapter} with empty knowledge 
	 * and {@link AggregateDifferencesDomain} for constructing the world
	 */
	public MetaBot(){
		this(SGQLearningAdapter.class.getName(), null, WorldFactory.AGGREGATE_DIFF);
	}
	
	public MetaBot(String learnerType, String pathToKnowledge, String worldModelName){
		this.learnerType = learnerType;
		this.pathToKnowledge = pathToKnowledge;
		this.worldModelName = worldModelName;
		
		// retrieves the learner, by processing learner type
		learner = getLearner();
		
		// resets internal structures and loads knowledge
		this.reset();
	}
	
	/**
	 * Returns a learner given its type
	 * @return
	 */
	private PersistentLearner getLearner() {
		// the reward function does not matter, since this agent does not process rewards
		JointRewardFunction rwdFunc = new SimpleWeightedFeatures();
		
		// creates the world object from the string specification
		World w = WorldFactory.fromString(worldModelName, rwdFunc);
		PersistentLearner agent = null;
		
		// QLearning -> SGQLearningAdapter
		if (learnerType.equalsIgnoreCase("QLearning") || 
				learnerType.equalsIgnoreCase(SGQLearningAdapter.class.getName()) || 
				learnerType.equalsIgnoreCase(SGQLearningAdapter.class.getSimpleName())){
			
			QLearning ql = new QLearning(null, 0.9, new SimpleHashableStateFactory(false), 1000, 0);
			agent = new SGQLearningAdapter(
				w.getDomain(), ql, "QLearning", 
				new SGAgentType("QLearning", w.getDomain().getActionTypes())
			);
			
		}
		
		// MinimaxQ -> PersistentMultiAgentQLearning
		if (learnerType.equalsIgnoreCase("MinimaxQ") || 
				learnerType.equalsIgnoreCase(PersistentMultiAgentQLearning.class.getName()) || 
				learnerType.equalsIgnoreCase(PersistentMultiAgentQLearning.class.getSimpleName())){
			
			
			// before returning: must call 'gameStarting'
			// therefore, must register an adversary in the world
			// and must know beforehand whether this is player 0 or 1
			agent = new PersistentMultiAgentQLearning(
				w.getDomain(), 0., 0., new SimpleHashableStateFactory(false), 
				1000, new MinMaxQ(), false, "MinimaxQ", 
				new SGAgentType("MinimaxQ", w.getDomain().getActionTypes())
			);
			
			// creates an adversary...
			MultiAgentQLearning adversary = new MultiAgentQLearning(w.getDomain(), 0., 0., new SimpleHashableStateFactory(false), 
				1000, new MinMaxQ(), false, "adversary", 
				new SGAgentType("MinimaxQ", w.getDomain().getActionTypes())
			);
			
			// makes agent and adversary join the world
			w.join(agent);
			w.join(adversary);
			
			// makes agent initialize its structures
			agent.gameStarting(w, 0);
			
			// hopefully, now agent is good to go
		}
		
		// BackwardInduction 
		if (learnerType.equalsIgnoreCase("BackwardInduction") || 
				learnerType.equalsIgnoreCase(BackwardInduction.class.getName()) || 
				learnerType.equalsIgnoreCase(BackwardInduction.class.getSimpleName())){
			
			agent = new BackwardInduction(
				"BackwardInduction", (EnumerableSGDomain) w.getDomain(), new MicroRTSTerminalFunction()
			);
			
		}
		if(agent == null) {
			// error: agent has not being initialized because name does not match
			throw new RuntimeException("Unrecognized learner type: " + learnerType);
		}
		
		if(pathToKnowledge != null){
			agent.loadKnowledge(pathToKnowledge);
		}
		return agent;
	}

	@Override
	/**
	 * Resets internal data and re-loads knowledge from file
	 */
	public void reset() {
		
		currentState = null;
		currentAlgorithm = null;
		
		if (pathToKnowledge != null){
			try {
				this.learner.loadKnowledge(pathToKnowledge);
			}
			catch(Exception e){
				System.err.println("Unable to load knowledge! Will proceed with 'dumb' agent");
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		// initializes the portfolio if needed
		if(portfolio == null){
			portfolio = ScriptActionTypes.getLearnerActionMapping(gs.getUnitTypeTable());
		}
		
		// retrieves the abstract state representation and retrieves an action
		MicroRTSState state = MicroRTSStateFactory.fromString(worldModelName, gs);
		
		// if state has changed, then I retrieve a new algorithm
		if(currentState == null || !currentState.equals(state)){
			currentState = state;
			Action a = learner.action(state);
			
			// returns the underlying game action dictated by the chosen script
			AI algorithm = portfolio.get(a.actionName());
			if(algorithm == null){
				System.out.println(
					String.format("Warning: unknown algorithm %s. Defaulting to WorkerRush")
				);
				algorithm = new WorkerRush(gs.getUnitTypeTable());
				
			}
			// now it is save to assign the current algorithm
			setCurrentAlgorithm(algorithm);
		}
		
		// return the action dictated by the selected algorithm
		return currentAlgorithm.getAction(player, gs);
	}

	@Override
	public AI clone() {
		// creates a new instance with the same attributes
		MetaBot newInstance = new MetaBot(learnerType, pathToKnowledge, worldModelName);
		newInstance.setCurrentAlgorithm(currentAlgorithm);
		newInstance.setCurrentState(currentState);
		
		return newInstance;
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		List<ParameterSpecification> parameters = new ArrayList<>();
		
		parameters.add(new ParameterSpecification("pathToKnowledge", String.class, null));
		parameters.add(new ParameterSpecification("worldModelName", String.class, WorldFactory.AGGREGATE_DIFF));
		parameters.add(new ParameterSpecification("learnerType", String.class, "QLearning"));
		
		return parameters;
	}
	
	public void setCurrentState(MicroRTSState s){
		currentState = s;
	}
	
	public void setCurrentAlgorithm(AI algorithm){
		currentAlgorithm = algorithm;
	}

}
