package tests;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ai.RandomBiasedAI;
import ai.abstraction.BuildBarracks;
import ai.abstraction.Expand;
import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.ahtn.AHTNAI;
import ai.core.AI;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.metagame.MetaBotAI;
import ai.metagame.MetaBotAIR1;
import ai.portfolio.NashPortfolioAI;
import ai.portfolio.PortfolioAI;
import ai.portfolio.portfoliogreedysearch.PGSAI;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;


public class MetaBotTest {
	public static void main(String args[]) throws Exception {
		List<AI> bots = new LinkedList<>();
        UnitTypeTable unitTypeTable = new UnitTypeTable();
        int featSize = unitTypeTable.getUnitTypes().size();
        double weightLow = -1/Math.sqrt(featSize*9*2*4);//quadrant 9 and player 2
        double weightHigh = 1/Math.sqrt(featSize*9*2*4);
        double range = weightHigh - weightLow;
        double [] initialWeights = new double[featSize*9*2*4];//4 AI-actions
        double [] features = new double[featSize*9*2];
        Arrays.fill(features, 0.1);
        Random r = new Random();
        for(int i=0;i<initialWeights.length;i++){
            initialWeights[i] = r.nextDouble() * range + weightLow;
 //           System.out.println(initialWeights[i]);
        }
        AI player1 = new MetaBotAIR1(
        		new AI[]{
    	    		new WorkerRush(unitTypeTable),
    	            new LightRush(unitTypeTable),
    	            new RangedRush(unitTypeTable),
    	            new HeavyRush(unitTypeTable),
    	            //new BuildBarracks(unitTypeTable),
    	            //new Expand(unitTypeTable)
                },
               new String[]{"WorkerRush","LightRush","RangedRush","HeavyRush"},
               initialWeights,
               features,
               unitTypeTable
            );
        /*AI player1 = new NashPortfolioAI(
    		new AI[]{
	    		new WorkerRush(unitTypeTable),
	            new LightRush(unitTypeTable),
	            new RangedRush(unitTypeTable),
	            new HeavyRush(unitTypeTable),
	            //new BuildBarracks(unitTypeTable),
	            //new Expand(unitTypeTable)
            },
           new boolean[]{true,true,true,true,},
           100, -1, 100,
           new SimpleSqrtEvaluationFunction3()
        );
       NashPortfolioAI.DEBUG = 0;*/
        
       AI player2 = new PortfolioAI(
    		new AI[]{
	    		new WorkerRush(unitTypeTable),
	            new LightRush(unitTypeTable),
	            new RangedRush(unitTypeTable),
	            new HeavyRush(unitTypeTable),
	            //new BuildBarracks(unitTypeTable),
	            //new Expand(unitTypeTable)
            },
           new boolean[]{true,true,true,true,},
           100, -1, 100,
           new SimpleSqrtEvaluationFunction3()
        );
        //AI player2 = new AHTNAI(unitTypeTable);
        
        bots.add(player1);
        System.out.println("Added first player.");
        
        bots.add(player2);
        System.out.println("Added second player.");
        
        PrintStream out = System.out;
        
        // prepares maps
        List<PhysicalGameState> maps = new LinkedList<PhysicalGameState>();        
        maps.add(PhysicalGameState.load("maps/basesWorkers24x24.xml", unitTypeTable));
        System.out.println("Maps prepared.");
        
        // runs the 'tournament'
        Experimenter.runExperiments(bots, maps, unitTypeTable, 1, 3000, 300, true, out,0,true,false);
        System.out.println("Done.");
	}
    
}
