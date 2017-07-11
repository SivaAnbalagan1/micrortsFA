package rl.models.aggregatediff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdom.JDOMException;

import burlap.mdp.core.state.State;
import rl.models.aggregate.AggregateState;
import rl.models.common.ScriptActionTypes;
import rl.models.stages.GameStages;
import rl.models.stages.GameStagesDomain;

/**
 * A domain with abstract representation of a microRTS state
 * quantities are 'discretized' according to empirical amounts
 * 
 * TODO record map file and reload initial state from it
 * @author anderson
 *
 */
public class AggregateDifferencesDomain extends GameStagesDomain {

	List<AggregateDiffState> allStates;
	
	/**
	 * Creates a default domain loading map maps/basesWorkers24x24.xml of microRTS
	 * @throws JDOMException
	 * @throws IOException
	 */
	public AggregateDifferencesDomain() throws JDOMException, IOException {
		this("maps/basesWorkers24x24.xml");
		
	}
	
	public AggregateDifferencesDomain(String pathToMap) throws JDOMException, IOException{
		super(pathToMap);
	
		//sets the joint action model containing the valid actions
		setJointActionModel(new AggregateDiffStateJAM(ScriptActionTypes.getLearnerActionMapping(unitTypeTable)));
	}
	
	/**
	 * Returns the initial state for the game
	 * @return
	 */
	public State getInitialState(){
		return new AggregateDiffState(gs);
	}
	
	@Override
	public List<? extends State> enumerate(){
		// initializes list with all states if needed
		if(allStates == null) {
			allStates = new ArrayList<>();
			
			// lists all possible stages and quantities (aggregates) to use in enumeration
			GameStages[] allStages = GameStages.values();
			AggregateDiff[] allAggregateDiffs = AggregateDiff.values();
			
			/*
			 * A list of lists of quantities, for example:
			 * [[OPENING, EARLY, MID, ...], [BEHIND, EVEN, AHEAD], [BEHIND, EVEN, AHEAD], ...]
			 * One list for each key in AggregateStateDiff class
			 */
			List<List<Object>> quantities = new ArrayList<>();
			
			// insert a list with all stages
			quantities.add(new ArrayList<Object>(Arrays.asList(allStages)));
			
			// for each key that is not 'stage', insert all Aggregates in quantities
			// (uses the initial state just to retrieve the keys)
			List<Object> keys = getInitialState().variableKeys();
			for(Object key : keys){
				String strKey = (String) key;
				if(strKey.equals(AggregateState.KEY_STAGE)) {
					continue;
				}
				quantities.add(new ArrayList<Object>(Arrays.asList(allAggregateDiffs)));
			}
			
			// calculates the cartesian product of lists in quantities
			List<List<Object>> cartesianProduct = cartesianProduct(quantities);
			
			// creates an AggregateDiffState for each item in the cartesian product
			for(List<Object> combination : cartesianProduct){
				
				// combination is a list of GameStage and AggregateDiff items, let's transform them in Strings
				List<String> combStrings = new ArrayList<>();
				for(Object item : combination) {
					combStrings.add(item.toString());
				}
				
				// creates a semicolon-separated string representing state quantities
				String representation = String.join(";", combStrings);
				
				// creates a new state and sets its properties according to the representation
				AggregateDiffState newState = AggregateDiffState.fromString(representation);
				
				//System.out.println("New state " + representation);
				
				//finally adds the new state to the list
				allStates.add(newState);
			}
		}
		
		return allStates;
		
	}
	
	/**
	 * Calculates the cartesian product of a list of lists.
	 * For example, if input is like [[A,B], [C,D]] it returns
	 * [[A,C], [A,D], [B,C], [B,D]]
	 * 
	 * Code by Philipp Meister in StackOverflow (http://stackoverflow.com/a/9496234)
	 * @param lists
	 * @return
	 */
	protected <T> List<List<T>> cartesianProduct(List<List<T>> lists) {
	    List<List<T>> resultLists = new ArrayList<List<T>>();
	    if (lists.size() == 0) {
	        resultLists.add(new ArrayList<T>());
	        
	        return resultLists;
	    } else {
	        List<T> firstList = lists.get(0);
	        List<List<T>> remainingLists = cartesianProduct(lists.subList(1, lists.size()));
	        for (T condition : firstList) {
	            for (List<T> remainingList : remainingLists) {
	                ArrayList<T> resultList = new ArrayList<T>();
	                resultList.add(condition);
	                resultList.addAll(remainingList);
	                resultLists.add(resultList);
	            }
	        }
	    }
	    return resultLists;
	}

}
