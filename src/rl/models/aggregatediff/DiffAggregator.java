package rl.models.aggregatediff;

import rts.GameState;
import rts.Player;
import rts.units.Unit;
import rts.units.UnitType;

/**
 * Defines useful methods to return  {@link AggregateDiff}s that 
 * express differences in quantities owned by players, from the 
 * point of view of a player (e.g. if this 'player' has less units 
 * than the opponent, we return BEHIND)
 * 
 * All levels are defined empirically
 * 
 * @author anderson
 *
 */
public class DiffAggregator {
	
	Player me, opponent;
	GameState gameState;
	
	/**
	 * 
	 * @param me Player object that is considered 'me'
	 * @param opponent Player object that is considered the 'opponent'
	 * @param gameState
	 */
	public DiffAggregator(Player me, Player opponent, GameState gameState){
		this.me = me;
		this.opponent = opponent;
		this.gameState = gameState;
	}
	
	/**
	 * Returns an {@link AggregateDiff} that expresses the difference of units
	 * BEHIND if difference < -1
	 * EVEN if -1 <= difference <= 1
	 * AHEAD if difference > 1
	 * 
	 * TODO might need to differentiate between workers and military (1 heavy can be great difference)
	 * @param ammount
	 * @return
	 */
	public AggregateDiff aggregateUnitDiff(UnitType type){
		
		int unitDiff = unitDiff(type);
		
		if(unitDiff < -1) return AggregateDiff.BEHIND;
		else if(unitDiff > 1) return AggregateDiff.AHEAD;
		return AggregateDiff.EVEN;
	}
	
	/**
	 * Returns an {@link AggregateDiff} that expresses the difference of buildings
	 * BEHIND if difference < 0
	 * EVEN if difference == 0
	 * AHEAD if difference > 0
	 * @param ammount
	 * @return
	 */
	public AggregateDiff aggregateBuildingDiff(UnitType type){
		int unitDiff = unitDiff(type);
		
		if(unitDiff < 0) return AggregateDiff.BEHIND;
		else if(unitDiff > 0) return AggregateDiff.AHEAD;
		return AggregateDiff.EVEN;
	}
	
	/**
	 * Returns an {@link AggregateDiff} that expresses the difference of resources
	 * BEHIND if difference < -3
	 * EVEN if -3 <= difference <= 3
	 * AHEAD if difference > 3
	 * @param ammount
	 * @return
	 */
	public AggregateDiff aggregateResourceDiff(){
		int diff = me.getResources() - opponent.getResources();
		
		if(diff < -3) return AggregateDiff.BEHIND;
		else if(diff > 3) return AggregateDiff.AHEAD;
		return AggregateDiff.EVEN;
	}
	
	/**
	 * Counts the units of a type that belong to a player
	 * @param type
	 * @param player
	 * @return
	 */
	public int countUnits(UnitType type, Player player){
		int count = 0;
		
		for(Unit u : gameState.getUnits()){
			if(u.getType().equals(type) && u.getPlayer() == player.getID()) count++;
		}
		
		return count;
	}
	
	/**
	 * Calculates the difference of units of a given type 
	 * owned by me and opponent
	 * @param type
	 * @return
	 */
	public int unitDiff(UnitType type){
		int myCount = countUnits(type, me);
		int oppCount = countUnits(type, opponent);
		
		return myCount - oppCount;
	}
}
