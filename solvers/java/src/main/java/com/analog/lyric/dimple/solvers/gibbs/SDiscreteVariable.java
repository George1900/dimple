/*******************************************************************************
*   Copyright 2012 Analog Devices, Inc.
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

package com.analog.lyric.dimple.solvers.gibbs;

import java.util.ArrayList;
import java.util.Arrays;

import com.analog.lyric.dimple.model.Discrete;
import com.analog.lyric.dimple.model.DimpleException;
import com.analog.lyric.dimple.model.Port;
import com.analog.lyric.dimple.model.VariableBase;
import com.analog.lyric.dimple.solvers.core.SVariableBase;
import com.analog.lyric.dimple.solvers.core.Utilities;



public class SDiscreteVariable extends SVariableBase implements ISolverVariableGibbs
{
	protected long[] _beliefHistogram;
	protected int _sampleIndex;
	protected double[] _priors;
	protected ArrayList<Integer> _sampleIndexArray;
	protected int _bestSampleIndex;
	protected double _beta = 1;
	protected Discrete _varDiscrete;

	public SDiscreteVariable(VariableBase var) 
	{
		super(var);
		_varDiscrete = (Discrete)_var;
		_beliefHistogram = new long[((Discrete)var).getDiscreteDomain().getElements().length];
		initialize();
		_priors = (double[]) getDefaultMessage(null);
	}

	public Object getDefaultMessage(Port port)
	{
		int domainLength = _varDiscrete.getDiscreteDomain().getElements().length;
		double[] retVal = new double[domainLength];
		Arrays.fill(retVal, 0);
		return retVal;
	}


	public void updateEdge(int outPortNum)
	{
		throw new DimpleException("Method not supported in Gibbs sampling solver.");
	}

	public void update()
	{
		ArrayList<Port> ports = _var.getPorts();
		double[] priors = (double[])_priors;
		int messageLength = priors.length;
		int numPorts = _var.getPorts().size();
		double minEnergy = Double.POSITIVE_INFINITY;

		double[][] inPortMsgs = new double[numPorts][];
		for (int port = 0; port < numPorts; port++) 
			inPortMsgs[port] = (double[])ports.get(port).getInputMsg();
		
		double[] conditionalProbability = new double[messageLength];

		// Compute the conditional probability (initially in energy representation before converting to probability)
		for (int index = 0; index < messageLength; index++)
		{
			double out = priors[index];						// Sum of the input prior...
			for (int port = 0; port < numPorts; port++)
				out += inPortMsgs[port][index];				// Plus each input message value
			
			if (out < minEnergy) minEnergy = out;			// For normalization

			conditionalProbability[index] = out;			// Initially in energy representation before converting to probability
		}

		// Convert to probability representation
		for (int index = 0; index < messageLength; index++)
		{
			double temperedValue = (conditionalProbability[index] - minEnergy) * _beta;
			double out = Math.exp(-temperedValue);
			conditionalProbability[index] = out;
		}

		// Sample from the conditional distribution
		_sampleIndex = Utilities.sampleFromMultinomial(conditionalProbability, GibbsSolverRandomGenerator.rand);

		// Send the sample value to all output ports
		for (int port = 0; port < numPorts; port++) 
			ports.get(port).setOutputMsg(_sampleIndex);
	}

	public void updateBelief()
	{
		_beliefHistogram[_sampleIndex]++;
	}

	public Object getBelief() 
	{
		double[] outBelief = new double[_varDiscrete.getDiscreteDomain().getElements().length];
		long sum = 0;
		for (int i = 0; i < _varDiscrete.getDiscreteDomain().getElements().length; i++)
			sum+= _beliefHistogram[i];
		if (sum != 0)
		{
			for (int i = 0; i < _varDiscrete.getDiscreteDomain().getElements().length; i++)
				outBelief[i] = (double)_beliefHistogram[i]/(double)sum;
		}
		else
		{
			for (int i = 0; i < _varDiscrete.getDiscreteDomain().getElements().length; i++)
				outBelief[i] = ((double[])_priors)[i];		// Disconnected variable that has never been updated
		}
		
		return outBelief;
	}

	public void setInput(Object priors) 
	{
		double[] vals = (double[])priors;
		if (vals.length != _varDiscrete.getDiscreteDomain().getElements().length)
			throw new DimpleException("Prior size must match domain length");
		
		// Convert to energy values
		_priors = new double[vals.length];
		for (int i = 0; i < vals.length; i++)
			_priors[i] = -Math.log(vals[i]);
	}
	
    public void saveAllSamples()
    {
    	_sampleIndexArray = new ArrayList<Integer>();
    }
    
    public void saveCurrentSample()
    {
    	if (_sampleIndexArray != null)
    		_sampleIndexArray.add(_sampleIndex);
    }
    
    public void saveBestSample()
    {
    	_bestSampleIndex = _sampleIndex;
    }
    
	public double getPotential()
	{
		return _priors[_sampleIndex];
	}

    public Object[] AllSamples() {return getAllSamples();}
    public Object[] getAllSamples()
    {
    	int length = _sampleIndexArray.size();
    	Object[] domain = _varDiscrete.getDiscreteDomain().getElements();
    	Object[] retval = new Object[length];
    	for (int i = 0; i < length; i++)
    		retval[i] = domain[_sampleIndexArray.get(i)];
    	return retval;
    }
    public int[] AllSampleIndices() {return getAllSampleIndices();}
    public int[] getAllSampleIndices()
    {
    	int length = _sampleIndexArray.size();
    	int[] retval = new int[length];
    	for (int i = 0; i < length; i++)
    		retval[i] = _sampleIndexArray.get(i);
    	return retval;
    }

    public Object Sample() {return getCurrentSample();}
    public Object getCurrentSample()
    {
    	return _varDiscrete.getDiscreteDomain().getElements()[_sampleIndex];
    }
    public int SampleIndex() {return getCurrentSampleIndex();}
    public int getCurrentSampleIndex()
    {
    	return _sampleIndex;
    }
    
    public Object BestSample() {return getBestSample();}
    public Object getBestSample()
    {
    	return _varDiscrete.getDiscreteDomain().getElements()[_bestSampleIndex];
    }
    public int BestSampleIndex() {return getBestSampleIndex();}
    public int getBestSampleIndex()
    {
    	return _bestSampleIndex;
    }
    
    public void setBeta(double beta)	// beta = 1/temperature
    {
    	_beta = beta;
    }
	
    
	public void initialize()
	{
		_bestSampleIndex = -1;
		for (int i = 0; i < _varDiscrete.getDiscreteDomain().getElements().length; i++) 
			_beliefHistogram[i] = 0;
	}
	
	public double getEnergy()  
	{
		throw new DimpleException("getEnergy not yet supported for gibbs");
	}
	
	public Object getGuess() 
	{
		throw new DimpleException("get and set guess not supported for this solver");
	}
	
	public void setGuess(Object guess) 
	{
		throw new DimpleException("get and set guess not supported for this solver");
	}


}
