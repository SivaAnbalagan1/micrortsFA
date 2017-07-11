package tests.rl.validate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rl.validate.CompareEpisodes;

public class CompareEpisodesTest {
	
	CompareEpisodes comparator;
	PrintStream out;

	@Before
	public void setUp() throws Exception {
		out = new PrintStream(new File("/dev/null"));
		//out = new PrintStream(System.out);
		comparator = new CompareEpisodes(out);
	}
	
	@After
	public void tearDown(){
		out.close();
	}

	@Test
	public void testCompare() {
		String dir = "src/tests/rl/validate/";
		//equal_0 and equal_1 are the same
		//notequal_0 has different joint rewards (compared to equal_*)
		//notequal_1 has different joint actions (compared to equal_*)
		//notequal_2 has different states (compared to equal_*)
		
		assertTrue(comparator.compare(dir + "equal_0.game", dir + "equal_1.game"));
		assertFalse(comparator.compare(dir + "equal_0.game", dir + "notequal_0.game"));
		assertFalse(comparator.compare(dir + "equal_0.game", dir + "notequal_1.game"));
		assertFalse(comparator.compare(dir + "equal_0.game", dir + "notequal_2.game"));
	}

	@Test
	public void testListCompare() {
		List<Integer> list1 = new ArrayList<>();
		List<Integer> list2 = new ArrayList<>();
		List<Integer> list3 = new ArrayList<>();

		// lists 1 and 2 have same elements, in reverse order
		list1.add(1);
		list1.add(2);
		
		list2.add(2);
		list2.add(1);
		
		// asserts that lists 1 and 2 are different
		assertNotEquals(list1, list2);
		
		// asserts listCompare does the same
		assertFalse(comparator.listCompare(list1, list2, "int"));
		
		// now populates a 3rd list which equals the first
		list3.add(1);
		list3.add(2);
		
		// asserts lists 1 and 3 are equal
		assertEquals(list1, list3);
		
		// asserts listCompare does the same
		assertTrue(comparator.listCompare(list1, list3, "int"));
		
		// adds a new item to list 3 and tests equality
		list3.add(3);
		assertFalse(comparator.listCompare(list1, list3, "int"));
	}
	
	@Test
	public void testListCompareWithListOfArrays(){
		List<double[]> first = new ArrayList<>();
		List<double[]> second = new ArrayList<>();
		List<double[]> third = new ArrayList<>();
		
		//first and second will be the same, third will be different
		first.add(new double[]{0.1, 0.2});
		second.add(new double[]{0.1, 0.2});
		third.add(new double[]{0.3, 0.1});
		
		assertTrue(comparator.listCompare(first, second, "double"));
		
	}

}
