package tests;

import javax.swing.JFrame;

import ai.RandomAI;
import ai.RandomBiasedAI;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.core.AI;
import gui.PhysicalGameStatePanel;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

public class RandomSwitch {
	public static void main(String args[]) throws Exception {
		UnitTypeTable utt = new UnitTypeTable();
		PhysicalGameState pgs = PhysicalGameState.load("maps/basesWorkers16x16.xml", utt);
		// PhysicalGameState pgs = MapGenerator.basesWorkers8x8Obstacle();

		GameState gs = new GameState(pgs, utt);
		int MAXCYCLES = 10000;
		int PERIOD = 20;
		boolean gameover = false;
		int i = 0;

		AI strategies[] = null;
		strategies = new AI[] { new WorkerRush(utt), new LightRush(utt), new RangedRush(utt), new RandomBiasedAI() };
		int n = strategies.length;

		JFrame w = PhysicalGameStatePanel.newVisualizer(gs, 640, 640, false, PhysicalGameStatePanel.COLORSCHEME_BLACK);

		long nextTimeToUpdate = System.currentTimeMillis() + PERIOD;
		do {
			if (System.currentTimeMillis() >= nextTimeToUpdate) {
				AI ai1 = strategies[i].clone();
				AI ai2 = new RandomAI(utt);
				System.out.println("strategy ai1: " + ai1);

				PlayerAction pa1 = ai1.getAction(0, gs);
				PlayerAction pa2 = ai2.getAction(1, gs);
				
				System.out.println("action pa1: " + pa1);
				System.out.println("action pa2: " + pa2);
				if (!pa1.isEmpty()){
					i = ++i % n;
				}
				gs.issueSafe(pa1);
				gs.issueSafe(pa2);

				// simulate:
				gameover = gs.cycle();
				w.repaint();
				nextTimeToUpdate += PERIOD;
			} else {
				try {
					Thread.sleep(1);
				} catch (Exception e) {
					e.printStackTrace();

				}
			}
		} while (!gameover && gs.getTime() < MAXCYCLES);

		System.out.println("Game Over");
	}
}
