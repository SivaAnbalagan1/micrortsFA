package rl.adapters.learners;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.stochasticgames.agents.interfacing.singleagent.LearningAgentToSGAgentInterface;
import burlap.behavior.valuefunction.QValue;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.SGDomain;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import rl.adapters.domain.EnumerableSGDomain;

/**
 * Adapts a single agent learning method to stochastic games
 * and provides saving and loading of knowledge.
 * So far, provides support for QLearning as the single agent learning method
 * @author anderson
 *
 */
public class SGQLearningAdapter extends LearningAgentToSGAgentInterface implements PersistentLearner {

	private QLearning learner;
	
	public SGQLearningAdapter(SGDomain domain, LearningAgent learningAgent, String agentName, SGAgentType agentType) {
		super(domain, learningAgent, agentName, agentType);
		
		if(learningAgent instanceof QLearning){
			learner = (QLearning) learningAgent;
		}
		else {
			throw new RuntimeException(
				"SGQLearningAdapter supports only QLearning as single agent learner"
			);
			//TODO make SGQLearningAdapter accept other single agent learning algorithms
		}
	}
	
	/**
	 * Returns the single agent learner object
	 * @return
	 */
	public LearningAgent getSingleAgentLearner(){
		return learner;
	}

	@Override
	public void saveKnowledge(String path) {
		//learner.writeQTable(path);
		
		BufferedWriter fileWriter;
		try {
			fileWriter = new BufferedWriter(new FileWriter(path));

			if (domain instanceof EnumerableSGDomain){
				EnumerableSGDomain enumDomain = (EnumerableSGDomain) domain;
				
				// xml root node
				fileWriter.write("<knowledge>\n\n"); 
				
				// information about who is saving knowledge, i.e., myself
				// this might be useful when loading knowledge later
				fileWriter.write(String.format(
					"<learner name='%s' type='%s' id='%d' />\n\n", 
					worldAgentName, this.getClass().getName(), agentNum
				));
				
				// a friendly remark
				fileWriter.write(
					"<!-- Note: 'a' stands for action -->\n\n"
				);
				
				for (State s : enumDomain.enumerate()) {
					// opens state tag
					fileWriter.write(String.format("<state id='%s'>\n", s)); 
					
					// runs through qValues for the current state
					for(QValue qValue : learner.qValues(s)){
						// writes the action tag
						fileWriter.write(String.format(
							"\t<a name='%s' value='%s' />\n",
							qValue.a,
							qValue.q
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
		//learner.loadQTable(path);
		
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
			//if node is 'state', loads the value of joint actions for that state
			else if (n.getNodeName() == "state"){
				Element e = (Element) n;
				String stateID = e.getAttribute("id");
				
				State s = nameToState.get(stateID);
				
				// clears and fills the list of QValues of current state with loaded values
				List<QValue> qValues = learner.qValues(s);
				qValues.clear();
				
				for(Node actionNode = n.getFirstChild(); actionNode != null; actionNode = actionNode.getNextSibling()){
					
					if (actionNode.getNodeType() != Node.ELEMENT_NODE) continue;	//prevents ClassCastException
					
					// retrieves the action object 
					Element actionElement = (Element) actionNode;
					String name = actionElement.getAttribute("name");
					Action a = this.domain.getActionType(name).associatedAction(null);
					
					// creates and stores the loaded QValue
					QValue loadedQ = new QValue(s, a, Float.parseFloat(actionElement.getAttribute("value")));
					qValues.add(loadedQ);
				}
			}
		}
				
	}

}
