package tests.rl.models.singlestate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdom.JDOMException;
import org.junit.Before;
import org.junit.Test;

import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.UniversalActionType;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import rl.RLParamNames;
import rl.RLParameters;
import rl.models.common.MicroRTSTerminalFunction;
import rl.models.common.ScriptActionTypes;
import rl.models.singlestate.SingleState;
import rl.models.singlestate.SingleStateJAM;
import rl.models.stages.GameStages;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class SingleStateJAMTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() throws JDOMException, IOException {
		// defines the state (both underlying and abstract)
		UnitTypeTable unitTypeTable = new UnitTypeTable();
		PhysicalGameState physicalGameState = PhysicalGameState.load(
			"src/tests/rl/models/singlestate/basesWorkers24x24.xml", 
			unitTypeTable
		);
		GameState gs = new GameState(physicalGameState, unitTypeTable);
		SingleState currentState = new SingleState(gs);
		
		// instantiates the JAM
		SingleStateJAM jointActionModel = new SingleStateJAM(
			ScriptActionTypes.getLearnerActionMapping(unitTypeTable)
		);
		
		// retrieves possible actions and defines the joint action
		Map<String, UniversalActionType> actionMapping = ScriptActionTypes.getMapToLearnerActionTypes();
		
		//regardless of the joint actions, the next state should be FINISHED
		for (UniversalActionType type1 : actionMapping.values()){
			for (UniversalActionType type2 : actionMapping.values()){
				// creates the joint action with current types
				List<Action> theActions = new ArrayList<>();
				theActions.add(type1.associatedAction(null));
				theActions.add(type2.associatedAction(null));
				JointAction ja = new JointAction(theActions);
				
				State newState = jointActionModel.sample(currentState, ja);
				
				assertTrue(newState instanceof SingleState);
				SingleState newSingleState = (SingleState) newState;
				assertTrue(newSingleState.getStage() == GameStages.FINISHED);
			}
		}
	}
	
	@Test
	public void testSampleUntilTimeout() throws JDOMException, IOException{
		
		// defines the state
		UnitTypeTable unitTypeTable = new UnitTypeTable();
		PhysicalGameState physicalGameState = PhysicalGameState.load(
			"src/tests/rl/models/aggregatediff/basesWorkers24x24.xml", 
			unitTypeTable
		);
		GameState gs = new GameState(physicalGameState, unitTypeTable);
		SingleState currentState = new SingleState(gs);
		
		// instantiates the JAM
		SingleStateJAM jointActionModel = new SingleStateJAM(
			ScriptActionTypes.getLearnerActionMapping(unitTypeTable)
		);
		
		// retrieves possible actions and defines the joint action
		Map<String, UniversalActionType> actionMapping = ScriptActionTypes.getMapToLearnerActionTypes();
		List<Action> theActions = new ArrayList<>();
		
		//will pair worker rush vs worker rush, who run until timeout
		theActions.add(actionMapping.get(ScriptActionTypes.WORKER_RUSH).associatedAction(null));
		theActions.add(actionMapping.get(ScriptActionTypes.WORKER_RUSH).associatedAction(null));
		JointAction ja = new JointAction(theActions);
		
		// samples a new state until timeout
		State newState;
		while(true){
			newState = jointActionModel.sample(currentState, ja);
			
			assertTrue(newState instanceof SingleState);
			SingleState newSingleState = (SingleState) newState;
			
			// new state should be different from previous
			assertFalse(newSingleState.equals(currentState));
			
			GameState underlyingState = newSingleState.getUnderlyingState();

			// not game over yet
			assertFalse(underlyingState.gameover());
			
			// tests whether timeout was reached
			if(underlyingState.getTime() >= 
					(int) RLParameters.getInstance().getParameter(RLParamNames.GAME_DURATION)){
				break;
			}
			currentState = newSingleState;
		}
		
		TerminalFunction tf = new MicroRTSTerminalFunction();
		assertTrue(((SingleState) newState).getStage() == GameStages.FINISHED);
		assertTrue(tf.isTerminal(newState));
	}
}
