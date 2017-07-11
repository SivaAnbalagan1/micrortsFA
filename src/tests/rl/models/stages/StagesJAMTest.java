package tests.rl.models.stages;

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
import rl.models.stages.GameStage;
import rl.models.stages.GameStages;
import rl.models.stages.GameStagesDomain;
import rl.models.stages.StagesJointActionModel;
import rts.GameState;

public class StagesJAMTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSample() throws JDOMException, IOException {
		GameStagesDomain domain = new GameStagesDomain(); 
		StagesJointActionModel jointActionModel = (StagesJointActionModel) domain.getJointActionModel();
		
		// gets the initial state
		GameStage initial = (GameStage) domain.getInitialState();
		
		// retrieves the available actions
		List<UniversalActionType> portfolio = ScriptActionTypes.getLearnerActionTypes(); 
		
		// tests for all action combinations
		for (UniversalActionType uat1 : portfolio){
			for(UniversalActionType uat2 : portfolio){
				JointAction ja = new JointAction();
				ja.addAction(uat1.associatedAction(null));
				ja.addAction(uat2.associatedAction(null));
				
				State newState = jointActionModel.sample(initial, ja);
				
				// regardless of which pair of actions is issued, I should get a different state than initial
				assertFalse(newState.equals(initial));
				
				//System.out.println("Stage is " + newState.get("stage") + ", action: " + ja);
				// moreover, next state should be 'EARLY' or 'FINISHED'
				assertTrue(
					"Stage is " + newState.get("stage") + ", action: " + ja,
					newState.get("stage") == GameStages.EARLY || 
					newState.get("stage") == GameStages.FINISHED 
				);
			}
		}
	}
	
	@Test
	public void testSampleUntilTimeout() throws JDOMException, IOException{
		GameStagesDomain domain = new GameStagesDomain(); 
		StagesJointActionModel jointActionModel = (StagesJointActionModel) domain.getJointActionModel();
		
		// gets the initial state
		GameStage currentState = (GameStage) domain.getInitialState();
		
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
			
			assertTrue(newState instanceof GameStage);
			GameStage newGameStage = (GameStage) newState;
			
			// new state should be different from previous
			assertFalse(newGameStage.equals(currentState));
			
			GameState underlyingState = newGameStage.getUnderlyingState();

			// not game over yet
			assertFalse(underlyingState.gameover());
			
			// tests whether timeout was reached
			if(underlyingState.getTime() >= 
					(int) RLParameters.getInstance().getParameter(RLParamNames.GAME_DURATION)){
				break;
			}
			currentState = newGameStage;
		}
		
		TerminalFunction tf = new MicroRTSTerminalFunction();
		assertTrue(((GameStage) newState).getStage() == GameStages.FINISHED);
		assertTrue(tf.isTerminal(newState));
	}

}
