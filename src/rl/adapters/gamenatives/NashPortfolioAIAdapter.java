package rl.adapters.gamenatives;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.portfolio.NashPortfolioAI;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.world.World;
import rl.RLParamNames;
import rl.RLParameters;
import rl.adapters.learners.PersistentLearner;
import rl.models.common.MicroRTSState;
import rl.models.common.ScriptActionTypes;
import rts.units.UnitTypeTable;

public class NashPortfolioAIAdapter implements PersistentLearner {
	
	String name;
	SGAgentType type;
	
	/**
	 * The underlying microRTS AI
	 */
	NashPortfolioAI portfolioAI;
	
	// PortfolioAI parameters
	int timeout, playouts, lookahead;
	
	/**
	 * Name of evaluation function to use
	 */
	String evalFuncName;
	
	Collection<AI> portfolio;
	
	AI currentStrategy;
	
	/**
	 * Maps AI names to stochastic game Actions
	 */
	Map<String, Action> nameToAction;
	
	/**
	 * Number of agent in the game (0 or 1)
	 */
	int agentNum;
	
	/**
	 * Creates a NashPortfolioAIAdapter with default timeout, playouts and lookahead
	 * @param agentName
	 * @param agentType
	 */
	public NashPortfolioAIAdapter(String agentName, SGAgentType agentType){
		this(agentName, agentType, 100, -1, 100, SimpleSqrtEvaluationFunction3.class.getSimpleName());
	}
	
	/**
	 * Creates a NashPortfolioAIAdapter specifying all parameters
	 * @param agentName
	 * @param agentType
	 * @param timeout computation budget, in milliseconds
	 * @param playouts number of playouts per computation
	 * @param lookahead max search tree depth (?)
	 */
	public NashPortfolioAIAdapter(String agentName, SGAgentType agentType, 
			int timeout, int playouts, int lookahead, String evalFuncName){
		this.name = agentName;
		this.type = agentType;
		this.timeout = timeout;
		this.playouts = playouts;
		this.lookahead = lookahead;
		this.evalFuncName = evalFuncName;
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
			throw new RuntimeException("NashPortfolioAIAdapter works only with MicroRTSStates. I received " + s);
		}
		
		MicroRTSState state = (MicroRTSState) s;
		if(portfolioAI == null){
			initializePortfolioAI(state.getUnderlyingState().getUnitTypeTable());
		}
		
        portfolioAI.startNewComputation(agentNum, state.getUnderlyingState().clone());
        try {
			portfolioAI.computeDuringOneGameFrame();
			currentStrategy = portfolioAI.getBestScriptSoFar();
		} catch (Exception e) {
			e.printStackTrace();
		}
        //System.out.println("Returning " + currentStrategy.getClass().getSimpleName());
        return nameToAction.get(currentStrategy.getClass().getSimpleName());
	}

	/**
	 * Initializes our portfolioAI object according to a unit type table
	 * @param unitTypeTable
	 */
	protected void initializePortfolioAI(UnitTypeTable unitTypeTable) {
		
		// initializes string -> action map
		nameToAction = ScriptActionTypes.getMapToLearnerActions();
		
		// retrieves the list of AIs and transforms it in an array
		Map<String, AI> actionMapping = ScriptActionTypes.getLearnerActionMapping(unitTypeTable);
		portfolio = actionMapping.values();
		AI[] portfolioArray = portfolio.toArray(new AI[portfolio.size()]);
		
		// selects the first AI as currentStrategy just to prevent NullPointerException
		currentStrategy = portfolioArray[0];
		
		// creates an array stating that all AIs in the portfolio are not deterministic
		// (we're being conservative by not making assumptions on deterministic-ness of AIs)
		boolean[] deterministic = new boolean[portfolio.size()];
		Arrays.fill(deterministic, false);
		/*for(int i = 0; i < deterministic.length; i++){
			deterministic[i] = false;
		}*/
		
		EvaluationFunction evalFunc = null;
		try {
			evalFunc = EvaluationFunctionFactory.fromString(evalFuncName);
		}
		catch (Exception e){
			System.err.println("An error has occurred while attempting to load an Evaluation Function.");
			System.err.println("Defaulting to SimpleSqrtEvaluationFunction3");
			evalFunc = new SimpleSqrtEvaluationFunction3();
			e.printStackTrace();
		}
		
		// finally creates the PortfolioAI w/ specified parameters
		portfolioAI = new NashPortfolioAI(
			portfolioArray, deterministic, timeout, 
			playouts, lookahead, evalFunc
		);
		
		// sets the debug level accordingly
		NashPortfolioAI.DEBUG = (int) RLParameters.getInstance().getParameter(RLParamNames.DEBUG_LEVEL);
	}
	
	public int getTimeout(){
		return timeout;
	}
	
	public int getPlayouts(){
		return playouts;
	}
	
	public int getLookahead() {
		return lookahead;
	}
	
	public String getEvaluationFunctionName() {
		return evalFuncName;
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
