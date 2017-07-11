package tests.rl.planners;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.Action;
import burlap.mdp.stochasticgames.JointAction;
import rl.models.aggregatediff.AggregateDiffState;
import rl.models.aggregatediff.AggregateDifferencesDomain;
import rl.models.common.MicroRTSTerminalFunction;
import rl.models.common.ScriptActionTypes;
import rl.planners.BackwardInduction;

public class TestBackwardInduction {

	final String PATH_TO_KNOWLEDGE = "src/tests/rl/planners/bi-knowledge.xml";
	BackwardInduction agent;
	
	@Before
	public void setUp() throws Exception {
		AggregateDifferencesDomain domain = new AggregateDifferencesDomain();
		TerminalFunction terminal = new MicroRTSTerminalFunction();
		
		agent = new BackwardInduction("tested", domain, terminal);
	}
	
	@Test
	public void testLoadKnowledge(){
		this.assertLoadedKnowledge(PATH_TO_KNOWLEDGE);
	}

	/**
	 * Checks whether loaded knowledge in specified path 
	 * matches the one defined in 'PATH_TO_KNOWLEDGE' file
	 * with some tolerance for numeric errors
	 * 
	 * Values in that file were made up and do not correspond to a real game situation
	 * @param path
	 */
	public void assertLoadedKnowledge(String path) {
		agent.loadKnowledge(path);
		
		AggregateDiffState state;
		JointAction ja;
		
		state = AggregateDiffState.fromString("OPENING;BEHIND;BEHIND;BEHIND;BEHIND;BEHIND;BEHIND;BEHIND");
		assertEquals(0., agent.value(state), 0.00001);
		ja = constructJointAction("LightRush", "LightRush");
		assertEquals(0.01, agent.value(state, ja), 0.00001);
		
		ja = constructJointAction("LightRush", "WorkerRush");
		assertEquals(0.7, agent.value(state, ja), 0.00001);
		
		ja = constructJointAction("HeavyRush", "WorkerRush");
		assertEquals(0., agent.value(state, ja), 0.00001);
		
		state = AggregateDiffState.fromString("MID;EVEN;EVEN;BEHIND;EVEN;EVEN;EVEN;AHEAD");
		assertEquals(1., agent.value(state), 0.00001);
		ja = constructJointAction("LightRush", "LightRush");
		assertEquals(1., agent.value(state, ja), 0.00001);
		
		ja = constructJointAction("LightRush", "Expand");
		assertEquals(-1., agent.value(state, ja), 0.00001);
		
		ja = constructJointAction("BuildBarracks", "WorkerRush");
		assertEquals(-1., agent.value(state, ja), 0.00001);
		
	}
	
	@Test
	public void testSaveKnowledge() throws FileNotFoundException {
		String tmpKnowledgeFile = "/tmp/bi-knowledge.xml";
		// if loadKnowledge is working, save knowledge should yield the same file
		agent.loadKnowledge(PATH_TO_KNOWLEDGE);
		agent.saveKnowledge(tmpKnowledgeFile);
		
		this.assertLoadedKnowledge(tmpKnowledgeFile);
	}
	
	private JointAction constructJointAction(String component1, String component2) {

		Map<String, Action> nameToAction = ScriptActionTypes.getMapToLearnerActions();
		List<Action> components = new ArrayList<>();
		components.add(nameToAction.get(component1));
		components.add(nameToAction.get(component2));
		
		return new JointAction(components);
	}

}
