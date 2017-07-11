package ai.abstraction;

import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.*;

public class BuildBarracks extends AbstractionLayerAI {
	Random r = new Random();
	UnitTypeTable utt;
	UnitType workerType;
	UnitType baseType;
	UnitType barracksType;

	// Strategy implemented by this class:
	// BuildBarracks: build a barrack to increase the army faster

	public BuildBarracks(UnitTypeTable a_utt) {
		this(a_utt, new AStarPathFinding());
	}

	public BuildBarracks(UnitTypeTable a_utt, PathFinding a_pf) {
		super(a_pf);
		utt = a_utt;
		workerType = utt.getUnitType("Worker");
		baseType = utt.getUnitType("Base");
		barracksType = utt.getUnitType("Barracks");
	}

	public void reset() {
		super.reset();
	}

	public AI clone() {
		return new BuildBarracks(utt, pf);
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
		if (nworkers < 1 && p.getResources() >= workerType.cost) {
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
		int nbarracks = 0;

		int resourcesUsed = 0;
		List<Unit> freeWorkers = new LinkedList<Unit>();
		freeWorkers.addAll(workers);

		if (workers.isEmpty()) {
			return;
		}

		for (Unit u2 : pgs.getUnits()) {
			if (u2.getType() == barracksType && u2.getPlayer() == p.getID()) {
				nbarracks++;
			}
		}
		List<Integer> reservedPositions = new LinkedList<Integer>();
		if (nbarracks < 10) {
			// build a barracks:
			int n_barracks = nbarracks;

			if (p.getResources() >= barracksType.cost + resourcesUsed && !freeWorkers.isEmpty()) {
				Unit u = freeWorkers.remove(0);
				int posX = u.getX();
				int posY = u.getY();
				for (int x = 0; x < pgs.getWidth(); x++) {
					for (int y = 0; y < pgs.getHeight(); y++) {
						boolean[][] free = pgs.getAllFree();
						if (posX + x < pgs.getWidth() && posY + y < pgs.getHeight() && free[posX + x][posY + y]) {
							Collection<Unit> Units = pgs.getUnitsAround(posX + x, posY + y, 1);
							if (Units.size() == 0) {
								if (buildIfNotAlreadyBuilding(u, barracksType, posX + x, posY + y, reservedPositions, p, pgs)) {
									resourcesUsed += barracksType.cost;
									nbarracks++;
									break;
								}
							}
						}
					}
					if (n_barracks != nbarracks) {
						break;
					}
				}
				if (n_barracks == nbarracks) {
					for (int x = 0; x < pgs.getWidth(); x++) {
						for (int y = 0; y < pgs.getHeight(); y++) {
							boolean[][] free = pgs.getAllFree();
							if (posX - x > 0 && posY - y > 0 && free[posX - x][posY - y]) {
								Collection<Unit> Units = pgs.getUnitsAround(posX - x, posY - y, 1);
								if (Units.size() == 0) {
									if (buildIfNotAlreadyBuilding(u, barracksType, posX - x, posY - y, reservedPositions, p, pgs)) {
										resourcesUsed += barracksType.cost;
										nbarracks++;
										break;
									}
								}
							}
						}
						if (n_barracks != nbarracks) {
							break;
						}
					}
				}
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
