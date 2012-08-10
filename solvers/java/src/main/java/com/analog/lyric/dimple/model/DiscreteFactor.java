package com.analog.lyric.dimple.model;

import java.util.ArrayList;

import com.analog.lyric.dimple.FactorFunctions.core.FactorFunction;
import com.analog.lyric.dimple.FactorFunctions.core.FactorTable;
import com.analog.lyric.dimple.FactorFunctions.core.TableFactorFunction;

public class DiscreteFactor extends Factor 
{
	public DiscreteFactor(int id, FactorFunction factorFunc,
			VariableBase[] variables)  
			{
		super(id, factorFunc, variables);
		
		if (!isDiscrete())
			throw new DimpleException("ack");
	}

	public FactorTable getFactorTable() 
	{
		return getFactorFunction().getFactorTable(getDomains());
	}

	
	public int[][] getPossibleBeliefIndices() 
	{
		return _solverFactor.getPossibleBeliefIndices();
	}
	

	public void replaceVariablesWithJoint(VariableBase [] variablesToJoin, VariableBase newJoint) 
	{
		//Support a mixture of variables referred to in this factor and previously not referred to in this factor
		ArrayList<Port> ports = getPorts();
		ArrayList<VariableBase> newVariables = new ArrayList<VariableBase>();
		

		//First we figure out which variables are not currently referred to in this factor.
		for (int i = 0; i < variablesToJoin.length; i++)
		{
			boolean exists = false;
			for (int j = 0; j < ports.size(); j++)
				if (ports.get(j).getConnectedNodeFlat().equals(variablesToJoin[i]))
				{
					exists = true;
					break;
				}
			
			if (!exists)
			{
				newVariables.add(variablesToJoin[i]);
			}
		}

		//Next we figure out the domain lengths of all the new variables
		DiscreteDomain [] newDomains = new DiscreteDomain[newVariables.size()];
		for (int i = 0; i < newDomains.length; i++)
			newDomains[i] = ((Discrete)newVariables.get(i)).getDiscreteDomain();
		 
		//Now, we modify the combo table to include the new variables. 
		if (newDomains.length > 0)
		{
			//getFactorFunction();
			FactorTable newTable = getFactorTable().createTableWithNewVariables(newDomains);
			setFactorFunction(new TableFactorFunction(getFactorFunction().getName(), newTable));
			
			for (VariableBase v : newVariables)
				addVariable(v);
		}		

		//Now get the indices of all the variables
		int [] factorVarIndices = new int[variablesToJoin.length];
		int [] indexToJointIndex = new int[variablesToJoin.length];
		
		//Figure out which are the new variables and store a mapping
		int index = 0;
		for (int i = 0; i < ports.size(); i++)
		{
			for (int j = 0; j < variablesToJoin.length; j++)
			{
				if (ports.get(i).getConnectedNodeFlat().equals(variablesToJoin[j]))
				{
					factorVarIndices[index] = i;
					indexToJointIndex[j] = index;
					index++;
					break;				
				}
			}
		}

		
		//Get all the domain lengths
		DiscreteDomain [] allDomains = new DiscreteDomain[ports.size()];
		for (int i = 0; i < allDomains.length; i++)
			allDomains[i] = ((Discrete)ports.get(i).getConnectedNodeFlat()).getDiscreteDomain();
		
		//Create the new combo table
		FactorTable newTable2 =  getFactorTable().joinVariablesAndCreateNewTable( 
				factorVarIndices,
				indexToJointIndex,
				allDomains,
				((Discrete)newJoint).getDiscreteDomain());
		setFactorFunction(new TableFactorFunction(getFactorFunction().getName(),newTable2));
		
		//Remove old ports.
		for (int i = 0; i < factorVarIndices.length; i++)
		{
			index = factorVarIndices[factorVarIndices.length-1-i];
			ports.remove(index);
		}
		
		//reset the ids of the Factor's ports
		for (int i = 0; i < ports.size(); i++)
		{
			ports.get(i).setId(i);
		}
		
		
		//Add the new joint variable
		addVariable(newJoint);

		//Tell all old variables to remove this factor graph. 
		for (VariableBase v : variablesToJoin)
			v.remove(this);
		
		
	}

	public String getFactorTableString() 
	{
		String s = "TableFactor [" + getLabel() + "] " + getFactorTable().toString();
		return s;
	}

}
