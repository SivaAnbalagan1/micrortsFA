package rl.adapters.gamenatives;

import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction2;
import ai.evaluation.SimpleSqrtEvaluationFunction3;

/**
 * Please don't confuse with Factory design pattern
 * This class just 'converts' a String to a {@link EvaluationFunction}
 * @author anderson
 *
 */
public class EvaluationFunctionFactory {
	public static EvaluationFunction fromString(String functionName){
		if(functionName.equalsIgnoreCase(SimpleSqrtEvaluationFunction3.class.getSimpleName())){
			return new SimpleSqrtEvaluationFunction3();
		}
		if(functionName.equalsIgnoreCase(SimpleSqrtEvaluationFunction2.class.getSimpleName())){
			return new SimpleSqrtEvaluationFunction2();
		}
		if(functionName.equalsIgnoreCase(SimpleSqrtEvaluationFunction.class.getSimpleName())){
			return new SimpleSqrtEvaluationFunction();
		}
		throw new RuntimeException("Unrecognized evaluation function: " + functionName);
	}
}
