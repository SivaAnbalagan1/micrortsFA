package rl.functionapprox.linearq;

import burlap.behavior.learningrate.ExponentialDecayLR;
import burlap.statehashing.HashableStateFactory;

public class LearningRateExpDecay extends ExponentialDecayLR {
    
	public LearningRateExpDecay(double initialLearningRate, double decayRate) {
		super(initialLearningRate, decayRate);
		// TODO Auto-generated constructor stub
	}

	public LearningRateExpDecay(double initialLearningRate, double decayRate, double minimumLearningRate) {
		super(initialLearningRate, decayRate, minimumLearningRate);
		// TODO Auto-generated constructor stub
	}

	public LearningRateExpDecay(double initialLearningRate, double decayRate, HashableStateFactory hashingFactory,
			boolean useSeparateLRPerStateAction) {
		super(initialLearningRate, decayRate, hashingFactory, useSeparateLRPerStateAction);
		// TODO Auto-generated constructor stub
	}

	public LearningRateExpDecay(double initialLearningRate, double decayRate, double minimumLearningRate,
			HashableStateFactory hashingFactory, boolean useSeparateLRPerStateAction) {
		super(initialLearningRate, decayRate, minimumLearningRate, hashingFactory, useSeparateLRPerStateAction);
		// TODO Auto-generated constructor stub
	}
	@Override
	public double nextLRVal(double cur) {
		return Math.max(cur * super.decayRate, super.minimumLR);
	}
	public double getinitialRate(){
		return super.initialLearningRate;
	}
}
