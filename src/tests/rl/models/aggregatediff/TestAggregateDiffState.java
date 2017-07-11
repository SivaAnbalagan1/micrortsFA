package tests.rl.models.aggregatediff;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rl.models.aggregate.AggregateState;
import rl.models.aggregatediff.AggregateDiff;
import rl.models.aggregatediff.AggregateDiffState;
import rl.models.singlestate.SingleState;
import rl.models.stages.GameStage;
import rl.models.stages.GameStages;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class TestAggregateDiffState {

	GameState underlyingState;
	
	@Before
	public void setUp() throws Exception {
		UnitTypeTable utt = new UnitTypeTable();
		underlyingState = new GameState(
			PhysicalGameState.load("src/tests/rl/models/aggregatediff/basesWorkers24x24.xml", utt), 
			utt
		);
	}

	@Test
	public void testVariableKeys() {
		List<String> expectedKeys = new ArrayList<>();
		expectedKeys.add(GameStage.KEY_STAGE);
		expectedKeys.add(AggregateState.KEY_WORKERS);
		expectedKeys.add(AggregateState.KEY_LIGHT);
		expectedKeys.add(AggregateState.KEY_RANGED);
		expectedKeys.add(AggregateState.KEY_HEAVY);
		expectedKeys.add(AggregateState.KEY_BASES);
		expectedKeys.add(AggregateState.KEY_BARRACKS);
		expectedKeys.add(AggregateState.KEY_RESOURCES);

		@SuppressWarnings("unchecked")
		List<String> actualKeys = (List<String>)(Object)new AggregateDiffState().variableKeys();
		
		assertArrayEquals(expectedKeys.toArray(), actualKeys.toArray());
	}

	@Test
	/**
	 * Tests whether a correct AggregateDiffState can be built from string
	 * This test covers {@link AggregateDiffState#set}
	 */
	public void testFromString() {
		String repr = "MID;AHEAD;BEHIND;EVEN;EVEN;BEHIND;AHEAD;AHEAD";
		
		AggregateDiffState newState = AggregateDiffState.fromString(repr);
		
		assertEquals(GameStages.MID, newState.getStage());
		assertEquals(AggregateDiff.AHEAD, newState.workerDiff);
		assertEquals(AggregateDiff.BEHIND, newState.lightDiff);
		assertEquals(AggregateDiff.EVEN, newState.rangedDiff);
		assertEquals(AggregateDiff.EVEN, newState.heavyDiff);
		assertEquals(AggregateDiff.BEHIND, newState.basesDiff);
		assertEquals(AggregateDiff.AHEAD, newState.barracksDiff);
		assertEquals(AggregateDiff.AHEAD, newState.resourcesDiff);
	}
	
	@Test
	public void testToString(){
		// assumes testFromString passed
		this.testFromString();
		
		String repr = "MID;AHEAD;BEHIND;EVEN;EVEN;BEHIND;AHEAD;AHEAD";
		AggregateDiffState newState = AggregateDiffState.fromString(repr);
		
		// string representation should be equivalent to the one used in creation
		assertEquals(repr, newState.toString());
	}
	
	@Test
	/**
	 * Assumes that testFromString is working
	 */
	public void testGet() {
		this.testFromString();
		
		// starts just like testFromString, but here we query the properties via {@link AggregateDiffState#get}
		String repr = "MID;AHEAD;BEHIND;EVEN;EVEN;BEHIND;AHEAD;AHEAD";
		AggregateDiffState newState = AggregateDiffState.fromString(repr);
		
		assertEquals(GameStages.MID, newState.get(AggregateState.KEY_STAGE));
		assertEquals(AggregateDiff.AHEAD, newState.get(AggregateState.KEY_WORKERS));
		assertEquals(AggregateDiff.BEHIND, newState.get(AggregateState.KEY_LIGHT));
		assertEquals(AggregateDiff.EVEN, newState.get(AggregateState.KEY_RANGED));
		assertEquals(AggregateDiff.EVEN, newState.get(AggregateState.KEY_HEAVY));
		assertEquals(AggregateDiff.BEHIND, newState.get(AggregateState.KEY_BASES));
		assertEquals(AggregateDiff.AHEAD, newState.get(AggregateState.KEY_BARRACKS));
		assertEquals(AggregateDiff.AHEAD, newState.get(AggregateState.KEY_RESOURCES));
	}
	
	@Test
	public void testEquals(){
		// assumes testFromString is working
		this.testFromString();
		
		// two states created from the same string should be equal
		String repr = "MID;AHEAD;BEHIND;EVEN;EVEN;BEHIND;AHEAD;AHEAD";
		AggregateDiffState someState = AggregateDiffState.fromString(repr);
		AggregateDiffState otherState = AggregateDiffState.fromString(repr);
		
		assertTrue(someState.equals(otherState));
		
		// a state with a different attribute should not be equal
		// otherRepr differs from repr in the last part
		String otherRepr = "MID;AHEAD;BEHIND;EVEN;EVEN;BEHIND;AHEAD;BEHIND"; 
		AggregateDiffState different = AggregateDiffState.fromString(otherRepr);
		assertFalse(someState.equals(different));
		assertFalse(otherState.equals(different));
		
		// also, if object is not AggregateDiffState, it ain't equal
		AggregateState duh = new AggregateState();
		assertFalse(someState.equals(duh));
		assertFalse(otherState.equals(duh));
		
	}
	
	@Test
	/**
	 * Tests the constructor that receives a GameState
	 */
	public void testAggregateDiffStateGameState() {
		AggregateDiffState state = new AggregateDiffState(underlyingState);
		
		/*
		 * In the underlying state, player 0 has:
		 * - more resources, workers, and bases
		 * - less heavy, barracks
		 * - even light, ranged
		 * 
		 * Also, stage is OPENING
		 */
		assertEquals(GameStages.OPENING, state.getStage());
		assertEquals(AggregateDiff.AHEAD, state.workerDiff);
		assertEquals(AggregateDiff.AHEAD, state.resourcesDiff);
		assertEquals(AggregateDiff.AHEAD, state.basesDiff);
		
		assertEquals(AggregateDiff.BEHIND, state.heavyDiff);
		assertEquals(AggregateDiff.BEHIND, state.barracksDiff);
		
		assertEquals(AggregateDiff.EVEN, state.lightDiff);
		assertEquals(AggregateDiff.EVEN, state.rangedDiff);
		
	}
	
	@Test
	/**
	 * This test addresses the error that a copy of SingleState instance 
	 * whose stage is FINISHED, has stage OPENING
	 */
	public void testCopy(){
		AggregateDiffState original = new AggregateDiffState(underlyingState);
		original.setStage(GameStages.FINISHED);
		
		AggregateDiffState copy = (AggregateDiffState) original.copy();
		
		// ensures that copy is FINISHED
		assertEquals(GameStages.FINISHED, copy.getStage());
		
		// moreover, ensure that copy equals the original
		assertEquals(original, copy);
	}

}
