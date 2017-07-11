package rl.models.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.abstraction.BuildBarracks;
import ai.abstraction.Expand;
import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.core.AI;
import ai.portfolio.NashPortfolioAI;
import ai.portfolio.PortfolioAI;
import ai.portfolio.portfoliogreedysearch.PGSAI;
import ai.puppet.PuppetSearchMCTS;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.UniversalActionType;
import rts.units.UnitTypeTable;

public class ScriptActionTypes {
	
	//action names for learning algorithms
	public static final String WORKER_RUSH = WorkerRush.class.getSimpleName();
	public static final String LIGHT_RUSH = LightRush.class.getSimpleName();
	public static final String RANGED_RUSH = RangedRush.class.getSimpleName();
	public static final String HEAVY_RUSH = HeavyRush.class.getSimpleName();
	public static final String EXPAND = Expand.class.getSimpleName();
	public static final String BUILD_BARRACKS = BuildBarracks.class.getSimpleName();
	
	// other action names
	public static final String PORTFOLIO_AI = PortfolioAI.class.getSimpleName();
	public static final String NASH_PORTFOLIO_AI = NashPortfolioAI.class.getSimpleName();
	public static final String PORTFOLIO_GREEDY_SEARCH = PGSAI.class.getSimpleName();
	public static final String PUPPET_SEARCH = PuppetSearchMCTS.class.getSimpleName();

	/**
	 * Returns the list of action types that learner algorithms can use
	 * @return
	 */
	public static List<UniversalActionType> getLearnerActionTypes() {
		List<UniversalActionType> actionTypes = new ArrayList<>();
		 
		actionTypes.add(new UniversalActionType(WORKER_RUSH));
		actionTypes.add(new UniversalActionType(LIGHT_RUSH));
		actionTypes.add(new UniversalActionType(RANGED_RUSH));
		actionTypes.add(new UniversalActionType(HEAVY_RUSH));
		actionTypes.add(new UniversalActionType(EXPAND));
		actionTypes.add(new UniversalActionType(BUILD_BARRACKS));
		
		return actionTypes;
	}
	
	/**
	 * Gets a broader list of action types
	 * @return
	 */
	public static List<UniversalActionType> getAllActionTypes() {
		List<UniversalActionType> actionTypes = getLearnerActionTypes();
		
		// adds the 'special' actions
		actionTypes.add(new UniversalActionType(PORTFOLIO_AI));
		actionTypes.add(new UniversalActionType(NASH_PORTFOLIO_AI));
		actionTypes.add(new UniversalActionType(PORTFOLIO_GREEDY_SEARCH));
		actionTypes.add(new UniversalActionType(PUPPET_SEARCH));
		
		
		return actionTypes;
	}
	
	/**
	 * Returns a map from action names to {@link UniversalActionType} that
	 * learning algorithms can use
	 * @return
	 */
	public static Map<String, UniversalActionType> getMapToLearnerActionTypes() {
		Map<String, UniversalActionType> actionTypeMap = new HashMap<>();
		 
		actionTypeMap.put(WORKER_RUSH, new UniversalActionType(WORKER_RUSH));
		actionTypeMap.put(LIGHT_RUSH, new UniversalActionType(LIGHT_RUSH));
		actionTypeMap.put(RANGED_RUSH, new UniversalActionType(RANGED_RUSH));
		actionTypeMap.put(HEAVY_RUSH, new UniversalActionType(HEAVY_RUSH));
		actionTypeMap.put(EXPAND, new UniversalActionType(EXPAND));
		actionTypeMap.put(BUILD_BARRACKS, new UniversalActionType(BUILD_BARRACKS));
		
		return actionTypeMap;
	}
	
	public static Map<String, UniversalActionType> getMapToAllActionTypes() {
		Map<String, UniversalActionType> actionTypeMap = getMapToLearnerActionTypes();
		
		//adds the special action types
		actionTypeMap.put(PORTFOLIO_AI, new UniversalActionType(PORTFOLIO_AI));
		actionTypeMap.put(NASH_PORTFOLIO_AI, new UniversalActionType(NASH_PORTFOLIO_AI));
		actionTypeMap.put(PORTFOLIO_GREEDY_SEARCH, new UniversalActionType(PORTFOLIO_GREEDY_SEARCH));
		actionTypeMap.put(PUPPET_SEARCH, new UniversalActionType(PUPPET_SEARCH));
		
		return actionTypeMap;
	}
	
	/**
	 * Returns a map from action names to {@link Action}s that learning
	 * algorithms can use
	 * @return
	 */
	public static Map<String, Action> getMapToLearnerActions() {
		Map<String, Action> actionMap = new HashMap<>();
		 
		actionMap.put(WORKER_RUSH, new UniversalActionType(WORKER_RUSH).associatedAction(null));
		actionMap.put(LIGHT_RUSH, new UniversalActionType(LIGHT_RUSH).associatedAction(null));
		actionMap.put(RANGED_RUSH, new UniversalActionType(RANGED_RUSH).associatedAction(null));
		actionMap.put(HEAVY_RUSH, new UniversalActionType(HEAVY_RUSH).associatedAction(null));
		actionMap.put(EXPAND, new UniversalActionType(EXPAND).associatedAction(null));
		actionMap.put(BUILD_BARRACKS, new UniversalActionType(BUILD_BARRACKS).associatedAction(null));
		
		return actionMap;
	}
	
	/**
	 * Returns a map with extra actions
	 * @return
	 */
	public static Map<String, Action> getMapToAllActions() {
		Map<String, Action> actionMap = getMapToLearnerActions();
		
		actionMap.put(PORTFOLIO_AI, new UniversalActionType(PORTFOLIO_AI).associatedAction(null));
		actionMap.put(NASH_PORTFOLIO_AI, new UniversalActionType(NASH_PORTFOLIO_AI).associatedAction(null));
		actionMap.put(PORTFOLIO_GREEDY_SEARCH, new UniversalActionType(PORTFOLIO_GREEDY_SEARCH).associatedAction(null));
		actionMap.put(PUPPET_SEARCH, new UniversalActionType(PUPPET_SEARCH).associatedAction(null));
		 
		return actionMap;
	}
	
	/**
	 * Returns a map from action names to actual microRTS scripts, i.e., {@link AI}
	 * @param unitTypeTable
	 * @return
	 */
	public static Map<String, AI> getLearnerActionMapping(UnitTypeTable unitTypeTable){
		Map<String, AI> actions = new HashMap<>();
		
		actions.put(WORKER_RUSH, new WorkerRush(unitTypeTable));
		actions.put(LIGHT_RUSH, new LightRush(unitTypeTable));
		actions.put(RANGED_RUSH, new RangedRush(unitTypeTable));
		actions.put(HEAVY_RUSH, new HeavyRush(unitTypeTable));
		actions.put(EXPAND, new Expand(unitTypeTable));
		actions.put(BUILD_BARRACKS, new BuildBarracks(unitTypeTable));
		
		return actions;
	}
	
	/**
	 * Adding a map with extra AIs
	 * @param unitTypeTable
	 * @return
	 */
	public static Map<String, AI> getAllActionMapping(UnitTypeTable unitTypeTable){
		Map<String, AI> actions = getLearnerActionMapping(unitTypeTable);
		
		actions.put(PORTFOLIO_AI, new PortfolioAI(unitTypeTable));
		actions.put(NASH_PORTFOLIO_AI, new NashPortfolioAI(unitTypeTable));
		actions.put(PORTFOLIO_GREEDY_SEARCH, new PGSAI(unitTypeTable));
		actions.put(PUPPET_SEARCH, new HeavyRush(unitTypeTable));
		
		return actions;
	}

}
