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

package com.analog.lyric.dimple.solvers.core.multithreading.alg1;

import java.util.concurrent.LinkedBlockingQueue;

import com.analog.lyric.dimple.model.DimpleException;
import com.analog.lyric.dimple.model.FactorGraph;
import com.analog.lyric.dimple.schedulers.dependencyGraph.crossiteration.DependencyGraphNode;
import com.analog.lyric.dimple.schedulers.dependencyGraph.crossiteration.ScheduleDependencyGraph;
import com.analog.lyric.dimple.schedulers.scheduleEntry.IScheduleEntry;
import com.analog.lyric.dimple.solvers.core.multithreading.MultiThreadingManager;
import com.analog.lyric.dimple.solvers.core.multithreading.MultithreadingAlgorithm;

/*
 * This is the original algorithm written for multithreading Dimple.  It unrolls across iterations,
 * builds a dependency graph, and then uses a single work queue to get work items.
 * It could probably be sped up by using the newer dependency graph classes which can be constructed much more quickly.
 */
public class SingleQueueCrossIterationMultithreadingAlgorithm extends MultithreadingAlgorithm 
{
	protected ScheduleDependencyGraph _scheduleDependencyGraph;
	protected long _graphVersionIdWhenLastBuilt = -1;
	protected long _scheduleVersionIdWhenLastBuilt = -1;
	protected long _iterationsWhenLastBuilt = -1;
	protected int _numScheduleEntriesRemaining;
	protected LinkedBlockingQueue<DependencyGraphNode<IScheduleEntry>> _workQueue = new LinkedBlockingQueue<DependencyGraphNode<IScheduleEntry>>();
	protected Thread[] _solverSubThreads;
	protected WaitNotify _waitNotify = new WaitNotify();
	protected Exception _subThreadException = null;


	public SingleQueueCrossIterationMultithreadingAlgorithm(MultiThreadingManager manager) 
	{
		super(manager);
	}
	
	public void setSubThreadException(Exception e)
	{
		_subThreadException = e;
	}
	
	public WaitNotify getWaitNotify()
	{
		return _waitNotify;
	}
	
	// Get the work queue
	protected LinkedBlockingQueue<DependencyGraphNode<IScheduleEntry>> getWorkQueue()
	{
		return _workQueue;
	}


	/*
	 * Performs the work
	 */
	@Override
	public void iterate(int numIters) 
	{
		//Build the dependency graph if it hasn't already been built.
		prepareForMultiThreading(numIters);	// Create _scheduleDependencyGraph if not already up-to-date
		_numScheduleEntriesRemaining = _scheduleDependencyGraph.size();
		_scheduleDependencyGraph.initialize();

		// Initialize the work queue
		_workQueue.clear();
		for (DependencyGraphNode<IScheduleEntry> entry : _scheduleDependencyGraph.getRootList())	// Start with the roots
			_workQueue.add(entry);

		// Create and start the threads
		_solverSubThreads = new Thread[getManager().getNumThreads()];
		for (int i = 0; i < getManager().getNumThreads(); i++)
		{
			_solverSubThreads[i] = new Thread(new SingleQueueWorker(this));
			_solverSubThreads[i].start();
		}

		// Wait for notification of completion, then stop all the sub-threads
		_waitNotify.doWait();
		for (Thread thread : _solverSubThreads)
			thread.interrupt();

		// If there was an exception in any of the sub-threads, pass it along
		if (_subThreadException != null)
			throw new DimpleException(_subThreadException.getMessage());		
	}
	
	public void prepareForMultiThreading()  
	{
		prepareForMultiThreading(getManager().getNumThreads());
	}
	
 	public void prepareForMultiThreading(int numIters)
	{
 		FactorGraph factorGraph = getManager().getFactorGraph();
		// Create a schedule dependency graph, but only if necessary
		if (
				(_scheduleDependencyGraph == null) ||
				!getManager().getFactorGraph().isUpToDateSchedulePresent() ||
				(_graphVersionIdWhenLastBuilt != factorGraph.getVersionId()) ||
				(_scheduleVersionIdWhenLastBuilt != factorGraph.getScheduleVersionId()) ||
				(_iterationsWhenLastBuilt != numIters))
		{
			_scheduleDependencyGraph = new ScheduleDependencyGraph(factorGraph, numIters);
			_graphVersionIdWhenLastBuilt = factorGraph.getVersionId();
			_scheduleVersionIdWhenLastBuilt = factorGraph.getScheduleVersionId();
			_iterationsWhenLastBuilt = numIters;
		}
	}

	// Callback from the threads; must be synchronized
	protected synchronized void scheduleEntryCompleted(DependencyGraphNode<IScheduleEntry> entry)
	{
		// Mark this entry completed
		entry.markCompleted();

		// Are we done?
		if (--_numScheduleEntriesRemaining == 0)
		{
			_waitNotify.doNotify();		// Notify the parent thread that we're done
			return;
		}

		// Run through the dependents of this node and see if they are ready to add to the work queue
		for (DependencyGraphNode<IScheduleEntry> dependentEntry : entry.getDependents())
			if (dependentEntry.allDependenciesMet())	// If all dependencies have now been met, the add it to the work queue
				_workQueue.add(dependentEntry);
	}
	
	protected class MonitorObject {}
	public class WaitNotify
	{
		MonitorObject _monitorObject = new MonitorObject();
		boolean _wasSignalled = false;

		public void doWait()
		{
			synchronized(_monitorObject)
			{
				while(!_wasSignalled)
				{
					try{_monitorObject.wait();}
					catch(InterruptedException e){throw new DimpleException(e.getMessage());}
				}
				_wasSignalled = false;
			}
		}

		public void doNotify()
		{
			synchronized (_monitorObject)
			{
				_wasSignalled = true;
				_monitorObject.notify();
			}
		}
	}

}
