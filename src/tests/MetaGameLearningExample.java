package tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.jdom.JDOMException;

import ai.abstraction.LightRush;
import ai.metagame.DummyPolicy;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.stochasticgames.GameEpisode;
import burlap.behavior.stochasticgames.PolicyFromJointPolicy;
import burlap.behavior.stochasticgames.agents.interfacing.singleagent.LearningAgentToSGAgentInterface;
import burlap.behavior.stochasticgames.agents.maql.MultiAgentQLearning;
import burlap.behavior.stochasticgames.auxiliary.GameSequenceVisualizer;
import burlap.behavior.stochasticgames.madynamicprogramming.SGBackupOperator;
import burlap.behavior.stochasticgames.madynamicprogramming.backupOperators.MinMaxQ;
import burlap.behavior.valuefunction.QValue;
import burlap.debugtools.DPrint;
import burlap.domain.stochasticgames.gridgame.GGVisualizer;
import burlap.mdp.auxiliary.DomainGenerator;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.action.ActionType;
import burlap.mdp.stochasticgames.JointAction;
import burlap.mdp.stochasticgames.SGDomain;
import burlap.mdp.stochasticgames.agent.SGAgent;
import burlap.mdp.stochasticgames.agent.SGAgentType;
import burlap.mdp.stochasticgames.model.JointRewardFunction;
import burlap.mdp.stochasticgames.world.World;
import burlap.statehashing.HashableStateFactory;
import burlap.statehashing.simple.SimpleHashableStateFactory;
import burlap.visualizer.Visualizer;
import rl.adapters.learners.PersistentMultiAgentQLearning;
import rl.adapters.learners.SGQLearningAdapter;
import rl.models.stages.GameStagesDomain;
import rl.models.stages.StagesDomainGenerator;
import rl.models.common.MicroRTSTerminalFunction;
import rl.models.common.WinLossRewardFunction;
import rl.models.stages.GameStage;

/**
 * An example of the Algorithm Selection Metagame in microRTS
 * 
 * @author anderson
 *
 */
public class MetaGameLearningExample {
	public MetaGameLearningExample() {
		StagesDomainGenerator microRTSGame = null;
		try {
			microRTSGame = new StagesDomainGenerator();
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		SGDomain microRTSDomain = null;
		try {
			microRTSDomain = new GameStagesDomain();
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}//(SGDomain) microRTSGame.generateDomain();
		SGAgentType agentType = new SGAgentType("agent", microRTSDomain.getActionTypes());

		JointRewardFunction rwdFunc = new WinLossRewardFunction();
		TerminalFunction terminalFunc = new MicroRTSTerminalFunction();

		// learning parameters
		final double discount = 0.95;
		final double learningRate = 0.1;
		final double defaultQ = 1;

		int ngames = 100;
		
		World w = new World(microRTSDomain, rwdFunc, terminalFunc, microRTSGame.getInitialState());
		
		// single agent Q-learning algorithms which will operate in our stochastic game
		// don't need to specify the domain, because the single agent interface will provide it
		QLearning ql1 = new QLearning(null, discount, new SimpleHashableStateFactory(false), defaultQ, learningRate);
		QLearning ql2 = new QLearning(null, discount, new SimpleHashableStateFactory(false), defaultQ, learningRate);
		
		// ql2 will be a dummy, always selecting the same behavior
		ql2.setLearningPolicy(new DummyPolicy(StagesDomainGenerator.RANGED_RUSH, ql2));
		
		// creates a MultiAgentQLearning extended with save/load knowledge 
		// queryOtherAgentsQSource must be false, otherwise the agent throws an exception
		PersistentMultiAgentQLearning minimaxQ = new PersistentMultiAgentQLearning(
			w.getDomain(), .9, .1, new SimpleHashableStateFactory(),
			1, new MinMaxQ(), false, "MAQL", 
			new SGAgentType("MiniMaxQ", w.getDomain().getActionTypes())
		);
		
		// creates an (old-style) adapter for the dummy player
		LearningAgentToSGAgentInterface dummy = new LearningAgentToSGAgentInterface(microRTSDomain, ql2, "agent1",
				agentType);
		
		// creates an adapter for the Single Agent QLearning player
		// this extends LearningAgentToSGAgentInterface with save/load capabilities
		SGQLearningAdapter sgql = new SGQLearningAdapter(microRTSDomain, ql1, "SGQL", agentType);
		
		//Uncomment the two players who will join the game
		w.join(sgql);
		//w.join(minimaxQ);
		w.join(dummy);
		
		//sgql.saveKnowledge("/tmp/qtable.sav");
		//ql3.saveKnowledge("/tmp/qtable-maql.sav");
		
		// don't have the world print out debug info (comment out if you want to
		// see it!)
		DPrint.toggleCode(w.getDebugId(), false);

		System.out.println("Starting training");
		
		List<GameEpisode> episodes = new ArrayList<GameEpisode>(ngames);
		PrintWriter output = null;
		
		try {
			output = new PrintWriter(new BufferedWriter(new FileWriter("output.txt", false)));
			for (int i = 0; i < ngames; i++) {
				System.out.print(String.format(
					"\rRunning game %7d", i
				));
				GameEpisode episode = w.runGame();
				//System.out.println("game finished...");
				episodes.add(episode);
				/*if (i % 10 == 0) {
					System.out.println("Game: " + i + ": " + episode.maxTimeStep());
				}*/
				episode.write("/tmp/qltest/qltest_" + i);
				sgql.saveKnowledge("/tmp/qltest/qtable0_" + i);
				//minimaxQ.saveKnowledge("/tmp/qltest/qtable1_" + i);
				
				output.println("Game: " + i);
				output.println("Value functions for agent 0");

				for (GameStage s : GameStage.allStates()) {
					output.println(String.format("%s: %.3f", s, ql1.value(s)));
					for (QValue q : ql1.qValues(s)) {
						output.println(String.format("%s: %.3f", q.a, q.q));
					}
				}

				output.println("Value functions for agent 1");
				
				//prints values of all joint actions for each state
				for (GameStage s : GameStage.allStates()) {
					//output.println(String.format("%s: %.3f", s, ql2.value(s)));
					
					List<JointAction> jointActions = JointAction.getAllJointActions(s, w.getRegisteredAgents());
					
					for(JointAction jointAction : jointActions){
						output.println(String.format(
							"\t%s: %.3f", jointAction, minimaxQ.getMyQSource().getQValueFor(s, jointAction).q
						));
					}
				}
			}
			//ql3.saveKnowledge("/tmp/qltest/qtable2");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		output.close();
		System.out.println("\nTraining finished!");	//has leading \n because print in loop has no \n
	}

	public static void main(String[] args) {

		new MetaGameLearningExample();
		//System.out.println("Learning finished.");
		try {
			Runtime.getRuntime().exec("python python/plot_actions.py");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Plot finished.");
	}

}
