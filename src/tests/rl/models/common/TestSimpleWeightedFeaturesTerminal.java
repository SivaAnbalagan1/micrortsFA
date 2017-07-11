package tests.rl.models.common;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.jdom.JDOMException;
import org.junit.Before;
import org.junit.Test;

import ai.evaluation.SimpleSqrtEvaluationFunction;
import burlap.mdp.stochasticgames.model.JointRewardFunction;
import rl.models.common.MicroRTSState;
import rl.models.common.SimpleWeightedFeaturesTerminal;
import rl.models.stages.GameStage;
import rts.GameState;

public class TestSimpleWeightedFeaturesTerminal {

	JointRewardFunction rwd;
	SimpleSqrtEvaluationFunction evalFunc;

	@Before
	public void setUp() throws Exception {
		rwd = new SimpleWeightedFeaturesTerminal();
		evalFunc = new SimpleSqrtEvaluationFunction();
	}


	@Test
	public void testIntermediateReward() throws JDOMException, IOException {
		GameState gameState = TestSimpleWeightedFeatures.loadGameState(
			"src/tests/rl/models/common/basesWorkers16x16.xml"
		);
		
		MicroRTSState theState = new GameStage(gameState);
		
		//ain't a terminal state, reward should be zero
		double[] actualRwd = rwd.reward(null, null, theState);
		assertEquals(0, actualRwd[0], 0.00001);
		assertEquals(0, actualRwd[0], 0.00001);
	}
	
	@Test
	public void testFinalReward() throws JDOMException, IOException{
		GameState gameState = TestSimpleWeightedFeatures.loadGameState(
			"src/tests/rl/models/common/basesWorkers16x16_finished.xml"
		);
		
		MicroRTSState theState = new GameStage(gameState);
		
		//in a terminal state, reward should be given by native eval. function
		double[] actualRwd = rwd.reward(null, null, theState);
		assertEquals(evalFunc.evaluate(0, 1, gameState), actualRwd[0], 0.00001);
		assertEquals(evalFunc.evaluate(1, 0, gameState), actualRwd[1], 0.00001);
	}

}
