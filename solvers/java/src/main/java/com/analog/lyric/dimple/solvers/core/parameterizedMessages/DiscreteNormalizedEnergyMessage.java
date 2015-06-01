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

package com.analog.lyric.dimple.solvers.core.parameterizedMessages;

import net.jcip.annotations.NotThreadSafe;

/**
 * DiscreteMessage with energy parameterization and normalization energy support.
 * <p>
 * This extends {@link DiscreteEnergyMessage} with support for storing the
 * {@linkplain #getNormalizationEnergy() normalization energy}.
 * <p>
 * @since 0.08
 * @author Christopher Barber
 */
@NotThreadSafe
public class DiscreteNormalizedEnergyMessage extends DiscreteEnergyMessage
{
	/*-------
	 * State
	 */
	
	private static final long serialVersionUID = 1L;

	private double _normalizationEnergy = 0.0;
	
	/*--------------
	 * Construction
	 */
	
	public DiscreteNormalizedEnergyMessage(double[] message, double normalizationEnergy)
	{
		super(message);
		_normalizationEnergy = normalizationEnergy;
	}
	
	public DiscreteNormalizedEnergyMessage(double[] message)
	{
		super(message);
	}
	
	public DiscreteNormalizedEnergyMessage(int size)
	{
		super(size);
	}
	
	@Override
	public DiscreteNormalizedEnergyMessage clone()
	{
		return new DiscreteNormalizedEnergyMessage(_message, _normalizationEnergy);
	}
	
	/*-------------------------
	 * DiscreteMessage methods
	 */

	@Override
	public double getNormalizationEnergy()
	{
		return _normalizationEnergy;
	}
	
	@Override
	public void setNormalizationEnergy(double normalizationEnergy)
	{
		_normalizationEnergy = normalizationEnergy;
	}
	
	@Override
	public final boolean storesNormalizationEnergy()
	{
		return true;
	}
	
	@Override
	protected void setNormalizationEnergyIfSupported(double normalizationEnergy)
	{
		_normalizationEnergy = normalizationEnergy;
	}
	
	@Override
	protected void incrementNormalizationEnergy(double additionalNormalizationEnergy)
	{
		_normalizationEnergy += additionalNormalizationEnergy;
	}
}
