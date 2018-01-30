package tests;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
import ai.mcts.believestatemcts.BS3_NaiveMCTS;
import ai.metagame.MetaBotAI;
import ai.metagame.MetaBotAIR1;
import ai.portfolio.NashPortfolioAI;
import ai.portfolio.PortfolioAI;
import ai.portfolio.portfoliogreedysearch.PGSAI;
import rl.functionapprox.linearq.LearningRateExpDecay;
import rts.PhysicalGameState;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import standard.StrategyTactics;


public class MetaBotTest {
	public static void main(String args[]) throws Exception {
		List<AI> bots = new LinkedList<>();
        UnitTypeTable unitTypeTable = new UnitTypeTable();
        String [] AInames ;
       AInames = new String[]{"WorkerRush","LightRush","RangedRush","HeavyRush","BuildBarracks","Expand"};
        //without LightRush
     //  AInames = new String[]{"WorkerRush","RangedRush","HeavyRush","BuildBarracks","Expand"};
        
        int runit =0;
		UnitTypeTable uTTable = new UnitTypeTable();
        for(UnitType ut: uTTable.getUnitTypes())if(ut.isResource)runit++;
        int quadFeat = 0;
        quadFeat = uTTable.getUnitTypes().size() -runit;
        int featSize = quadFeat *9*2 +9+9+2+1;//intercept+1;// 2-resource, 9-health for 2 players and time
        //1 more for intercept
        //int featSize = unitTypeTable.getUnitTypes().size()-1;
        //featSize = featSize*9 *2;
        //featSize = featSize + 9+9+2+1;
        double weightLow = -1/Math.sqrt(featSize);//quadrant 9 and player 2
        double weightHigh = 1/Math.sqrt(featSize);
        double range = weightHigh - weightLow;
        double [] initialWeights = new double[featSize*6];//4 AI-actions
        double [] features = new double[featSize];
        double [] loadedWeights; 
        Arrays.fill(features, 0.0);
    
        String path = "/media1/siva/OS/Lancaster/Dissertation/git/microrts-burlap-integration/experiments/FunctionApprox/q_MetaBot_final.txt";
        loadedWeights = loadKnowledge(path,featSize,AInames);
       if(loadedWeights!= null)initialWeights = loadedWeights;
       else{
            Random r = new Random();
            for(int i=0;i<initialWeights.length;i++){
            initialWeights[i] = r.nextDouble() * range + weightLow;
             }
        }
        AI player1 = new MetaBotAIR1(
        		new AI[]{
    	    		new WorkerRush(unitTypeTable),
    	            new LightRush(unitTypeTable),
    	            new RangedRush(unitTypeTable),
    	            new HeavyRush(unitTypeTable),
    	            new BuildBarracks(unitTypeTable),
    	            new Expand(unitTypeTable)
                },
        		AInames,
               initialWeights,
               features,
               unitTypeTable,new LearningRateExpDecay(0,0,0),0,0.9996001,0.3
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
        
       /*AI player2 = new PortfolioAI(
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
        );*/
        //AI player2 = new AHTNAI(unitTypeTable);
        
     //   AI player2 = new StrategyTactics(unitTypeTable);
       //AI player2 = new BS3_NaiveMCTS(unitTypeTable);
       AI player2 = new  LightRush(unitTypeTable);
        bots.add(player1);
        System.out.println("Added first player.");
        
        bots.add(player2);
        System.out.println("Added second player.");
        
        PrintStream out = System.out;
        
        // prepares maps
        List<PhysicalGameState> maps = new LinkedList<PhysicalGameState>();        
        maps.add(PhysicalGameState.load("maps/basesWorkers24x24.xml", unitTypeTable));
     //   maps.add(PhysicalGameState.load("maps/BroodWar/BloodBath.scmB.xml", unitTypeTable));
        System.out.println("Maps prepared.");
        
        // runs the 'tournament'
        Experimenter.runExperiments(bots, maps, unitTypeTable, 1, 12000, 300, true, out,0,true,false);
        System.out.println("Done.");
	}
	
	public static double[] loadKnowledge(String path,int featSize,String [] AInames) {
		//learner.loadQTable(path);
		Map<String, double[]> weightInit;
		weightInit = new HashMap();
		double [] initialWeights = new double [featSize*6];
		//opens xml file
		DocumentBuilder dBuilder = null;
		Document doc = null;
		try {
			dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			doc = dBuilder.parse(new File(path));
		} catch (ParserConfigurationException|SAXException|IOException e) {
			System.out.println("Previous knowledge file doesnt exist " + path +". knowledge NOT LOADED!");
			//initWeight = true;
//			e.printStackTrace();
			initialWeights = null;
			return initialWeights;
		}
	//	initWeight = false;
		//retrieves the game states, they'll be useful for filling the knowledge in
	//	Map<String, State> nameToState = ((EnumerableSGDomain)domain).namesToStates();
		
		//traverses all 1st level nodes of xml file (children of root node) 
		NodeList nList = doc.getDocumentElement().getChildNodes();
		
		for (int i = 0; i < nList.getLength(); i++){
			Node n = nList.item(i);
			//if node is 'learner', checks if ID is the same as mine
			if (n.getNodeName() == "learner"){
				Element e = (Element) n;
				if(! e.getAttribute("type").equals("rl.adapters.gamenatives.MetaBotAIAdapterLQ")){
					System.err.println(
						"WARNING! Loading knowledge of agent with different type. This might end up in error..."
					);
				}
				if(Integer.parseInt(e.getAttribute("id")) != 0){
					System.err.println(
						"WARNING! Loading knowledge of agent with different ID. "
					);
				}
			}
			else if (n.getNodeName() == "state"){
				Element e = (Element) n;
				for(Node actionNode = n.getFirstChild(); actionNode != null; actionNode = actionNode.getNextSibling()){
					if (actionNode.getNodeType() != Node.ELEMENT_NODE) continue;	//prevents ClassCastException
					// retrieves the action object 
					Element actionElement = (Element) actionNode;
					String actionName = actionElement.getAttribute("name");
					// creates and stores the loaded QValue
					if(!actionName.contains("SGD")){
					String value = actionElement.getAttribute("value");
					String value1 = value.substring(1, value.length()-1);
					String [] weightString = value1.split(",");
					double [] weightDouble = Arrays.stream(weightString).mapToDouble(Double::parseDouble).toArray();
					//System.out.println(actionName.substring(actionName.length()-2,3));
					weightInit.put(actionName, weightDouble.clone());
					}
				}
			}
			}
		int srcPos =0,destPos=0;
          for(String act: AInames){
        	  System.out.println(" action " + act);
        	  double [] tempcopy = weightInit.get(act).clone();
        	  System.arraycopy(tempcopy, srcPos, initialWeights, destPos, featSize);
        	  destPos=destPos+featSize;
          }		
          System.out.println("Loaded Knowledge...");
          return initialWeights;
		}

    
}
