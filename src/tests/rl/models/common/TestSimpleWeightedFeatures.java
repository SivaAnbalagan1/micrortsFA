package tests.rl.models.common;

import static org.junit.Assert.*;

import java.io.IOException;

import org.jdom.JDOMException;
import org.junit.Before;
import org.junit.Test;

import ai.evaluation.SimpleSqrtEvaluationFunction;
import burlap.mdp.stochasticgames.model.JointRewardFunction;
import rl.models.common.MicroRTSState;
import rl.models.common.SimpleWeightedFeatures;
import rl.models.stages.GameStage;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class TestSimpleWeightedFeatures {
	
	JointRewardFunction rwd;
	SimpleSqrtEvaluationFunction evalFunc;

	@Before
	public void setUp() throws Exception {
		rwd = new SimpleWeightedFeatures();
		evalFunc = new SimpleSqrtEvaluationFunction();
	}

	@Test
	public void testReward() throws JDOMException, IOException {
		
		GameState gameState = loadGameState("src/tests/rl/models/common/basesWorkers16x16.xml");
		MicroRTSState theState = new GameStage(gameState);
		
		double[] reward = rwd.reward(null, null, theState);
		
		// compares the received reward with the expected from native evaluation function
		assertEquals(evalFunc.evaluate(0, 1, gameState), reward[0], 0.0001);
		assertEquals(evalFunc.evaluate(1, 0, gameState), reward[1], 0.0001);
		
	}

	/**
	 * Loads a {@link GameState} from a given path, using a default {@link UnitTypeTable}
	 * TODO move this to an auxiliary/common class
	 * @param path
	 * @return
	 * @throws JDOMException
	 * @throws IOException
	 */
	public static GameState loadGameState(String path) throws JDOMException, IOException{
		UnitTypeTable utt = new UnitTypeTable();
		PhysicalGameState physicalGameState = PhysicalGameState.load(path, utt); 
		return new GameState(physicalGameState, utt);
	}
}
