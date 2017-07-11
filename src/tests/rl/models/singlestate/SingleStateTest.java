package tests.rl.models.singlestate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rl.models.singlestate.SingleState;
import rl.models.stages.GameStage;
import rl.models.stages.GameStages;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class SingleStateTest {
	
	GameState underlyingState;

	@Before
	public void setUp() throws Exception {
		UnitTypeTable utt = new UnitTypeTable();
		underlyingState = new GameState(
			PhysicalGameState.load("src/tests/rl/models/singlestate/basesWorkers24x24.xml", utt), 
			utt
		);
	}
	
	

	@Test
	public void testVariableKeys() {
		List<String> expectedKeys = new ArrayList<>();
		expectedKeys.add(GameStage.KEY_STAGE);

		@SuppressWarnings("unchecked")
		List<String> actualKeys = (List<String>)(Object)new SingleState().variableKeys();
		
		assertArrayEquals(expectedKeys.toArray(), actualKeys.toArray());
	}
	
	@Test
	public void testFrameToStage(){
		assertEquals(GameStages.OPENING, SingleState.frameToStage(9000));
	}
	
	@Test
	/**
	 * This test addresses the error that a copy of SingleState instance 
	 * whose stage is FINISHED, has stage OPENING
	 */
	public void testCopy(){
		SingleState original = new SingleState(underlyingState);
		original.setStage(GameStages.FINISHED);
		
		SingleState copy = (SingleState) original.copy();
		
		// ensures that copy is FINISHED
		assertEquals(GameStages.FINISHED, copy.getStage());
		
		// moreover, ensure that copy equals the original
		assertEquals(original, copy);
	}
	
	@Test
	public void testAllStates(){
		List<GameStage> allStates = SingleState.allStates();
		assertEquals(1, allStates.size());
		assertEquals(GameStages.OPENING, allStates.get(0).getStage());
	}

}
