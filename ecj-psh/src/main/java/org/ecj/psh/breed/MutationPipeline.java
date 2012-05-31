package org.ecj.psh.breed;

import org.ecj.psh.PshNodeSelector;

import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;

/**
 * 
 * Mutation pipeline based on Psh implementation
 * 
 * @author Tomasz Kamiñski
 * 
 */
public class MutationPipeline extends PshBreedingPipeline {

	public static final String P_MUTATION = "mutate";
	public static final String P_FAIR = "use-fair";
	public static final String P_FAIRRANGE = "fair-mutation-range";
	public static final int NUM_SOURCES = 1;

	/** How the pipeline chooses a subtree to mutate */
	public PshNodeSelector nodeSelector;

	public boolean useFairMutation;

	public float fairMutationRange;

	@Override
	public Parameter defaultBase() {
		return PshBreedDefaults.base().push(P_MUTATION);
	}

	@Override
	public int numSources() {
		return NUM_SOURCES;
	}

	@Override
	public Object clone() {
		MutationPipeline c = (MutationPipeline) (super.clone());
		// deep-cloned stuff
		c.nodeSelector = (PshNodeSelector) (nodeSelector.clone());
		return c;
	}

	@Override
	public void setup(final EvolutionState state, final Parameter base) {
		super.setup(state, base);

		Parameter def = defaultBase();
		Parameter p = base.push(P_NODESELECTOR).push("" + 0);
		Parameter d = def.push(P_NODESELECTOR).push("" + 0);

		nodeSelector = (PshNodeSelector) (state.parameters
				.getInstanceForParameter(p, d, PshNodeSelector.class));
		nodeSelector.setup(state, p);

		// should we use fair mutation mode?
		useFairMutation = state.parameters.getBoolean(base.push(P_FAIR),
				def.push(P_FAIR), true);
		
		// fair mutation range
		fairMutationRange = state.parameters.getFloatWithDefault(base.push(P_FAIRRANGE),
				def.push(P_FAIRRANGE), 0.3);
	}

	@Override
	public int produce(int min, int max, int start, int subpopulation,
			Individual[] inds, EvolutionState state, int thread) {
		// TODO implement mutation operator based on Psh code
		return 0;
	}

}
