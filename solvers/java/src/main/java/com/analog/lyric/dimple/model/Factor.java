package com.analog.lyric.dimple.model;

import java.util.ArrayList;

import com.analog.lyric.dimple.FactorFunctions.core.FactorFunction;
import com.analog.lyric.dimple.FactorFunctions.core.FactorFunctionBase;
import com.analog.lyric.dimple.FactorFunctions.core.JointFactorFunction;
import com.analog.lyric.dimple.solvers.interfaces.ISolverFactor;
import com.analog.lyric.dimple.solvers.interfaces.ISolverFactorGraph;



public class Factor extends FactorBase implements Cloneable
{
	private String _modelerFunctionName = "";
	protected ISolverFactor _solverFactor = null;
	private FactorFunction _factorFunction;
	
	@Override
	public final Factor asFactor()
	{
		return this;
	}
	
	@Override
	public final boolean isFactor()
	{
		return true;
	}
	
	public String getModelerFunctionName()
	{
		return _modelerFunctionName;
	}
	
	public Factor(int id,FactorFunction factorFunc, VariableBase [] variables) 
	{
		super(id);
		
		_factorFunction = factorFunc;
		_modelerFunctionName = factorFunc.getName();
		
		for (int i = 0; i < variables.length; i++)
			addVariable(variables[i]);
	}
	
	public Factor(int id,VariableBase [] variables, String modelerFunctionName) 
	{
		super(id);
		_modelerFunctionName = modelerFunctionName;
		for (int i = 0; i < variables.length; i++)
		{
			Port p = new Port(this,_ports.size());
			_ports.add(p);
			variables[i].connect(p);
		}
	}

	
	public boolean isDiscrete()
	{
		for (Port p : getPorts())
		{
			VariableBase vb = (VariableBase)p.getConnectedNodeFlat();
			if (! vb.getDomain().isDiscrete())
			{
				return false;
			}
		}
		
		return true;
	}
	
	public FactorFunction getFactorFunction()
	{
		return _factorFunction;
	}
	
	public void setFactorFunction(FactorFunction function)
	{
		_factorFunction = function;
	}
	
	protected void addVariable(VariableBase variable) 
	{
		Port p = new Port(this,_ports.size());
		_ports.add(p);
		variable.connect(p);
	}
	
	
	
	
	@Override
	public String getLabel()
	{
		String name = _label;
		if(name == null)
		{
			name = _name;
			if(name == null)
			{
				name = getModelerFunctionName() + "_" + getId();
			}
		}
		return name;
	}

	
	@Override
	public ISolverFactor getSolver()
	{
		return _solverFactor;
	}

	@Override
	public String getClassLabel()
    {
    	return "Factor";
    }
	
	public void attach(ISolverFactorGraph factorGraph) 
	{
		//_solverFactor = factorGraph.createCustomFactor(this);
		_solverFactor = factorGraph.createFactor(this);
		initialize();
		//TODO: do stuff
	}
	
	public Domain [] getDomains()
	{
		Domain [] retval = new Domain[getVariables().size()];
		
		for (int i = 0; i < getVariables().size(); i++)
		{
			retval[i] = getVariables().getByIndex(i).getDomain();
		}
		
		return retval;
	}
	
	@Override
	public double getEnergy() 
	{
		if (_solverFactor == null)
			throw new DimpleException("solver needs to be set before calculating energy");
		
		return _solverFactor.getEnergy();
	}

	
	@Override
	public Factor clone()
	{
		/*******
		 * NOTE: Any derived class that defines instance variables that are
		 * objects (rather than primitive types) must implement clone(), which
		 * must first call super.clone(), and then deep-copy those instance
		 * variables to the clone.
		 *******/
		Factor f = (Factor) super.clone();
		
		//TODO: cloning solver factor?
		
		return f;
	}

	public double [] getBelief() 
	{
		return (double[])_solverFactor.getBelief();
	}

	/*
	public int[][] getPossibleBeliefIndices() 
	{
		return _solverFactor.getPossibleBeliefIndices();
	}
	*/
	
	public void initialize() 
	{
		for (int i = 0; i < _ports.size(); i++)
		{
			initializePortMsg(_ports.get(i));
		}
		if (_solverFactor != null)
			_solverFactor.initialize();
	}
	
	public void initializePortMsg(Port port)
	{
		if (_solverFactor == null)
			port.setInputMsg(null);
		else
			port.setInputMsg(_solverFactor.getDefaultMessage(port));
	}
		
	public VariableList getVariables()
	{
		VariableList vl = new VariableList();
		//Discrete [] retval = new Discrete[_ports.size()];
		for (int i = 0; i < _ports.size(); i++)
		{
			//retval[i] = (Discrete)_ports.get(i).getConnectedNode();
			vl.add((VariableBase)_ports.get(i).getConnectedNodeFlat());
		}
		
		//FactorList fl = new FactorList();
		//fl.addAll(retval);
		// TODO Auto-generated method stub
		return vl;
	}

	@Override
	public void update() 
	{
		checkSolverNotNull();
		_solverFactor.update();
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateEdge(int outPortNum) 
	{
		// TODO Auto-generated method stub
		checkSolverNotNull();
		_solverFactor.updateEdge(outPortNum);
	}
	
	private void checkSolverNotNull() 
	{
		if (_solverFactor == null)
			throw new DimpleException("solver must be set before performing this action.");
	}
	
	void replaceVariablesWithJoint(VariableBase [] variablesToJoin, VariableBase newJoint) 
	{
		throw new DimpleException("not implemented");
	}
	
	Factor join(Factor other) 
	{
		ArrayList<VariableBase> varList = new ArrayList<VariableBase>();
		
		//go through variables from first factor and
		ArrayList<Integer> map1 = new ArrayList<Integer>();
		
		
		//First get a list of variables in common.
		for (int i = 0; i < getPorts().size(); i++)
		{
			map1.add(i);
			varList.add((VariableBase)getPorts().get(i).getConnectedNodeFlat());
			
		}
		
		ArrayList<Integer> map2 = new ArrayList<Integer>();
		
		int newnuminputs = map1.size();
		
		for (int i = 0; i < other.getPorts().size(); i++)
		{
			boolean found = false;
			
			for (int j = 0; j < getPorts().size(); j++)
			{
				
				if (getPorts().get(j).getConnectedNodeFlat() == other.getPorts().get(i).getConnectedNodeFlat())
				{
					found = true;
					map2.add(j);
					break;
				}
				
			}
			if (!found)
			{
				map2.add(varList.size());
				newnuminputs++;
				varList.add((VariableBase)other.getPorts().get(i).getConnectedNodeFlat());
			}
		}
		
		FactorFunctionBase ff1 = this.getFactorFunction();
		FactorFunctionBase ff2 = other.getFactorFunction();
		//String newname = ff1.getName() + "_" + ff2.getName();
		JointFactorFunction jff = getParentGraph().getJointFactorFunction(ff1,ff2,map1,map2);
		//JointFactorFunction jff = new JointFactorFunction(newname, ff1, ff2, newnuminputs,map1,map2);
		
		//Remove the two old factors.
		getParentGraph().remove(this);
		getParentGraph().remove(other);
		
		//Create the table factor using the variables
		VariableBase [] vars = new VariableBase[newnuminputs];
		varList.toArray(vars);
		return getParentGraph().addFactor(jff, vars);

	}


}
