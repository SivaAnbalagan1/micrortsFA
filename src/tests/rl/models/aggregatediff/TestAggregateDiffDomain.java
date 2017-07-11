package tests.rl.models.aggregatediff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import burlap.mdp.core.state.State;
import rl.models.aggregatediff.AggregateDiffState;
import rl.models.aggregatediff.AggregateDifferencesDomain;

public class TestAggregateDiffDomain {

	AggregateDifferencesDomain testedDomain;
	
	@Before
	public void setUp() throws Exception {
		testedDomain = new AggregateDifferencesDomain(
			"src/tests/rl/models/aggregatediff/basesWorkers24x24.xml"
		);
		
	}

	@Test
	public void testEnumerate() {
		List<? extends State> allStates = testedDomain.enumerate();
		// the domain should have 6*3^7 = 13122 states
		assertEquals(13122, allStates.size());
		
		/* 
		 * the following state was not being 'pre-allocated' in backward induction:
		 * END;AHEAD;AHEAD;EVEN;EVEN;AHEAD;AHEAD;EVEN
		 * Is it being enumerated?
		 */
		AggregateDiffState perhapsMissing = AggregateDiffState.fromString("END;AHEAD;AHEAD;EVEN;EVEN;AHEAD;AHEAD;EVEN");
		for(State s : allStates){
			if(s.equals(perhapsMissing)){
				return;
			}
		}
		fail("State 'END;AHEAD;AHEAD;EVEN;EVEN;AHEAD;AHEAD;EVEN' does not exist!");
		
	}

}
