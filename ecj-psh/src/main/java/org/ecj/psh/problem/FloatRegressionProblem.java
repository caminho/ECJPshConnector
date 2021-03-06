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

package org.ecj.psh.problem;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

import org.ecj.psh.PshEvolutionState;
import org.ecj.psh.PshIndividual;
import org.ecj.psh.PshProblem;
import org.spiderland.Psh.Interpreter;
import org.spiderland.Psh.Program;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.koza.KozaFitness;
import ec.util.Parameter;

/**
 * Simple symbolic regression problem for floating point numbers
 * 
 * @author Tomasz Kamiński
 * 
 */
public class FloatRegressionProblem extends PshProblem {
	
	public static final String P_TESTCASES = "test-cases";
	public static final String P_REPEATFLOATSTACK = "repeat-float-stack"; 

	/** How many times should input number be duplicated in float stack */
	public int repeatFloatStack;
	
	// symbolic regression test cases
	public ArrayList<Float[]> testCases;
	
	
	@Override
	public void setup(final EvolutionState state, final Parameter base) {
		super.setup(state, base);
		Parameter def = this.defaultBase();

		state.output.message(base.push(P_TESTCASES).toString());
		state.output.message(def.push(P_TESTCASES).toString());
		File testCasesFile = state.parameters.getFile(base.push(P_TESTCASES),
				def.push(P_TESTCASES));
		state.output.message(testCasesFile.toString());
		testCases = new ArrayList<Float[]>();
		FileReader reader = null;
		try {
			reader = new FileReader(testCasesFile);
			Scanner scanner = new Scanner(reader);
			scanner.useLocale(Locale.US);
			while (scanner.hasNextFloat()) {
				float input = scanner.nextFloat();
				float output = scanner.nextFloat();
				testCases.add(new Float[]{input,output});
			}
		} catch (IOException e) {
			state.output.fatal("Couldn't read test cases for float symbolic regression.");
		} finally {
			if (reader != null)	try {
					reader.close();
				} catch (IOException e) { 
			}
		}
		state.output.message("Test cases: ");
		for (Float[] testCase : testCases) {
			state.output.message("input = " + testCase[0] + ", output = "
					+ testCase[1]);
		}
		
		repeatFloatStack = state.parameters.getIntWithDefault(
				base.push(P_REPEATFLOATSTACK), def.push(P_REPEATFLOATSTACK), 1);
	}
	
	private float evaluateTestCase(Interpreter interpreter, Program program, float input, float output) {
		
		interpreter.ClearStacks();
		
		// pushing input value to float stack
		for (int i = 0; i < repeatFloatStack; i++) {
			interpreter.floatStack().push(input);
		}
		
		// setting input value to input stack
		interpreter.inputStack().push((Float)input);

		// executing the program
		interpreter.Execute(program,
				interpreter.getExecutionLimit());

		// Penalize individual if there is no result on the stack.
		if (interpreter.floatStack().size() == 0) {
			return 1000.0f;
		}

		// compute result as absolute difference
		float error = Math.abs(interpreter.floatStack().top() - output);
		
		return error;
	}
	
	@Override
	public void evaluate(EvolutionState state, Individual ind,
			int subpopulation, int threadnum) {

		if (ind.evaluated)
			return;

		if (!(ind instanceof PshIndividual)) {
			state.output.fatal("This is not PshIndividual instance!");
		}
		
		Interpreter interpreter = ((PshEvolutionState) state).interpreter[threadnum];
		Program program = ((PshIndividual) ind).program;
		
		float fitness = 0.0f;
		int hits = 0;
		
		for (Float[] testCase : testCases) {
			float input = testCase[0];
			float output = testCase[1];

			float error = evaluateTestCase(interpreter, program, input, output);
			
			if (error < 0.01)
				hits++;
			fitness += error;
		}
		if (Float.isInfinite(fitness)) {
			fitness = Float.MAX_VALUE;
		} else {
			// compute mean absolute error
			fitness = fitness / (float) testCases.size();
		}
		
		KozaFitness f = (KozaFitness) ind.fitness; 
		f.setStandardizedFitness(state, fitness);
		f.hits = hits;
		ind.evaluated = true;
	}

}
