package tests.rl.models.singleagent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdom.JDOMException;
import org.junit.Before;
import org.junit.Test;

import ailoader.AILoader;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.UniversalActionType;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import rl.RLParamNames;
import rl.RLParameters;
import rl.models.aggregatediff.AggregateDiffState;
import rl.models.common.MicroRTSTerminalFunction;
import rl.models.common.ScriptActionTypes;
import rl.models.singleagent.SingleAgentJAM;
import rl.models.stages.GameStages;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class SingleAgentJAMTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSampleChangesStage() throws JDOMException, IOException {
		// defines the state
		UnitTypeTable unitTypeTable = new UnitTypeTable();
		PhysicalGameState physicalGameState = PhysicalGameState.load(
			"src/tests/rl/models/aggregatediff/basesWorkers24x24.xml", 
			unitTypeTable
		);
		GameState gs = new GameState(physicalGameState, unitTypeTable);
		AggregateDiffState currentState = new AggregateDiffState(gs);

		
		// retrieves possible actions and defines the joint action
		Map<String, UniversalActionType> actionMapping = ScriptActionTypes.getMapToLearnerActionTypes();
		List<Action> theActions = new ArrayList<>();
		theActions.add(actionMapping.get(ScriptActionTypes.WORKER_RUSH).associatedAction(null));
		theActions.add(actionMapping.get(ScriptActionTypes.WORKER_RUSH).associatedAction(null));
		JointAction ja = new JointAction(theActions);
		
		SingleAgentJAM jointActionModel = new SingleAgentJAM(
			ScriptActionTypes.getLearnerActionMapping(unitTypeTable), 
			AILoader.loadAI(ScriptActionTypes.PORTFOLIO_GREEDY_SEARCH, unitTypeTable)
		);
		
		State newState = jointActionModel.sample(currentState, ja);
		
		assertTrue(newState instanceof AggregateDiffState);
		AggregateDiffState newAggrState = (AggregateDiffState) newState;
		
		assertFalse(newAggrState.equals(currentState));
		
		//System.out.println(newAggrState.variableKeys());
		//System.out.println(newAggrState);
	}

	//@Test
	public void testSampleUntilTimeout() throws JDOMException, IOException{
		// defines the state
		UnitTypeTable unitTypeTable = new UnitTypeTable();
		PhysicalGameState physicalGameState = PhysicalGameState.load(
			"src/tests/rl/models/aggregatediff/basesWorkers24x24.xml", 
			unitTypeTable
		);
		GameState gs = new GameState(physicalGameState, unitTypeTable);
		AggregateDiffState currentState = new AggregateDiffState(gs);

		
		// retrieves possible actions and defines the joint action
		Map<String, UniversalActionType> actionMapping = ScriptActionTypes.getMapToLearnerActionTypes();
		List<Action> theActions = new ArrayList<>();
		
		//will pair worker rush vs worker rush, who run until timeout
		theActions.add(actionMapping.get(ScriptActionTypes.WORKER_RUSH).associatedAction(null));
		theActions.add(actionMapping.get(ScriptActionTypes.WORKER_RUSH).associatedAction(null));
		JointAction ja = new JointAction(theActions);
		
		SingleAgentJAM jointActionModel = new SingleAgentJAM(
			ScriptActionTypes.getLearnerActionMapping(unitTypeTable), 
			AILoader.loadAI(ScriptActionTypes.PUPPET_SEARCH, unitTypeTable)
		);
		
		// samples a new state until timeout
		State newState;
		while(true){
			newState = jointActionModel.sample(currentState, ja);
			
			assertTrue(newState instanceof AggregateDiffState);
			AggregateDiffState newAggrState = (AggregateDiffState) newState;
			
			// new state should be different from previous
			assertFalse(newAggrState.equals(currentState));
			
			GameState underlyingState = newAggrState.getUnderlyingState();

			// not game over yet
			assertFalse(underlyingState.gameover());
			
			// tests whether timeout was reached
			if(underlyingState.getTime() >= 
					(int) RLParameters.getInstance().getParameter(RLParamNames.GAME_DURATION)){
				break;
			}
			currentState = newAggrState;
		}
		
		TerminalFunction tf = new MicroRTSTerminalFunction();
		assertTrue(((AggregateDiffState) newState).getStage() == GameStages.FINISHED);
		assertTrue(tf.isTerminal(newState));
	}
}
