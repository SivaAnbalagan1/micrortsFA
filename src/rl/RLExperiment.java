package rl;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import burlap.behavior.stochasticgames.GameEpisode;
import burlap.debugtools.DPrint;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.world.World;
import rl.adapters.learners.PersistentLearner;
import rl.models.common.MicroRTSState;

/**
 * Manages a Reinforcement Learning experiment in microRTS The RL experiment has
 * two players (or learning agents), the 'world' which consists of an microRTS
 * abstraction and some experiment parameters
 * 
 * @author anderson
 *
 */
public class RLExperiment {

	public static void main(String[] args) {

		// parses command line arguments
		CommandLine cmdLine = processCommandLine(args);
		if (!cmdLine.hasOption(RLParamNames.CONFIG_FILE)) {
			System.err.println("Please provide the configuration file with -c or " + RLParamNames.CONFIG_FILE);
			System.exit(0);
		}

		// loads parameters from file
		Map<String, Object> parameters = null;

		RLParameters rlParams = RLParameters.getInstance();

		try {
			parameters = rlParams.loadFromFile(cmdLine.getOptionValue(RLParamNames.CONFIG_FILE));
		} catch (SAXException | IOException | ParserConfigurationException e) {
			System.err.println("Error while parsing configuration file...");
			e.printStackTrace();
			System.exit(0);
		}

		// overrides parameters with ones supplied in command line (if needed)
		parameters = rlParams.parametersFromCommandLine(cmdLine);
		
		// checks for the quiet parameter
		boolean quietLearning = (boolean) rlParams.getParameter(RLParamNames.QUIET_LEARNING);

		// adds players to the world
		World gameWorld = (World) rlParams.getParameter(RLParamNames.ABSTRACTION_MODEL);
		@SuppressWarnings("unchecked")
		List<PersistentLearner> agents = (List<PersistentLearner>) rlParams.getParameter(RLParamNames.PLAYERS);

		if (agents.size() < 2) {
			throw new RuntimeException("Less than 2 players were specified for the experiment");
		}
		
		// checks whether player 1 knowledge must be loaded
		if(parameters.containsKey(RLParamNames.PLAYER1_POLICY)){
			String path = (String) parameters.get(RLParamNames.PLAYER1_POLICY);
			agents.get(0).loadKnowledge(path);
			System.out.println("Loaded player 1 policy from " + path);
		}
		
		// checks whether player 2 knowledge must be loaded
		if(parameters.containsKey(RLParamNames.PLAYER2_POLICY)){
			String path = (String) parameters.get(RLParamNames.PLAYER2_POLICY);
			agents.get(1).loadKnowledge(path);
			System.out.println("Loaded player 2 policy from " + path);
		}
		
		for (SGAgent agent : agents) {
			gameWorld.join(agent);
		}

		// runs the experiment
		System.out.println("Starting experiment");
		
		Timestamp timestamp_i = new Timestamp(System.currentTimeMillis());
	        
		// don't have the world print out debug info (uncomment if you want to
		// see it!)
		DPrint.toggleCode(gameWorld.getDebugId(), false);
		int numEpisodes = (int) parameters.get(RLParamNames.EPISODES);
		// List<GameEpisode> episodes = new ArrayList<GameEpisode>(numEpisodes);

		// retrieves output dir
		String outDir = (String) parameters.get(RLParamNames.OUTPUT_DIR);
		File dir = new File(outDir);
		if (!dir.exists()) {
			if(dir.mkdirs() == false){
				throw new RuntimeException("Unable to create directory " + outDir);
			}
		}
		//PrintWriter output = null;

		// declares episode variable (will store final episode info after loop
		// finishes)
		GameEpisode episode = null;

		for (int episodeNumber = 0; episodeNumber < numEpisodes; episodeNumber++) {
			Timestamp timestamp_epi_i = new Timestamp(System.currentTimeMillis());
			episode = gameWorld.runGame();
			Timestamp timestamp_epi_f = new Timestamp(System.currentTimeMillis());
			
			// writes episode data using our format and BURLAP's default
			printEpisodeInfo(episodeNumber, episode, outDir + "/episode_" + episodeNumber + 
					".xml", timestamp_epi_i, timestamp_epi_f, agents);
			
			episode.write(String.format("%s/episode_%d", outDir, episodeNumber));
			
			// let them know that episode has finished
			System.out.print(String.format("\rEpisode #%7d finished.", episodeNumber));

			// writes knowledge of agents, unless we're meant to be quiet
			if (!quietLearning) {
				for (PersistentLearner agent : agents) {
					agent.saveKnowledge(String.format("%s/q_%s_%d.txt", outDir, agent.agentName(), episodeNumber));
				}
			}
		}

		Timestamp timestamp_f = new Timestamp(System.currentTimeMillis());
		
		// finished training
		System.out.println("\nTraining finished"); // has leading \n because previous print has no trailing \n

		// write final knowledge of agents
		for (PersistentLearner agent : agents) {
			agent.saveKnowledge(String.format("%s/q_%s_final.txt", outDir, agent.agentName()));
		}

		// prints experiment summary
		printExperimentSummary(outDir + "/summary.xml", args, timestamp_i, timestamp_f, numEpisodes);
	}

	private static CommandLine processCommandLine(String[] args) {
		// create Options object
		Options options = new Options();

		options.addOption("c", RLParamNames.CONFIG_FILE, true, "Path to configuration file.");
		options.addOption("o", RLParamNames.OUTPUT_DIR, true, "Directory to generate output.");
		options.addOption("q", RLParamNames.QUIET_LEARNING, true, "Don't output agent knowledge every episode.");
		options.addOption(RLParamNames.PLAYER1_POLICY, true, "Path to player 1 policy");
		options.addOption(RLParamNames.PLAYER2_POLICY, true, "Path to player 2 policy");
		
		CommandLine line = null;
		CommandLineParser parser = new DefaultParser();

		// parse the command line arguments
		try {
			line = parser.parse(options, args);
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Command line parsing failed.");
			exp.printStackTrace();
			System.err.println("Exiting");
			System.exit(0);
		}

		return line;

	}

	private static void printEpisodeInfo(int episodeNumber, GameEpisode episode, String path, Timestamp timestamp_epi_i, Timestamp timestamp_epi_f, List<PersistentLearner> agents) {
		MicroRTSState finalState = (MicroRTSState) episode.states.get(episode.states.size() - 1);		
		
		try {
			DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc_writer = dBuilder.newDocument();

			Element rootElement = doc_writer.createElement("episode");
			doc_writer.appendChild(rootElement);
			
			Attr attribute_ep = doc_writer.createAttribute("value");  
			attribute_ep.setValue(Integer.toString(episodeNumber));  
			rootElement.setAttributeNode(attribute_ep);
						
			int num = 0;
			for (PersistentLearner agent : agents) {
				Element names = doc_writer.createElement("agent_" + num);
				rootElement.appendChild(names);
			
				Attr attribute_dur = doc_writer.createAttribute("name");  
				attribute_dur.setValue(agent.agentName());  
				names.setAttributeNode(attribute_dur);
				num++;				
			}
			
			Element timeElement = doc_writer.createElement("timestamp");
			rootElement.appendChild(timeElement);

			Element initialTimestamp = doc_writer.createElement("initial-timestamp");
			timeElement.appendChild(initialTimestamp);
			
			Attr attribute_epi_i = doc_writer.createAttribute("value");  
			attribute_epi_i.setValue(timestamp_epi_i.toString());  
			initialTimestamp.setAttributeNode(attribute_epi_i);  
			
			Element finalTimestamp = doc_writer.createElement("final-timestamp");
			timeElement.appendChild(finalTimestamp);
			
			Attr attribute_epi_f = doc_writer.createAttribute("value");  
			attribute_epi_f.setValue(timestamp_epi_f.toString());  
			finalTimestamp.setAttributeNode(attribute_epi_f);
			
			Element duration = doc_writer.createElement("duration");
			rootElement.appendChild(duration);
			
			Attr attribute_dur = doc_writer.createAttribute("value");  
			attribute_dur.setValue(Double.toString(episode.numTimeSteps()));  
			duration.setAttributeNode(attribute_dur);
			
			Element states = doc_writer.createElement("states-visited");
			rootElement.appendChild(states);
			
			Attr attribute_st = doc_writer.createAttribute("value");  
			attribute_st.setValue(Integer.toString(episode.states.size()));  
			states.setAttributeNode(attribute_st);
			
			Element f_state = doc_writer.createElement("final-state");
			rootElement.appendChild(f_state);
			
			Attr attribute_f = doc_writer.createAttribute("value");  
			attribute_f.setValue(episode.states.get(episode.states.size() - 1).toString());  
			f_state.setAttributeNode(attribute_f);
			
			Element jointR = doc_writer.createElement("joint-rewards");
			rootElement.appendChild(jointR);
			
			for (double[] jr : episode.jointRewards) {
				Element joint_reward = doc_writer.createElement("joint-reward");
				jointR.appendChild(joint_reward);
				
				Attr attribute_jr = doc_writer.createAttribute("value");  
				attribute_jr.setValue(String.format(Locale.ROOT, "%f, %f", jr[0], jr[1]));  
				joint_reward.setAttributeNode(attribute_jr); 
			}
						
			Element jointAction = doc_writer.createElement("joint-actions");
			rootElement.appendChild(jointAction);
			
			for (JointAction ja : episode.jointActions) {
				Element joint_action = doc_writer.createElement("joint-action");
				jointAction.appendChild(joint_action);
				
				Attr attribute_i = doc_writer.createAttribute("value");  
				attribute_i.setValue(ja.toString());  
				joint_action.setAttributeNode(attribute_i); 
			}
			
			Element f_state_dump = doc_writer.createElement("final-state-dump");
			f_state_dump.appendChild(doc_writer.createTextNode(finalState.dump()));  
			rootElement.appendChild(f_state_dump);
			
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc_writer);
			StreamResult result = new StreamResult(new File(path));

			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);

			transformer.transform(source, result);
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		}	
	}

	private static void printExperimentSummary(String path, String[] args, Timestamp timestamp_i, Timestamp timestamp_f, int numEpisodes) {
		CommandLine cmdLine = processCommandLine(args);

		DocumentBuilder dBuilder = null;
		try {
			dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Document doc_reader = null;
		try {
			doc_reader = dBuilder.parse(new File(cmdLine.getOptionValue(RLParamNames.CONFIG_FILE)));
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			DocumentBuilder dBuilder2 = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc_writer = dBuilder2.newDocument();

			String root = doc_reader.getDocumentElement().getNodeName();
			Element rootElement = doc_writer.createElement(root);
			doc_writer.appendChild(rootElement);
			
			Element timeElement = doc_writer.createElement("timestamp");
			rootElement.appendChild(timeElement);

			Element initialTime = doc_writer.createElement("initial-timestamp");
			timeElement.appendChild(initialTime);
			
			Attr attribute_i = doc_writer.createAttribute("value");  
			attribute_i.setValue(timestamp_i.toString());  
			initialTime.setAttributeNode(attribute_i);  
			
			Element finalTime = doc_writer.createElement("final-timestamp");
			timeElement.appendChild(finalTime);
			
			Attr attribute_f = doc_writer.createAttribute("value");  
			attribute_f.setValue(timestamp_f.toString());  
			finalTime.setAttributeNode(attribute_f);  

			NodeList nList = doc_reader.getDocumentElement().getChildNodes();

			for (int i = 0; i < nList.getLength(); i++) {
				Node n = nList.item(i);

				if (n.getNodeName() == "parameters") {
					Element params = doc_writer.createElement(n.getNodeName());  
					rootElement.appendChild(params);
					
					NodeList paramList = n.getChildNodes();
					for (int j = 0; j < paramList.getLength(); j++) {
						Node paramNode = paramList.item(j);
												
						if (paramNode.getNodeName() != "#text" && paramNode.getNodeName() != "#comment") {
							Element e = doc_writer.createElement(paramNode.getNodeName());
							
							Attr attribute = doc_writer.createAttribute("value");  
							attribute.setValue(paramNode.getAttributes().item(0).getNodeValue());  
							e.setAttributeNode(attribute);  
							
							params.appendChild(e);							
						}
					}					 
				}

				if (n.getNodeName().equals("player")) {
					Element params = doc_writer.createElement(n.getNodeName()); 
					
					NamedNodeMap attrPlayer = n.getAttributes();
					for (int k = 0; k < attrPlayer.getLength(); k++) {
						Attr attribute = doc_writer.createAttribute(n.getAttributes().item(k).getNodeName());  
						attribute.setValue(n.getAttributes().item(k).getNodeValue());  
						params.setAttributeNode(attribute);  
					}
					
					rootElement.appendChild(params);
					
					NodeList playerList = n.getChildNodes();
					for (int j = 0; j < playerList.getLength(); j++) {
						Node playerNode = playerList.item(j); 
						
						if (playerNode.getNodeName() != "#text" && playerNode.getNodeName() != "#comment") {
							Element e = doc_writer.createElement(playerNode.getNodeName());
							
							Attr attribute = doc_writer.createAttribute("value");  
							attribute.setValue(playerNode.getAttributes().item(0).getNodeValue());  
							e.setAttributeNode(attribute); 
							
							params.appendChild(e);							
						}
					}		
				}
			}
			
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc_writer);
			StreamResult result = new StreamResult(new File(path));

			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);

			transformer.transform(source, result);

			System.out.println("File saved!");

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		}
	}

}
