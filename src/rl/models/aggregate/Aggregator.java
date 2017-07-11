package rl.models.aggregate;

/**
 * Defines useful methods to return  {@link Aggregate}s that 
 * express quantities.
 * 
 * All levels are defined empirically
 * 
 * @author anderson
 *
 */
public class Aggregator {

	/**
	 * Returns an {@link Aggregate} that expresses the amount of workers
	 * @param ammount
	 * @return
	 */
	public Aggregate aggregateUnits(int amount){
		if(amount <= 3) return Aggregate.FEW;
		else if(amount <= 6) return Aggregate.FAIR;
		return Aggregate.MANY;
	}
	
	/**
	 * Returns an {@link Aggregate} that expresses the amount of buildings
	 * @param ammount
	 * @return
	 */
	public Aggregate aggregateBuildings(int amount){
		if(amount <= 1) return Aggregate.FEW;
		else if(amount <= 3) return Aggregate.FAIR;
		return Aggregate.MANY;
	}
	
	/**
	 * Returns an {@link Aggregate} that expresses the amount of resources
	 * @param ammount
	 * @return
	 */
	public Aggregate aggregateResources(int amount){
		if(amount <= 1) return Aggregate.FEW;
		else if(amount <= 4) return Aggregate.FAIR;
		return Aggregate.MANY;
	}
}

