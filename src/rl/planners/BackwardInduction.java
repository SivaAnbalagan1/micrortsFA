package rl.planners;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jdom.JDOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.world.World;
import rl.adapters.domain.EnumerableSGDomain;
import rl.adapters.learners.PersistentLearner;
import rl.models.aggregatediff.AggregateDifferencesDomain;
import rl.models.common.MicroRTSState;
import rl.models.common.MicroRTSTerminalFunction;
import util.Pair;

public class BackwardInduction implements PersistentLearner {
	
	/**
	 * Agent's name
	 */
	String name;
	
	/**
	 * Agent's type
	 */
	SGAgentType type;
	
	/**
	 * Directory to use for communicating with Gambit library
	 */
	String workingDir;
	
	/**
	 * Agent's number in a game (0 or 1) -- important when selecting the policy
	 */
	int agentNumber;
	
	/**
	 * State value function
	 */
	Map<MicroRTSState, Double> V;
	
	/**
	 * State-action value function
	 */
	Map<MicroRTSState, Map<JointAction, Double>> Q;
	
	/**
	 * Stores which states were already visited
	 */
	Set<MicroRTSState> visited;
	
	/**
	 * The domain in which this agent is in
	 */
	EnumerableSGDomain domain;
	
	/**
	 * A function to test whether state is terminal 
	 */
	TerminalFunction terminalFunction;
	
	/**
	 * 
	 * @param name
	 * @param domain
	 * @param terminal
	 */
	public BackwardInduction(String name, EnumerableSGDomain domain, TerminalFunction terminal){
		this.name = name;
		this.type = new SGAgentType("BackwardInduction", domain.getActionTypes());
		
		V = new HashMap<>();
		Q = new HashMap<>();
		visited = new HashSet<>();
		
		this.domain = domain;
		terminalFunction = terminal;

		// uses a unique identifier for the working dir
		workingDir = "/tmp/bi_" + UUID.randomUUID().toString() + "/";
		
		// creates the working dir
		File dir = new File(workingDir);

		if(dir.mkdirs() == false){
			throw new RuntimeException("Unable to create working directory " + workingDir);
		}
		
		preAllocate();
	}
	
	/**
	 * Returns the value of a given state
	 * @param s
	 * @return
	 */
	public double value(State s){
		return V.get(s);
	}
	
	/**
	 * Returns the value of a given state-joint action pair
	 * @param s
	 * @param ja
	 * @return
	 */
	public double value(State s, JointAction ja){
		return Q.get(s).get(ja);
	}
	
	/**
	 * Uses the backward induction algorithm to solve and return the
	 * value of a state. Caches solved states so that they don't need
	 * to be solved twice
	 * @param s
	 * @return
	 */
	public double solve(MicroRTSState s){
		
		// for a terminal state, store its value and return it
		if(terminalFunction.isTerminal(s)){
			// 0 or 1 if a player wins, or -1 for draw
			int winner = s.getUnderlyingState().winner();
			
			if (winner == -1){ // draw
				V.put(s, 0.);
			}
			else if (winner == 0){ // first player wins
				V.put(s, 1.);
			}
			else { // second player wins
				V.put(s, -1.); 
			}
			
			return V.get(s);
		}
		
		// finds the value of the state
		if(! visited.contains(s)){
			//System.out.print("\rEntering " + s +"     ");
			for(ActionType a : type.actions){
				for(ActionType o : type.actions){
					JointAction ja = new JointAction();
					ja.addAction(a.associatedAction(null));
					ja.addAction(o.associatedAction(null));
					
					MicroRTSState next = (MicroRTSState) domain.getJointActionModel().sample(s, ja);
					try {
						if(!Q.containsKey(s)){
							System.err.println("WARNING: missing key for " + s);
							Q.put(s, new HashMap<>());
						}
						Q.get(s).put(ja, solve(next));		// recursive call
					} catch (NullPointerException e) {
						System.out.println("NPE in state " + s);
						e.printStackTrace();
						System.exit(0);
					}
					
				}
			}
			V.put(s, calculateValue(s)); 
			visited.add(s);
			
			
			System.out.print(String.format("\rClosed state number %7d", visited.size()));
		}
		
		// s is solved, return its value
		return V.get(s);
	}
	
	/**
	 * Creates entries in V and Q tables by enumerating states and joint actions
	 */
	public void preAllocate(){
		
		List<? extends State> allStates = domain.enumerate();
		
		//System.out.println("Pre-allocating for " + allStates.size() + " states");
		
		// creates an entry in V for each state
		for(State s : allStates){
			MicroRTSState state = (MicroRTSState) s;
			V.put(state, 0.);
			
			// creates an entry in Q for each state and joint action
			Q.put(state, new HashMap<>());
			
			for(ActionType a : type.actions){
				for(ActionType o : type.actions){
					JointAction ja = new JointAction();
					ja.addAction(a.associatedAction(null));
					ja.addAction(o.associatedAction(null));
					
					Q.get(state).put(ja, 0.);
				}
			}
		}
		
		//System.out.println("Done.");
		//System.out.println("V entries: " + V.size());
		//System.out.println("Q entries: " + Q.size());
	}
	
	/**
	 * Solves the game for a state and returns its value
	 * @param s
	 * @return
	 */
	public double calculateValue(State s) {
		Pair<double[], double[]> policies = null;
		try {
			policies = getPoliciesFor(s);
		} catch (IOException | InterruptedException e) {
			System.err.println("Error while calculating policy for state " + s);
			e.printStackTrace();
			System.exit(0);
		}
		
		double[] agentPolicy = policies.m_a;
		double[] opponentPolicy = policies.m_b;
		
		double stateValue = 0;
		int agentActionIndex = 0;
		for(ActionType a : type.actions){
			int opponentActionIndex = 0;
			for(ActionType o : type.actions){
				JointAction ja = new JointAction();
				ja.addAction(a.associatedAction(null));
				ja.addAction(o.associatedAction(null));
				
				/*
				 *  adds the value of each outcome by the probability of its occurrence
				 *  which is dictated by the policies
				 */
				stateValue += agentPolicy[agentActionIndex] * opponentPolicy[opponentActionIndex]
						* Q.get(s).get(ja);
				
				opponentActionIndex++;
			}
			agentActionIndex++;
		}
		
		return stateValue;
	}
	
	/**
	 * Solves the game for a state and returns the equilibrium policy
	 * @param s
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * 
	 * TODO extract the part that formulates the NFG so that it can be reused
	 */
	public Pair<double[], double[]>  getPoliciesFor(State s) throws IOException, InterruptedException{
		
		// writes a file with a normal-form game for Gambit
		String fileForGambit = workingDir + "/state.nfg";
		BufferedWriter fileWriter;
		fileWriter = new BufferedWriter(new FileWriter(fileForGambit));
		
		fileWriter.write("NFG 1 R \"" + s + "\"\n");
		fileWriter.write(
			String.format("{\"Player0\" \"Player1\"}{%d %d}\n", type.actions.size(), type.actions.size())
		);
		
		//fills Q values for the given state 
		for(ActionType a : type.actions){
			for(ActionType o : type.actions){
				JointAction ja = new JointAction();
				ja.addAction(a.associatedAction(null));
				ja.addAction(o.associatedAction(null));
				
				double myPayoff = Q.get(s).get(ja);
				fileWriter.write(
					String.format(Locale.ROOT, "%f %f ", myPayoff, -myPayoff)
				);
			}
		}
		fileWriter.close();
		
		// starts gambit and captures its output
		// TODO: checks for trivial games, avoiding unnecessary calls to gambit
		
		// using gambit-lcp because it returns a single equilibrium
		// 15-digit precision, hope numeric errors don't accumulate
		Process gambit = Runtime.getRuntime().exec("gambit-lcp -d 15 -q " + fileForGambit);
		BufferedReader bri = new BufferedReader (new InputStreamReader(gambit.getInputStream()));
		String result = bri.readLine();	//get the first equilibrium
	    bri.close();
		gambit.waitFor();
		
		// resulting String is NE,prob1a,prob1b,...,prob2a,prob2b 
		String[] parts = result.trim().split(",");
		
		double[] agentPolicy = new double[type.actions.size()];
		double[] opponentPolicy = new double[type.actions.size()];
		
		// reads policies
		for(int i = 0; i < agentPolicy.length; i++){
			// i+1 to skip the first token 'NE'
			agentPolicy[i] = Double.parseDouble(parts[i+1]);
			
			// opponent policy offset 'length' from agentPolicy 
			opponentPolicy[i] = Double.parseDouble(parts[i + agentPolicy.length + 1]);
		}
		
		return new Pair<double[], double[]>(agentPolicy, opponentPolicy);
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
		this.agentNumber = agentNum;
	}

	@Override
	public Action action(State s) {
		Pair<double[], double[]> policies = null;
		try {
			policies = getPoliciesFor(s);
		} catch (IOException | InterruptedException e) {
			System.err.println("Error while getting action for state " + s);
			e.printStackTrace();
			return null;
		}
		
		// uses the policy of row or column player depending on my number
		double[] myPolicy = agentNumber == 0 ? policies.m_a : policies.m_b;
		
		// use myPolicy to perform a roulette selection
		return rouletteSelection(myPolicy);
		
	}
	
	/**
	 * Selects an action according to the given probability vector
	 * Code from http://stackoverflow.com/a/10949834
	 * @param probabilities
	 * @return
	 */
	public Action rouletteSelection(double[] probabilities) {
	    float totalScore = 0;
	    float runningScore = 0;
	    for (double prob : probabilities) {
	        totalScore += prob;
	    }

	    float rnd = (float) (Math.random() * totalScore);

	    List<ActionType> actionTypes = this.type.actions;
	    
	    //for (gene g : genes) {
	    for(int i = 0; i < probabilities.length; i++){
	        if (rnd >= runningScore && rnd<=runningScore+ probabilities[i]){
	            //selected
	        	return actionTypes.get(i).associatedAction(null);
	        }
	        runningScore += probabilities[i];
	    }
	    
	    // wut? didn't find an action?
	    return null;
	}

	@Override
	public void observeOutcome(State s, JointAction jointAction, double[] jointReward, State sprime,
			boolean isTerminal) {
		// do nothing
	}

	@Override
	public void gameTerminated() {
		// do nothing
	}

	@Override
	public void saveKnowledge(String path) {
		if (!(domain instanceof EnumerableSGDomain)){
			System.err.println("Can only save knowledge for EnumerableSGDomains, this one is: " + domain.getClass().getName());
			return;
		}
		
		BufferedWriter fileWriter;
		try {
			fileWriter = new BufferedWriter(new FileWriter(path));

			EnumerableSGDomain enumDomain = (EnumerableSGDomain) domain;
			
			// xml root node
			fileWriter.write("<knowledge>\n\n"); 
			
			// information about who is saving knowledge, i.e., myself
			// this might be useful when retrieving the joint action later
			fileWriter.write(String.format(
				"<learner name='%s' type='%s' id='%d' />\n\n", 
				agentName(), this.getClass().getName(), agentNumber
			));
			
			// a friendly remark
			fileWriter.write(
				"<!-- Note: 'ja' stands for joint action\n"
				+ "Joint action name is agent0Action;agent1Action;... "
				+ " always in this order -->\n\n"
			);
			
			for (State s : enumDomain.enumerate()) {
				// opens state tag
				fileWriter.write(
					String.format(Locale.ROOT, "<state id='%s' value='%f'>\n", s, V.get(s))
				); 
				
				for(ActionType playerAction : this.type.actions){
					for(ActionType opponentAction : this.type.actions){
						JointAction ja = new JointAction();
						ja.addAction(playerAction.associatedAction(null));
						ja.addAction(opponentAction.associatedAction(null));
						
						// writes the joint action tag
						// action name is agent0Action;agent1Action;...
						// always in this order
						fileWriter.write(String.format(
							Locale.ROOT,
							"\t<ja name='%s' value='%f' />\n",
							ja.actionName(),
							Q.get(s).get(ja)
						));
						
					}
				}
				
					
				// closes state tag
				fileWriter.write("</state>\n\n");
			}
			
			// closes xml root
			fileWriter.write("</knowledge>\n"); 
			fileWriter.close();
		
		} catch (IOException e) {
			System.err.println("ERROR: Unable to save knowledge to file " + path);
			e.printStackTrace();
		}
	}

	@Override
	public void loadKnowledge(String path) {
		if (! (domain instanceof EnumerableSGDomain)){
			throw new RuntimeException("Cannot loadKnowledge if domain is not a EnumerableSGDomain");
		}
		
		//opens xml file
		DocumentBuilder dBuilder = null;
		Document doc = null;
		try {
			dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc = dBuilder.parse(new File(path));
		} catch (ParserConfigurationException|SAXException|IOException e) {
			System.err.println("ERROR when parsing " + path +". knowledge NOT LOADED!");
			e.printStackTrace();
			return;
		}
		
		//retrieves the game states, they'll be useful for filling the knowledge in
		Map<String, State> nameToState = ((EnumerableSGDomain)domain).namesToStates();
		
		//traverses all 1st level nodes of xml file (children of root node) 
		NodeList nList = doc.getDocumentElement().getChildNodes();
		
		for (int i = 0; i < nList.getLength(); i++){
			Node n = nList.item(i);

			//if node is 'learner', checks if ID is the same as mine
			if (n.getNodeName() == "learner"){
				Element e = (Element) n;
				if(Integer.parseInt(e.getAttribute("id")) != agentNumber){
					System.err.println(
						"WARNING! Loading knowledge with agent with different ID. "
						+ "I'll be probably loading a transposed reward matrix >:("
					);
				}
			}
			
			//if node is 'state', loads its value and that of joint actions for it
			else if (n.getNodeName() == "state"){
				Element e = (Element) n;
				
				String stateID = e.getAttribute("id");
				MicroRTSState state = (MicroRTSState) nameToState.get(stateID);
				V.put(state, Double.parseDouble(e.getAttribute("value")));
				
				// checks if need to allocate Q
				if(! Q.containsKey(state)){
					Q.put(state, new HashMap<>());
				}
				
				//jaNode stands for joint action node
				for(Node jaNode = n.getFirstChild(); jaNode != null; jaNode = jaNode.getNextSibling()){
					
					if (jaNode.getNodeType() != Node.ELEMENT_NODE) continue;	//prevents ClassCastException
					
					// process the node, filling in the joint action value
					Element jaElement = (Element) jaNode;
					
					// names of action components are separated by semicolon
					String names[] = jaElement.getAttribute("name").split(";");
					
					// fills the list of joint action components
					List<Action> components = new ArrayList<>();
					for (String name : names){
						components.add(
							this.domain.getActionType(name).associatedAction(null)
						);
					}
					
					// retrieves the joint action and sets its value
					JointAction ja = new JointAction(components);
					//JAQValue jaq = myQSource.getQValueFor(s, ja); 
					Q.get(state).put(ja, Double.parseDouble(jaElement.getAttribute("value")));
				}
			}
		}
	}
	
	/**
	 * Runs a test of Backward Induction
	 * @param args
	 * @throws IOException 
	 * @throws JDOMException 
	 */
	public static void main(String[] args) throws JDOMException, IOException{
		TerminalFunction tf = new MicroRTSTerminalFunction();
		AggregateDifferencesDomain domain = new AggregateDifferencesDomain();
		
		BackwardInduction bi = new BackwardInduction("test", domain, tf);
		
		System.out.println("Solving...");
		bi.solve((MicroRTSState) domain.getInitialState());
		System.out.println("\nSolved.");
		
		System.out.println("Saving...");
		bi.saveKnowledge("/tmp/backward-induction.xml");
		System.out.println("Done.");
		
	}
}
