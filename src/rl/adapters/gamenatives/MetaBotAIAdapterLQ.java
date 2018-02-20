package rl.adapters.gamenatives;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.metagame.MetaBotAI;
import ai.metagame.MetaBotAIR1;
import ai.portfolio.NashPortfolioAI;
import burlap.behavior.learningrate.LearningRate;
import burlap.behavior.valuefunction.QValue;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.SGDomain;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.world.World;

import rl.RLParamNames;
import rl.RLParameters;
import rl.adapters.domain.EnumerableSGDomain;
import rl.adapters.learners.PersistentLearner;
import rl.functionapprox.linearq.GameInfo;
import rl.functionapprox.linearq.LearningRateExpDecay;
import rl.models.common.MicroRTSState;
import rl.models.common.ScriptActionTypes;
import rts.GameState;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import ai.abstraction.LightRush;

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
	String[] AInames;
	Map<String, ArrayList<Double>> predError;
	Map<String, Map<String,ArrayList<Double>>> SGD;
	Map<String, double[]> weightInit;
	int featSize;
	/**
	 * Maps AI names to stochastic game Actions
	 */
	Map<String, Action> nameToAction;
	
	/**
	 * Number of agent in the game (0 or 1)
	 */
	int agentNum;
	SGDomain domain;
	boolean initWeight;
	String path;
	boolean onlyCount;
	double [] initialWeights,sameWeights;
	double epsilon,epsiDecay,lambdaEtrace;
	LearningRateExpDecay learningRate;
	double finalReward;
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
	/*public MetaBotAIAdapterLQ(SGDomain domain,String agentName, SGAgentType agentType){
		this.name=agentName;this.type=agentType;this.domain = domain;
	}*/
	public MetaBotAIAdapterLQ(SGDomain domain,String agentName, SGAgentType agentType,
			String path, LearningRateExpDecay lr,double epi,double epiDecay,double lEtrace){
		this.name=agentName;this.type=agentType;this.domain = domain;this.path=path;
		this.learningRate = lr; this.epsilon=epi;this.epsiDecay = epiDecay; this.lambdaEtrace=lEtrace;
	
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
		gs = state.getUnderlyingState();
		if(metaBotAI == null){
			initializeMetaBotAI(gs.getUnitTypeTable());
		}
		try{
			
		String currentAI = metaBotAI.tdFA.getAction(metaBotAI.getFeature(gs));
		currentStrategy = metaBotAI.AIlookup.get(currentAI).clone();
		} catch (Exception e) {
		e.printStackTrace();
		}
	    return nameToAction.get(currentStrategy.getClass().getSimpleName());
	}

	/**
	 * Initializes our portfolioAI object according to a unit type table
	 * @param unitTypeTable
	 */
	protected void initializeMetaBotAI(UnitTypeTable unitTypeTable) {
		// initializes string -> action map
		nameToAction = ScriptActionTypes.getMapToLearnerActions();
		weightInit = new HashMap();
		// retrieves the list of AIs and transforms it in an array
		Map<String, AI> actionMapping = ScriptActionTypes.getLearnerActionMapping(unitTypeTable);
/* uncomment for no light rush test*/		
//		actionMapping.remove(LightRush.class.getSimpleName());
		portfolio = actionMapping.values();
		AI[] portfolioArray = portfolio.toArray(new AI[portfolio.size()]);
		names = actionMapping.keySet();
		AInames = names.toArray(new String[names.size()]); 
		System.out.println("Strategies used:");
		for(String action: AInames)System.out.println(action);
		// selects the first AI as currentStrategy just to prevent NullPointerException
		currentStrategy = portfolioArray[0];
		int runit =0;
		UnitTypeTable uTTable = new UnitTypeTable();
        for(UnitType ut: uTTable.getUnitTypes())if(ut.isResource)runit++;
        int quadFeat = 0;
        quadFeat = uTTable.getUnitTypes().size() -runit;
        featSize = quadFeat *9*2 +9+9+2+1;//intercept+1;// 2-resource, 9-health for 2 players,time and bias
 
        int actionnum = portfolioArray.length;
        double [] features = new double[featSize];
        Arrays.fill(features, 0.0);
        initWeight = false;
        initialWeights = new double[featSize*actionnum];
        sameWeights = new double[featSize];//same weights for all actions, for testing diff
		if(path==null)initWeight=true;
		else loadKnowledge(path);
		
		if(initWeight){
		// Create an initial weights for each action (AIs).
	       double weightLow = -1/Math.sqrt(featSize);//quadrant 9 and player 2
	        double weightHigh = 1/Math.sqrt(featSize);
	        double range = weightHigh - weightLow;
	        
           Random r = new Random();
           r.setSeed(100);
	       for(int i=0;i<initialWeights.length;i++){
	            initialWeights[i] = r.nextDouble() * range + weightLow;
	 //           System.out.println(initialWeights[i]);
	        }
	        
		}
		// finally creates the MetaBotAI w/ specified parameters
		metaBotAI = new MetaBotAIR1(
			portfolioArray, AInames,initialWeights,features, 
	unitTypeTable,learningRate,epsilon,epsiDecay,lambdaEtrace
		);
//		System.out.println("Agent num-start" + this.agentNum);
//		setLearningRate(0.001);
//		setEpsilon(0.5);
		// sets the debug level accordingly
		//NashPortfolioAI.DEBUG = (int) RLParameters.getInstance().getParameter(RLParamNames.DEBUG_LEVEL);
	}
	@Override
	public void observeOutcome(State s, JointAction jointAction, double[] jointReward, State sprime,
			boolean isTerminal) {
		if(isTerminal)finalReward = jointReward[0];
	}

	@Override
	public void gameTerminated() {
		metaBotAI.tdFA.setReward(finalReward);
//		metaBotAI.tdFA.printweights();
		String dummycall = metaBotAI.tdFA.getAction(metaBotAI.getFeature(gs));
		metaBotAI.tdFA.setReward(0);
//		metaBotAI.tdFA.printweights();
		decayEpsilon();//decay epsilon over episodes.
	}
	public void decayEpsilon() {
		metaBotAI.tdFA.decayEpslion();
	}
	public void setEpsilon(double epi) {
		metaBotAI.tdFA.setEpslion(epi);
	}
	public void setLearningRate(double lr) {
		metaBotAI.tdFA.setLearninRate(lr);
	}
	@Override
	public void saveKnowledge(String path) {
		BufferedWriter fileWriter;
		String [] actions;
		try {
			fileWriter = new BufferedWriter(new FileWriter(path));

			if (domain instanceof EnumerableSGDomain){
				EnumerableSGDomain enumDomain = (EnumerableSGDomain) domain;
				
				// xml root node
				fileWriter.write("<knowledge>\n\n"); 
				
				// information about who is saving knowledge, i.e., myself
				// this might be useful when loading knowledge later
				fileWriter.write(String.format(
					"<learner name='%s' type='%s' id='%d' LearningRate='%f' />\n\n", 
					name, this.getClass().getName(), agentNum,metaBotAI.tdFA.getLearninRate()
				));
				
				fileWriter.write(String.format("<state id='dummy'>\n")); 
				    actions = metaBotAI.tdFA.actionNames();
				    Map<String,double []> actionWeights1 = metaBotAI.tdFA.actionWeightret();
				    predError = metaBotAI.tdFA.getPredError();
				    for(String action: actions ){
							fileWriter.write(String.format(
								"\t<a name='%s' value='%s' />\n",
								action,
								Arrays.toString(actionWeights1.get(action)))
								);

					}
				
			//	    SGD = metaBotAI.tdFA.getSGD();
					for(String action: actions ){
						fileWriter.write(String.format(
							"\t<a name='%sSGD' value='%s' />\n",
							action,
						Arrays.toString(predError.get(action).toArray()))
							);
				}
				// closes state tag
				fileWriter.write("</state>\n\n");
					
				// closes xml root
				fileWriter.write("</knowledge>\n"); 
				fileWriter.close();
			}
			else {
				System.err.println("Can only save knowledge for EnumerableSGDomains, this one is: " + domain.getClass().getName());
			}
		
		} catch (IOException e) {
			System.err.println("ERROR: Unable to save knowledge to file " + path);
			e.printStackTrace();
		}
		
		
	}

	@Override
	public void loadKnowledge(String path) {
		//learner.loadQTable(path);
		
		//opens xml file
		DocumentBuilder dBuilder = null;
		Document doc = null;
		try {
			dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc = dBuilder.parse(new File(path));
		} catch (ParserConfigurationException|SAXException|IOException e) {
			System.out.println("Previous knowledge file doesnt exist " + path +". knowledge NOT LOADED!");
			initWeight = true;
//			e.printStackTrace();
			return;
		}
		initWeight = false;
		//retrieves the game states, they'll be useful for filling the knowledge in
	//	Map<String, State> nameToState = ((EnumerableSGDomain)domain).namesToStates();
		
		//traverses all 1st level nodes of xml file (children of root node) 
		NodeList nList = doc.getDocumentElement().getChildNodes();
		
		for (int i = 0; i < nList.getLength(); i++){
			Node n = nList.item(i);
			//if node is 'learner', checks if ID is the same as mine
			if (n.getNodeName() == "learner"){
				Element e = (Element) n;
				if(! e.getAttribute("type").equals(this.getClass().getName())){
					System.err.println(
						"WARNING! Loading knowledge of agent with different type. This might end up in error..."
					);
				}
				if(Integer.parseInt(e.getAttribute("id")) != agentNum){
					System.err.println(
						"WARNING! Loading knowledge of agent with different ID. "
					);
				}
			}
			else if (n.getNodeName() == "state"){
				Element e = (Element) n;
				for(Node actionNode = n.getFirstChild(); actionNode != null; actionNode = actionNode.getNextSibling()){
					if (actionNode.getNodeType() != Node.ELEMENT_NODE) continue;	//prevents ClassCastException
					// retrieves the action object 
					Element actionElement = (Element) actionNode;
					String actionName = actionElement.getAttribute("name");
					// creates and stores the loaded QValue
					if(!actionName.contains("SGD")){
					/* uncomment for no light rush test*/
					//	if(!actionName.contains("Light")){
					String value = actionElement.getAttribute("value");
					String value1 = value.substring(1, value.length()-1);
					String [] weightString = value1.split(",");
					double [] weightDouble = Arrays.stream(weightString).mapToDouble(Double::parseDouble).toArray();
					//System.out.println(actionName.substring(actionName.length()-2,3));
					weightInit.put(actionName, weightDouble.clone());
					//	}
					}
				}
			}
			}
		int srcPos =0,destPos=0;
          for(String act: AInames){
        	  double [] tempcopy = weightInit.get(act).clone();
        	  System.arraycopy(tempcopy, srcPos, initialWeights, destPos, featSize);
        	  destPos=destPos+featSize;
          }		
          System.out.println("Loaded Knowledge...");
		}
}
