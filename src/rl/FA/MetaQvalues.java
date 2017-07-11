package rl.FA;

import java.util.ArrayList;
import java.util.List;

import burlap.behavior.functionapproximation.sparse.LinearVFA;
import burlap.behavior.functionapproximation.sparse.StateFeature;
import burlap.behavior.valuefunction.QProvider;
import burlap.behavior.valuefunction.QValue;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;

public class MetaQvalues implements QProvider {
    LinearVFAExtend LVFA;
    List<StateFeature> actionFeature;
    List<QValue> Qval,returnQval,storeQval;
    List<MetaAction> metaActions;
    public MetaQvalues() {
		// TODO Auto-generated constructor stub
	}
	public MetaQvalues(LinearVFAExtend LVFA,List<MetaAction> metaActions) {
        this.LVFA = LVFA;this.metaActions = metaActions;
        Qval = new ArrayList<QValue>();
	}

	@Override
	public double qValue(State s, Action a) {
		//return LVFA.evaluate(s, a);
		double q=0;
		for(QValue ql: storeQval){if(ql.s == s && ql.a ==a) q= ql.q;}
      //  System.out.println("q-val" + q);
		return q;
	}
	
	@Override
	public double value(State s) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<QValue> qValues(State s) {
		double val; 
		QValue qtemp;
		for(MetaAction ma : metaActions){
			val = LVFA.evaluate(s,ma);
			qtemp = new QValue(s, ma,val);
			Qval.add(qtemp);
		}
		returnQval = new ArrayList<>(Qval);storeQval = new ArrayList<>(Qval);
		Qval.clear();
		return returnQval;
	}

}
