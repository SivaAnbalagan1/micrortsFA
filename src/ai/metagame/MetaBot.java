package ai.metagame;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.abstraction.BuildBarracks;
import ai.abstraction.Expand;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import burlap.behavior.policy.GreedyDeterministicQPolicy;
import burlap.behavior.policy.Policy;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.valuefunction.QValue;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.statehashing.simple.SimpleHashableStateFactory;
import rl.models.stages.GameStage;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

public class MetaBot extends AIWithComputationBudget {

	final static int DEFAULT_TIME_BUDGET = 100;
	final static int DEFAULT_ITERATIONS_BUDGET = -1;
	final static String DEFAULT_AI = "WorkerRush";
	
	//private Map<String, AI> portfolio;
	//private String currentAI;
	private Map<String, AI> portfolio;
	private String currentAIName;
	private int currentAIIndex;
	
	private UnitTypeTable unitTypeTable;
	private int timeBudget;
	private int iterationsBudget;
	
	private Policy policy;
	
	private String pathToQ; 
	
	/**
	 * Default constructor with no parameters
	 * TODO: create setters for the attributes!
	 */
	public MetaBot(){
		super(DEFAULT_TIME_BUDGET, DEFAULT_ITERATIONS_BUDGET);
		
		this.unitTypeTable = new UnitTypeTable();
		this.timeBudget = DEFAULT_TIME_BUDGET;
		this.iterationsBudget = DEFAULT_ITERATIONS_BUDGET;
		this.pathToQ = "";
		
		loadDefaultPortfolio();
		currentAIName = DEFAULT_AI;
	}
	
	/**
	 * Default constructor with same parameters as AIWithComputationBudget.
	 * Initializes a default portfolio
	 * @param timeBudget
	 * @param iterationsBudget
	 */
	public MetaBot(int timeBudget, int iterationsBudget, UnitTypeTable unitTypeTable, String pathToQ){
		super(timeBudget, iterationsBudget);
		
		this.unitTypeTable = unitTypeTable;
		this.timeBudget = timeBudget;
		this.iterationsBudget = iterationsBudget;
		this.pathToQ = pathToQ;
		
		//discount = 0.9, defaultQ = 1, learningRate = 0.1
		QLearning qLearner = new QLearning(null, 0.9, new SimpleHashableStateFactory(false), 1, 0.1);
		
		//loads Q 'table' from given path and extracts the policy
		qLearner.loadQTable(pathToQ); 
		//qLearner.loadQTable();
		System.out.println("Loaded Q values from " + pathToQ);
		policy = new GreedyDeterministicQPolicy(qLearner);
		
		loadDefaultPortfolio();
		
		currentAIName = "WorkerRush";
		
	}

	/**
	 * Loads a portfolio with default behaviors
	 * @param unitTypeTable
	 */
	private void loadDefaultPortfolio() {
		//loads AIs into the portfolio
		portfolio = new HashMap<>();
		
		portfolio.put(WorkerRush.class.getSimpleName(), new WorkerRush(unitTypeTable));
		portfolio.put(LightRush.class.getSimpleName(), new LightRush(unitTypeTable));
		portfolio.put(RangedRush.class.getSimpleName(), new RangedRush(unitTypeTable));
		portfolio.put(Expand.class.getSimpleName(), new Expand(unitTypeTable));
		portfolio.put(BuildBarracks.class.getSimpleName(), new BuildBarracks(unitTypeTable));
	}
	
	/**
	 * Configures a policy for this bot
	 * @param policy
	 */
	void setPolicy(Policy policy){
		this.policy = policy;
	}
		
	@Override
	public void reset() {
		for(AI ai : portfolio.values()){
			ai.reset();
		}

	}

	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		if (gs.canExecuteAnyAction(player)) {
			
			String oldBehavior = currentAIName;	//stores current AI to check for changes
			
			//retrieves the AI dictated by the policy
			State state = new GameStage(gs);
			Action action = policy.action(state);
			
			currentAIName = action.actionName();
			
			if (! oldBehavior.equals(currentAIName)){
				System.out.println(String.format(
					"[%d] Changed from %s to %s", gs.getTime(), oldBehavior, currentAIName
				));
			}
			
			return portfolio.get(currentAIName).getAction(player, gs);
			
			//cycles through AIs every 200 frames
			/*if(gs.getTime() % 200 == 0){
				currentAIIndex = (currentAIIndex + 1) % portfolio.values().size();
				currentAIName = (String)portfolio.keySet().toArray()[currentAIIndex];
				System.out.println("MetaBot: changed to " + currentAIName);
			}
			return portfolio.get(currentAIName).getAction(player, gs);*/ 
        } else {
            return new PlayerAction();        
        }       
	}

	@Override
	public AI clone() {
		return new MetaBot(timeBudget, iterationsBudget, unitTypeTable, pathToQ);
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		// TODO Auto-generated method stub
		return null;
	}

}
