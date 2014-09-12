/*******************************************************************************
*   Copyright 2014 Analog Devices, Inc.
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

package com.analog.lyric.dimple.jsproxy;


/**
 * Base JavaScript/Dimple proxy wrapper.
 * <p>
 * {@code Delegate} is the type of the object to which this delegates.
 * <p>
 * @since 0.07
 * @author Christopher Barber
 */
public abstract class JSProxyObject<Delegate>
{
	final Delegate _delegate;
	
	JSProxyObject(Delegate delegate)
	{
		_delegate = delegate;
	}
	
	/*-----------------------
	 * JSProxyObject methods
	 */
	
	/**
	 * Returns the dimple object wrapped by this proxy and which does the real work.
	 * <p>
	 * This can be used to access functionality that is not otherwise exposed by the
	 * proxy layer. It is strongly recommended that the proxy API is used instead of
	 * going through the delegate object whenever possible.
	 * <p>
	 * @since 0.07
	 */
	public Delegate getDelegate()
	{
		return _delegate;
	}

	public abstract DimpleApplet getApplet();
}
