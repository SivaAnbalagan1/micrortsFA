package tests.rl.models.aggregate;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import rl.models.aggregate.AggregateStateDomain;

public class TestAggregateDomain {

	AggregateStateDomain testedDomain;
	
	@Before
	public void setUp() throws Exception {
		testedDomain = new AggregateStateDomain("src/tests/rl/models/aggregate/basesWorkers24x24.xml");
		
	}

	@Test
	public void testEnumerate() {
		// the domain should have 28697814 states -- OutOfMemoryError on the way :(
		assertEquals(28697814, testedDomain.enumerate().size());
	}

}
