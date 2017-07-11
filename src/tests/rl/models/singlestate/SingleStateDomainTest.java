package tests.rl.models.singlestate;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.jdom.JDOMException;
import org.junit.Before;
import org.junit.Test;

import burlap.mdp.core.state.State;
import rl.models.singlestate.SingleStateDomain;
import rl.models.stages.GameStage;
import rl.models.stages.GameStages;

public class SingleStateDomainTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testEnumerate() throws JDOMException, IOException {
		List<? extends State> allStates = new SingleStateDomain().enumerate();
		// the domain should have 1 state
		assertEquals(1, allStates.size());
		
		// the only state should be 'OPENING'
		@SuppressWarnings("unchecked")
		List<GameStage> theList = (List<GameStage>) allStates;
		assertEquals(GameStages.OPENING, theList.get(0).getStage());
	}

}
