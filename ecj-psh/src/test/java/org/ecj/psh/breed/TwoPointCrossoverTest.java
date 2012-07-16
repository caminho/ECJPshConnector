package org.ecj.psh.breed;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import org.ecj.psh.PshIndividual;
import org.junit.Test;

public class TwoPointCrossoverTest extends CrossoverTest {

	@Override
	public void prepare() {
		super.prepare();
		
		// setting up operator
		crossover = new TwoPointCrossover();
		crossover.parents = new PshIndividual[2];
		crossover.parents[0] = new PshIndividual();
		crossover.parents[1] = new PshIndividual();
		// ten tries before giving up (but it'll never be reached)
		crossover.tries = 10;
		// safe limit for program length
		when(interpreter.getMaxPointsInProgram()).thenReturn(50);
	}
		
	@Test
	public void _2PX_test_too_short_programs() throws Exception {
		crossover.homologous = false;		

		System.out.println("*** 2PX too short programs:");
		do_crossover_test(
			new String[]{ "(  )", "( B1 B2 )" },
			new String[]{ "(  )", "( B1 B2 )" },
			true
		);
		do_crossover_test(
			new String[]{ "( A1 A2 )", "(  )" },
			new String[]{ "( A1 A2)", "(  )" },
			true
		);
	}
	
	@Test
	public void _2PX_test_cutpoints_borderline_cases() throws Exception {
		crossover.homologous = false;		

		System.out.println("*** 2PX cutpoints borderline cases:");
				

		// cutpoints: (0, 0), (1, 1)
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 0, 0, 1, 1 );
		do_crossover_test(
			new String[]{ "( A1 )", "( B1 B2 )" },
			new String[]{ "( B2 )", "( B1 A1 )" },
			true
		);
		
		// cutpoints: (0, 0), (1, 1)
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 0, 0, 1, 1 );
		do_crossover_test(
			new String[]{ "( A1 A1 )", "( B1 B2 )" },
			new String[]{ "( B2 A1 )", "( B1 A1 )" },
			true
		);
		
		// cutpoints: (0, 0), (1, 1)
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 0, 0, 1, 1 );
		do_crossover_test(
			new String[]{ "( A1 A2 A3 A4 A5 )", "( B1 B2 B3 B4 B5 )" },
			new String[]{ "( B2 A2 A3 A4 A5 )", "( B1 A1 B3 B4 B5 )" },
			true
		);
		
		// cutpoints: (0, 4), (0, 4)
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 0, 4, 0, 4 );
		do_crossover_test(
			new String[]{ "( A1 A2 A3 A4 A5 )", "( B1 B2 B3 B4 B5 )" },
			new String[]{ "( B1 B2 B3 B4 B5 )", "( A1 A2 A3 A4 A5 )" },
			true
		);
		
		// cutpoints: (0, 0), (0, 4)
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 0, 0, 4, 0 );	// should swap 0 and 4
		do_crossover_test(
			new String[]{ "( A1 A2 A3 A4 A5 )", "( B1 B2 B3 B4 B5 )" },
			new String[]{ "( B1 B2 B3 B4 B5 A2 A3 A4 A5 )", "( A1 )" },
			true
		);
	}
	
	@Test
	public void _2PX_test_some_normal_cases() throws Exception {
		crossover.homologous = false;		

		System.out.println("*** 2PX normal cases:");
		// cutpoints: (0, 1), (3, 4)
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 0, 1, 3, 4 );
		do_crossover_test(
			new String[]{ "( A1 A2 A3 A4 A5 )", "( B1 B2 B3 B4 B5 )" },
			new String[]{ "( B4 B5 A3 A4 A5 )", "( B1 B2 B3 A1 A2 )" },
			true
		);
	}
	
	
	@Test
	public void _2PXh_test_homologous_cutting_points() throws Exception {
		crossover.homologous = true;		

		System.out.println("*** 2PXh normal cases:");
		// cutpoints: (0, 3)
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 0, 3 );
		do_crossover_test(
			new String[]{ "( A1 A2 A3 A4 A5 )", "( B1 B2 B3 B4 B5 )" },
			new String[]{ "( B1 B2 B3 B4 A5 )", "( A1 A2 A3 A4 B5 )" },
			true
		);
		// cutpoints: (1, 2)
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 1, 2 );
		do_crossover_test(
			new String[]{ "( A1 A2 A3 A4 A5 )", "( B1 B2 B3 B4 B5 )" },
			new String[]{ "( A1 B2 B3 A4 A5 )", "( B1 A2 A3 B4 B5 )" },
			true
		);
	}
	
	@Test
	public void _2PX_test_limited_tries() throws Exception {
		crossover.homologous = false;
		when(interpreter.getMaxPointsInProgram()).thenReturn(5);
		crossover.tries = 1;
		
		System.out.println("*** 2PX limited tries cases:");
		// cutpoints: (0, 1), (0, 1) - should swap
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 0, 1, 0, 1 );
		do_crossover_test(
			new String[]{ "( A1 A2 A3 A4 A5 )", "( B1 B2 B3 B4 B5 )" },
			new String[]{ "( B1 B2 A3 A4 A5 )", "( A1 A2 B3 B4 B5 )" },
			true
		);
		// cutpoints: (0, 0), (0, 1) - should give up after one attemption
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 0, 0, 0, 1 );
		do_crossover_test(
			new String[]{ "( A1 A2 A3 A4 A5 )", "( B1 B2 B3 B4 B5 )" },
			new String[]{ "( A1 A2 A3 A4 A5)", "( B1 B2 B3 B4 B5 )" },
			true
		);
		
		crossover.tries = 2;
		// cutpoints: (0, 0), (0, 1) - should do crossover in second attemption
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 
				0, 0, 0, 1, // first attemption 
				0, 0  // second attemption  
		);
		do_crossover_test(
			new String[]{ "( A1 A2 A3 A4 A5 )", "( B1 B2 B3 B4 B5 )" },
			new String[]{ "( B1 A2 A3 A4 A5 )", "( A1 B2 B3 B4 B5 )" },
			true
		);
		
		crossover.tries = 2;
		// cutpoints: (0, 0), (0, 1) - should do crossover in second attemption
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 
				0, 2, 0, 0, // first attemption 
				0, 0, 0, 0  // second attemption  
		);
		do_crossover_test(
			new String[]{ "( A1 A2 A3 A4 A5 )", "( B1 B2 B3 B4 B5 )" },
			new String[]{ "( B1 A2 A3 A4 A5 )", "( A1 B2 B3 B4 B5 )" },
			true
		);
		
		crossover.tries = 2;
		// cutpoints: (0, 0), (0, 1) - should give up after 2 tries
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 
				0, 2, 0, 0, // first attemption 
				0, 2, 0, 0  // second attemption  
		);
		do_crossover_test(
			new String[]{ "( A1 A2 A3 A4 A5 )", "( B1 B2 B3 B4 B5 )" },
			new String[]{ "( A1 A2 A3 A4 A5 )", "( B1 B2 B3 B4 B5 )" },
			true
		);
		
		crossover.tries = 1;
		// cutpoints: (0, 0), (0, 1) - should in first attemption
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 
				0, 2, 0, 0 // first attemption 
		);
		do_crossover_test(
			new String[]{ "( A1 A2 A3 A4 A5 )", "( B1 B2 B3 B4 B5 )" },
			new String[]{ "( B1 A4 A5 )", "( B1 B2 B3 B4 B5 )" },
			false 			// we don't bother with second parent (thus we pass)
		);
	}
	
}