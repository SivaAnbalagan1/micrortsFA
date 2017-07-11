package tests.rl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.metagame.DummyPolicy;
import burlap.behavior.learningrate.ExponentialDecayLR;
import burlap.behavior.learningrate.LearningRate;
import burlap.behavior.policy.EpsilonGreedy;
import burlap.behavior.policy.Policy;
import burlap.behavior.singleagent.MDPSolver;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.stochasticgames.PolicyFromJointPolicy;
import burlap.behavior.stochasticgames.madynamicprogramming.policies.EMinMaxPolicy;
import burlap.behavior.valuefunction.QFunction;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.world.World;
import rl.RLParamNames;
import rl.RLParameters;
import rl.adapters.gamenatives.PortfolioAIAdapter;
import rl.adapters.learners.PersistentMultiAgentQLearning;
import rl.adapters.learners.SGQLearningAdapter;
import rl.models.aggregatediff.AggregateDifferencesDomain;
import rl.models.common.MicroRTSRewardFactory;
import rl.models.singleagent.SingleAgentDomain;
import tests.rl.adapters.learners.SGQLearningAdapterTest;

public class RLParametersTest {
	
	@Before
	public void setUp(){
		// reset is required, because the instance persists between tests
		RLParameters.getInstance().reset();
	}

	@Test
	/**
	 * Tests whether configurations in example.xml is properly loaded
	 */
	public void testExampleXML() throws SAXException, IOException, ParserConfigurationException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		RLParameters rlParams = RLParameters.getInstance();
		
		Map<String, Object> parameters =  rlParams.loadFromFile("src/tests/rl/example.xml"); //may throw exceptions
		
		// tests parameter values
		assertEquals(100, (int)parameters.get(RLParamNames.EPISODES));
		assertEquals(MicroRTSRewardFactory.SIMPLE_WEIGHTED, parameters.get(RLParamNames.REWARD_FUNCTION));
		assertTrue((boolean) parameters.get(RLParamNames.QUIET_LEARNING));
		
		World w = (World) rlParams.getParameter(RLParamNames.ABSTRACTION_MODEL);
		assertTrue(w.getDomain() instanceof AggregateDifferencesDomain);
		
		@SuppressWarnings("unchecked")
		List<SGAgent> players = (List<SGAgent>) rlParams.getParameter(RLParamNames.PLAYERS);
		assertEquals(2, players.size());
		
		for(SGAgent player : players){
			
			//casts the player and tests its attributes 
			SGQLearningAdapter sgql = (SGQLearningAdapter) player;
			if (sgql.agentName().equals("learner")){ 
				
				QLearning ql = (QLearning) sgql.getSingleAgentLearner();
				//tests whether attributes were correctly loaded
				
				//code to test epsilon
				Field policyField = revealField(ql, "learningPolicy");
				Policy pi = (Policy) policyField.get(ql);
				assertTrue(pi instanceof EpsilonGreedy);
				assertEquals(0, ((EpsilonGreedy)pi).getEpsilon(), 0.000001);
				
				//code to test learning rate:
				Field lrField = revealField(ql, "learningRate");
				LearningRate lr = (LearningRate) lrField.get(ql);
				assertEquals(0.1, lr.peekAtLearningRate(null, null), 0.0000001);
				
				//code to test: initialQ
				Field initialQField = revealField(ql, "qInitFunction");
				QFunction initialQ = (QFunction) initialQField.get(ql);
				assertEquals(1, initialQ.value(null), 0.0000001);
				
				//code to test discount 
				Field discountField = revealField(MDPSolver.class, "gamma");
				double discount = (double) discountField.get(ql);
				assertEquals(0.9, discount, 0.0000001);
				
				// tests player knowledge
				SGQLearningAdapterTest test = new SGQLearningAdapterTest();
				test.assertLoadedKnowledgeInAggrDiffModel(sgql);
				
			}
			else if (player.agentName().equals("dummy")){ 
				//casts the player and tests its attributes 

				QLearning ql = (QLearning) sgql.getSingleAgentLearner();
				//tests whether we have a dummy policy
				Field policyField = revealField(ql, "learningPolicy");
				Policy thePolicy = (Policy) policyField.get(ql);
				assertTrue(thePolicy instanceof DummyPolicy);
				
			}
			else {
				fail(
					"Player name is neither learner or dummy. It is: " + 
					player.agentName()
				);
			}
		}
	}
	
	@Test
	public void testEpsilonInMultiAgentQLearning() throws SAXException, IOException, ParserConfigurationException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException{
		RLParameters rlParams = RLParameters.getInstance();
		
		//this file also has a different epsilon for the 2nd player
		rlParams.loadFromFile("src/tests/rl/example_learning-rates.xml");
		
		@SuppressWarnings("unchecked")
		List<SGAgent> players = (List<SGAgent>) rlParams.getParameter(RLParamNames.PLAYERS);
		
		for(SGAgent player : players){
			
			if (player.agentName().equals("normal-LR")){ 
				// do nothing
			}
			
			// tests 'decay-RL' parameters
			else if (player.agentName().equals("decay-LR")){ 
				PersistentMultiAgentQLearning mmq = (PersistentMultiAgentQLearning) player;
				
				// retrieves learningPolicy from PersistentMultiAgentQLearning
				Field policyField = revealField(mmq, "learningPolicy");
				PolicyFromJointPolicy pi = (PolicyFromJointPolicy) policyField.get(mmq);
				
				// retrieves epsilon from the JointPolicy
				assertTrue(pi.getJointPolicy() instanceof EMinMaxPolicy);
				EMinMaxPolicy mmpi = (EMinMaxPolicy) pi.getJointPolicy();
				Field epsilonField = revealField(mmpi, "epsilon");
				double epsilon = (double) epsilonField.get(mmpi);
				assertEquals(0.15, epsilon, 0.0000001);
			}
			else {
				fail("Unknown player name: " + player.agentName());
			}
		}
	}
	
	@Test
	/**
	 * Tests loading example_portfolioAI.xml
	 */
	public void testExamplePortfolioAI() throws Exception {
		RLParameters rlParams = RLParameters.getInstance();
		
		Map<String, Object> parameters = rlParams.loadFromFile("src/tests/rl/example_portfolioAI.xml"); //may throw exceptions
		
		//tests parameter values
		assertEquals(200, (int) rlParams.getParameter(RLParamNames.EPISODES));
		assertEquals(2, (int) rlParams.getParameter(RLParamNames.DEBUG_LEVEL));
		
		World w = (World) rlParams.getParameter(RLParamNames.ABSTRACTION_MODEL);
		assertTrue(w.getDomain() instanceof AggregateDifferencesDomain);
		
		assertEquals(MicroRTSRewardFactory.SIMPLE_WEIGHTED, parameters.get(RLParamNames.REWARD_FUNCTION));
		
		@SuppressWarnings("unchecked")
		List<SGAgent> players = (List<SGAgent>) rlParams.getParameter(RLParamNames.PLAYERS);
		assertEquals(2, players.size());
		
		for(SGAgent player : players){
			
			// tests 'learner' parameters
			if (player.agentName().equals("learner")){ 
				SGQLearningAdapter sgql = (SGQLearningAdapter) player;
				
				QLearning ql = (QLearning) sgql.getSingleAgentLearner();
				//tests whether attributes were correctly loaded
				//code to test learning rate:
				Field lrField = revealField(ql, "learningRate");
				LearningRate lr = (LearningRate) lrField.get(ql);
				assertEquals(0.1, lr.peekAtLearningRate(null, null), 0.0000001);
				
				//code to test: initialQ
				Field initialQField = revealField(ql, "qInitFunction");
				QFunction initialQ = (QFunction) initialQField.get(ql);
				assertEquals(1, initialQ.value(null), 0.0000001);
				
				//code to test discount 
				Field discountField = revealField(MDPSolver.class, "gamma");
				double discount = (double) discountField.get(ql);
				assertEquals(0.9, discount, 0.0000001);
				
			}
			
			// tests searcher parameters
			else if (player.agentName().equals("searcher")){ 
				PortfolioAIAdapter portfAI = (PortfolioAIAdapter) player;
				
				//tests the value of parameters
				assertEquals(50, portfAI.getTimeout());
				assertEquals(150, portfAI.getPlayouts());
				assertEquals(1000, portfAI.getLookahead());
				assertEquals(
					SimpleSqrtEvaluationFunction3.class.getSimpleName(), 
					portfAI.getEvaluationFunctionName()
				);
				
			}
			else {
				fail(
					"Player name is neither learner or dummy. It is: " + 
					player.agentName()
				);
			}
		}
	}
	
	@Test
	/**
	 * Tests whether configurations in example_learningRates.xml are properly loaded
	 */
	public void testSpecialLearningRates() throws Exception {
		RLParameters rlParams = RLParameters.getInstance();
		
		rlParams.loadFromFile("src/tests/rl/example_learning-rates.xml");
		
		@SuppressWarnings("unchecked")
		List<SGAgent> players = (List<SGAgent>) rlParams.getParameter(RLParamNames.PLAYERS);
		
		for(SGAgent player : players){
			
			// tests 'normal-LR' parameters
			if (player.agentName().equals("normal-LR")){ 
				SGQLearningAdapter sgql = (SGQLearningAdapter) player;
				
				QLearning ql = (QLearning) sgql.getSingleAgentLearner();
				//tests whether attributes were correctly loaded
				//code to test learning rate:
				Field lrField = revealField(ql, "learningRate");
				LearningRate lr = (LearningRate) lrField.get(ql);
				assertEquals(0.1, lr.peekAtLearningRate(null, null), 0.0000001);
			}
			
			// tests 'decay-RL' parameters
			else if (player.agentName().equals("decay-LR")){ 
				PersistentMultiAgentQLearning mmq = (PersistentMultiAgentQLearning) player;
				
				// retrieves learningRate from PersistentMultiAgentQLearning
				Field lrField = revealField(mmq, "learningRate");
				LearningRate lr = (LearningRate) lrField.get(mmq);
				assertTrue(lr instanceof ExponentialDecayLR);
				assertEquals(1., lr.peekAtLearningRate(null, null), 0.0000001);
				//decreases the learning rate
				lr.pollLearningRate(1, null, null);
				assertTrue(lr.peekAtLearningRate(null, null) < 1);
				
				// retrieves decayRate from ExponentialDecayLR
				Field decayField = revealField(lr, "decayRate");
				assertEquals(0.999, (double) decayField.get(lr), 0.000001);
				
				// retrieves minimumLR from ExponentialDecayLR
				Field minField = revealField(lr, "minimumLR");
				assertEquals(0.1, (double) minField.get(lr), 0.000001);
				
				
			}
			else {
				fail("Unknown player name: " + player.agentName());
			}
		}
	}
	
	@Test
	/**
	 * Tests loading of example_microrts-opponent.xml
	 */
	public void testMicroRTSOpponent() throws SAXException, IOException, ParserConfigurationException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		RLParameters rlParams = RLParameters.getInstance();
		
		rlParams.loadFromFile("src/tests/rl/example_microrts-opponent.xml"); //may throw exceptions
		
		assertEquals("PGSAI", rlParams.getOpponentName());
		assertTrue(rlParams.getWorld().getDomain() instanceof SingleAgentDomain);
	}
	
	/**
	 * Changes visibility (private -> public) of a specified object field
	 * and returns it  
	 * @param obj the object whose field will be revealed
	 * @param fieldName
	 * @return
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	private Field revealField(Object obj, String fieldName) throws NoSuchFieldException, SecurityException{
		
		/**
		 * Goes through class and superclasses looking for the field
		 * Code adapted from http://stackoverflow.com/a/16296241
		 */
		Class<?> current = obj.getClass();
		while(current.getSuperclass() != null){ // we don't want to process Object.class
			
			try{
				Field theField = current.getDeclaredField(fieldName);
				theField.setAccessible(true);
				return theField;
				
			} catch (NoSuchFieldException e) {
				// it's ok bro
			}
			current = current.getSuperclass(); // recurse
		}
		
		throw new NoSuchFieldException("Field not found in class and superclasses: " + fieldName);
	}
	
	/**
	 * Changes visibility (private -> public) of a specified class field
	 * and returns it  
	 * @param cls
	 * @param fieldName
	 * @return
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	private Field revealField(Class<?> cls, String fieldName) throws NoSuchFieldException, SecurityException{
		Field theField = cls.getDeclaredField(fieldName);
		theField.setAccessible(true);
		return theField;
	}

}
