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
import rl.functionapprox.linearq.GameInfo;
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
	
	int LOOKAHEAD = 500;
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
//To do change default weights and initial fvalues.		
	public MetaBotAIR1(UnitTypeTable utt) {
		this(new AI[]{new WorkerRush(utt),
                   new LightRush(utt),
                   new RangedRush(utt),
                   new HeavyRush(utt)},
          new String[]{"WorkerRush","LightRush","RangedRush","HeavyRush"},
          new double[]{},
          new double[]{},
        		  100, -1, 100,
          new SimpleSqrtEvaluationFunction3(),utt);
	    }
	   
	public MetaBotAIR1(AI s[], String n[], double [] weights,double [] featureinit,int time, int max_playouts, int la,
			EvaluationFunction e,UnitTypeTable utt) {
	        super(time, max_playouts);
	        tdFA =  new TDFA(s,n,featureinit,weights);
	        weightsclone = weights; featinit = featureinit;
	        LOOKAHEAD = la;givenAIs = s;names=n;utt1=utt;
	        AIlookup = new HashMap<String,AI>();
	        unitTypes = utt.getUnitTypes();
	        for(int i=0;i<s.length;i++)AIlookup.put(n[i], s[i]);
	    }
	    
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		 if (gs.canExecuteAnyAction(player)) {
			 
			 activeAI = AIlookup.get(tdFA.getAction(getFeature(gs))).clone();
			return activeAI.getAction(player, gs);
		 }
		 else{
			 return new PlayerAction();
		 }
		
	}
	public double[] getFeature(GameState gs){
		 gameInfo = new GameInfo(gs,names,unitTypes);
		 featureValue = gameInfo.findGamePosition(gs);
         return featureValue; 
	}
	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

    
    @Override
    public AI clone() {
        return new MetaBotAIR1(givenAIs, names, weightsclone,featinit,TIME_BUDGET, ITERATIONS_BUDGET, LOOKAHEAD, evaluation,utt1);
    }
    
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + TIME_BUDGET + ", " + ITERATIONS_BUDGET + ", " + LOOKAHEAD + ", " + evaluation + ")";
    }

    
    @Override
    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();
        
        parameters.add(new ParameterSpecification("TimeBudget",int.class,100));
        parameters.add(new ParameterSpecification("IterationsBudget",int.class,-1));
        parameters.add(new ParameterSpecification("PlayoutLookahead",int.class,100));
        parameters.add(new ParameterSpecification("EvaluationFunction", EvaluationFunction.class, new SimpleSqrtEvaluationFunction3()));
        
        return parameters;
    }
    
    
    public int getPlayoutLookahead() {
        return LOOKAHEAD;
    }
    
    
    public void setPlayoutLookahead(int a_pola) {
        LOOKAHEAD = a_pola;
    }
       
    
    public EvaluationFunction getEvaluationFunction() {
        return evaluation;
    }
    
    
    public void setEvaluationFunction(EvaluationFunction a_ef) {
        evaluation = a_ef;
    }           
}


