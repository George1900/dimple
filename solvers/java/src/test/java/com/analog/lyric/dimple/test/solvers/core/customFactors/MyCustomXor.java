/*******************************************************************************
*   Copyright 2015 Analog Devices, Inc.
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

package com.analog.lyric.dimple.test.solvers.core.customFactors;

import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.solvers.interfaces.ISolverFactorGraph;

/**
 * For use in TestCustomFactors
 * @since 0.08
 * @author Christopher Barber
 */
public class MyCustomXor extends MyCustomFactor
{
	public MyCustomXor(Factor factor, ISolverFactorGraph parent)
	{
		this(factor, parent, "xor");
	}
	
	public MyCustomXor(Factor factor, ISolverFactorGraph parent, String tag)
	{
		super(factor, parent, tag);
	}

	@Override
	protected void doUpdateEdge(int edge)
	{
	}

}
