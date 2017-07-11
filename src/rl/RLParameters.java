package rl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.metagame.DummyPolicy;
import ai.metagame.RandomPolicy;
import burlap.behavior.learningrate.ConstantLR;
import burlap.behavior.learningrate.ExponentialDecayLR;
import burlap.behavior.learningrate.LearningRate;
import burlap.behavior.policy.EpsilonGreedy;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.stochasticgames.PolicyFromJointPolicy;
import burlap.behavior.stochasticgames.madynamicprogramming.backupOperators.MinMaxQ;
import burlap.behavior.stochasticgames.madynamicprogramming.policies.EMinMaxPolicy;
import burlap.behavior.valuefunction.ConstantValueFunction;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.model.JointRewardFunction;
import burlap.mdp.stochasticgames.world.World;
import burlap.statehashing.simple.SimpleHashableStateFactory;
import rl.adapters.domain.EnumerableSGDomain;
import rl.adapters.gamenatives.MetaBotAIAdapter;
import rl.adapters.gamenatives.NashPortfolioAIAdapter;
import rl.adapters.gamenatives.PortfolioAIAdapter;
import rl.adapters.learners.MultiAgentRandom;
import rl.adapters.learners.PersistentLearner;
import rl.adapters.learners.PersistentMultiAgentQLearning;
import rl.adapters.learners.SGQLearningAdapter;
import rl.models.common.MicroRTSRewardFactory;
import rl.models.common.MicroRTSTerminalFunction;
import rl.models.common.ScriptActionTypes;
import rl.planners.BackwardInduction;


/**
 * Singleton class that takes care of RL experiment parameters
 * @author anderson
 *
 */
public class RLParameters {
	
	/**
	 * The singleton instance of this class
	 */
	private static RLParameters instance;
	
	/**
	 * A {@link Set} with names of integer parameters
	 */
	private  Set<String> integerParams = null;
	
	/**
	 * A {@link Set} with names of float parameters
	 */
	private  Set<String> floatParams = null;
	
	/**
	 * A {@link Set} with names of boolean parameters
	 */
	private Set<String> boolParams = null;
	
	/**
	 * A {@link Map} String -> Object with parameter values
	 */
	private Map<String, Object> params;
	
	/**
	 * Stores the world specified in parameters
	 */
	private World world;
	
	/**
	 * Stores the joint reward function
	 */
	private JointRewardFunction jointRwd;
	
	/**
	 * Stores players' information contained in config. file
	 */
	List<Node> playerNodes; 
	
	/**
	 * Stores the actual player objects
	 */
	List<PersistentLearner> players;
	
	/**
	 * Initializes with default parameters
	 */
	private RLParameters(){
		params = defaultParameters();
	}
	
	/**
	 * Returns the singleton instance of this class
	 * @return
	 */
	public static RLParameters getInstance(){
		if (instance == null){
			instance = new RLParameters();
		}
		
		return instance;
	}
	
	/**
	 * Returns a {@link Set} with the name of integer parameters of the experiment
	 * @return
	 */
	public Set<String> integerParameters(){
		if (integerParams == null){
			integerParams = new HashSet<>();
			integerParams.add(RLParamNames.EPISODES);
			integerParams.add(RLParamNames.GAME_DURATION);
			integerParams.add(RLParamNames.TIMEOUT);
			integerParams.add(RLParamNames.PLAYOUTS);
			integerParams.add(RLParamNames.LOOKAHEAD);
			integerParams.add(RLParamNames.DEBUG_LEVEL);
		}
		return integerParams;
	}
	
	/**
	 * Returns a {@link Set} with the name of float parameters of the experiment
	 * @return
	 */
	public Set<String> floatParameters(){
		if (floatParams == null){
			floatParams = new HashSet<>();
			floatParams.add(RLParamNames.DISCOUNT);
			floatParams.add(RLParamNames.EPSILON); 
			floatParams.add(RLParamNames.INITIAL_Q);
		}
		return floatParams;
	}
	
	public Set<String> boolParameters(){
		if(boolParams == null){
			boolParams = new HashSet<>();
			boolParams.add(RLParamNames.QUIET_LEARNING);
		}
		
		return boolParams;
	}
	
	
	/**
	 * Returns the default parameters
	 * @return {@link Map}
	 */
	public Map<String, Object> defaultParameters(){
		Map<String, Object> params = new HashMap<>();
		
		// experiment parameters
		params.put(RLParamNames.EPISODES, 100);
		params.put(RLParamNames.GAME_DURATION, 2500);
		params.put(RLParamNames.OUTPUT_DIR, "/tmp/rl-experiment/");
		
		params.put(RLParamNames.REWARD_FUNCTION, MicroRTSRewardFactory.WIN_LOSS);
		params.put(RLParamNames.ABSTRACTION_MODEL, WorldFactory.STAGES);
		params.put(RLParamNames.DEBUG_LEVEL, 0); // currently only affects PortfolioAI
		params.put(RLParamNames.QUIET_LEARNING, true);
		
		params.put(RLParamNames.MICRORTS_OPPONENT, ScriptActionTypes.PORTFOLIO_AI);
		
		
		// parameters of RL methods
		params.put(RLParamNames.DISCOUNT, 0.9f);
		params.put(RLParamNames.LEARNING_RATE, 0.1f);
		params.put(RLParamNames.INITIAL_Q, 1.0f);
		params.put(RLParamNames.EPSILON, 0.1f);
		
		// parameters of search methods
		params.put(RLParamNames.TIMEOUT, 100);
		params.put(RLParamNames.PLAYOUTS, -1);
		params.put(RLParamNames.LOOKAHEAD, 100);
		params.put(RLParamNames.EVALUATION_FUNCTION, SimpleSqrtEvaluationFunction3.class.getSimpleName());

		return params;
	}
	
	/**
	 * Resets parameters to their default values and 
	 * cleans up internal structures
	 */
	public void reset(){
		// resets stuff
		params = defaultParameters();
		players = null;
		world = null;
		jointRwd = null;
		playerNodes = null;
	}

	/**
	 * Reads the parameters of a xml file
	 * @param path the path to the xml file
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public Map<String, Object> loadFromFile(String path) throws SAXException, IOException, ParserConfigurationException{
		// initializes player nodes that will be parsed from file
		playerNodes = new ArrayList<>();
		
		// opens xml file
		DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = dBuilder.parse(new File(path));
		
		// traverses all 1st level nodes of xml file (children of root node) 
		NodeList nList = doc.getDocumentElement().getChildNodes();
		
		for (int i = 0; i < nList.getLength(); i++){
			Node n = nList.item(i);

			//if node is 'parameters', traverses all its child nodes, setting experiment parameters accordingly
			if (n.getNodeName() == "parameters"){
				fillParameters(n, params);
			}
			
			//if node is 'player', stores its node for processing afterwards
			else if (n.getNodeName().equals("player")){
				playerNodes.add(n.cloneNode(true));
			}
			
		}
		
		return params;
	}
	
	/**
	 * Returns the value of a parameter identified by its name
	 * @param name
	 * @return
	 */
	public Object getParameter(String name){
		// some parameters require initialization before returning
		if(name.equalsIgnoreCase(RLParamNames.ABSTRACTION_MODEL)){
			return getWorld();
		}
		else if (name.equalsIgnoreCase(RLParamNames.REWARD_FUNCTION)){
			return getJointReward();
		}
		else if (name.equalsIgnoreCase(RLParamNames.PLAYERS)){
			return getPlayers();
		}
		
		// ordinary parameters are retrieved directly from params
		return params.get(name);
	}
	
	/**
	 * Returns the actual {@link World} object instead of its name  
	 * stored in {@link #params} map.
	 * @return
	 */
	public World getWorld(){
		// initializes world if necessary
		if(world == null){ 
			String worldModelName = (String) params.get(RLParamNames.ABSTRACTION_MODEL);
			world = WorldFactory.fromString(worldModelName, getJointReward());
		}
		return world;
	}
	
	/**
	 * Returns the actual {@link JointRewardFunction} object instead of its name  
	 * stored in {@link #params} map.
	 * @return
	 */
	public JointRewardFunction getJointReward(){
		// initializes joint reward if necessary
		if(jointRwd == null){
			jointRwd = MicroRTSRewardFactory.getRewardFunction(
				(String) params.get(RLParamNames.REWARD_FUNCTION)
			);
		}
		return jointRwd;
	}
	
	public List<PersistentLearner> getPlayers(){
		
		// initializes list of players if needed 
		if(players == null){
			
			if(playerNodes.size() < 2){
				throw new RuntimeException("You must specify at least two players.");
			}
			
			// process previously stored player nodes and creates players accordingly
			players = new ArrayList<>();
			for(Node n : playerNodes){
				players.add(processPlayerNode(n));
			}
		}
		return players;
	}
	
	public String getOpponentName(){
		return (String) params.get(RLParamNames.MICRORTS_OPPONENT);
		/*List<String> names = new ArrayList<>();
		for(Node n : playerNodes){
			names.add(processNode(n));
			
		}
		return names.get(1);*/
	}
	
	private String processNode(Node playerNode){
		
		// tests which type of player is specified and properly loads an agent
		Element e = (Element) playerNode;
		
		if ((e.getAttribute("type").equalsIgnoreCase("QLearning")) || 
				(e.getAttribute("type").equalsIgnoreCase("SGQLearningAdapter")) || 
				(e.getAttribute("type").equalsIgnoreCase("RandomSA")) || 
				(e.getAttribute("type").equalsIgnoreCase("Dummy"))){
			return "rl.adapters.learners.SGQLearningAdapter";
		}
		
		else if ((e.getAttribute("type").equalsIgnoreCase("RandomMA"))){
			return "rl.adapters.learners.MultiAgentRandom";
		}
		
		else if(e.getAttribute("type").equalsIgnoreCase("minimaxQ")) {
			return "rl.adapters.learners.PersistentMultiAgentQLearning";
		}
		
		else if(e.getAttribute("type").equalsIgnoreCase("PortfolioAI") || 
				e.getAttribute("type").equalsIgnoreCase("PortfolioAIAdapter")) {
			return "rl.adapters.gamenatives.PortfolioAIAdapter";
		}
		
		else if(e.getAttribute("type").equalsIgnoreCase("NashPortfolioAI") || 
				e.getAttribute("type").equalsIgnoreCase("NashPortfolioAIAdapter") || 
				e.getAttribute("type").equalsIgnoreCase("EnhancedPortfolioAI")|| 
				e.getAttribute("type").equalsIgnoreCase("EnhancedPortfolioAIAdapter")) {
			return "rl.adapters.gamenatives.NashPortfolioAIAdapter";
		}
		else if(e.getAttribute("type").equalsIgnoreCase("MetaBotAI") || 
				e.getAttribute("type").equalsIgnoreCase("MetaBotAIAdapter")) {
			return "rl.adapters.gamenatives.MetaBotAIAdapter";
		}
		else if(e.getAttribute("type").equalsIgnoreCase("BI") || 
				e.getAttribute("type").equalsIgnoreCase("BackwardInduction")) {
			return "rl.planners.BackwardInduction";
		}

		
		return null;
	}
	
	/**
	 * Processes some parameters from command line
	 * @param line
	 * @return
	 */
	public Map<String, Object> parametersFromCommandLine(CommandLine line) {
		
		//Map<String, Object> params = defaultParameters();
		
		if(line.hasOption(RLParamNames.OUTPUT_DIR)){
			params.put(
				RLParamNames.OUTPUT_DIR, 
				line.getOptionValue(RLParamNames.OUTPUT_DIR)
			);
		}
		
		if(line.hasOption(RLParamNames.QUIET_LEARNING)){
			String quietValue = line.getOptionValue(RLParamNames.QUIET_LEARNING);
			if(quietValue.equalsIgnoreCase("true")){
				params.put(RLParamNames.QUIET_LEARNING, true);
			}
			else {
				params.put(RLParamNames.QUIET_LEARNING, false);
			}
		}
		
		if(line.hasOption(RLParamNames.PLAYER1_POLICY)){
			params.put(RLParamNames.PLAYER1_POLICY, line.getOptionValue(RLParamNames.PLAYER1_POLICY));
		}
		
		if(line.hasOption(RLParamNames.PLAYER2_POLICY)){
			params.put(RLParamNames.PLAYER2_POLICY, line.getOptionValue(RLParamNames.PLAYER2_POLICY));
		}
		
		return params;
	}
	
	/**
	 * Extracts all relevant information about the player node
	 * @param playerNode
	 */
	private PersistentLearner processPlayerNode(Node playerNode){
		
		// retrieves the world model (needed for agent creation)
		World world = getWorld();
		
		// tests which type of player is specified and properly loads an agent
		Element e = (Element) playerNode;
		
		// loads parameters in a map
		Map<String, Object> playerParams = fillParameters(playerNode, defaultParameters());
		
		PersistentLearner agent = null;
		
		// QLearning or SGQLearningAdapter
		if ((e.getAttribute("type").equalsIgnoreCase("QLearning")) || 
				(e.getAttribute("type").equalsIgnoreCase("SGQLearningAdapter"))){
			
			QLearning ql = new QLearning(
				null, 
				(float) playerParams.get(RLParamNames.DISCOUNT), 
				new SimpleHashableStateFactory(false), 
				(float) playerParams.get(RLParamNames.INITIAL_Q),
				0.0	// dummy learning rate, will be changed right away
			);
			// sets the appropriate learning rate
			ql.setLearningRateFunction((LearningRate) playerParams.get(RLParamNames.LEARNING_RATE)); 
			
			// sets epsilon
			ql.setLearningPolicy(new EpsilonGreedy(ql, (float) playerParams.get(RLParamNames.EPSILON)));

			// create a single-agent interface for the learning algorithm
			agent = new SGQLearningAdapter(
				world.getDomain(), ql, e.getAttribute("name"), 
				new SGAgentType("QLearning", world.getDomain().getActionTypes())
			);
		}
		
		// Random single-agent
		else if ((e.getAttribute("type").equalsIgnoreCase("RandomSA"))){
			
			QLearning ql = new QLearning(
				null, 
				(float) playerParams.get(RLParamNames.DISCOUNT), 
				new SimpleHashableStateFactory(false), 
				(float) playerParams.get(RLParamNames.INITIAL_Q),
				0.0	// dummy learning rate, will be changed right away
			);
			
			ql.setLearningPolicy(new RandomPolicy(ql));

			// create a single-agent interface for the learning algorithm
			agent = new SGQLearningAdapter(
				world.getDomain(), ql, e.getAttribute("name"), 
				new SGAgentType("RandomSA", world.getDomain().getActionTypes())
			);
		}
		
		// Random multi-agent
		else if ((e.getAttribute("type").equalsIgnoreCase("RandomMA"))){

			// create a multi-agent random interface
			agent = new MultiAgentRandom(
				world.getDomain(), 
				0.9, 
				0.1, 
				new SimpleHashableStateFactory(),
				1000, 
				new MinMaxQ(), false, 
				e.getAttribute("name"), 
				new SGAgentType("RandomMA", world.getDomain().getActionTypes())
			);
		}
		
		// Dummy
		else if (e.getAttribute("type").equalsIgnoreCase("Dummy")) {
			//dummy is QLearning with 'dummy' learning policy
			
			QLearning ql = new QLearning(
				null, 
				0, // discount 
				new SimpleHashableStateFactory(false), 
				0, // initial q 
				0.1  // learning rate - not zero just to test knowledge at the end
			);
			
			ql.setLearningPolicy(
				new DummyPolicy((String) playerParams.get(RLParamNames.DUMMY_POLICY), ql)
			);
			
			// create a single-agent interface for the learning algorithm
			agent = new SGQLearningAdapter(
				world.getDomain(), ql, e.getAttribute("name"), 
				new SGAgentType("Dummy", world.getDomain().getActionTypes())
			);
		}
		
		// minimax-Q
		else if(e.getAttribute("type").equalsIgnoreCase("minimaxQ")) {
		
			//MinimaxQ example: https://groups.google.com/forum/#!topic/burlap-discussion/QYP6FKDGDnM
			PersistentMultiAgentQLearning pmaq = new PersistentMultiAgentQLearning(
				world.getDomain(), 
				(float) playerParams.get(RLParamNames.DISCOUNT), 
				(LearningRate) playerParams.get(RLParamNames.LEARNING_RATE), 
				new SimpleHashableStateFactory(),
				new ConstantValueFunction((float) playerParams.get(RLParamNames.INITIAL_Q)), 
				new MinMaxQ(), false, 
				e.getAttribute("name"), 
				new SGAgentType("MiniMaxQ", world.getDomain().getActionTypes())
			);
			
			// sets epsilon
			float epsilon = (float) playerParams.get(RLParamNames.EPSILON);
			pmaq.setLearningPolicy(new PolicyFromJointPolicy(new EMinMaxPolicy(epsilon)));
			
			// passes the PersistentMultiAgentQLearning as the game playing agent
			agent = pmaq;
		}
		
		// PortfolioAI or PortfolioAIAdapter
		else if(e.getAttribute("type").equalsIgnoreCase("PortfolioAI") || 
				e.getAttribute("type").equalsIgnoreCase("PortfolioAIAdapter")) {
			
			agent = new PortfolioAIAdapter(
				e.getAttribute("name"), 
				new SGAgentType("PortfolioAI", world.getDomain().getActionTypes()),
				(int) playerParams.get(RLParamNames.TIMEOUT),
				(int) playerParams.get(RLParamNames.PLAYOUTS),
				(int) playerParams.get(RLParamNames.LOOKAHEAD),
				(String) playerParams.get(RLParamNames.EVALUATION_FUNCTION)
			);
			
		}
		
		// NashPortfolioAI or NashPortfolioAIAdapter or 
		// EnhancedPortfolioAI or EnhancedPortfolioAIAdapter
		else if(e.getAttribute("type").equalsIgnoreCase("NashPortfolioAI") || 
				e.getAttribute("type").equalsIgnoreCase("NashPortfolioAIAdapter") || 
				e.getAttribute("type").equalsIgnoreCase("EnhancedPortfolioAI")|| 
				e.getAttribute("type").equalsIgnoreCase("EnhancedPortfolioAIAdapter")) {
			
			agent = new NashPortfolioAIAdapter(
				e.getAttribute("name"), 
				new SGAgentType("PortfolioAI", world.getDomain().getActionTypes()),
				(int) playerParams.get(RLParamNames.TIMEOUT),
				(int) playerParams.get(RLParamNames.PLAYOUTS),
				(int) playerParams.get(RLParamNames.LOOKAHEAD),
				(String) playerParams.get(RLParamNames.EVALUATION_FUNCTION)
			);
			
			
		}
		
		// Backward Induction
		else if(e.getAttribute("type").equalsIgnoreCase("BI") || 
				e.getAttribute("type").equalsIgnoreCase("BackwardInduction")) {
			
			agent = new BackwardInduction(
				e.getAttribute("name"), 
				(EnumerableSGDomain) world.getDomain(), 
				new MicroRTSTerminalFunction()
			);

			
		}
		// MetaBotAI or MetaBotAIAdapter or 
		else if(e.getAttribute("type").equalsIgnoreCase("MetaBotAI") || 
				e.getAttribute("type").equalsIgnoreCase("MetaBotAIAdapter")) {
			System.out.println("inside");
			agent = new MetaBotAIAdapter(
				e.getAttribute("name"), 
				new SGAgentType("MetaBotAI", world.getDomain().getActionTypes()),
				(int) playerParams.get(RLParamNames.TIMEOUT),
				(int) playerParams.get(RLParamNames.PLAYOUTS),
				(int) playerParams.get(RLParamNames.LOOKAHEAD),
				(String) playerParams.get(RLParamNames.EVALUATION_FUNCTION)
			);
		}
		
		if(agent == null) {
			// error: agent not initialized because type does not match
			throw new RuntimeException("Unrecognized player type: " + e.getAttribute("type"));
		}
		
		// checks whether knowledge should be loaded
		if(playerParams.get(RLParamNames.PATH_TO_KNOWLEDGE) != null){
			agent.loadKnowledge((String) playerParams.get(RLParamNames.PATH_TO_KNOWLEDGE));
		}
		
		return agent;
		
	}

	/**
	 * Processes the children of a {@link Node}, and return their values in a Map (paramName -> value)
	 * @param node a node containing parameters as in <node> <param1 value="1"/> <param2 value="2"/> </node>
	 * @return
	 *
	private Map<String, Object> fillParameters(Node node) {
		return fillParameters(node, new HashMap<String, Object>());
	}*/
	
	/**
	 * Processes the children of a {@link Node}, and return their values in a Map (paramName -> value)
	 * @param node a node containing parameters as in <node> <param1 value="1"/> <param2 value="2"/> </node>
	 * @param initialParameters a map containing the parameters, so that new values are overwritten
	 * @return
	 */
	private Map<String, Object> fillParameters(Node node, Map<String, Object> initialParameters) {
		
		for(Node parameter = node.getFirstChild(); parameter != null; parameter = parameter.getNextSibling()){
			
			if (parameter.getNodeType() != Node.ELEMENT_NODE) continue;	//prevents ClassCastException
			
			// treats parameters by type
			Element paramElement = (Element) parameter;
			
			// integers
			if (integerParameters().contains(parameter.getNodeName())){
				initialParameters.put(parameter.getNodeName(), Integer.parseInt(paramElement.getAttribute("value")));
			}
			
			// floats
			else if (floatParameters().contains(parameter.getNodeName())){
				initialParameters.put(parameter.getNodeName(), Float.parseFloat(paramElement.getAttribute("value")));
			}
			
			// booleans
			else if(boolParameters().contains(parameter.getNodeName())){
				initialParameters.put(parameter.getNodeName(), Boolean.parseBoolean(paramElement.getAttribute("value")));
			}
			
			// special parameter: learning rate
			else if(parameter.getNodeName().equalsIgnoreCase(RLParamNames.LEARNING_RATE)){
				LearningRate learningRate = null;
				
				// tests for constant learning rate
				if(! paramElement.hasAttribute("type") || paramElement.getAttribute("type").equalsIgnoreCase("constant")){
					learningRate = new ConstantLR(
						Double.parseDouble(paramElement.getAttribute("value"))
					);
				}
				
				// tests for Exponential Decay
				else if(paramElement.getAttribute("type").equalsIgnoreCase("exponential-decay")){
					learningRate = new ExponentialDecayLR(
						Float.parseFloat(paramElement.getAttribute("initial")), 
						Float.parseFloat(paramElement.getAttribute("rate")), 
						Float.parseFloat(paramElement.getAttribute("final"))
					);
				}
				
				// unknown learning rate, raises exception
				else {
					throw new RuntimeException("Unknown learning rate type: " + paramElement.getAttribute("type"));
				}
				
				// finally sets the parsed learning rate
				initialParameters.put(
					RLParamNames.LEARNING_RATE, learningRate
				);
			}
			else {	//parameter is a string (probably)
				initialParameters.put(parameter.getNodeName(), paramElement.getAttribute("value"));
			}
		}
		return initialParameters;
	}
}
