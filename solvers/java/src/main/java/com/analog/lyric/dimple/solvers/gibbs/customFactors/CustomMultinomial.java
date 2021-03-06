/*******************************************************************************
*   Copyright 2013 Analog Devices, Inc.
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
********************************************************************************/

package com.analog.lyric.dimple.solvers.gibbs.customFactors;

import static java.util.Objects.*;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import com.analog.lyric.dimple.factorfunctions.Multinomial;
import com.analog.lyric.dimple.factorfunctions.core.FactorFunction;
import com.analog.lyric.dimple.model.core.EdgeState;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.Variable;
import com.analog.lyric.dimple.model.variables.VariableBlock;
import com.analog.lyric.dimple.schedulers.schedule.IGibbsSchedule;
import com.analog.lyric.dimple.schedulers.scheduleEntry.BlockScheduleEntry;
import com.analog.lyric.dimple.solvers.core.parameterizedMessages.DirichletParameters;
import com.analog.lyric.dimple.solvers.gibbs.GibbsDirichletEdge;
import com.analog.lyric.dimple.solvers.gibbs.GibbsDiscrete;
import com.analog.lyric.dimple.solvers.gibbs.GibbsRealFactor;
import com.analog.lyric.dimple.solvers.gibbs.GibbsRealJoint;
import com.analog.lyric.dimple.solvers.gibbs.GibbsSolverEdge;
import com.analog.lyric.dimple.solvers.gibbs.GibbsSolverGraph;
import com.analog.lyric.dimple.solvers.gibbs.GibbsVariableBlock;
import com.analog.lyric.dimple.solvers.gibbs.samplers.block.BlockMHInitializer;
import com.analog.lyric.dimple.solvers.gibbs.samplers.conjugate.DirichletSampler;
import com.analog.lyric.dimple.solvers.gibbs.samplers.conjugate.IRealJointConjugateSamplerFactory;
import com.analog.lyric.dimple.solvers.interfaces.SolverNodeMapping;

public class CustomMultinomial extends GibbsRealFactor implements IRealJointConjugateFactor, MultinomialBlockProposal.ICustomMultinomial
{
	private @Nullable GibbsDiscrete[] _outputVariables;
	private @Nullable GibbsDiscrete _NVariable;
	private @Nullable GibbsRealJoint _alphaVariable;
	private int _dimension;
	private int _alphaParameterEdge;
	private int _constantN;
	private @Nullable double[] _constantAlpha;
	private @Nullable int[] _constantOutputCounts;
	private boolean _hasConstantN;
	private boolean _hasConstantAlpha;
	private boolean _hasConstantOutputs;
	private @Nullable boolean[] _hasConstantOutput;
	private static final int NO_PORT = -1;
	private static final int ALPHA_PARAMETER_INDEX_FIXED_N = 0;	// If N is in constructor then alpha is first index (0)
	private static final int OUTPUT_MIN_INDEX_FIXED_N = 1;		// If N is in constructor then output starts at second index (1)
	private static final int N_PARAMETER_INDEX = 0;				// If N is not in constructor then N is the first index (0)
	private static final int ALPHA_PARAMETER_INDEX = 1;			// If N is not in constructor then alpha is second index (1)
	private static final int OUTPUT_MIN_INDEX = 2;				// If N is not in constructor then output starts at third index (2)
	
	public CustomMultinomial(Factor factor, GibbsSolverGraph parent)
	{
		super(factor, parent);
	}

	@Override
	public @Nullable GibbsSolverEdge<?> createEdge(EdgeState edge)
	{
		if (edge.getFactorToVariableEdgeNumber() == _alphaParameterEdge)
		{
			return new GibbsDirichletEdge(_dimension);
		}
		
		return null;
	}
	
	@SuppressWarnings("null")
	@Override
	public void updateEdgeMessage(EdgeState modelEdge, GibbsSolverEdge<?> solverEdge)
	{
		final int portNum = modelEdge.getFactorToVariableEdgeNumber();
		if (portNum == _alphaParameterEdge)
		{
			// Output port is the joint alpha parameter input
			// Determine sample alpha vector of the conjugate Dirichlet distribution
			
			DirichletParameters outputMsg = (DirichletParameters)solverEdge.factorToVarMsg;

			// Clear the output counts
			outputMsg.setNull(_dimension);

			// Get the current output counts
			if (!_hasConstantOutputs)
			{
				for (int i = 0; i < _dimension; i++)
					outputMsg.add(i, _outputVariables[i].getCurrentSampleIndex());
			}
			else	// Some or all outputs are constant
			{
				for (int i = 0, iVar = 0, iConst = 0; i < _dimension; i++)
					outputMsg.add(i, _hasConstantOutput[i] ? _constantOutputCounts[iConst++] : _outputVariables[iVar++].getCurrentSampleIndex());
			}
		}
		else
			super.updateEdgeMessage(modelEdge, solverEdge);
	}
	
	
	@Override
	public Set<IRealJointConjugateSamplerFactory> getAvailableRealJointConjugateSamplers(int portNumber)
	{
		Set<IRealJointConjugateSamplerFactory> availableSamplers = new HashSet<IRealJointConjugateSamplerFactory>();
		if (isPortAlphaParameter(portNumber))						// Conjugate sampler if edge is alpha parameter input
			availableSamplers.add(DirichletSampler.factory);		// Parameter inputs have conjugate Dirichlet distribution
		return availableSamplers;
	}
	
	public boolean isPortAlphaParameter(int portNumber)
	{
		determineConstantsAndEdges();	// Call this here since initialize may not have been called yet
		return (portNumber == _alphaParameterEdge);
	}

	// For MultinomialBlockProposal.ICustomMultinomial interface
	@SuppressWarnings("null")
	@Override
	public final double[] getCurrentAlpha()
	{
		return (_hasConstantAlpha ? _constantAlpha : _alphaVariable.getCurrentSample()).clone();
	}
	@Override
	public final boolean isAlphaEnergyRepresentation()
	{
		return false;
	}
	@Override
	public final boolean hasConstantN()
	{
		return _hasConstantN;
	}
	@Override
	public final int getN()
	{
		return _constantN;
	}

	
	
	@Override
	public void initialize()
	{
		super.initialize();
		
		
		// Determine what parameters are constants or edges, and save the state
		determineConstantsAndEdges();
		
		
		// Create a block schedule entry with a BlockMHSampler and a MultinomialBlockProposal kernel
		final GibbsDiscrete[] outputVariables = Objects.requireNonNull(_outputVariables);
		Variable[] nodeList = new Variable[outputVariables.length + (_hasConstantN ? 0 : 1)];
		int nodeIndex = 0;
		if (!_hasConstantN)
			nodeList[nodeIndex++] = Objects.requireNonNull(_NVariable).getModelObject();
		for (int i = 0; i < outputVariables.length; i++, nodeIndex++)
			nodeList[nodeIndex] = outputVariables[i].getModelObject();
		
		GibbsSolverGraph parent = getParentGraph();
		VariableBlock block = parent.getModel().addVariableBlock(nodeList);
		GibbsVariableBlock sblock = requireNonNull(parent.getSolverVariableBlock(block, true));
		BlockMHInitializer blockSampler = new BlockMHInitializer(sblock, new MultinomialBlockProposal(this));
		BlockScheduleEntry blockScheduleEntry = new BlockScheduleEntry(blockSampler, block);
		
		// Add the block updater to the schedule
		GibbsSolverGraph rootGraph = (GibbsSolverGraph)parent.getRootSolverGraph(); // FIXME don't assume root
		IGibbsSchedule schedule = rootGraph.getSchedule(); // Assumes scheduler for Gibbs solver is flattened to root graph
		schedule.addBlockScheduleEntry(blockScheduleEntry);
		
		// Use the block sampler to initialize the neighboring variables
		rootGraph.addBlockInitializer(blockSampler);
	}
	
	
	private void determineConstantsAndEdges()
	{
		final Factor factor = _model;
		FactorFunction factorFunction = factor.getFactorFunction();
		Multinomial specificFactorFunction = (Multinomial)factorFunction;

		final int prevAlphaParameterEdge = _alphaParameterEdge;
		
		// Pre-determine whether or not the parameters are constant
		List<? extends Variable> siblings = factor.getSiblings();
		int alphaParameterIndex;
		int outputMinIndex;
		_constantN = -1;
		_NVariable = null;
		if (specificFactorFunction.hasConstantNParameter())		// N parameter is constructor constant
		{
			_hasConstantN = true;
			_constantN = specificFactorFunction.getN();
			alphaParameterIndex = ALPHA_PARAMETER_INDEX_FIXED_N;
			outputMinIndex = OUTPUT_MIN_INDEX_FIXED_N;
		}
		else	// Variable or constant N parameter
		{
			_hasConstantN = factor.hasConstantAtIndex(N_PARAMETER_INDEX);
			if (_hasConstantN)
				_constantN = requireNonNull(factor.getConstantValueByIndex(N_PARAMETER_INDEX)).getInt();
			else
				_NVariable = (GibbsDiscrete)getSibling(factor.argIndexToSiblingNumber(N_PARAMETER_INDEX));
			alphaParameterIndex = ALPHA_PARAMETER_INDEX;
			outputMinIndex = OUTPUT_MIN_INDEX;
		}
		
		// Save the alpha parameter constant or variables
		_hasConstantAlpha = false;
		_constantAlpha = null;
		_alphaVariable = null;
		_alphaParameterEdge = NO_PORT;
		if (factor.hasConstantAtIndex(alphaParameterIndex))
		{
			_hasConstantAlpha = true;
			_constantAlpha =
				requireNonNull(factor.getConstantValueByIndex(alphaParameterIndex)).getDoubleArray();
		}
		else
		{
			_alphaParameterEdge = factor.argIndexToSiblingNumber(alphaParameterIndex);
			_alphaVariable = (GibbsRealJoint)getSibling(_alphaParameterEdge);
		}
		
		final SolverNodeMapping solvers = getSolverMapping();
		
		// Save the output constant or variables as well
		final int nEdges = getSiblingCount();
		int numOutputEdges = nEdges - factor.argIndexToSiblingNumber(outputMinIndex);
		final GibbsDiscrete[] outputVariables = _outputVariables = new GibbsDiscrete[numOutputEdges];
		_hasConstantOutputs = factor.hasConstantAtOrAboveIndex(outputMinIndex);
		_constantOutputCounts = null;
		_hasConstantOutput = null;
		_dimension = -1;
		if (_hasConstantOutputs)
		{
			int numConstantOutputs = factor.numConstantsAtOrAboveIndex(outputMinIndex);
			_dimension = numOutputEdges + numConstantOutputs;
			final boolean[] hasConstantOutput = _hasConstantOutput = new boolean[_dimension];
			final int[] constantOutputCounts = _constantOutputCounts = new int[numConstantOutputs];
			for (int i = 0, index = outputMinIndex; i < _dimension; i++, index++)
			{
				if (factor.hasConstantAtIndex(index))
				{
					hasConstantOutput[i] = true;
					constantOutputCounts[i] = requireNonNull(factor.getConstantValueByIndex(index)).getInt();
				}
				else
				{
					hasConstantOutput[i] = false;
					int outputEdge = factor.argIndexToSiblingNumber(index);
					outputVariables[i] = (GibbsDiscrete)solvers.getSolverVariable(siblings.get(outputEdge));
				}
			}
		}
		else	// No constant outputs
		{
			_dimension = numOutputEdges;
			for (int i = 0, index = outputMinIndex; i < _dimension; i++, index++)
			{
				int outputEdge = factor.argIndexToSiblingNumber(index);
				outputVariables[i] = (GibbsDiscrete)solvers.getSolverVariable(siblings.get(outputEdge));
			}
		}

		if (_alphaParameterEdge != prevAlphaParameterEdge)
		{
			removeSiblingEdgeState();
		}
	}
}
