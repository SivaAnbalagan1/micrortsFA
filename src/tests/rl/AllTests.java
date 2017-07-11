package tests.rl;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import tests.rl.adapters.learners.PersistentMultiAgentQLearningTest;
import tests.rl.adapters.learners.SGQLearningAdapterTest;
import tests.rl.models.aggregate.AggregateJAMTest;
import tests.rl.models.aggregatediff.AggregateDiffStateJAMTest;
import tests.rl.models.aggregatediff.TestAggregateDiffDomain;
import tests.rl.models.aggregatediff.TestAggregateDiffState;
import tests.rl.models.common.TestSimpleWeightedFeatures;
import tests.rl.models.common.TestSimpleWeightedFeaturesTerminal;
import tests.rl.models.singleagent.SingleAgentDomainTest;
import tests.rl.models.singlestate.SingleStateJAMTest;
import tests.rl.models.singlestate.SingleStateTest;
import tests.rl.models.stages.GameStageTest;
import tests.rl.models.stages.StagesJAMTest;
import tests.rl.planners.TestBackwardInduction;
import tests.rl.validate.CompareEpisodesTest;

@RunWith(Suite.class)
@SuiteClasses({
	// misc
	RLParametersTest.class, 
	SGQLearningAdapterTest.class,
	PersistentMultiAgentQLearningTest.class,
	TestBackwardInduction.class,
	CompareEpisodesTest.class,
	
	// joint action models
	StagesJAMTest.class,
	AggregateJAMTest.class,
	AggregateDiffStateJAMTest.class,
	SingleStateJAMTest.class,
	
	// states
	SingleStateTest.class,
	GameStageTest.class,
	TestAggregateDiffState.class,
	
	// domains
	TestAggregateDiffDomain.class,
	SingleAgentDomainTest.class,
	
	//reward functions
	TestSimpleWeightedFeatures.class,
	TestSimpleWeightedFeaturesTerminal.class,
	
})

public class AllTests {

}
