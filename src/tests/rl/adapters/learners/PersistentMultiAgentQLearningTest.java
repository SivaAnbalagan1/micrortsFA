package tests.rl.adapters.learners;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import burlap.behavior.stochasticgames.madynamicprogramming.backupOperators.MinMaxQ;
import burlap.debugtools.DPrint;
import burlap.mdp.core.action.Action;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.world.World;
import burlap.statehashing.simple.SimpleHashableStateFactory;
import rl.WorldFactory;
import rl.adapters.learners.PersistentMultiAgentQLearning;
import rl.models.stages.GameStage;
import rl.models.stages.GameStages;

public class PersistentMultiAgentQLearningTest {

	final String PATH_TO_KNOWLEDGE = "src/tests/rl/maql-player-example.xml";
	
	PersistentMultiAgentQLearning player;
	PersistentMultiAgentQLearning rival;
	
	World microRTSStages;
	
	@Before
	public void setUp() throws Exception {
		
		microRTSStages = WorldFactory.stages();
		
		//agent to be tested
		player = new PersistentMultiAgentQLearning(
			microRTSStages.getDomain(), .9, .1, new SimpleHashableStateFactory(),
			1, new MinMaxQ(), true, "PLAYER", 
			new SGAgentType("MiniMaxQ", microRTSStages.getDomain().getActionTypes())
		);
		
		//another agent
		rival = new PersistentMultiAgentQLearning(
			microRTSStages.getDomain(), .9, .1, new SimpleHashableStateFactory(),
			1, new MinMaxQ(), true, "RIVAL", 
			new SGAgentType("MiniMaxQ", microRTSStages.getDomain().getActionTypes())
		);
		
		//must 'prepare' a match so that agents initialize their structures
		microRTSStages.join(player);
		microRTSStages.join(rival);
		
		DPrint.toggleCode(microRTSStages.getDebugId(), false);
		//for (int i = 0; i < 20; i++)
			microRTSStages.runGame(0);	//run a game with zero stages, so that functions are not updated (?)
		
		
	}

	@Test
	public void testLoadKnowledge() {
		this.assertLoadedKnowledge(PATH_TO_KNOWLEDGE);
	}
	
	/**
	 * Checks whether loaded knowledge in specified path 
	 * matches the one defined in 'PATH_TO_KNOWLEDGE' file
	 * with some tolerance for numeric errors
	 * @param path
	 */
	private void assertLoadedKnowledge(String path){
		player.loadKnowledge(path);
		
		/*
		 * most states and joint actions have value 1.0 in the file
		 * but here we test the ones whose value is different
		 */
		
		// declares the state and the joint action which are used throughout this test
		GameStage state = new GameStage();
		JointAction ja;
		
		
		// testing state 'OPENING' and joint action 'LightRush;LightRush'
		state.setStage(GameStages.OPENING);
		ja = constructJointAction("LightRush", "LightRush");
		assertEquals(.7, player.getMyQSource().getQValueFor(state, ja).q, 0.00001);
		
		
		// testing state 'EARLY' and joint action 'RangedRush;Expand'
		state.setStage(GameStages.EARLY);
		ja = constructJointAction("RangedRush", "Expand");
		assertEquals(-.33, player.getMyQSource().getQValueFor(state, ja).q, 0.00001);
		
		
		// testing state 'MID' and joint action 'WorkerRush;BuildBarracks' value='0.89'
		state.setStage(GameStages.MID);
		ja = constructJointAction("WorkerRush", "BuildBarracks");
		assertEquals(.89, player.getMyQSource().getQValueFor(state, ja).q, 0.00001);
		
		// testing state 'LATE' and joint action 'WorkerRush;RangedRush' value='-1'
		state.setStage(GameStages.LATE);
		ja = constructJointAction("WorkerRush", "RangedRush");
		assertEquals(-1, player.getMyQSource().getQValueFor(state, ja).q, 0.00001);
		
		// testing state 'END' and joint action 'WorkerRush;WorkerRush' value='0' 
		state.setStage(GameStages.END);
		ja = constructJointAction("WorkerRush", "WorkerRush");
		assertEquals(0, player.getMyQSource().getQValueFor(state, ja).q, 0.00001);
		
		// TODO test whether other states have 1.0 in value 
	}
	
	@Test
	/**
	 * This test assumes that testLoadKnowledge is working
	 */
	public void testSaveKnowledge() throws FileNotFoundException {
		String tmpKnowledgeFile = "/tmp/player-knowledge.xml";
		// if loadKnowledge is working, save knowledge should yield the same file
		player.loadKnowledge(PATH_TO_KNOWLEDGE);
		player.saveKnowledge(tmpKnowledgeFile);
		
		this.assertLoadedKnowledge(tmpKnowledgeFile);
	}

	private JointAction constructJointAction(String component1, String component2) {

		List<Action> components = new ArrayList<>();
		components.add(
			microRTSStages.getDomain().getActionType(component1).associatedAction(null)
		);
		components.add(
			microRTSStages.getDomain().getActionType(component2).associatedAction(null)
		);
		
		return new JointAction(components);
	}

}
