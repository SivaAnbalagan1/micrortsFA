package tests.ailoader;

import static org.junit.Assert.*;

import org.junit.Test;

import ai.RandomAI;
import ai.metagame.MetaBot;
import ailoader.AILoader;
import rts.units.UnitTypeTable;

public class AILoaderTest {

	@Test
	public void testLoadAI() {
		UnitTypeTable utt = new UnitTypeTable();
		//test for RandomAI
		assertTrue(AILoader.loadAI("ai.RandomAI", utt) instanceof RandomAI);
		
		//test for MetaBot
		assertTrue(AILoader.loadAI("ai.metabot.MetaBot", utt) instanceof MetaBot);
	}

}
