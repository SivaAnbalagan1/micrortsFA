package ai.metagame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import burlap.behavior.learningrate.LearningRate;
import rl.functionapprox.linearq.GameInfo;
import rl.functionapprox.linearq.LearningRateExpDecay;
import rl.functionapprox.linearq.LinearQStateFeatures;
import rl.functionapprox.linearq.TDFA;
import rl.models.stages.GameStage;
import rl.models.stages.GameStages;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;
import rts.units.*;
public class MetaBotAIR1 extends AIWithComputationBudget {
	GameState gs1;
	AI activeAI;
	AI [] givenAIs;
	
	
    AI strategies[] = null;
    EvaluationFunction evaluation = null;
    UnitTypeTable utt1;
    public TDFA tdFA;
    GameInfo gameInfo;
    GameStage gameStage;
    String []names; String aiName;
	LinearQStateFeatures featureList;
	List<UnitType> unitTypes;
	GameStages gameStages;
	public Map<String, AI> AIlookup;
	double [] featinit,weightsclone;
	double [] weights,featureValue;
	int playerGameInfo;
	double  epsi, epsiDecay,l_etrace;
	LearningRateExpDecay lRate;
	public MetaBotAIR1(UnitTypeTable utt) {
		this(new AI[]{new WorkerRush(utt),
                   new LightRush(utt),
                   new RangedRush(utt),
                   new HeavyRush(utt)},
          new String[]{"WorkerRush","LightRush","RangedRush","HeavyRush"},
          new double[]{},
          new double[]{},
                 utt, new LearningRateExpDecay(0.001,1,0.001),0,1,0.3);
	    }
	   
	public MetaBotAIR1(AI s[], String n[], double [] weights,double [] featureinit,
			UnitTypeTable utt, LearningRateExpDecay learningRate,double epsilon, double decayEpsilon,double lEtrace) {
	        super(3000, 10);
	        
	        tdFA =  new TDFA(n,featureinit,weights,learningRate,epsilon,decayEpsilon,lEtrace);
	        weightsclone = weights; featinit = featureinit;
	        lRate = learningRate; epsi = epsilon; epsiDecay = decayEpsilon;
	        l_etrace = lEtrace;
	        givenAIs = s;names=n;utt1=utt;
	        AIlookup = new HashMap<String,AI>();
	        unitTypes = utt.getUnitTypes();
	        for(int i=0;i<s.length;i++)AIlookup.put(n[i], s[i]);
	    }
	    
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		 if (gs.canExecuteAnyAction(player)) {
			 playerGameInfo = player;
			 activeAI = AIlookup.get(tdFA.getAction(getFeature(gs))).clone();
	//		 System.out.println("AI" + activeAI.toString());
			return activeAI.getAction(player, gs);
		 }
		 else{
			 return new PlayerAction();
		 }
		
	}
	public double[] getFeature(GameState gs){
		 gameInfo = new GameInfo(gs,names,unitTypes,playerGameInfo);
		 featureValue = gameInfo.findGamePosition(gs);
         return featureValue; 
	}
	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

    
    @Override
    public AI clone() {
        return new MetaBotAIR1(givenAIs, names, weightsclone,featinit,utt1,lRate,epsi,epsiDecay,l_etrace);
    }
    
    
    @Override
    public String toString() {
        return getClass().getSimpleName(); // + "(" + TIME_BUDGET + ", " + ITERATIONS_BUDGET + ", " + LOOKAHEAD + ", " + evaluation + ")";
    }

    
    @Override
    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();
        
        
        return parameters;
    }
    
          
}

