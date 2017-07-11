package rl.models.aggregatediff;

import java.util.List;

import burlap.mdp.core.state.State;
import rl.models.aggregate.AggregateState;
import rl.models.stages.GameStage;
import rl.models.stages.GameStages;
import rts.GameState;

/**
 * Measures the difference in quantities owned by player and opponent
 * and classifies it into 'BEHIND', 'EVEN' and 'AHEAD' depending
 * on the difference.
 * 
 * Properties are public for serialization, be careful!
 * @author anderson
 *
 */
public class AggregateDiffState extends GameStage {
	
	private static List<Object> keys;
	
	/**
	 * Properties are public for serialization, be careful!
	 */
	public AggregateDiff workerDiff, lightDiff, rangedDiff, heavyDiff, basesDiff, barracksDiff, resourcesDiff;

	/**
	 * Sets all properties based on the values found in the game state.
	 * Assumes that 'player' has index 0 and 'opponent' has index 1
	 * TODO: check if gs is 'gameOver' and set stage to FINISHED
	 * @param gs
	 */
	public AggregateDiffState(GameState gs) {
		super(gs);
		
		DiffAggregator diffAggr = new DiffAggregator(gs.getPlayer(0), gs.getPlayer(1), gs);
		
		workerDiff = diffAggr.aggregateUnitDiff(gs.getUnitTypeTable().getUnitType("Worker"));
		lightDiff = diffAggr.aggregateUnitDiff(gs.getUnitTypeTable().getUnitType("Light"));
		rangedDiff = diffAggr.aggregateUnitDiff(gs.getUnitTypeTable().getUnitType("Ranged"));
		heavyDiff = diffAggr.aggregateUnitDiff(gs.getUnitTypeTable().getUnitType("Heavy"));
		
		basesDiff = diffAggr.aggregateBuildingDiff(gs.getUnitTypeTable().getUnitType("Base"));
		barracksDiff = diffAggr.aggregateBuildingDiff(gs.getUnitTypeTable().getUnitType("Barracks"));
		
		resourcesDiff = diffAggr.aggregateResourceDiff();
		
	}

	public AggregateDiffState() {
		super();
	}

	@Override
	public List<Object> variableKeys() {
		// initializes keys if needed
		if(keys == null){
			// gets keys from GameStage - TODO: maybe we don't need this, just add 'STAGE' as key...
			keys = super.variableKeys();
			
			// adds a key for each feature we measure 
			// we use the same keys in AggregateState, but we measure differences instead of 'absolute' values
			keys.add(String.format("%s", AggregateState.KEY_WORKERS));
			keys.add(String.format("%s", AggregateState.KEY_LIGHT));
			keys.add(String.format("%s", AggregateState.KEY_RANGED));
			keys.add(String.format("%s", AggregateState.KEY_HEAVY));
			keys.add(String.format("%s", AggregateState.KEY_BASES));
			keys.add(String.format("%s", AggregateState.KEY_BARRACKS));
			keys.add(String.format("%s", AggregateState.KEY_RESOURCES));
		}
		
		return keys;
	}

	@Override
	public Object get(Object variableKey) {
		if(variableKey.equals(GameStage.KEY_STAGE)){
			return getStage();
		}
		if(variableKey.equals(AggregateState.KEY_WORKERS)){
			return workerDiff;
		}
		else if(variableKey.equals(AggregateState.KEY_LIGHT)){
			return lightDiff;
		}
		else if(variableKey.equals(AggregateState.KEY_RANGED)){
			return rangedDiff;
		}
		else if(variableKey.equals(AggregateState.KEY_HEAVY)){
			return heavyDiff;
		}
		else if(variableKey.equals(AggregateState.KEY_BASES)){
			return basesDiff;
		}
		else if(variableKey.equals(AggregateState.KEY_BARRACKS)){
			return barracksDiff;
		}
		else if(variableKey.equals(AggregateState.KEY_RESOURCES)){
			return resourcesDiff;
		}
		throw new IllegalArgumentException("Unrecognized variable key: " + variableKey);
	}
	
	/**
	 * The counterpart of {@link #get(Object)}
	 * @param variableKey
	 * @param value
	 */
	public void set(Object variableKey, Object value){
		
		String strKey = (String) variableKey;
		String strValue = (String) value;
		
		if(strKey.equals(GameStage.KEY_STAGE)){
			setStage(GameStages.valueOf(strValue));
		}
		else if(strKey.equals(AggregateState.KEY_WORKERS)){
			workerDiff = AggregateDiff.valueOf(strValue);
		}
		else if(strKey.equals(AggregateState.KEY_LIGHT)){
			lightDiff = AggregateDiff.valueOf(strValue);
		}
		else if(strKey.equals(AggregateState.KEY_RANGED)){
			rangedDiff = AggregateDiff.valueOf(strValue);
		}
		else if(strKey.equals(AggregateState.KEY_HEAVY)){
			heavyDiff = AggregateDiff.valueOf(strValue);
		}
		else if(strKey.equals(AggregateState.KEY_BASES)){
			basesDiff = AggregateDiff.valueOf(strValue);
		}
		else if(strKey.equals(AggregateState.KEY_BARRACKS)){
			barracksDiff = AggregateDiff.valueOf(strValue);
		}
		else if(strKey.equals(AggregateState.KEY_RESOURCES)){
			resourcesDiff = AggregateDiff.valueOf(strValue);
		}
		else {
			throw new IllegalArgumentException("Unrecognized variable key: " + strKey);
		}
	}
	
	/**
	 * Creates a new state from a String representation. 
	 * The string should have values separated by semicolons, 
	 * in the same order that {@link #variableKeys()} list is 
	 * constructed
	 * 
	 * @param repr
	 * @return
	 */
	public static AggregateDiffState fromString(String repr){
		AggregateDiffState newState = new AggregateDiffState();
		
		String[] parts = repr.split(";");
		List<Object> variableKeys = newState.variableKeys();
		
		for(int index = 0; index < variableKeys.size(); index++){
			newState.set(variableKeys.get(index), parts[index]);
		}
		
		return newState;
	}

	@Override
	public State copy() {
		// if underlying state has not changed, copy will return a equivalent object
		AggregateDiffState theCopy = new AggregateDiffState(underlyingState.clone());
		
		// stage is set externally, we must ensure that it is properly propagated:
		if(this.stage == GameStages.FINISHED){
			theCopy.setStage(GameStages.FINISHED);
		}
		return theCopy;
	}
	
	@Override
	public String toString(){
		String repr = "";
		
		for(Object key : variableKeys()){
			repr += get(key) + ";";
		}
		
		return repr.replaceAll(";+$", "");	// trims trailing semicolon
	}
	
	@Override
	public boolean equals(Object other){
		if(! (other instanceof AggregateDiffState)){
			return false;
		}
		AggregateDiffState otherState = (AggregateDiffState)other; 
		
		for(Object key : variableKeys()){
			if(get(key) != otherState.get(key)){
				return false;
			}
		}
		return true;
	}
	
	@Override
	/**
	 * Returns a hash code according to the string representation
	 */
	public int hashCode(){
		return toString().hashCode();
	}

}
