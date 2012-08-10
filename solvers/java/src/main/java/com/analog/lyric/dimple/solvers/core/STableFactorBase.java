package com.analog.lyric.dimple.solvers.core;

import com.analog.lyric.dimple.FactorFunctions.core.FactorTable;
import com.analog.lyric.dimple.model.DimpleException;
import com.analog.lyric.dimple.model.Factor;
import com.analog.lyric.dimple.solvers.interfaces.ISolverTableFactor;

public abstract class STableFactorBase extends SFactorBase implements ISolverTableFactor
{
	//protected Factor _factor;
	//protected TableFactor _tableFactor;
	protected final FactorTable _factorTable;

	public STableFactorBase(Factor factor) 
	{
		super(factor);
		
		if (!factor.isDiscrete())
			throw new DimpleException("only discrete factors supported");
		
		_factorTable = factor.getFactorFunction().getFactorTable(factor.getDomains());

		//if (!factor.isDiscreteFactor())
		//	throw new Exception
		
		//_tableFactor = (TableFactor)factor;
	}
	
	public final FactorTable getFactorTable()
	{
		return this._factorTable;
	}

	@Override
	public int [][] getPossibleBeliefIndices() 
	{
		return _factorTable.getIndices();
	}

}
