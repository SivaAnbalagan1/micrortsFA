package rl.adapters.learners;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import burlap.behavior.learningrate.LearningRate;
import burlap.behavior.stochasticgames.agents.maql.MultiAgentQLearning;
import burlap.behavior.stochasticgames.madynamicprogramming.JAQValue;
import burlap.behavior.stochasticgames.madynamicprogramming.SGBackupOperator;
import burlap.behavior.valuefunction.QFunction;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.SGDomain;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.statehashing.HashableStateFactory;
import rl.adapters.domain.EnumerableSGDomain;

/**
 * Extends {@link MultiAgentQLearning} class by implementing {@link PersistentLearner}
 * thus allowing saving and loading knowledge
 * @author anderson
 *
 */
public class PersistentMultiAgentQLearning extends MultiAgentQLearning implements PersistentLearner {

	public PersistentMultiAgentQLearning(SGDomain d, double discount, double learningRate,
			HashableStateFactory hashFactory, double qInit, SGBackupOperator backupOperator,
			boolean queryOtherAgentsForTheirQValues, String agentName, SGAgentType agentType) {
		super(d, discount, learningRate, hashFactory, qInit, backupOperator, queryOtherAgentsForTheirQValues, agentName,
				agentType);
	}

	public PersistentMultiAgentQLearning(SGDomain d, double discount, LearningRate learningRate,
			HashableStateFactory hashFactory, QFunction qInit, SGBackupOperator backupOperator,
			boolean queryOtherAgentsForTheirQValues, String agentName, SGAgentType agentType) {
		super(d, discount, learningRate, hashFactory, qInit, backupOperator, queryOtherAgentsForTheirQValues, agentName,
				agentType);
	}

	@Override
	public void saveKnowledge(String path) {
		if(world == null){
			System.err.println(
				"ERROR: cannot save knowledge because world is null. Have I been initialized?"
			);
			return;
		}
		
		/*if(world.getRegisteredAgents().size() != 2){
			System.err.println(
				"ERROR: cannot save knowledge because world does not have 2 agents. It has: " +
				world.getRegisteredAgents().size()
			);
			return;
		}*/
		
		BufferedWriter fileWriter;
		//Yaml yaml = new Yaml();
		try {
			fileWriter = new BufferedWriter(new FileWriter(path));

			if (domain instanceof EnumerableSGDomain){
				EnumerableSGDomain enumDomain = (EnumerableSGDomain) domain;
				
				// xml root node
				fileWriter.write("<knowledge>\n\n"); 
				
				// information about who is saving knowledge, i.e., myself
				// this might be useful when retrieving the joint action later
				fileWriter.write(String.format(
					"<learner name='%s' type='%s' id='%d' />\n\n", 
					worldAgentName, this.getClass().getName(), agentNum
				));
				
				// a friendly remark
				fileWriter.write(
					"<!-- Note: 'ja' stands for joint action\n"
					+ "Joint action name is agent0Action;agent1Action;... "
					+ " always in this order -->\n\n"
				);
				
				for (State s : enumDomain.enumerate()) {
					// opens state tag
					fileWriter.write(String.format("<state id='%s'>\n", s)); 
					
					// runs through joint actions and write their values
					List<JointAction> jointActions = JointAction.getAllJointActions(
						s, 
						world.getRegisteredAgents()
					);
					
					for(JointAction jointAction : jointActions){
						// writes the joint action tag
						// action name is agent0Action;agent1Action;...
						// always in this order
						fileWriter.write(String.format(
							"\t<ja name='%s' value='%s' />\n",
							jointAction.actionName(),
							getMyQSource().getQValueFor(s, jointAction).q
						));
					}
					
					// closes state tag
					fileWriter.write("</state>\n\n");
				}
				
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
				if(Integer.parseInt(e.getAttribute("id")) != agentNum){
					System.err.println(
						"WARNING! Loading knowledge with agent with different ID. "
						+ "I'll be probably loading a transposed reward matrix >:("
					);
				}
			}
			//if node is 'state', loads the value of joint actions for that state
			else if (n.getNodeName() == "state"){
				Element e = (Element) n;
				String stateID = e.getAttribute("id");
				
				
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
					State s = nameToState.get(stateID);
					JointAction ja = new JointAction(components);
					JAQValue jaq = myQSource.getQValueFor(s, ja); 
					jaq.q = Float.parseFloat(jaElement.getAttribute("value"));
				}
			}
		}
	}
}
