package rl.models.stages;

import java.util.Map;

import javax.swing.JFrame;

import ai.core.AI;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.model.JointModel;
import gui.PhysicalGameStatePanel;
import rl.RLParamNames;
import rl.RLParameters;
import rts.GameState;
import rts.PlayerAction;

public class StagesJointActionModel implements JointModel {
	
	Map<String, AI> actions;
	int maxCycles;
	
	public StagesJointActionModel(Map<String, AI> actions) {
		this.actions = actions;
		maxCycles = (int) RLParameters.getInstance().getParameter(RLParamNames.GAME_DURATION);
	}
	

	@Override
	public State sample(State s, JointAction ja) {
		GameStage state = (GameStage)s;
		
		GameState gameState = state.getUnderlyingState().clone();
		GameStages currentStage = state.getStage();
		
		boolean gameOver = false;
		boolean changedStage = false;	//stores whether game has advanced a stage
		
		//JFrame w = PhysicalGameStatePanel.newVisualizer(gameState, 640, 640, false, PhysicalGameStatePanel.COLORSCHEME_BLACK);
		
		long nextTimeToUpdate = System.currentTimeMillis() + StagesDomainGenerator.PERIOD;
		
		//instantiates the AIs that players selected (clones the objects)
		AI ai1 = actions.get(ja.action(0).actionName()).clone();
		AI ai2 = actions.get(ja.action(1).actionName()).clone();
		//System.out.println("Actions: P1: " + ai1 + " / P2: " + ai2);
		
		//advance game until next stage is reached or game finishes
		do {
			//if (System.currentTimeMillis() >= nextTimeToUpdate) {

				PlayerAction pa1 = null, pa2 = null;
				try {
					pa1 = ai1.getAction(0, gameState);
					pa2 = ai2.getAction(1, gameState);
					
				} catch (Exception e) {
					System.err.println("An error happened when getting action for a player :(");
					e.printStackTrace();
				}
				
				gameState.issueSafe(pa1);
				gameState.issueSafe(pa2);

				// simulate:
				gameOver = gameState.cycle();
				
				//updates display
				//w.repaint();
				
				//checks whether game has advanced to a new stage
				changedStage = currentStage != GameStage.frameToStage(gameState.getTime()); 
				
				//w.repaint();
				nextTimeToUpdate += StagesDomainGenerator.PERIOD;
			/*} else {
				try {
					Thread.sleep(1);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}*/
		} while (!gameOver && !changedStage && gameState.getTime() < maxCycles);
		
		//returns the new State associated with current underlying game state
		GameStage newStage = new GameStage(gameState);

		//timeout is not checked inside GameStage constructor, set finished here
		if(gameState.getTime() >= maxCycles){
			newStage.setStage(GameStages.FINISHED);
		}
		return newStage; 
		
	}

}
