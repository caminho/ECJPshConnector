/*
* Copyright 2012 Tomasz Kamiński
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ecj.psh.breed;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import org.ecj.psh.PshIndividual;
import org.junit.Test;

public class PshCrossoverTest extends CrossoverTest {

	@Override
	public void prepare() {
		super.prepare();
		
		// setting up operator
		crossover = new PshCrossover();
		crossover.parents = new PshIndividual[2];
		crossover.parents[0] = new PshIndividual();
		crossover.parents[1] = new PshIndividual();
		// safe limit for program length
		when(interpreter.getMaxPointsInProgram()).thenReturn(50);
	}
		
	@Test
	public void _PSHX_test_too_short_programs() throws Exception {
		System.out.println("*** PSHX too short programs:");
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
	public void _PSHX_some_normal_cases() throws Exception {
		System.out.println("*** PSHX some normal cases:");
		// cutpoints: 1, 1
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 0, 2 );
		do_crossover_test(
			new String[]{ "( A1 )", "( B1 B2 B3 B4 )" },
			new String[]{ "( B3 )", "( B1 B2 A1 B4 )" },
			true
		);
		
		// cutpoints: 1, 1
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 0, 0 );
		do_crossover_test(
			new String[]{ "( A1 A2 A3 A4 )", "( B1 B2 B3 B4 )" },
			new String[]{ "( B1 A2 A3 A4 )", "( A1 B2 B3 B4 )" },
			true
		);
		// cutpoints: 3, 3
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 3, 3 );
		do_crossover_test(
			new String[]{ "( A1 A2 A3 (A4 A5) )", "( B1 B2 B3 B4 )" },
			new String[]{ "( A1 A2 A3 B4 )", "( B1 B2 B3 (A4 A5) )" },
			true
		);
		// cutpoints: 1, 3
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 1, 3 );
		do_crossover_test(
			new String[]{ "( A1 A2 A3 (A4 A5))", "( B1 B2 B3 B4 )" },
			new String[]{ "( A1 B4 A3 (A4 A5))", "( B1 B2 B3 A2 )" },
			true
		);
	}
	

	@Test
	public void _PSHX_homologous() throws Exception {
		System.out.println("*** PSHX homologous:");
		
		this.crossover.homologous = true;
		
		// cutpoints: 0
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 0 );
		do_crossover_test(
			new String[]{ "( A1 )", "( B1 B2 B3 B4 )" },
			new String[]{ "( B1 )", "( A1 B2 B3 B4 )" },
			true
		);
		
		// cutpoints: 1
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 1 );
		do_crossover_test(
			new String[]{ "( A1 A2 A3 A4 )", "( B1 B2 B3 B4 )" },
			new String[]{ "( A1 B2 A3 A4 )", "( B1 A2 B3 B4 )" },
			true
		);
		// cutpoints: 3
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 3 );
		do_crossover_test(
			new String[]{ "( A1 A2 A3 (A4 A5) )", "( B1 B2 B3 B4 )" },
			new String[]{ "( A1 A2 A3 B4 )", "( B1 B2 B3 (A4 A5) )" },
			true
		);
	}
	
	
	@Test
	public void _PSHX_test_swapping_nested_subtrees() throws Exception {
		System.out.println("*** PSHX test swapping nested subtrees:");
		this.crossover.homologous = false;

		// cutpoints: 5, 7
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 5, 7 );
		do_crossover_test(
			new String[]{ "( A1 (A2 A3 (A4 A5) ))", "( B1 (B2 (2 3) B3 ) B4 )" },
			new String[]{ "( A1 (A2 A3 (3 A5) ))", "( B1 (B2 (2 A4) B3 ) B4 )" },
			true
		);
	}

}
