package com.analog.lyric.dimple.solvers.minsum;

import com.analog.lyric.dimple.model.Discrete;
import com.analog.lyric.dimple.model.DimpleException;
import com.analog.lyric.dimple.model.Factor;
import com.analog.lyric.dimple.model.Port;
import com.analog.lyric.dimple.model.VariableBase;
import com.analog.lyric.dimple.solvers.core.SVariableBase;


public class SVariable extends SVariableBase
{
	protected Discrete _varDiscrete;

	public SVariable(VariableBase var) 
	{
		super(var);
		_varDiscrete = (Discrete)_var;
		_input = MessageConverter.initialValue(_varDiscrete.getDiscreteDomain().getElements().length);
	}

	/*
	 * We cache all of the double arrays we use during the update.  This saves
	 * time when performing the update.
	 */		
	private boolean _initCalled = true;
	protected double[][] _inPortMsgs = null;
	protected double[][] _outMsgArray = null;
	protected double [][] _savedOutMsgArray;
	protected double [] _dampingParams = new double[0];
	protected double [] _input;


	public Object getInitialMsgValue()
	{
		double[] retVal = new double[_varDiscrete.getDiscreteDomain().getElements().length];
		for (int i = 0; i < _varDiscrete.getDiscreteDomain().getElements().length; i++) retVal[i] = 0;
		return retVal;
	}    

	public Object getDefaultMessage(Port port) 
	{
		return getInitialMsgValue();
	}

	public void updateEdge(int outPortNum)
	{
		updateCache();

		double[] priors = (double[])_input;
		int numPorts = _var.getPorts().size();
		int numValue = priors.length;

		// Compute the sum of all messages   
		double minPotential = Double.POSITIVE_INFINITY;
		double[] outMsgs = _outMsgArray[outPortNum];

		// Save previous output for damping
		double[] saved;
		double damping = _dampingParams[outPortNum];
		if (damping != 0)
		{
			saved = _savedOutMsgArray[outPortNum];
			for (int i = 0; i < outMsgs.length; i++)
				saved[i] = outMsgs[i];
		}

		for (int i = 0; i < numValue; i++)
		{
			double out = priors[i];
			for (int port = 0; port < numPorts; port++)
				if (port != outPortNum) out += _inPortMsgs[port][i];
			outMsgs[i] = out;

			if (out < minPotential) 
				minPotential = out;
		}

		// Damping
		if (damping != 0)
		{
			saved = _savedOutMsgArray[outPortNum];
			for (int m = 0; m < numValue; m++)
				outMsgs[m] = outMsgs[m]*(1-damping) + saved[m]*damping;
		}

		// Normalize the min
		for (int i = 0; i < numValue; i++) 
			outMsgs[i] -= minPotential;
	}




	public void update()
	{
		updateCache();

		double[] priors = (double[])_input;
		int numPorts = _var.getPorts().size();
		int numValue = priors.length;

		// Compute the sum of all messages   
		double[] beliefs = new double[numValue];

		for (int i = 0; i < numValue; i++)
		{
			double sum = priors[i];
			for (int port = 0; port < numPorts; port++) 
				sum += _inPortMsgs[port][i];
			beliefs[i] = sum;
		}


		// Now compute output messages for each outgoing edge
		for (int port = 0; port < numPorts; port++ )
		{
			double[] outMsgs = _outMsgArray[port];
			double minPotential = Double.POSITIVE_INFINITY;
			
			// Save previous output for damping
			double[] saved;
			double damping = _dampingParams[port];
			if (damping != 0)
			{
				saved = _savedOutMsgArray[port];
				for (int i = 0; i < outMsgs.length; i++)
					saved[i] = outMsgs[i];
			}

			double[] inPortMsgsThisPort = _inPortMsgs[port];
			for (int i = 0; i < numValue; i++)
			{
				double out = beliefs[i] - inPortMsgsThisPort[i];
				if (out < minPotential) 
					minPotential = out;
				outMsgs[i] = out;
			}

			// Damping
			if (_dampingParams[port] != 0)
			{
				saved = _savedOutMsgArray[port];
				for (int m = 0; m < numValue; m++)
					outMsgs[m] = outMsgs[m]*(1-damping) + saved[m]*damping;
			}

			// Normalize the min
			for (int i = 0; i < numValue; i++) 
				outMsgs[i] -= minPotential;
		}


	}

	public Object getBelief() 
	{
		updateCache();

		double[] priors = (double[])_input;
		double[] outBelief = new double[priors.length];
		int numValue = priors.length;
		int numPorts = _var.getPorts().size();


		for (int i = 0; i < numValue; i++)
		{
			double sum = priors[i];
			for (int port = 0; port < numPorts; port++) sum += _inPortMsgs[port][i];
			outBelief[i] = sum;
		}

		// Convert to probabilities since that's what the interface expects        
		return MessageConverter.toProb(outBelief);
	}


	public void setInput(Object value) 
	{
		// Convert from probabilities since that's what the interface provides        
		_input = MessageConverter.fromProb((double[])value);
	}

	public void initialize()
	{

		//Flag that init was called so that we can update the cache next time we need cached
		//values.  We can't do the same thing as the tableFunction (update the cache here)
		//because the function init gets called after variable init.  If we updated teh cache
		//here, the table function init would replace the arrays for the outgoing message
		//and our update functions would update stale messages.
		super.initialize();
		//System.out.println("Variable init");
		_initCalled = true;
	}

	private void updateCache()
	{
		if (_initCalled)
		{
			int numPorts = _var.getPorts().size();
			_initCalled = false;
			_inPortMsgs = new double[numPorts][];
			_outMsgArray = new double[numPorts][];
			_savedOutMsgArray = new double[numPorts][];

			if (_dampingParams.length != numPorts)
			{
				double[] tmp = new double[numPorts];
				for (int i = 0; i < _dampingParams.length; i++)
					if (i < tmp.length)
						tmp[i] = _dampingParams[i];
				_dampingParams = tmp;
			}

			for (int i = 0; i < numPorts; i++)
			{
				_inPortMsgs[i] = (double[])_var.getPorts().get(i).getInputMsg();
				_outMsgArray[i] = (double[])_var.getPorts().get(i).getOutputMsg();
				_savedOutMsgArray[i] = new double[_outMsgArray[i].length];
			}
		}
	}

	public void remove(Factor factor)
	{
		_initCalled = true;
	}

	public void setDamping(int portIndex, double dampingVal)
	{
		if (portIndex >= _dampingParams.length)
		{
			double[] tmp = new double [portIndex+1];
			for (int i = 0; i < _dampingParams.length; i++)				
				tmp[i] = _dampingParams[i];

			_dampingParams = tmp;
		}

		_dampingParams[portIndex] = dampingVal;
	}

	public double getDamping(int portIndex)
	{
		if (portIndex >= _dampingParams.length)
			return 0;
		else
			return _dampingParams[portIndex];
	}


	@Override
	public void connectPort(Port p)  
	{
		// TODO Auto-generated method stub
		_initCalled = true;

	}


	public double getEnergy()  
	{
		throw new DimpleException("getEnergy not yet supported for MinSum");
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
