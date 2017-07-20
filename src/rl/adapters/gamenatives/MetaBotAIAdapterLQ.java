package rl.adapters.gamenatives;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.metagame.MetaBotAI;
import ai.metagame.MetaBotAIR1;
import ai.portfolio.NashPortfolioAI;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.world.World;
import rl.RLParamNames;
import rl.RLParameters;
import rl.adapters.learners.PersistentLearner;
import rl.functionapprox.linearq.GameInfo;
import rl.models.common.MicroRTSState;
import rl.models.common.ScriptActionTypes;
import rts.GameState;
import rts.units.UnitTypeTable;

public class MetaBotAIAdapterLQ implements PersistentLearner {
	
	/**
	 * 
	 */
	
	String name;
	SGAgentType type;
	
	/**
	 * The underlying microRTS AI
	 */
	MetaBotAIR1 metaBotAI;
	

	
	
	/**
	 * Name of evaluation function to use
	 */
	String evalFuncName;
	
	Collection<AI> portfolio;
	
	AI currentStrategy;
	
	Collection<String> names;
	GameState gs;
	
	/**
	 * Maps AI names to stochastic game Actions
	 */
	Map<String, Action> nameToAction;
	
	/**
	 * Number of agent in the game (0 or 1)
	 */
	int agentNum;
	
	/**
	 * Creates a MetaBotAIAdapter with default timeout, playouts and lookahead
	 * @param agentName
	 * @param agentType
	 */
	/*public MetaBotAIAdapterLQ(String agentName, SGAgentType agentType){
		this(agentName, agentType );
	}*/
	
	/**
	 * Creates a MetaBotAIAdapter specifying all parameters
	 * @param agentName
	 * @param agentType
	 * @param timeout computation budget, in milliseconds
	 * @param playouts number of playouts per computation
	 * @param lookahead max search tree depth (?)
	 */
	public MetaBotAIAdapterLQ(String agentName, SGAgentType agentType){
		this.name=agentName;this.type=agentType;
	}

	@Override
	public String agentName() {
		return name;
	}

	@Override
	public SGAgentType agentType() {
		return type;
	}

	@Override
	public void gameStarting(World w, int agentNum) {
		this.agentNum = agentNum;
	}

	@Override
	public Action action(State s) {
		if(!(s instanceof MicroRTSState)){
			throw new RuntimeException("MetaBotAIAdapter works only with MicroRTSStates. I received " + s);
		}
		
		MicroRTSState state = (MicroRTSState) s;
		if(metaBotAI == null){
			gs = state.getUnderlyingState();
			initializeMetaBotAI(gs.getUnitTypeTable());
		}
		try{
			
		String currentAI = metaBotAI.tdFA.getAction(metaBotAI.getFeature(gs));
		currentStrategy = metaBotAI.AIlookup.get(currentAI).clone();
		} catch (Exception e) {
		e.printStackTrace();
		}
	
        /*portfolioAI.startNewComputation(agentNum, state.getUnderlyingState().clone());
        try {
			portfolioAI.computeDuringOneGameFrame();
			currentStrategy = portfolioAI.getBestScriptSoFar();
		} catch (Exception e) {
			e.printStackTrace();
		}*/
        //System.out.println("Returning " + currentStrategy.getClass().getSimpleName());
        return nameToAction.get(currentStrategy.getClass().getSimpleName());
	}

	/**
	 * Initializes our portfolioAI object according to a unit type table
	 * @param unitTypeTable
	 */
	protected void initializeMetaBotAI(UnitTypeTable unitTypeTable) {
		// initializes string -> action map
		nameToAction = ScriptActionTypes.getMapToLearnerActions();
		
		// retrieves the list of AIs and transforms it in an array
		Map<String, AI> actionMapping = ScriptActionTypes.getLearnerActionMapping(unitTypeTable);
		portfolio = actionMapping.values();
		AI[] portfolioArray = portfolio.toArray(new AI[portfolio.size()]);
		names = actionMapping.keySet();
		String[] AInames = names.toArray(new String[names.size()]); 
		
		// selects the first AI as currentStrategy just to prevent NullPointerException
		currentStrategy = portfolioArray[0];
		
		UnitTypeTable uTTable = new UnitTypeTable();
        int featSize = uTTable.getUnitTypes().size();
        int actionnum = portfolioArray.length;
		// Create an initial weights for each action (AIs).
	       double weightLow = -1/Math.sqrt(featSize*9*2);//quadrant 9 and player 2
	        double weightHigh = 1/Math.sqrt(featSize*9*2);
	        double range = weightHigh - weightLow;
	        double [] initialWeights = new double[featSize*9*2*actionnum];
	        double [] features = new double[featSize*9*2];
	        Arrays.fill(features, 0.1);
	        //System.out.println(features.length);
	        Random r = new Random();
	        for(int i=0;i<initialWeights.length;i++){
	            initialWeights[i] = r.nextDouble() * range + weightLow;
	 //           System.out.println(initialWeights[i]);
	        }
	 	

		
		// finally creates the MetaBotAI w/ specified parameters
		metaBotAI = new MetaBotAIR1(
			portfolioArray, AInames,initialWeights,features, 
	unitTypeTable
		);
		
		// sets the debug level accordingly
		//NashPortfolioAI.DEBUG = (int) RLParameters.getInstance().getParameter(RLParamNames.DEBUG_LEVEL);
	}
	


	@Override
	public void observeOutcome(State s, JointAction jointAction, double[] jointReward, State sprime,
			boolean isTerminal) {
		// does nothing
	}

	@Override
	public void gameTerminated() {
		// does nothing
	}

	@Override
	public void saveKnowledge(String path) {
		// does nothing
		
	}

	@Override
	public void loadKnowledge(String path) {
		// also does nothing
		
	}

}
