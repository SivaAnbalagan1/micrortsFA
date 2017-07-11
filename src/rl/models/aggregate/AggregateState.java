package rl.models.aggregate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.mdp.core.state.State;
import rl.models.stages.GameStage;
import rl.models.stages.GameStages;
import rts.GameState;
import rts.Player;
import rts.units.Unit;

/**
 * This class groups quantities of state features as 'aggregates'
 * i.e. few, fair and many. 
 * How many does constitute each aggregate is empirically defined
 * 
 * @author anderson
 *
 */
public class AggregateState extends GameStage{
	
	public static final String KEY_WORKERS = "WORKERS";
	public static final String KEY_LIGHT = "LIGHT";
	public static final String KEY_RANGED = "RANGED";
	public static final String KEY_HEAVY = "HEAVY";
	public static final String KEY_BASES = "BASES";
	public static final String KEY_BARRACKS = "BARRACKS";
	public static final String KEY_RESOURCES = "RESOURCES";
	
	// special 'key' used internally
	public static final String PLAYER_FEATURES = "PLAYERS";
	
	/**
	 * One map of variable-key to value per player 
	 */
	private Map<Integer, Map<String, Object>> playerFeatures;
	
	/**
	 * One map of string->object per common state features
	 */
	private Map<String, Object> stateVariables;
	
	
	/**
	 * List of state variable keys
	 */
	private static List<Object> keys;
	

	public AggregateState(GameState gameState) {
		super(gameState);
		
		//fills the state variables into a map
		stateVariables = new HashMap<>();
		stateVariables.put(KEY_STAGE, frameToStage(gameState.getTime()));
		
		// creates player features and fills it for each player
		playerFeatures = new HashMap<>();
		
		for(Player player : gameState.getPhysicalGameState().getPlayers()){
			// fills state features of current player
			Map<String, Object> currentPlayerFeatures = new HashMap<>();
			
			Aggregator aggr = new Aggregator();
			
			// put aggregates of units
			currentPlayerFeatures.put(
				KEY_WORKERS, aggr.aggregateUnits(countUnits("Worker", player.getID()))
			);
			currentPlayerFeatures.put(
				KEY_LIGHT, aggr.aggregateUnits(countUnits("Light", player.getID()))
			);
			currentPlayerFeatures.put(
				KEY_RANGED, aggr.aggregateUnits(countUnits("Ranged", player.getID()))
			);
			currentPlayerFeatures.put(
				KEY_HEAVY, aggr.aggregateUnits(countUnits("Heavy", player.getID()))
			);
			currentPlayerFeatures.put(
				KEY_BASES, aggr.aggregateBuildings(countUnits("Base", player.getID()))
			);
			currentPlayerFeatures.put(
				KEY_BARRACKS, aggr.aggregateBuildings(countUnits("Barracks", player.getID()))
			);
			currentPlayerFeatures.put(
				KEY_RESOURCES, aggr.aggregateResources(player.getResources())
			);
			
			playerFeatures.put(player.getID(), currentPlayerFeatures);
		}
		
		stateVariables.put(PLAYER_FEATURES, playerFeatures);
		
	}
	
	/**
	 * Empty constructor + getters/setters make object serializable, right?
	 */
	public AggregateState() {
		super();
		
		// initializes default structures to prevent NullPointerException
		stateVariables = new HashMap<>();
		stateVariables.put(KEY_STAGE, GameStages.OPENING);
		
		playerFeatures = new HashMap<>();
		playerFeatures.put(0, new HashMap<>()); // <- need to init all properties?
		playerFeatures.put(1, new HashMap<>());
		
		stateVariables.put(PLAYER_FEATURES, playerFeatures);
	}

	public Object getPlayerFeature(int playerID, String featureName){
		return playerFeatures.get(playerID).get(featureName);
	}
	
	/**
	 * Counts the units of a type that a player has
	 * @param unitTypeName
	 * @param playerID
	 * @return
	 */
	public int countUnits(String unitTypeName, int playerID){
		int count = 0;
		for(Unit u : underlyingState.getUnits()){
			if(u.getType().name.equals(unitTypeName)) {
				count++;
			}
		}
		
		return count;
	}

	@Override
	public List<Object> variableKeys() {
		if(keys == null) {
			// adds keys for common state features
			keys = super.variableKeys();
			
			// adds keys for each player feature, key is playerID;feature
			List<Integer> playerIDs = new ArrayList<>(2);
			if(underlyingState == null){
				//initializes a default list with two player IDs if I don't have underlying state
				playerIDs.add(0);
				playerIDs.add(1);
			}
			else { //if I have underlying state, get actual player IDs
				for(Player player : underlyingState.getPhysicalGameState().getPlayers()){
					playerIDs.add(player.getID());
				}
			}
			
			//now, traverses playerIDs and sets up the correct keys
			for(int id : playerIDs){
				keys.add(String.format("%d-%s", id, KEY_WORKERS));
				keys.add(String.format("%d-%s", id, KEY_LIGHT));
				keys.add(String.format("%d-%s", id, KEY_RANGED));
				keys.add(String.format("%d-%s", id, KEY_HEAVY));
				keys.add(String.format("%d-%s", id, KEY_BASES));
				keys.add(String.format("%d-%s", id, KEY_BARRACKS));
				keys.add(String.format("%d-%s", id, KEY_RESOURCES));
			}
		}
		return keys;
	}

	@Override
	public Object get(Object variableKey) {
		
		// first tests for common state features
		if(variableKey.equals(KEY_STAGE)) {
			return getStage();
		}
		
		// then breaks player;key to retrieve a player feature
		String key = (String) variableKey;
		String[] parts = key.split("-");
		
		try{
			return getPlayerFeature(Integer.parseInt(parts[0]), parts[1]);
		}
		catch(NullPointerException e){
			System.err.println("ERROR: player feature not found: " + variableKey);
		}
		
		return null;
	}
	
	/**
	 * Sets value to a variable identified by a name (a valid key).
	 * This method is the 'counterpart' of {@link #get}
	 * @param key
	 */
	public void setVariable(String name, Object value){
		// check if variable to be set is 'stage'
		if(name.equals(KEY_STAGE)) {
			setStage((GameStages) value);
		}
		
		else {	// otherwise, it is a player variable
			setPlayerVariable(name, value);
		}
		
	}

	/**
	 * Set a player variable
	 * @param name
	 * @param value
	 */
	private void setPlayerVariable(String name, Object value) {
		// name is playerNumber-keyName
		String[] parts = name.split("-");
		
		int playerNumber = Integer.parseInt(parts[0]);
		
		// safety: check whether features for that player were initialized
		if(playerFeatures.get(playerNumber) == null){
			playerFeatures.put(playerNumber, new HashMap<>());
		}
		
		// now sets the feature there (parts[1] is the key name)
		playerFeatures.get(playerNumber).put(parts[1], value);
		
		
	}

	@Override
	public State copy() {
		// if underlying game state has not changed, new object is identical
		return new AggregateState(underlyingState.clone());
	}
	
	/**
	 * Returns a String representation of state variables
	 * Keys on first line, values on second
	 * To get just the variable names, see {@link #keysToString()}
	 * To get just variable values, see {@link #valuesToString()}
	 */
	@Override
	public String toString(){
		return keysToString() + "\n" + valuesToString();
	}
	
	/**
	 * Returns a String representation of state variable values
	 * It is a semicolon-separated string
	 * @return
	 */
	public String valuesToString(){
		String ret = "";
		for(Object key : variableKeys()){
			ret += get(key) + ";";
		}
		return ret.replaceAll(";+$", "");	//trims trailing semicolon
	}
	
	/**
	 * Returns a String representation of state variable keys
	 * It is a semicolon-separated string
	 * @return
	 */
	public String keysToString(){
		String ret = "";
		for(Object key : variableKeys()){
			ret += key + ";";
		}
		return ret.replaceAll(";+$", "");	//trims trailing semicolon
	}
	
	@Override
	public boolean equals(Object other){
		if(! (other instanceof AggregateState) ){
			return false;
		}
		
		// compares all state features
		AggregateState otherState = (AggregateState) other;
		
		for(Object feature : variableKeys()){
			
			if(! get(feature).equals(otherState.get(feature))){
				return false;
			}
		}
		
		return true;
	}
	
	public static List<AggregateState> enumerate(){
		
		return null;
	}

	/**
	 * Getter to make object serializable
	 * @return
	 */
	public Map<String, Object> getStateVariables() {
		return stateVariables;
	}

	/**
	 * Setter to make object serializable
	 * @param stateVariables
	 */
	public void setStateVariables(Map<String, Object> stateVariables) {
		this.stateVariables = stateVariables;
	}

	/**
	 * Getter for serializability
	 * @return
	 */
	protected Map<Integer, Map<String, Object>> getPlayerFeatures() {
		return playerFeatures;
	}

	/**
	 * Setter for serializability
	 * @param playerFeatures
	 */
	protected void setPlayerFeatures(Map<Integer, Map<String, Object>> playerFeatures) {
		this.playerFeatures = playerFeatures;
	}
}
