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

package com.analog.lyric.dimple.model.repeated;

import org.eclipse.jdt.annotation.Nullable;

import com.analog.lyric.dimple.model.core.Port;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.Variable;
import com.analog.lyric.dimple.solvers.interfaces.ISolverBlastFromThePastFactor;
import com.analog.lyric.dimple.solvers.interfaces.ISolverFactorGraph;
import com.analog.lyric.util.misc.Internal;

public class BlastFromThePastFactor extends Factor
{
	
	private final Port _portForOtherVariable;
	private final Variable _variableConnectedToBlast;
	
	@Internal
	public BlastFromThePastFactor(Variable varConnectedToBlast, Port portForOtherVar)
	{
		super(((Factor)portForOtherVar.getSiblingNode()).getFactorFunction());
		
		_portForOtherVariable = portForOtherVar;
		_variableConnectedToBlast = varConnectedToBlast;
	}
	
	@Override
	public Factor clone()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void createSolverObject(@Nullable ISolverFactorGraph factorGraph)
	{
		if (factorGraph != null)
		{
			factorGraph.getSolverFactor(this, true);
		}
	}
	
	
	public void advance()
	{
		((ISolverBlastFromThePastFactor)requireSolver("advance")).advance();
	}
	
	
	@Override
	public void initialize()
	{
		// Specifically remove from super class things that have to do with directedness.
		// Need to clear flags directly because we are not calling super.initialize().
		clearFlags();
	}

	@Override
	protected boolean canBeDirected()
	{
		return false;
	}
	
	@Internal
	public final Port getPortForOtherVariable()
	{
		return _portForOtherVariable;
	}
	
	@Internal
	public final Variable getVariableConnectedToBlast()
	{
		return _variableConnectedToBlast;
	}
}
