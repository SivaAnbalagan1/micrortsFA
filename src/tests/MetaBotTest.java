package tests;

import java.io.PrintStream;
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
import ai.portfolio.NashPortfolioAI;
import ai.portfolio.PortfolioAI;
import ai.portfolio.portfoliogreedysearch.PGSAI;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;


public class MetaBotTest {
	public static void main(String args[]) throws Exception {
		List<AI> bots = new LinkedList<>();
        UnitTypeTable unitTypeTable = new UnitTypeTable();
        
        //bots.add(new MetaBot(timeBudget, iterationsBudget, unitTypeTable, "/tmp/qltest/qtable0_99"));
        //bots.add(new ai.rl.MetaBot("BackwardInduction", "/tmp/solution-winloss.xml", "aggregatediff"));
        //bots.add(new ai.rl.MetaBot("MinimaxQ", "/tmp/solution-winloss.xml", "aggregatediff"));
        //bots.add(new ai.rl.MetaBot());
        double weightLow = -1/Math.sqrt(8);
        double weightHigh = 1/Math.sqrt(8);
        double range = weightHigh - weightLow;
        double [] initialWeights = new double[8*4];
        Random r = new Random();
        for(int i=0;i<8*4;i++){//4 AIs and 8 features.
            initialWeights[i] = r.nextDouble() * range + weightLow;
            System.out.println(initialWeights[i]);
        }
        AI player1 = new MetaBotAI(
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
            /*  new double[]{
            		  		1,1,1,1,1,1,1,1,
            		  		1,1,1,1,1,1,1,1,
            		  		1,1,1,1,1,1,1,1,
            		  		1,1,1,1,1,1,1,1},*/
              100, -1, 100,
               new SimpleSqrtEvaluationFunction3()
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
        Experimenter.runExperiments(bots, maps, unitTypeTable, 1, 3000, 300, true, out);
        System.out.println("Done.");
	}
    
}
