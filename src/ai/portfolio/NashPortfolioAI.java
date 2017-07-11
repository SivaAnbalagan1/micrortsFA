package ai.portfolio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.UUID;

import ai.core.AI;
import ai.evaluation.EvaluationFunction;
import rl.planners.BackwardInduction;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

/**
 * Extends {@link PortfolioAI} by calculating the Nash Equilibrium
 * of the normal-form game with reward matrix obtained via playouts
 * 
 * @author anderson
 *
 */
public class NashPortfolioAI extends PortfolioAI {
	
	String workingDir;
	
	public NashPortfolioAI(UnitTypeTable utt){
		super(utt);
	}

	public NashPortfolioAI(AI[] s, boolean[] d, int time, int max_playouts, int la, EvaluationFunction e) {
		super(s, d, time, max_playouts, la, e);
		
		DEBUG = 1;
		
		workingDir = "/tmp/nash_" + UUID.randomUUID().toString() + "/";
		
		// creates the working dir
		File dir = new File(workingDir);

		if(dir.mkdirs() == false){
			throw new RuntimeException("Unable to create working directory " + workingDir);
		}
		
		if(DEBUG > 0) {
			System.out.println("NashPortfolioAI on working dir: " + workingDir);
		}
		
	}
	
	@Override
    public void computeDuringOneGameFrame() throws Exception { 
		 super.computeDuringOneGameFrame();
	}
	
	@Override
	public PlayerAction getBestActionSoFar() throws Exception {
        AI ai = getBestScriptSoFar();
        return ai.getAction(playerForThisComputation, gs_to_start_from);
    }
	
	@Override
	/**
	 * Determines the Nash Equilibrium in the normal-form game induced
	 * by the matrix constructed via playouts and returns a script
	 * according to the equilibrium policy
	 * 
	 * @return
	 */
	public AI getBestScriptSoFar() {
		int n = strategies.length;
        if (DEBUG>=1) {
            System.out.println("PortfolioAI, game cycle: " + gs_to_start_from.getTime());
            System.out.println("  counts:");
            for(int i = 0;i<n;i++) {
                System.out.print("    ");
                for(int j = 0;j<n;j++) {
                    System.out.print(counts[i][j] + "\t");
                }
                System.out.println("");
            }
            System.out.println("  scores:");
            for(int i = 0;i<n;i++) {
                System.out.print("    ");
                for(int j = 0;j<n;j++) {
                	System.out.print(String.format(Locale.ROOT, "%.5f\t", scores[i][j]/counts[i][j]));
                }
                System.out.println("");
            }
        }
        
        double[] policy;
		try {
			policy = solve();
		} catch (Exception e) {
			System.err.println("Error while solfing the game from current state. Using default policy");
			e.printStackTrace();
			
			AI ai = strategies[0].clone();
			ai.reset();
			return ai;
		}
        
        // roulette selection
        AI ai = rouletteSelection(policy);
        
        if (DEBUG>=1) {
            System.out.println("NashPortfolioAI: selected " + ai);
        }
        
        // use a clone of the selected AI
        ai = ai.clone();
        ai.reset();
		return ai;
	}
	
	/**
	 * Formulates a normal-form game from the playout matrix, solves it
	 * and returns the Nash Equilibrium policy for the row player
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public double[] solve() throws IOException, InterruptedException{
				
		// writes a file with a normal-form game for Gambit
		String fileForGambit = workingDir + "/state.nfg";
		BufferedWriter fileWriter;
		fileWriter = new BufferedWriter(new FileWriter(fileForGambit));
		
		fileWriter.write("NFG 1 R \"state\"\n");	// default header
		fileWriter.write(
			String.format("{\"Player0\" \"Player1\"}{%d %d}\n", strategies.length, strategies.length)
		);
		
		//fills Q values for the given state 
		for(int i = 0; i < strategies.length; i++){
			for(int j = 0; j < strategies.length; j++){
				
				double myPayoff = scores[i][j] / counts[i][j];
				
				// replaces NaN with zero to avoid empty output from Gambit
				myPayoff = Double.isNaN(myPayoff) ? 0. : myPayoff;
				
				fileWriter.write(
					String.format(Locale.ROOT, "%f %f ", myPayoff, -myPayoff)
				);
			}
		}
		fileWriter.close();
		
		// starts gambit and captures its output
		// TODO: checks for trivial games, avoiding unnecessary calls to gambit
		
		// using gambit-lcp because it returns a single equilibrium
		// 15-digit precision, hope numeric errors don't accumulate
		Process gambit = Runtime.getRuntime().exec("gambit-lcp -d 15 -q " + fileForGambit);
		BufferedReader bri = new BufferedReader (new InputStreamReader(gambit.getInputStream()));
		String result = bri.readLine();	//get the first equilibrium
	    bri.close();
		gambit.waitFor();
		
		// resulting String is NE,prob1a,prob1b,...,prob2a,prob2b 
		String[] parts = result.trim().split(",");
		
		double[] agentPolicy = new double[strategies.length];
		double[] opponentPolicy = new double[strategies.length];
		
		// reads policies
		for(int i = 0; i < agentPolicy.length; i++){
			// i+1 to skip the first token 'NE'
			agentPolicy[i] = Double.parseDouble(parts[i+1]);
			
			// opponent policy offset 'length' from agentPolicy 
			opponentPolicy[i] = Double.parseDouble(parts[i + agentPolicy.length + 1]);
		}
		
		return agentPolicy;
	}
	
	/**
	 * Selects an action according to the given probability vector
	 * Code from http://stackoverflow.com/a/10949834
	 * @param probabilities
	 * @return
	 * FIXME: almost a duplicate from {@link BackwardInduction#rouletteSelection(double[])}
	 */
	public AI rouletteSelection(double[] probabilities) {
	    float totalScore = 0;
	    float runningScore = 0;
	    for (double prob : probabilities) {
	        totalScore += prob;
	    }

	    float rnd = (float) (Math.random() * totalScore);

	    for(int i = 0; i < probabilities.length; i++){
	        if (rnd >= runningScore && rnd<=runningScore+ probabilities[i]){
	            //selected
	        	return strategies[i];
	        }
	        runningScore += probabilities[i];
	    }
	    
	    // wut? didn't find an action?
	    return null;
	}
	
	@Override
    public AI clone() {
        return new NashPortfolioAI(strategies, deterministic, TIME_BUDGET, ITERATIONS_BUDGET, LOOKAHEAD, evaluation);
    }

}
