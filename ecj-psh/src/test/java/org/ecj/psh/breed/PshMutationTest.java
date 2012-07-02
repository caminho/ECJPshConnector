package org.ecj.psh.breed;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import org.ecj.psh.PshEvaluator;
import org.ecj.psh.PshIndividual;
import org.junit.Before;
import org.junit.Test;
import org.spiderland.Psh.Interpreter;
import org.spiderland.Psh.Program;

import ec.EvolutionState;
import ec.util.MersenneTwisterFast;
import ec.util.Output;

public class PshMutationTest {

	protected MutationPipeline mutation;
	
	protected EvolutionState state;
	protected Output stateOutput;
	protected MersenneTwisterFast[] stateRandom;
	protected PshEvaluator stateEvaluator;
	protected Interpreter interpreter;
	protected int thread;
    
	
	@Before
	public void prepare() {
	
        // mock the evolution state because it is too expensive
        // to provide it properly configured
        state = mock(EvolutionState.class);
        stateOutput = mock(Output.class);
        stateRandom = new MersenneTwisterFast[1];
        stateRandom[0] = mock(MersenneTwisterFast.class);
        stateEvaluator = mock(PshEvaluator.class);
        interpreter = mock(Interpreter.class);
        thread = 0;
        
        // make state output fatal throw a new exception
        doThrow(IllegalStateException.class).when(stateOutput).fatal(anyString());

        // mocking the evolution state
        state.output = stateOutput;
        state.random = stateRandom;
        state.evaluator = stateEvaluator;
                
        stateEvaluator.interpreter = new Interpreter[1];
        stateEvaluator.interpreter[0] = interpreter;
        
        mutation = new MutationPipeline();
                
        // safe limit for program length
     	when(interpreter.getMaxPointsInProgram()).thenReturn(50);
     	// max. random code size
     	when(interpreter.getMaxRandomCodeSize()).thenReturn(20);
        
	}
	
	@Test
	public void do_mutation_test() throws Exception {
		PshIndividual parent = new PshIndividual();
		parent.program = new Program("( 1 2 3 integer.dup integer.+ integer.* )");
		
		mutation.useFairMutation = false;
		
		when(stateRandom[0].nextInt(anyInt())).thenReturn( 3, 1 );
		when(interpreter.RandomAtom()).thenReturn((Integer)123);
		
		mutation.mutate(parent, state, thread);
		
		assertEquals(new Program("( 1 2 3 123 integer.+ integer.* )"), parent.program);
		
	}
	
}
