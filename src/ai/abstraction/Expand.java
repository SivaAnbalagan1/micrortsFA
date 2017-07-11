package ai.abstraction;

import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.ParameterSpecification;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.*;

public class Expand extends AbstractionLayerAI {

	Random r = new Random();
	UnitTypeTable utt;
	UnitType workerType;
	UnitType baseType;
	UnitType barracksType;
	UnitType lightType;
	UnitType resourceType;
	
	// Strategy implemented by this class:
	// Expand: build a new base close to another resources area

	public Expand(UnitTypeTable a_utt) {
		this(a_utt, new AStarPathFinding());
	}

	public Expand(UnitTypeTable a_utt, PathFinding a_pf) {
		super(a_pf);
		utt = a_utt;
		workerType = utt.getUnitType("Worker");
		baseType = utt.getUnitType("Base");
		barracksType = utt.getUnitType("Barracks");
		lightType = utt.getUnitType("Light");
		resourceType = utt.getUnitType("Resource");
	}

	public void reset() {
		super.reset();
	}

	public AI clone() {
		return new Expand(utt, pf);
	}

	/*
	 * This is the main function of the AI. It is called at each game cycle with
	 * the most up to date game state and returns which actions the AI wants to
	 * execute in this cycle. The input parameters are: - player: the player
	 * that the AI controls (0 or 1) - gs: the current game state This method
	 * returns the actions to be sent to each of the units in the gamestate
	 * controlled by the player, packaged as a PlayerAction.
	 */
	public PlayerAction getAction(int player, GameState gs) {
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Player p = gs.getPlayer(player);
	
		// behavior of bases:
		for (Unit u : pgs.getUnits()) {
			if (u.getType() == baseType && u.getPlayer() == player && gs.getActionAssignment(u) == null) {
				baseBehavior(u, p, pgs);
			}
		}

		// behavior of melee units:
		for (Unit u : pgs.getUnits()) {
			if (u.getType().canAttack && !u.getType().canHarvest && u.getPlayer() == player
					&& gs.getActionAssignment(u) == null) {
				meleeUnitBehavior(u, p, pgs);
			}
		}

		// behavior of workers:
		List<Unit> workers = new LinkedList<Unit>();
		for (Unit u : pgs.getUnits()) {
			if (u.getType().canHarvest && u.getPlayer() == player) {
				workers.add(u);
			}
		}
		workersBehavior(workers, p, pgs);

		// This method simply takes all the unit actions executed so far, and
		// packages them into a PlayerAction
		return translateActions(player, gs);
	}

	public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) {
		int nworkers = 0;
		for (Unit u2 : pgs.getUnits()) {
			if (u2.getType() == workerType && u2.getPlayer() == p.getID()) {
				nworkers++;
			}
		}
		if (nworkers < 8 && p.getResources() >= workerType.cost) {
			train(u, workerType);
		}
	}

	public void meleeUnitBehavior(Unit u, Player p, PhysicalGameState pgs) {
		Unit closestEnemy = null;
		int closestDistance = 0;
		for (Unit u2 : pgs.getUnits()) {
			if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
				int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
				if (closestEnemy == null || d < closestDistance) {
					closestEnemy = u2;
					closestDistance = d;
				}
			}
		}
		if (closestEnemy != null) {
			attack(u, closestEnemy);
		}
	}

	public void workersBehavior(List<Unit> workers, Player p, PhysicalGameState pgs) {
		int nbases = 0;

		int resourcesUsed = 0;
		List<Unit> freeWorkers = new LinkedList<Unit>();
		freeWorkers.addAll(workers);
		List<Unit> resources = new LinkedList<Unit>();

		if (workers.isEmpty()) {
			return;
		}

		for (Unit u2 : pgs.getUnits()) {
			if (u2.getType() == baseType && u2.getPlayer() == p.getID()) {
				nbases++;
			}
		}

		for (Unit u2 : pgs.getUnits()) {
			if (u2.getType() == resourceType) {
				resources.add(u2);
			}
		}

		List<Integer> reservedPositions = new LinkedList<Integer>();
		if (nbases == 0 && !freeWorkers.isEmpty()) {
			// build a base:
			if (p.getResources() >= baseType.cost + resourcesUsed) {
				Unit u = freeWorkers.remove(0);
				buildIfNotAlreadyBuilding(u, baseType, u.getX(), u.getY(), reservedPositions, p, pgs);
				resourcesUsed += baseType.cost;
			}
		} else if (nbases > 0 && !freeWorkers.isEmpty()) {
			// build a base:
			int min_d = Integer.MAX_VALUE;
			int best_resource_x = 0;
			int best_resource_y = 0;
			for (Unit resource : resources) {
				for (Unit u2 : pgs.getUnits()) {
					if (u2.getType() == baseType && u2.getPlayer() == p.getID()) {
						int d = Math.abs(u2.getX() - resource.getX()) + Math.abs(u2.getY() - resource.getY());
						if (d < min_d && d > 5){
							min_d = d;
							best_resource_x = resource.getX();
							best_resource_y = resource.getY();
						}
					}
				}
			}
			if (p.getResources() >= baseType.cost + resourcesUsed) {
				Unit u = freeWorkers.remove(0);
				buildIfNotAlreadyBuilding(u, baseType, best_resource_x, best_resource_y,
						reservedPositions, p, pgs);
				resourcesUsed += baseType.cost;
			}
		}

		// harvest with all the free workers:
		for (Unit u : freeWorkers) {
			Unit closestBase = null;
			Unit closestResource = null;
			int closestDistance = 0;
			for (Unit u2 : pgs.getUnits()) {
				if (u2.getType().isResource) {
					int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
					if (closestResource == null || d < closestDistance) {
						closestResource = u2;
						closestDistance = d;
					}
				}
			}
			closestDistance = 0;
			for (Unit u2 : pgs.getUnits()) {
				if (u2.getType().isStockpile && u2.getPlayer() == p.getID()) {
					int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
					if (closestBase == null || d < closestDistance) {
						closestBase = u2;
						closestDistance = d;
					}
				}
			}
			if (closestResource != null && closestBase != null) {
				AbstractAction aa = getAbstractAction(u);
				if (aa instanceof Harvest) {
					Harvest h_aa = (Harvest) aa;
					if (h_aa.target != closestResource || h_aa.base != closestBase)
						harvest(u, closestResource, closestBase);
				} else {
					harvest(u, closestResource, closestBase);
				}
			}
		}
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		List<ParameterSpecification> parameters = new ArrayList<>();

		parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));

		return parameters;
	}
}
