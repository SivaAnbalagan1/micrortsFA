package ai.metagame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.RandomBiasedAI;
import ai.abstraction.BuildBarracks;
import ai.abstraction.Expand;
import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.portfolio.PortfolioAI;

import burlap.behavior.singleagent.vfa.common.LinearFVVFA;
import burlap.behavior.functionapproximation.sparse.LinearVFA;
import burlap.behavior.functionapproximation.sparse.StateFeature;
import burlap.mdp.core.state.State;
import rl.models.stages.GameStage;
import rl.models.stages.GameStages;
import rl.FA.TDlinearFA;
import rl.FA.MetaStateFeatures;
import rl.FA.MetaState;
import rl.models.aggregate.AggregateState;
import rl.models.aggregatediff.*;
import rts.GameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

public class MetaBotAI extends AIWithComputationBudget {
	GameState gs1;
	AI activeAI;
	AI [] givenAIs;
	
	int LOOKAHEAD = 500;
    AI strategies[] = null;
    EvaluationFunction evaluation = null;
    public TDlinearFA tdLinearFA;
    GameStage gameStage;
    String []names; String aiName;
	List<StateFeature> FeatureList;
	AggregateDiff aggrDiff,aggrDiffTest;
	GameStages gameStages;
	public Map<String, AI> AIlookup;
	double [] initialfValues;
	double [] weightsclone = new double[8];
	public MetaBotAI(UnitTypeTable utt) {
		this(new AI[]{new WorkerRush(utt),
                   new LightRush(utt),
                   new RangedRush(utt),
                   new HeavyRush(utt)},
          new String[]{"WorkerRush","LightRush","RangedRush","HeavyRush"},
          new double[]{
        		  0.2972181985397605,-0.0018355752925197089,-0.10642760449301844,-0.20269212399723863,0.1060424502713414,-0.25050793011429706,0.18168086644062953,0.2610385129775247,
        		  0.12602836453955507,-0.19954993301886906,-0.2761414412352535,0.06776460036725779,0.34240939719608954,0.09141589184892335,0.16315958744551107,-0.25363403458127476,
        		  -0.1724987586057259,0.13292574296207776,0.23825508122796923,0.1813770754438091,0.07573342656729948,0.3333801523422337,-0.2220965896910001,-0.3121358221849899,
        		  0.23257900733000736,0.3017366203684604,0.22508345397026253,0.0798713369801311,-0.12842335459552995,-0.12833803851961367,0.10741777900545108,0.1368704572879647}, 
        		  100, -1, 100,
          new SimpleSqrtEvaluationFunction3());
	    }
	   
	public MetaBotAI(AI s[], String n[], double [] weights,int time, int max_playouts, int la,EvaluationFunction e) {
	        super(time, max_playouts);
	        initialfValues = new double [] {0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1};
	        tdLinearFA =  new TDlinearFA(s,n,weights,initialfValues);
	        weightsclone = weights;
	        LOOKAHEAD = la;givenAIs = s;names=n;
	        AIlookup = new HashMap<String,AI>();FeatureList = new ArrayList<StateFeature>();
	        for(int i=0;i<s.length;i++)AIlookup.put(n[i], s[i]);
	    }
	    
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		 if (gs.canExecuteAnyAction(player)) {
			activeAI = AIlookup.get(tdLinearFA.getAction(getFeatures(gs))).clone();
			return activeAI.getAction(player, gs);
		 }
		 else{
			 return new PlayerAction();
		 }
		
	}
		
	public List<StateFeature> getFeatures(GameState gs) throws Exception {
		int id;	double stageVal = 0;AggregateDiffState aggDiffState;
		
		aggDiffState = new AggregateDiffState(gs);
		FeatureList.clear();
		//id for workers-1,light-2,ranged-3,heavy-4,bases-5,barracks-6,resources-7
		//and values are -1,1 and 0 for behind, ahead and even
		id=1;
		aggrdiffAdd((AggregateDiff) aggDiffState.get(AggregateState.KEY_WORKERS),id);id++;
		aggrdiffAdd((AggregateDiff) aggDiffState.get(AggregateState.KEY_LIGHT),id);id++;
		aggrdiffAdd((AggregateDiff) aggDiffState.get(AggregateState.KEY_RANGED),id);id++;
		aggrdiffAdd((AggregateDiff) aggDiffState.get(AggregateState.KEY_HEAVY),id);id++;
		aggrdiffAdd((AggregateDiff) aggDiffState.get(AggregateState.KEY_BASES),id);id++;
		aggrdiffAdd((AggregateDiff) aggDiffState.get(AggregateState.KEY_BARRACKS),id);id++;
		aggrdiffAdd((AggregateDiff) aggDiffState.get(AggregateState.KEY_RESOURCES),id);id++;
		
		gameStage = new GameStage(gs);
		gameStages = gameStage.getStage();
		switch(gameStages){
			case OPENING: stageVal = 1;break;
			case EARLY: stageVal = 2;break;
			case MID: stageVal = 3;break;
			case LATE: stageVal = 4;break;
			case END: stageVal = 5;break;
			case FINISHED: stageVal = 6;break;
		}
		
		FeatureList.add(new StateFeature(id,stageVal));
//		System.out.println(id + " " + stageVal);
		return new ArrayList<StateFeature>(FeatureList);
	}
	public void aggrdiffAdd(AggregateDiff aggrdiff,int id){
		int val =1;
		switch(aggrdiff){
		case BEHIND: val = 1;break;
		case AHEAD: val = 3;break;
		case EVEN: val = 2;break;
		}
//		System.out.println(id + " " + val);
		FeatureList.add(new StateFeature(id,val));
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

    
    @Override
    public AI clone() {
        return new MetaBotAI(givenAIs, names, weightsclone,TIME_BUDGET, ITERATIONS_BUDGET, LOOKAHEAD, evaluation);
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


