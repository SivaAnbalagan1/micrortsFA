package tests.rl.models.stages;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import rl.models.singlestate.SingleState;
import rl.models.stages.GameStage;
import rl.models.stages.GameStages;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class GameStageTest {
	
	GameState underlyingState;

	@Before
	public void setUp() throws Exception {
		UnitTypeTable utt = new UnitTypeTable();
		underlyingState = new GameState(
			PhysicalGameState.load("src/tests/rl/models/stages/basesWorkers24x24.xml", utt), 
			utt
		);
	}

	@Test
	/**
	 * This test addresses the error that a copy of SingleState instance 
	 * whose stage is FINISHED, has stage OPENING
	 */
	public void testCopy(){
		GameStage original = new GameStage(underlyingState);
		original.setStage(GameStages.FINISHED);
		
		GameStage copy = (GameStage) original.copy();
		
		// ensures that copy is FINISHED
		assertEquals(GameStages.FINISHED, copy.getStage());
		
		// moreover, ensure that copy equals the original
		assertEquals(original, copy);
	}

}
