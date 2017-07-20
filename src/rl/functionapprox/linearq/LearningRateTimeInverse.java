package rl.functionapprox.linearq;

/*
 * Copy of burlap.behavior.learningrate.SoftTimeInverseDecayLR.
 * Fixed few errors that couldn't be fixed using extends.
 */
import burlap.statehashing.HashableStateFactory;
import burlap.behavior.learningrate.LearningRate;
import burlap.mdp.core.action.Action;
import burlap.mdp.core.state.State;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;
import java.util.HashMap;
import java.util.Map;

public class LearningRateTimeInverse implements LearningRate {
	protected double initialLearningRate;
	protected double decayConstantShift;
	protected double minimumLR = 2.225073858507201E-308D;

	protected int universalTime = 1;
	protected Map<HashableState, StateWiseTimeIndex> stateWiseMap;
	protected Map<Integer, StateWiseTimeIndex> featureWiseMap;
	protected boolean useStateWise = false;

	protected boolean useStateActionWise = false;
	protected HashableStateFactory hashingFactory;
	protected int lastPollTime = -1;

	public LearningRateTimeInverse(double initialLearningRate, double decayConstantShift) {
		this.initialLearningRate = initialLearningRate;
		this.decayConstantShift = decayConstantShift;
	}

	public LearningRateTimeInverse(double initialLearningRate, double decayConstantShift, double minimumLearningRate) {
		this.initialLearningRate = initialLearningRate;
		this.decayConstantShift = decayConstantShift;
		this.minimumLR = minimumLearningRate;
	}

	public LearningRateTimeInverse(double initialLearningRate, double decayConstantShift,
			HashableStateFactory hashingFactory, boolean useSeparateLRPerStateAction) {
		this.initialLearningRate = initialLearningRate;
		this.decayConstantShift = decayConstantShift;

		this.useStateWise = true;
		this.useStateActionWise = useSeparateLRPerStateAction;
		this.hashingFactory = hashingFactory;
		this.stateWiseMap = new HashMap();
		this.featureWiseMap = new HashMap();
	}

	public LearningRateTimeInverse(double initialLearningRate, double decayConstantShift, double minimumLearningRate,
			HashableStateFactory hashingFactory, boolean useSeparateLRPerStateAction) {
		this.initialLearningRate = initialLearningRate;
		this.decayConstantShift = decayConstantShift;
		this.minimumLR = minimumLearningRate;

		this.useStateWise = !useSeparateLRPerStateAction;
		this.useStateActionWise = useSeparateLRPerStateAction;
		this.hashingFactory = hashingFactory;
		this.stateWiseMap = new HashMap();
		this.featureWiseMap = new HashMap();
	}

	public double peekAtLearningRate(State s, Action ga) {
		if (!(this.useStateWise)) {
			return learningRate(this.universalTime);
		}

		StateWiseTimeIndex slr = getStateWiseTimeIndex(s);
		if (!(this.useStateActionWise)) {
			return learningRate(slr.timeIndex);
		}

		return learningRate(slr.getActionTimeIndexEntry(ga).mi);
	}

	public double pollLearningRate(int agentTime, State s, Action ga) {
		if ((this.useStateWise)) {
			double oldVal = learningRate(this.universalTime);
			if (agentTime > this.lastPollTime) {
				this.universalTime += 1;
				this.lastPollTime = agentTime;
			}
			return oldVal;
		}

		StateWiseTimeIndex slr = getStateWiseTimeIndex(s);
		if (!(this.useStateActionWise)) {
			double oldVal = learningRate(slr.timeIndex);
			if (agentTime > slr.lastPollTime) {
				slr.timeIndex += 1;
				slr.lastPollTime = agentTime;
			}
			return oldVal;
		}

		MutableInt md = slr.getActionTimeIndexEntry(ga);
		double oldVal = learningRate(md.mi);
		if (agentTime > md.lastPollTime) {
			md.mi += 1;
			slr.actionLearningRates.put(ga.actionName(), md);
			md.lastPollTime = agentTime;
		}
		return oldVal;
	}

	public double peekAtLearningRate(int featureId) {
		if (!(this.useStateWise)) {
			return learningRate(this.universalTime);
		}

		StateWiseTimeIndex slr = getFeatureWiseTimeIndex(featureId);

		return learningRate(slr.timeIndex);
	}

	public double pollLearningRate(int agentTime, int featureId) {
		if (!(this.useStateWise)) {
			double oldVal = learningRate(this.universalTime);
			if (agentTime > this.lastPollTime) {
				this.universalTime += 1;
				this.lastPollTime = agentTime;
			}
			return oldVal;
		}

		StateWiseTimeIndex slr = getFeatureWiseTimeIndex(featureId);

		double oldVal = learningRate(slr.timeIndex);
		if (agentTime > slr.lastPollTime) {
			slr.timeIndex += 1;
			slr.lastPollTime = agentTime;
		}
		return oldVal;
	}

	public void resetDecay() {
		this.universalTime = 1;
		this.stateWiseMap.clear();
		this.featureWiseMap.clear();
	}

	protected double learningRate(int time) {
		double r;
		if (time == 0) {
			r = this.initialLearningRate;
		} else {
			r = this.initialLearningRate * (this.decayConstantShift + 1.0D) / (this.decayConstantShift + time);
		}
		double r1 = Math.max(r, this.minimumLR);
		return r1;
	}

	protected StateWiseTimeIndex getStateWiseTimeIndex(State s) {
		HashableState sh = this.hashingFactory.hashState(s);
		StateWiseTimeIndex slr = (StateWiseTimeIndex) this.stateWiseMap.get(sh);
		if (slr == null) {
			slr = new StateWiseTimeIndex();
			this.stateWiseMap.put(sh, slr);
		}
		return slr;
	}

	protected StateWiseTimeIndex getFeatureWiseTimeIndex(int featureId) {
		StateWiseTimeIndex slr = (StateWiseTimeIndex) this.featureWiseMap.get(Integer.valueOf(featureId));
		if (slr == null) {
			slr = new StateWiseTimeIndex();
			this.featureWiseMap.put(Integer.valueOf(featureId), slr);
		}
		return slr;
	}

	protected class MutableInt {
		int mi;
		int lastPollTime = -1;

		public MutableInt(int mi) {
			this.mi = mi;
		}
	}

	protected class StateWiseTimeIndex {
		int timeIndex;
		Map<String, LearningRateTimeInverse.MutableInt> actionLearningRates = null;
		int lastPollTime = -1;

		public StateWiseTimeIndex() {
			this.timeIndex = 1;
			if (LearningRateTimeInverse.this.useStateActionWise)
				this.actionLearningRates = new HashMap();
		}

		public LearningRateTimeInverse.MutableInt getActionTimeIndexEntry(Action ga) {
			LearningRateTimeInverse.MutableInt entry = 
					(LearningRateTimeInverse.MutableInt) this.actionLearningRates
					.get(ga.actionName());
			if (entry == null) {
				entry = new LearningRateTimeInverse.MutableInt(1);
				this.actionLearningRates.put(ga.actionName(), entry);
			}
			return entry;
		}
	}
}