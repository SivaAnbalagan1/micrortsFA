/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.abstraction.pathfinding;

import java.io.Serializable;

import rts.GameState;
import rts.ResourceUsage;
import rts.UnitAction;
import rts.units.Unit;

/**
 *
 * @author santi
 */
public abstract class PathFinding implements Serializable{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public abstract boolean pathExists(Unit start, int targetpos, GameState gs, ResourceUsage ru);
    public abstract boolean pathToPositionInRangeExists(Unit start, int targetpos, int range, GameState gs, ResourceUsage ru);
    public abstract UnitAction findPath(Unit start, int targetpos, GameState gs, ResourceUsage ru);
    public abstract UnitAction findPathToPositionInRange(Unit start, int targetpos, int range, GameState gs, ResourceUsage ru);
    public abstract UnitAction findPathToAdjacentPosition(Unit start, int targetpos, GameState gs, ResourceUsage ru);

    public String toString() {
        return getClass().getSimpleName();
    }
}
