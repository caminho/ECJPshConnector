/*
 * Copyright 2009-2010 Jon Klein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.spiderland.Psh;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ecj.psh.PshDefaults;

import ec.EvolutionState;
import ec.Prototype;
import ec.util.MersenneTwisterFast;
import ec.util.Parameter;

/**
 * The Push language interpreter.
 */

public class Interpreter implements Prototype {
	private static final long serialVersionUID = 1L;

	public static final String P_INTERPRETER = "interpreter";

	public static final String P_INSTRUCTIONLIST = "instruction-list";

	public static final String P_MAXRANDCODESIZE = "max-random-code-size";
	public static final String P_EXECUTIONLIMIT = "execution-limit";
	public static final String P_MAXPOINTSINPROG = "max-points-in-program";

	public static final String P_USEFRAMES = "push-frame-mode";

	public static final String P_MAXRANDINT = "max-random-integer";
	public static final String P_MINRANDINT = "min-random-integer";
	public static final String P_RANDINTRES = "random-integer-res";

	public static final String P_MAXRANDFLOAT = "max-random-float";
	public static final String P_MINRANDFLOAT = "min-random-float";
	public static final String P_RANDFLOATRES = "random-float-res";

	public static final String P_GENERATEFLAT = "generate-flat";

	public enum StackType {
		INT_STACK, FLOAT_STACK, BOOL_STACK, CODE_STACK, NAME_STACK, EXEC_STACK, INPUT_STACK
	}
	
	// Random code generator
	protected MersenneTwisterFast _RNG;

	protected HashMap<String, Instruction> _instructions = new HashMap<String, Instruction>();

	// References to stack instructions. This is needed in order to update stack references
	// in these instructions
	protected Map<StackType, List<Instruction>> _stackInstructions = new HashMap<StackType, List<Instruction>>();
	
	// All generators
	protected HashMap<String, AtomGenerator> _generators = new HashMap<String, AtomGenerator>();
	protected ArrayList<AtomGenerator> _randomGenerators = new ArrayList<AtomGenerator>();

	// Create the stacks.
	protected intStack _intStack;
	protected floatStack _floatStack;
	protected booleanStack _boolStack;
	protected ObjectStack _codeStack;
	protected ObjectStack _nameStack;
	protected ObjectStack _execStack = new ObjectStack();

	protected ObjectStack _inputStack = new ObjectStack();

	// This arraylist will hold all custom stacks that can be created by the
	// problem classes
	protected ArrayList<Stack> _customStacks = new ArrayList<Stack>();

	/*
	 * Since the _inputStack will not change after initialization, it will not
	 * need a frame stack.
	 */
	protected ObjectStack _intFrameStack = new ObjectStack();
	protected ObjectStack _floatFrameStack = new ObjectStack();
	protected ObjectStack _boolFrameStack = new ObjectStack();
	protected ObjectStack _codeFrameStack = new ObjectStack();
	protected ObjectStack _nameFrameStack = new ObjectStack();

	protected int _totalStepsTaken;
	protected long _evaluationExecutions = 0;

	protected int _maxRandomCodeSize;
	protected int _executionLimit;
	protected int _maxPointsInProgram;

	protected boolean _useFrames;

	protected int _maxRandomInt;
	protected int _minRandomInt;
	protected int _randomIntResolution;

	protected float _maxRandomFloat;
	protected float _minRandomFloat;
	protected float _randomFloatResolution;

	protected boolean _generateFlatPrograms;

	protected InputPusher _inputPusher = new InputPusher();

	public void setRNG(MersenneTwisterFast _RNG) {
		this._RNG = _RNG;
	}

	public Interpreter() {
	}

	@Override
	public Parameter defaultBase() {
		return PshDefaults.base().push(P_INTERPRETER);
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		} // never happens
	}

	@Override
	public void setup(EvolutionState state, Parameter base) {

		Parameter def = defaultBase();

		// max. random code size, default 30
		setMaxRandomCodeSize(state.parameters.getIntWithDefault(
				base.push(P_MAXRANDCODESIZE), def.push(P_MAXRANDCODESIZE), 30));
		// execution limit for Push programs
		setExecutionLimit(state.parameters.getIntWithDefault(
				base.push(P_EXECUTIONLIMIT), def.push(P_EXECUTIONLIMIT), 100));
		// max number of points in program
		setMaxPointsInProgram(state.parameters.getIntWithDefault(
				base.push(P_MAXPOINTSINPROG), def.push(P_MAXPOINTSINPROG), 100));

		// maximum random integer
		_maxRandomInt = state.parameters.getIntWithDefault(
				base.push(P_MAXRANDINT), def.push(P_MAXRANDINT), 10);
		// minimum random integer
		_minRandomInt = state.parameters.getIntWithDefault(
				base.push(P_MINRANDINT), def.push(P_MINRANDINT), -10);
		// random integer resolution
		_randomIntResolution = state.parameters.getIntWithDefault(
				base.push(P_RANDINTRES), def.push(P_RANDINTRES), 1);

		// maximum random float
		_maxRandomFloat = state.parameters.getFloatWithDefault(
				base.push(P_MAXRANDFLOAT), def.push(P_MAXRANDFLOAT), 10.0);
		// minimum random float
		_minRandomFloat = state.parameters.getFloatWithDefault(
				base.push(P_MINRANDFLOAT), def.push(P_MINRANDFLOAT), -10.0);
		// random integer float
		_randomFloatResolution = state.parameters.getFloatWithDefault(
				base.push(P_RANDFLOATRES), def.push(P_RANDFLOATRES), 0.01);

		// should we use push frame mode
		_useFrames = state.parameters.getBoolean(base.push(P_USEFRAMES),
				def.push(P_USEFRAMES), false);

		// should we generate flat programs (without parentheses)
		_generateFlatPrograms = state.parameters.getBoolean(
				base.push(P_GENERATEFLAT), def.push(P_GENERATEFLAT), false);

		File instructionListFile = state.parameters.getFile(
				base.push(P_INSTRUCTIONLIST), def.push(P_INSTRUCTIONLIST));
		StringBuilder sb = new StringBuilder();

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(instructionListFile.getAbsolutePath())));
			try {
				String line;
				while ((line = br.readLine()) != null) {
					if (sb.length() > 0)
						sb.append(" ").append(line);
					else
						sb.append("(").append(line);
				}
				sb.append(")");
			} finally {
				br.close();
			}
		} catch (IOException e) {
			state.output.fatal("Can't read instruction list from file "
					+ instructionListFile);
		}
		try {
			Program instructionList = new Program(sb.toString());
			state.output.message("Instruction list for PushGP: "
					+ instructionList);
			SetInstructions(instructionList);
		} catch (Exception e) {
			state.output.fatal("Can't set instruction list");
		}
	}

	public void Initialize(MersenneTwisterFast _RNG) {

		if (_RNG == null) {
			throw new InternalError();
		}

		this._RNG = _RNG;

		_useFrames = false;
		PushStacks();

		DefineInstruction("integer.+", new IntegerAdd());
		DefineInstruction("integer.-", new IntegerSub());
		DefineInstruction("integer./", new IntegerDiv());
		DefineInstruction("integer.%", new IntegerMod());
		DefineInstruction("integer.*", new IntegerMul());
		DefineInstruction("integer.pow", new IntegerPow());
		DefineInstruction("integer.log", new IntegerLog());
		DefineInstruction("integer.=", new IntegerEquals());
		DefineInstruction("integer.>", new IntegerGreaterThan());
		DefineInstruction("integer.<", new IntegerLessThan());
		DefineInstruction("integer.min", new IntegerMin());
		DefineInstruction("integer.max", new IntegerMax());
		DefineInstruction("integer.abs", new IntegerAbs());
		DefineInstruction("integer.neg", new IntegerNeg());
		DefineInstruction("integer.ln", new IntegerLn());
		DefineInstruction("integer.fromfloat", new IntegerFromFloat());
		DefineInstruction("integer.fromboolean", new IntegerFromBoolean());
		DefineInstruction("integer.rand", new IntegerRand(this._RNG));

		DefineInstruction("float.+", new FloatAdd());
		DefineInstruction("float.-", new FloatSub());
		DefineInstruction("float./", new FloatDiv());
		DefineInstruction("float.%", new FloatMod());
		DefineInstruction("float.*", new FloatMul());
		DefineInstruction("float.pow", new FloatPow());
		DefineInstruction("float.log", new FloatLog());
		DefineInstruction("float.=", new FloatEquals());
		DefineInstruction("float.>", new FloatGreaterThan());
		DefineInstruction("float.<", new FloatLessThan());
		DefineInstruction("float.min", new FloatMin());
		DefineInstruction("float.max", new FloatMax());
		DefineInstruction("float.sin", new FloatSin());
		DefineInstruction("float.cos", new FloatCos());
		DefineInstruction("float.tan", new FloatTan());
		DefineInstruction("float.exp", new FloatExp());
		DefineInstruction("float.abs", new FloatAbs());
		DefineInstruction("float.neg", new FloatNeg());
		DefineInstruction("float.ln", new FloatLn());
		DefineInstruction("float.frominteger", new FloatFromInteger());
		DefineInstruction("float.fromboolean", new FloatFromBoolean());
		DefineInstruction("float.rand", new FloatRand(this._RNG));

		DefineInstruction("boolean.=", new BoolEquals());
		DefineInstruction("boolean.not", new BoolNot());
		DefineInstruction("boolean.and", new BoolAnd());
		DefineInstruction("boolean.or", new BoolOr());
		DefineInstruction("boolean.xor", new BoolXor());
		DefineInstruction("boolean.frominteger", new BooleanFromInteger());
		DefineInstruction("boolean.fromfloat", new BooleanFromFloat());
		DefineInstruction("boolean.rand", new BoolRand(this._RNG));

		DefineInstruction("code.quote", new Quote());
		DefineInstruction("code.fromboolean", new CodeFromBoolean());
		DefineInstruction("code.frominteger", new CodeFromInteger());
		DefineInstruction("code.fromfloat", new CodeFromFloat());
		DefineInstruction("code.noop", new ExecNoop());

		DefineInstruction("exec.k", AddStackInstruction(new ExecK(_execStack), StackType.EXEC_STACK));
		DefineInstruction("exec.s", AddStackInstruction(new ExecS(_execStack,
				getMaxPointsInProgram()), StackType.EXEC_STACK));
		DefineInstruction("exec.y", AddStackInstruction(new ExecY(_execStack),StackType.EXEC_STACK));
		DefineInstruction("exec.noop", new ExecNoop());

		DefineInstruction("exec.do*times", AddStackInstruction(new ExecDoTimes(this), StackType.EXEC_STACK));
		DefineInstruction("code.do*times", AddStackInstruction(new CodeDoTimes(this), StackType.CODE_STACK));
		DefineInstruction("exec.do*count", AddStackInstruction(new ExecDoCount(this), StackType.EXEC_STACK));
		DefineInstruction("code.do*count", AddStackInstruction(new CodeDoCount(this), StackType.CODE_STACK));
		DefineInstruction("exec.do*range", AddStackInstruction(new ExecDoRange(this), StackType.EXEC_STACK));
		DefineInstruction("code.do*range", AddStackInstruction(new CodeDoRange(this), StackType.CODE_STACK));
		DefineInstruction("code.=", AddStackInstruction(new ObjectEquals(_codeStack), StackType.CODE_STACK));
		DefineInstruction("exec.=", AddStackInstruction(new ObjectEquals(_execStack), StackType.EXEC_STACK));
		DefineInstruction("code.if", AddStackInstruction(new If(_codeStack), StackType.CODE_STACK));
		DefineInstruction("exec.if", AddStackInstruction(new If(_execStack), StackType.EXEC_STACK));
		DefineInstruction("code.rand",
				AddStackInstruction(new RandomPushCode(_codeStack, this._RNG), StackType.CODE_STACK));
		DefineInstruction("exec.rand",
				AddStackInstruction(new RandomPushCode(_execStack, this._RNG), StackType.EXEC_STACK));

		DefineInstruction("true", new BooleanConstant(true));
		DefineInstruction("false", new BooleanConstant(false));

		DefineInstruction("input.index", AddStackInstruction(new InputIndex(_inputStack), StackType.INPUT_STACK));
		DefineInstruction("input.inall", AddStackInstruction(new InputInAll(_inputStack), StackType.INPUT_STACK));
		DefineInstruction("input.inallrev", AddStackInstruction(new InputInRev(_inputStack), StackType.INPUT_STACK));
		DefineInstruction("input.stackdepth", AddStackInstruction(new Depth(_inputStack), StackType.INPUT_STACK));

		DefineStackInstructions("integer", _intStack, StackType.INT_STACK);
		DefineStackInstructions("float", _floatStack, StackType.FLOAT_STACK);
		DefineStackInstructions("boolean", _boolStack, StackType.BOOL_STACK);
		DefineStackInstructions("name", _nameStack, StackType.NAME_STACK);
		DefineStackInstructions("code", _codeStack, StackType.CODE_STACK);
		DefineStackInstructions("exec", _execStack, StackType.EXEC_STACK);

		DefineInstruction("frame.push", new PushFrame());
		DefineInstruction("frame.pop", new PopFrame());

		_generators.put("float.erc", new FloatAtomGenerator());
		_generators.put("integer.erc", new IntAtomGenerator());
	}

	/**
	 * Enables experimental Push "frames"
	 * 
	 * When frames are enabled, each Push subtree is given a fresh set of stacks
	 * (a "frame") when it executes. When a frame is pushed, the top value from
	 * each stack is passed to the new frame, and likewise when the frame pops,
	 * allowing for input arguments and return values.
	 */

	public void SetUseFrames(boolean inUseFrames) {
		_useFrames = inUseFrames;
	}

	/**
	 * Defines the instruction set used for random code generation in this Push
	 * interpreter.
	 * 
	 * @param inInstructionList
	 *            A program consisting of a list of string instruction names to
	 *            be placed in the instruction set.
	 */

	public void SetInstructions(Program inInstructionList)
			throws RuntimeException {
		_randomGenerators.clear();

		for (int n = 0; n < inInstructionList.size(); n++) {
			Object o = inInstructionList.peek(n);
			String name = null;

			if (o instanceof Instruction) {
				String keys[] = _instructions.keySet().toArray(
						new String[_instructions.size()]);

				for (String key : keys)
					if (_instructions.get(key) == o) {
						name = key;
						break;
					}
			} else if (o instanceof String) {
				name = (String) o;
			} else
				throw new RuntimeException(
						"Instruction list must contain a list of Push instruction names only");

			// Check for registered
			if (name.indexOf("registered.") == 0) {
				String registeredType = name.substring(11);

				if (!registeredType.equals("integer")
						&& !registeredType.equals("float")
						&& !registeredType.equals("boolean")
						&& !registeredType.equals("exec")
						&& !registeredType.equals("code")
						&& !registeredType.equals("name")
						&& !registeredType.equals("input")
						&& !registeredType.equals("frame")) {
					System.err.println("Unknown instruction \"" + name
							+ "\" in instruction set");
				} else {
					// Legal stack type, so add all generators matching
					// registeredType to _randomGenerators.
					Object keys[] = _instructions.keySet().toArray();

					for (int i = 0; i < keys.length; i++) {
						String key = (String) keys[i];
						if (key.indexOf(registeredType) == 0) {
							AtomGenerator g = _generators.get(key);
							_randomGenerators.add(g);
						}
					}

					if (registeredType.equals("boolean")) {
						AtomGenerator t = _generators.get("true");
						_randomGenerators.add(t);
						AtomGenerator f = _generators.get("false");
						_randomGenerators.add(f);
					}
					if (registeredType.equals("integer")) {
						AtomGenerator g = _generators.get("integer.erc");
						_randomGenerators.add(g);
					}
					if (registeredType.equals("float")) {
						AtomGenerator g = _generators.get("float.erc");
						_randomGenerators.add(g);
					}

				}
			} else if (name.indexOf("input.makeinputs") == 0) {
				String strnum = name.substring(16);
				int num = Integer.parseInt(strnum);

				for (int i = 0; i < num; i++) {
					DefineInstruction("input.in" + i, new InputInN(i));
					AtomGenerator g = _generators.get("input.in" + i);
					_randomGenerators.add(g);
				}
			} else {
				AtomGenerator g = _generators.get(name);

				if (g == null) {
					throw new RuntimeException("Unknown instruction \"" + name
							+ "\" in instruction set");
				} else {
					_randomGenerators.add(g);
				}
			}
		}
	}

	public void AddInstruction(String inName, Instruction inInstruction) {
		InstructionAtomGenerator iag = new InstructionAtomGenerator(inName);
		_instructions.put(inName, inInstruction);
		_generators.put(inName, iag);
		_randomGenerators.add(iag);
	}

	protected Instruction AddStackInstruction(Instruction instr, StackType stackType) {
		if (! _stackInstructions.containsKey(stackType)) {
			_stackInstructions.put(stackType, new ArrayList<Instruction>());
		}
		_stackInstructions.get(stackType).add(instr);
		return instr;
	}
	
	protected void DefineInstruction(String inName, Instruction inInstruction) {
		_instructions.put(inName, inInstruction);
		_generators.put(inName, new InstructionAtomGenerator(inName));
	}

	protected void DefineStackInstructions(String inTypeName, Stack inStack, StackType stackType) {
		DefineInstruction(inTypeName + ".pop", AddStackInstruction(new Pop(inStack), stackType));
		DefineInstruction(inTypeName + ".swap", AddStackInstruction(new Swap(inStack), stackType));
		DefineInstruction(inTypeName + ".rot", AddStackInstruction(new Rot(inStack), stackType));
		DefineInstruction(inTypeName + ".flush", AddStackInstruction(new Flush(inStack), stackType));
		DefineInstruction(inTypeName + ".dup", AddStackInstruction(new Dup(inStack), stackType));
		DefineInstruction(inTypeName + ".stackdepth", AddStackInstruction(new Depth(inStack), stackType));
		DefineInstruction(inTypeName + ".shove", AddStackInstruction(new Shove(inStack), stackType));
		DefineInstruction(inTypeName + ".yank", AddStackInstruction(new Yank(inStack), stackType));
		DefineInstruction(inTypeName + ".yankdup", AddStackInstruction(new YankDup(inStack), stackType));
	}

	protected void UpdateStackInstructions(StackType stackType) {
		Stack stack = null;
		switch (stackType) {
		case INT_STACK:		stack = intStack();	break;
		case FLOAT_STACK:	stack = floatStack(); break;
		case BOOL_STACK:	stack = boolStack(); break;
		case CODE_STACK:	stack = codeStack(); break;
		case NAME_STACK:	stack = nameStack(); break;
		case EXEC_STACK:	stack = execStack(); break;
		case INPUT_STACK:	stack = inputStack();
		}
		for (Instruction instr : _stackInstructions.get(stackType)) {
			if (instr instanceof StackInstruction) {
				((StackInstruction) instr).setStack(stack);
			} else if (instr instanceof ObjectStackInstruction) {
				((ObjectStackInstruction) instr)
						.setStack((ObjectStack) stack);
			}
		}
	}
	
	protected void UpdateStackInstructions() {
		for (StackType stackType :_stackInstructions.keySet()) {
			UpdateStackInstructions(stackType);
		}
	}
	
	protected void printStackInstructions() {
		for (Entry<StackType, List<Instruction>> entry : _stackInstructions
				.entrySet()) {
			System.out.println("=========== "+entry.getKey());
			for (Instruction instr : entry.getValue()) {
				for (Entry<String, Instruction> instrEntry : _instructions.entrySet()) {
					if (instr.equals(instrEntry.getValue())) {
						System.out.println(instrEntry.getKey());
					}
				}
			}
		}
	}
	
	
	/**
	 * Sets the parameters for the ERCs.
	 * 
	 * @param minRandomInt
	 * @param maxRandomInt
	 * @param randomIntResolution
	 * @param minRandomFloat
	 * @param maxRandomFloat
	 * @param randomFloatResolution
	 */
	public void SetRandomParameters(int minRandomInt, int maxRandomInt,
			int randomIntResolution, float minRandomFloat,
			float maxRandomFloat, float randomFloatResolution,
			int maxRandomCodeSize, int maxPointsInProgram) {

		_minRandomInt = minRandomInt;
		_maxRandomInt = maxRandomInt;
		_randomIntResolution = randomIntResolution;

		_minRandomFloat = minRandomFloat;
		_maxRandomFloat = maxRandomFloat;
		_randomFloatResolution = randomFloatResolution;

		setMaxRandomCodeSize(maxRandomCodeSize);
		setMaxPointsInProgram(maxPointsInProgram);
	}

	/**
	 * Executes a Push program with no execution limit.
	 * 
	 * @return The number of instructions executed.
	 */

	public int Execute(Program inProgram) {
		return Execute(inProgram, -1);
	}

	/**
	 * Executes a Push program with a given instruction limit.
	 * 
	 * @param inMaxSteps
	 *            The maximum number of instructions allowed to be executed.
	 * @return The number of instructions executed.
	 */

	public int Execute(Program inProgram, int inMaxSteps) {
		return Execute(inProgram, inMaxSteps, true);
	}
	
	/**
	 * Executes a Push program with a given instruction limit.
	 * 
	 * @param inMaxSteps
	 *            The maximum number of instructions allowed to be executed.
	 * @param countSteps should steps be counted
	 * @return The number of instructions executed.
	 */

	public int Execute(Program inProgram, int inMaxSteps, boolean countSteps) {
		if (countSteps)
			_evaluationExecutions++;
		LoadProgram(inProgram); // Initializes program
		return Step(inMaxSteps, countSteps);
	}	

	/**
	 * Loads a Push program into the interpreter's exec and code stacks.
	 * 
	 * @param inProgram
	 *            The program to load.
	 */

	public void LoadProgram(Program inProgram) {
		_codeStack.push(inProgram);
		_execStack.push(inProgram);
	}

	/**
	 * Steps a Push interpreter forward with a given instruction limit.
	 * 
	 * This method assumes that the intepreter is already setup with an active
	 * program (typically using \ref Execute).
	 * 
	 * @param inMaxSteps
	 *            The maximum number of instructions allowed to be executed.
	 * @return The number of instructions executed.
	 */

	public int Step(int inMaxSteps) {
		return Step(inMaxSteps, true);
	}	
	
	/**
	 * Steps a Push interpreter forward with a given instruction limit.
	 * 
	 * This method assumes that the intepreter is already setup with an active
	 * program (typically using \ref Execute).
	 * 
	 * @param inMaxSteps
	 *            The maximum number of instructions allowed to be executed.
	 * @param countSteps
	 *            should steps be counted
	 * @return The number of instructions executed.
	 */

	public int Step(int inMaxSteps, boolean countSteps) {
		int executed = 0;
		while (inMaxSteps != 0 && _execStack.size() > 0) {
			if (ExecuteInstruction(_execStack.pop()) == -1)
				throw new InternalError("Can't execute instruction");
			inMaxSteps--;
			executed++;
		}

		if (countSteps)
			_totalStepsTaken += executed;

		return executed;
	}

	public int ExecuteInstruction(Object inObject) {

		if (inObject instanceof Program) {
			Program p = (Program) inObject;

			if (_useFrames) {
				_execStack.push("frame.pop");
			}

			p.PushAllReverse(_execStack);

			if (_useFrames) {
				_execStack.push("frame.push");
			}

			return 0;
		}

		if (inObject instanceof Integer) {
			_intStack.push((Integer) inObject);
			return 0;
		}

		if (inObject instanceof Number) {
			_floatStack.push(((Number) inObject).floatValue());
			return 0;
		}
		// We assume that Instruction objects can belong to different
		// Interpreter instances, so they cannot be executed by other Interpreters.
		// Moreover, random code generators don't produce such references, so we can 
		// throw away the following piece of code safely.
		/*
		if (inObject instanceof Instruction) {
			((Instruction) inObject).Execute(this);
			return 0;
		}
		 */
		if (inObject instanceof String) {
			Instruction i = _instructions.get(inObject);

			if (i != null) {
				i.Execute(this);
			} else {
				_nameStack.push(inObject);
			}

			return 0;
		}

		return -1;
	}

	/**
	 * Fetch the active integer stack.
	 */

	public intStack intStack() {
		return _intStack;
	}

	/**
	 * Fetch the active float stack.
	 */

	public floatStack floatStack() {
		return _floatStack;
	}

	/**
	 * Fetch the active exec stack.
	 */

	public ObjectStack execStack() {
		return _execStack;
	}

	/**
	 * Fetch the active code stack.
	 */

	public ObjectStack codeStack() {
		return _codeStack;
	}

	/**
	 * Fetch the active bool stack.
	 */

	public booleanStack boolStack() {
		return _boolStack;
	}

	/**
	 * Fetch the active name stack.
	 */

	public ObjectStack nameStack() {
		return _nameStack;
	}

	/**
	 * Fetch the active input stack.
	 */

	public ObjectStack inputStack() {
		return _inputStack;
	}

	/**
	 * Fetch the indexed custom stack
	 */
	public Stack getCustomStack(int inIndex) {
		return _customStacks.get(inIndex);
	}

	/**
	 * Add a custom stack, and return that stack's index
	 */
	public int addCustomStack(Stack inStack) {
		_customStacks.add(inStack);
		return _customStacks.size() - 1;
	}

	protected void AssignStacksFromFrame() {
		_floatStack = (floatStack) _floatFrameStack.top();
		_intStack = (intStack) _intFrameStack.top();
		_boolStack = (booleanStack) _boolFrameStack.top();
		_codeStack = (ObjectStack) _codeFrameStack.top();
		_nameStack = (ObjectStack) _nameFrameStack.top();
		
		UpdateStackInstructions();
	}

	public void PushStacks() {
		_floatFrameStack.push(new floatStack());
		_intFrameStack.push(new intStack());
		_boolFrameStack.push(new booleanStack());
		_codeFrameStack.push(new ObjectStack());
		_nameFrameStack.push(new ObjectStack());

		AssignStacksFromFrame();
	}

	public void PopStacks() {
		_floatFrameStack.pop();
		_intFrameStack.pop();
		_boolFrameStack.pop();
		_codeFrameStack.pop();
		_nameFrameStack.pop();

		AssignStacksFromFrame();
	}

	public void PushFrame() {
		if (_useFrames) {
			boolean boolTop = _boolStack.top();
			int intTop = _intStack.top();
			float floatTop = _floatStack.top();
			Object nameTop = _nameStack.top();
			Object codeTop = _codeStack.top();

			PushStacks();

			_floatStack.push(floatTop);
			_intStack.push(intTop);
			_boolStack.push(boolTop);

			if (nameTop != null)
				_nameStack.push(nameTop);
			if (codeTop != null)
				_codeStack.push(codeTop);
		}
	}

	public void PopFrame() {
		if (_useFrames) {
			boolean boolTop = _boolStack.top();
			int intTop = _intStack.top();
			float floatTop = _floatStack.top();
			Object nameTop = _nameStack.top();
			Object codeTop = _codeStack.top();

			PopStacks();

			_floatStack.push(floatTop);
			_intStack.push(intTop);
			_boolStack.push(boolTop);

			if (nameTop != null)
				_nameStack.push(nameTop);
			if (codeTop != null)
				_codeStack.push(codeTop);
		}
	}

	/**
	 * Prints out the current stack states.
	 */

	public void PrintStacks() {
		System.out.println(this);
	}

	/**
	 * Returns a string containing the current Interpreter stack states.
	 */

	public String toString() {
		String result = "";
		result += "exec stack: " + _execStack + "\n";
		result += "code stack: " + _codeStack + "\n";
		result += "int stack: " + _intStack + "\n";
		result += "float stack: " + _floatStack + "\n";
		result += "boolean stack: " + _boolStack + "\n";
		result += "name stack: " + _nameStack + "\n";
		result += "input stack: " + _inputStack + "\n";

		return result;
	}

	/**
	 * Resets the Push interpreter state by clearing all of the stacks.
	 */

	public void ClearStacks() {
		_intStack.clear();
		_floatStack.clear();
		_execStack.clear();
		_nameStack.clear();
		_boolStack.clear();
		_codeStack.clear();
		_inputStack.clear();

		// Clear all custom stacks
		for (Stack s : _customStacks) {
			s.clear();
		}
	}

	/**
	 * Returns a string list of all instructions enabled in the interpreter.
	 */
	public String GetRegisteredInstructionsString() {
		Object keys[] = _instructions.keySet().toArray();
		Arrays.sort(keys);
		String list = "";

		for (int n = 0; n < keys.length; n++)
			list += keys[n] + " ";

		return list;
	}

	/**
	 * Returns a string of all the instructions used in this run.
	 * 
	 * @return
	 */
	public String GetInstructionsString() {
		Object keys[] = _instructions.keySet().toArray();
		ArrayList<String> strings = new ArrayList<String>();
		String str = "";

		for (int i = 0; i < keys.length; i++) {
			String key = (String) keys[i];

			if (_randomGenerators.contains(_generators.get(key))) {
				strings.add(key);
			}

		}

		if (_randomGenerators.contains(_generators.get("float.erc"))) {
			strings.add("float.erc");
		}
		if (_randomGenerators.contains(_generators.get("integer.erc"))) {
			strings.add("integer.erc");
		}

		Collections.sort(strings);
		for (String s : strings) {
			str += s + " ";
		}

		return str.substring(0, str.length() - 1);
	}

	/**
	 * Returns the Instruction whose name is given in instr.
	 * 
	 * @param instr
	 * @return the Instruction or null if no such Instruction.
	 */
	public Instruction GetInstruction(String instr) {
		return _instructions.get(instr);
	}

	/**
	 * Returns the number of evaluation executions so far this run.
	 * 
	 * @return The number of evaluation executions during this run.
	 */
	public long GetEvaluationExecutions() {
		return _evaluationExecutions;
	}

	public InputPusher getInputPusher() {
		return _inputPusher;
	}

	public void setInputPusher(InputPusher _inputPusher) {
		this._inputPusher = _inputPusher;
	}

	/**
	 * Generates a single random Push atom (instruction name, integer, float,
	 * etc) for use in random code generation algorithms.
	 * 
	 * @return A random atom based on the interpreter's current active
	 *         instruction set.
	 */

	public Object RandomAtom() {
		int index = this._RNG.nextInt(_randomGenerators.size());

		return _randomGenerators.get(index).Generate(this);
	}

	/**
	 * Generates a random Push program of a given size.
	 * 
	 * @param inSize
	 *            The requested size for the program to be generated.
	 * @return A random Push program of the given size.
	 */

	public Program RandomCode(int inSize) {
		Program p = new Program();

		List<Integer> distribution = RandomCodeDistribution(inSize - 1,
				inSize - 1);

		for (int i = 0; i < distribution.size(); i++) {
			int count = distribution.get(i);

			if (count == 1) {
				p.push(RandomAtom());
			} else {
				if (_generateFlatPrograms) {
					for (int j = 0; j < count; j++) {
						p.push(RandomAtom());
					}
				} else {
					p.push(RandomCode(count));
				}
			}
		}

		return p;
	}

	/**
	 * Generates a list specifying a size distribution to be used for random
	 * code.
	 * 
	 * Note: This method is called "decompose" in the lisp implementation.
	 * 
	 * @param inCount
	 *            The desired resulting program size.
	 * @param inMaxElements
	 *            The maxmimum number of elements at this level.
	 * @return A list of integers representing the size distribution.
	 */

	public List<Integer> RandomCodeDistribution(int inCount, int inMaxElements) {
		ArrayList<Integer> result = new ArrayList<Integer>();

		RandomCodeDistribution(result, inCount, inMaxElements);

		for (int i = 0; i < result.size(); i++) {
			int j = this._RNG.nextInt(result.size());
			if (i == j)
				continue;
			int iElem = result.get(i);
			result.set(i, result.get(j));
			result.set(j, iElem);
		}

		return result;
	}

	/**
	 * The recursive worker function for the public RandomCodeDistribution.
	 * 
	 * @param ioList
	 *            The working list of distribution values to append to.
	 * @param inCount
	 *            The desired resulting program size.
	 * @param inMaxElements
	 *            The maxmimum number of elements at this level.
	 */

	private void RandomCodeDistribution(List<Integer> ioList, int inCount,
			int inMaxElements) {
		if (inCount < 1)
			return;

		int thisSize = inCount < 2 ? 1 : (this._RNG.nextInt(inCount) + 1);

		ioList.add(thisSize);

		RandomCodeDistribution(ioList, inCount - thisSize, inMaxElements - 1);
	}

	public int getMaxRandomCodeSize() {
		return _maxRandomCodeSize;
	}

	public void setMaxRandomCodeSize(int _maxRandomCodeSize) {
		this._maxRandomCodeSize = _maxRandomCodeSize;
	}

	public int getMaxPointsInProgram() {
		return _maxPointsInProgram;
	}

	public void setMaxPointsInProgram(int _maxPointsInProgram) {
		this._maxPointsInProgram = _maxPointsInProgram;
	}

	public int getExecutionLimit() {
		return _executionLimit;
	}

	public void setExecutionLimit(int _executionLimit) {
		this._executionLimit = _executionLimit;
	}

	public boolean isGenerateFlatPrograms() {
		return _generateFlatPrograms;
	}

	public void setGenerateFlatPrograms(boolean _generateFlatPrograms) {
		this._generateFlatPrograms = _generateFlatPrograms;
	}
	
	public int getTotalStepsTaken() {
		return _totalStepsTaken;
	}

	public long getEvaluationExecutions() {
		return _evaluationExecutions;
	}

	public abstract class AtomGenerator implements Serializable {
		private static final long serialVersionUID = 1L;

		public abstract Object Generate(Interpreter inInterpreter);
	}

	public class InstructionAtomGenerator extends AtomGenerator {
		private static final long serialVersionUID = 1L;

		String _instruction;

		public InstructionAtomGenerator(String inInstructionName) {
			_instruction = inInstructionName;
		}

		public Object Generate(Interpreter inInterpreter) {
			return _instruction;
		}
	}

	public class FloatAtomGenerator extends AtomGenerator {
		private static final long serialVersionUID = 1L;

		public Object Generate(Interpreter inInterpreter) {
			float r = inInterpreter._RNG.nextFloat()
					* (_maxRandomFloat - _minRandomFloat);

			r -= (r % _randomFloatResolution);

			return r + _minRandomFloat;
		}
	}

	public class IntAtomGenerator extends AtomGenerator {
		private static final long serialVersionUID = 1L;

		public Object Generate(Interpreter inInterpreter) {
			int r = inInterpreter._RNG.nextInt(_maxRandomInt - _minRandomInt);

			r -= (r % _randomIntResolution);

			return r + _minRandomInt;
		}
	}

}
