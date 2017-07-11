package tests.rl.adapters.learners;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;

import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.valuefunction.QValue;
import burlap.mdp.core.action.Action;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.world.World;
import burlap.statehashing.simple.SimpleHashableStateFactory;
import rl.WorldFactory;
import rl.adapters.learners.SGQLearningAdapter;
import rl.models.aggregate.Aggregate;
import rl.models.aggregate.AggregateState;
import rl.models.aggregatediff.AggregateDiffState;
import rl.models.common.ScriptActionTypes;
import rl.models.stages.GameStage;
import rl.models.stages.GameStages;

public class SGQLearningAdapterTest {
	
	final static String QTABLE_STAGES = "src/tests/rl/adapters/learners/sgql_stages.xml";
	final static String QTABLE_AGGREGATE = "src/tests/rl/adapters/learners/sgql_aggregate.yaml";
	final static String QTABLE_AGGREGATEDIFF = "src/tests/rl/adapters/learners/sgql_aggrdiff.xml";
	
	@Before
	public void setUp(){
		
	}
	
	public SGQLearningAdapter prepareLearner(World w){
		QLearning ql = new QLearning(
			null, 0.9, new SimpleHashableStateFactory(false), 
			1.0, 0.1
		);
		
		SGQLearningAdapter agent = new SGQLearningAdapter(
			w.getDomain(), ql, "Learner", 
			new SGAgentType("QLearning", w.getDomain().getActionTypes())
		);
		
		return agent;
	}

	@Test
	public void testLoadKnowledgeWithStagesModel(){
		World world = WorldFactory.fromString(WorldFactory.STAGES);
		SGQLearningAdapter sgql = prepareLearner(world);
		sgql.loadKnowledge(QTABLE_STAGES);
		
		assertLoadedKnowledgeInStagesWorld(sgql);
		
	}
	
	@Test
	/**
	 * This test assumes {@link testLoadKnowledge} is working properly
	 * @throws FileNotFoundException
	 */
	public void testSaveKnowledgeWithStagesModel() throws FileNotFoundException {
		// make sure loadKnowledge is working:
		testLoadKnowledgeWithStagesModel();
		
		// an agent loads knowledge and saves it in another file ...
		SGQLearningAdapter sgql = prepareLearner(WorldFactory.fromString(WorldFactory.STAGES));
		String tmpQTableFile = "/tmp/test-save-sgql_stages.xml";
		sgql.loadKnowledge(QTABLE_STAGES);
		sgql.saveKnowledge(tmpQTableFile);
		
		// ...then another agent loads it and its knowledge must be the same ;)
		SGQLearningAdapter newAgent = prepareLearner(WorldFactory.fromString(WorldFactory.STAGES));
		newAgent.loadKnowledge(tmpQTableFile);
		assertLoadedKnowledgeInStagesWorld(newAgent);
	}
	
	/**
	 * Verifies whether knowledge in agent corresponds to that specified 
	 * in {@link #QTABLE_STAGES} file
	 * @param agent
	 */
	private void assertLoadedKnowledgeInStagesWorld(SGQLearningAdapter agent){
		GameStage state = new GameStage();
		QLearning qLearner = (QLearning) agent.getSingleAgentLearner();
		
		//retrieves objects regarding game actions
		Map<String, Action> theActions = ScriptActionTypes.getMapToLearnerActions();
		Action lightRush = theActions.get(ScriptActionTypes.LIGHT_RUSH);
		Action buildBarracks = theActions.get(ScriptActionTypes.BUILD_BARRACKS);
		Action rangedRush = theActions.get(ScriptActionTypes.RANGED_RUSH);
		Action expand = theActions.get(ScriptActionTypes.EXPAND);
		Action workerRush = theActions.get(ScriptActionTypes.WORKER_RUSH);
		
		/*
		 * Expected values of actions for stage 'OPENING' are:
		 * LightRush: 0.0
		 * BuildBarracks: 0.5
		 * RangedRush: 0.0
		 * Expand: -0.5
		 * WorkerRush: 0.75
		 */
		state.setStage(GameStages.OPENING);
		assertEquals(0.0, qLearner.qValue(state, lightRush), 0.00001);
		assertEquals(0.5, qLearner.qValue(state, buildBarracks), 0.00001);
		assertEquals(0.0, qLearner.qValue(state, rangedRush), 0.00001);
		assertEquals(-0.5, qLearner.qValue(state, expand), 0.00001);
		assertEquals(0.75, qLearner.qValue(state, workerRush), 0.00001);
		
		
		/*
		 * Expected values of actions for stage 'EARLY' are:
		 * LightRush: 0.5
		 * BuildBarracks: 0.0
		 * RangedRush: 0.6
		 * Expand: 0.3
		 * WorkerRush: -0.1
		 */
		state.setStage(GameStages.EARLY);
		assertEquals(0.5, qLearner.qValue(state, lightRush), 0.00001);
		assertEquals(0.0, qLearner.qValue(state, buildBarracks), 0.00001);
		assertEquals(0.6, qLearner.qValue(state, rangedRush), 0.00001);
		assertEquals(0.3, qLearner.qValue(state, expand), 0.00001);
		assertEquals(-0.1, qLearner.qValue(state, workerRush), 0.00001);
		

		/*
		 * Expected values for other states are not specified in file,
		 * thus they should keep the default (1.0) 
		 */
		
		for(GameStage s : GameStage.allStates()){
			//skips OPENING and EARLY (previously tested)
			if (s.getStage().equals(GameStages.OPENING) || s.getStage().equals(GameStages.EARLY)){
				continue;
			}
			
			for (QValue q : qLearner.qValues(s)) {
				assertEquals(
					String.format("state %s / action %s", s, q.a), 
					1.0, q.q, 0.00001
				);
			}
		}
	}
	
	@Test
	public void testLoadKnowledgeWithAggregateDiffModel(){
		SGQLearningAdapter learner = prepareLearner(WorldFactory.fromString(WorldFactory.AGGREGATE_DIFF));
		learner.loadKnowledge(QTABLE_AGGREGATEDIFF);
		assertLoadedKnowledgeInAggrDiffModel(learner);
	}

	@Test 
	public void testSaveKnowledgeWithAggregateDiffModel() throws FileNotFoundException{
		// makes sure that loadKnowledge is working
		testLoadKnowledgeWithAggregateDiffModel();
		
		// an agent loads expected knowledge and SAVES it to a different file ...
		SGQLearningAdapter sgql = prepareLearner(WorldFactory.fromString(WorldFactory.AGGREGATE_DIFF));
		String tmpQTableFile = "/tmp/test-save-sgql-aggrdiff.xml";
		sgql.loadKnowledge(QTABLE_AGGREGATEDIFF);
		sgql.saveKnowledge(tmpQTableFile);

		// ... then another agent loads this knowledge and it must match the expected knowledge ;)
		SGQLearningAdapter newAgent =  prepareLearner(WorldFactory.fromString(WorldFactory.AGGREGATE_DIFF));
		newAgent.loadKnowledge(tmpQTableFile);
		
		assertLoadedKnowledgeInAggrDiffModel(newAgent);
	}
	
	/**
	 * Verifies whether knowledge present in agent matches the one specified
	 * in {@link #QTABLE_AGGREGATEDIFF} file
	 * @param agent
	 */
	public void assertLoadedKnowledgeInAggrDiffModel(SGQLearningAdapter agent){
		QLearning qLearner = (QLearning) agent.getSingleAgentLearner();
		// retrieves the actions for querying later
		Map<String, Action> theActions = ScriptActionTypes.getMapToLearnerActions();

		// string representation of first state on file
		String repr = "OPENING;BEHIND;BEHIND;BEHIND;BEHIND;BEHIND;BEHIND;BEHIND";
		
		/**
		 * For the state given in repr, expected values are:
		 * LightRush: 0.7
		 * BuildBarracks: 0.6
		 * RangedRush: -0.9
		 * other actions: 1
		 */
		AggregateDiffState state = AggregateDiffState.fromString(repr);
		assertEquals(0.7, qLearner.qValue(state, theActions.get(ScriptActionTypes.LIGHT_RUSH)), 0.00001);
		assertEquals(0.6, qLearner.qValue(state, theActions.get(ScriptActionTypes.BUILD_BARRACKS)), 0.00001);
		assertEquals(-0.9, qLearner.qValue(state, theActions.get(ScriptActionTypes.RANGED_RUSH)), 0.00001);
		assertEquals(1.0, qLearner.qValue(state, theActions.get(ScriptActionTypes.EXPAND)), 0.00001);
		assertEquals(1.0, qLearner.qValue(state, theActions.get(ScriptActionTypes.WORKER_RUSH)), 0.00001);
		
		// string representation of second state on file
		repr = "LATE;EVEN;AHEAD;AHEAD;AHEAD;EVEN;BEHIND;AHEAD";
		
		/**
		 * For the state given in repr, expected values are:
		 * Expand: -0.5
		 * WorkerRush: 0.33
		 * other actions: 1
		 */
		state = AggregateDiffState.fromString(repr);
		assertEquals(1.0, qLearner.qValue(state, theActions.get(ScriptActionTypes.LIGHT_RUSH)), 0.00001);
		assertEquals(1.0, qLearner.qValue(state, theActions.get(ScriptActionTypes.BUILD_BARRACKS)), 0.00001);
		assertEquals(1.0, qLearner.qValue(state, theActions.get(ScriptActionTypes.RANGED_RUSH)), 0.00001);
		assertEquals(-0.5, qLearner.qValue(state, theActions.get(ScriptActionTypes.EXPAND)), 0.00001);
		assertEquals(0.33, qLearner.qValue(state, theActions.get(ScriptActionTypes.WORKER_RUSH)), 0.00001);
		
		// for all other states, all actions have value = 1
		// TODO enumerate other states and test their values
	}
	
	//@Test -- suspended until aggregate model becomes serializable or save/load knowledge stops depending on that
	public void testLoadKnowledgeWithAggregateModel() {
		SGQLearningAdapter sgql = prepareLearner(WorldFactory.fromString(WorldFactory.AGGREGATE));
		sgql.loadKnowledge(QTABLE_AGGREGATE);
		
		// stores the available actions
		Map<String, Action> theActions = ScriptActionTypes.getMapToLearnerActions();
		
		// creates a map with state/player features to manipulate and retrieve specific info
		Map<Integer, Map<String, Object>> playerFeatures = constructPlayerFeatures();

		AggregateDiffState state = new AggregateDiffState();
		QLearning qLearner = (QLearning) sgql.getSingleAgentLearner();
		
		/**
		 * Stage of first test has the following properties:
		 * Stage: OPENING
		 * 
		 * player0 features:
		 * FAIR;FEW;FEW;FEW;FAIR;FEW;MANY
		 * 
		 * player1 features:
		 * FAIR;FEW;FEW;FEW;FAIR;FEW;MANY
		 * 
		 * Player features are:
		 * WORKERS;LIGHT;RANGED;HEAVY;BASES;BARRACKS;RESOURCES
		 */
		state.setStage(GameStages.OPENING);
		//assertEquals(0.0, qLearner.qValue(state, theActions.get(ScriptActionTypes.LIGHT_RUSH)), 0.00001);
		//assertEquals(0.5, qLearner.qValue(state, theActions.get(ScriptActionTypes.BUILD_BARRACKS)), 0.00001);
		
	}

	private Map<Integer, Map<String, Object>> constructPlayerFeatures() {
		Map<Integer, Map<String, Object>> playerFeatures = new HashMap<>();
		
		// creates a map of features for each player
		for(int player = 0; player <= 1; player ++){
			
			// crates a map for current player with 'FEW' in all quantities
			Map<String, Object> currentPlayerFeatures = new HashMap<>();
			
			currentPlayerFeatures.put(
				AggregateState.KEY_WORKERS, Aggregate.FEW
			);
			currentPlayerFeatures.put(
				AggregateState.KEY_LIGHT, Aggregate.FEW
			);
			currentPlayerFeatures.put(
				AggregateState.KEY_RANGED, Aggregate.FEW
			);
			currentPlayerFeatures.put(
				AggregateState.KEY_HEAVY, Aggregate.FEW
			);
			currentPlayerFeatures.put(
				AggregateState.KEY_BASES, Aggregate.FEW
			);
			currentPlayerFeatures.put(
				AggregateState.KEY_BARRACKS, Aggregate.FEW
			);
			currentPlayerFeatures.put(
				AggregateState.KEY_RESOURCES, Aggregate.FEW
			);
			
			playerFeatures.put(player, currentPlayerFeatures);
		}
		return playerFeatures;
	}
}
