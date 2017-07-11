package rl.models.singlestate;

import java.util.Map;

import javax.swing.JFrame;

import ai.core.AI;
import burlap.mdp.core.state.State;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.model.JointModel;
import gui.PhysicalGameStatePanel;
import rl.RLParamNames;
import rl.RLParameters;
import rl.models.singlestate.SingleState;
import rl.models.stages.GameStages;
import rl.models.stages.StagesDomainGenerator;
import rts.GameState;
import rts.PlayerAction;

public class SingleStateJAM implements JointModel {
	
	Map<String, AI> actions;
	int maxCycles;
	
	public SingleStateJAM(Map<String, AI> actions) {
		this.actions = actions;
		maxCycles = (int) RLParameters.getInstance().getParameter(RLParamNames.GAME_DURATION);
	}

	@Override
	public State sample(State s, JointAction ja) {
		SingleState state = (SingleState)s;
		
		GameState gameState = state.getUnderlyingState().clone();
		GameStages currentStage = state.getStage();// GameStages.OPENING;
		
		boolean gameOver = false;
		boolean changedStage = false;	//stores whether game has advanced a stage
		
		//JFrame w = PhysicalGameStatePanel.newVisualizer(gameState, 640, 640, false, PhysicalGameStatePanel.COLORSCHEME_BLACK);
		int delay = 0; //milisseconds
		long nextTimeToUpdate = System.currentTimeMillis() + delay;
		
		//instantiates the AIs that players selected (clones the objects)
		AI ai1 = actions.get(ja.action(0).actionName()).clone();
		AI ai2 = actions.get(ja.action(1).actionName()).clone();
		
		//advance game until next stage is reached or game finishes
		do {
			if (System.currentTimeMillis() >= nextTimeToUpdate) {

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
				changedStage = currentStage != SingleState.frameToStage(gameState.getTime()); 
				
				//w.repaint();
				nextTimeToUpdate += delay;
			}
		} while (!gameOver && !changedStage && gameState.getTime() < maxCycles);
		
		//returns the new State, with a 'finished' on it
		SingleState theNewState = new SingleState(gameState); 
		theNewState.setStage(GameStages.FINISHED);
		//System.out.println(theNewState);
		return theNewState; 
	}
	
}
