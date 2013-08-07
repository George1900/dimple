package com.analog.lyric.dimple.test.model;

import static com.analog.lyric.math.Utilities.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.analog.lyric.collect.ArrayUtil;
import com.analog.lyric.dimple.model.DimpleException;
import com.analog.lyric.dimple.model.DiscreteDomain;
import com.analog.lyric.dimple.model.DiscreteDomainList;
import com.analog.lyric.dimple.model.DiscreteDomainListConverter;
import com.analog.lyric.util.test.SerializationTester;

public class TestDiscreteDomainList
{
	private Random rand = new Random(1323);
	
	@Test
	public void test()
	{
		DiscreteDomain d2 = DiscreteDomain.intRangeFromSize(2);
		DiscreteDomain d3 = DiscreteDomain.intRangeFromSize(3);
	
		try
		{
			DiscreteDomainList.create();
			fail("should not get here");
		}
		catch (DimpleException ex)
		{
		}
		try
		{
			DiscreteDomainList.create((DiscreteDomain[])null);
			fail("should not get here");
		}
		catch (DimpleException ex)
		{
		}
		
		DiscreteDomainList dl2by3 = DiscreteDomainList.create(d2, d3);
		testInvariants(dl2by3);
		assertEquals(dl2by3, DiscreteDomainList.create(d2, d3));
		assertNotEquals(dl2by3, DiscreteDomainList.create(d3, d2));
		
		DiscreteDomainList dl2to3 = DiscreteDomainList.create(new int[] { 0 }, new DiscreteDomain[] {d2, d3 });
		testInvariants(dl2to3);
		assertNotEquals(dl2by3, dl2to3);
		assertNotEquals(dl2to3, dl2by3);
		assertNotEquals(dl2by3.hashCode(), dl2to3.hashCode());
	}
	
	public static void testInvariants(DiscreteDomainList domainList)
	{
		assertTrue(domainList.equals(domainList));
		assertFalse(domainList.equals("foo"));
		
		final int size = domainList.size();
		assertTrue(size > 0);
		
		final int inSize = domainList.getInputSize();
		assertTrue(inSize >= 0);
		assertTrue(inSize < size);
		
		final int outSize = domainList.getOutputSize();
		assertTrue(outSize >= 0);
		assertTrue(outSize <= size);
		assertEquals(size, inSize + outSize);
		
		final int cardinality = domainList.getCardinality();
		assertTrue(cardinality > 1);
		
		final int inCardinality = domainList.getInputCardinality();
		assertTrue(inCardinality >= 1);
		assertTrue(inCardinality <= cardinality);
		
		final int outCardinality = domainList.getOutputCardinality();
		assertTrue(outCardinality >= 1);
		assertTrue(outCardinality <= cardinality);
		assertEquals(cardinality, inCardinality * outCardinality);
		
		int i = 0;
		for (DiscreteDomain domain : domainList)
		{
			assertSame(domain, domainList.get(i));
			assertEquals(domain.size(), domainList.getDomainSize(i));
			++i;
		}
		assertEquals(size, i);
		
		BitSet inSet = domainList.getInputSet();
		BitSet outSet = domainList.getOutputSet();
		
		int[] inIndices = domainList.getInputIndices();
		int[] outIndices = domainList.getOutputIndices();

		final int[] indices = new int[size], indices2 = new int[size];
		final Object[] elements = new Object[size], elements2 = new Object[size];
		
		for (i = 0; i < cardinality; ++i)
		{
			assertSame(indices, domainList.undirectedJointIndexToIndices(i, indices));
			assertArrayEquals(indices, domainList.undirectedJointIndexToIndices(i, null));
			assertArrayEquals(indices, domainList.undirectedJointIndexToIndices(i, new int[0]));
			assertSame(elements, domainList.undirectedJointIndexToElements(i, elements));
			assertArrayEquals(elements, domainList.undirectedJointIndexToElements(i, null));
			assertArrayEquals(elements, domainList.undirectedJointIndexToElements(i, new Object[0]));
			for (int j = 0; j < size; ++ j)
			{
				assertTrue(indices[j] >= 0);
				assertTrue(indices[j] < domainList.getDomainSize(j));
				assertEquals(elements[j], domainList.get(j).getElement(indices[j]));
			}
			
			assertEquals(i, domainList.undirectedJointIndexFromElements(elements));
			assertEquals(i, domainList.undirectedJointIndexFromIndices(indices));
			
			int in = domainList.inputIndexFromIndices(indices);
			assertEquals(in, domainList.inputIndexFromElements(elements));
			
			int out = domainList.outputIndexFromIndices(indices);
			assertEquals(out, domainList.outputIndexFromElements(elements));
			
			int ji = domainList.jointIndexFromIndices(indices);
			assertEquals(ji, domainList.jointIndexFromElements(elements));
			
			Arrays.fill(indices2, -1);
			Arrays.fill(elements2, null);
			assertSame(indices2, domainList.jointIndexToIndices(ji, indices2));
			assertArrayEquals(indices, indices2);
			assertSame(elements2, domainList.jointIndexToElements(ji, elements2));
			assertArrayEquals(elements, elements2);
			
			if (!domainList.isDirected())
			{
				assertEquals(ji, i);
			}
			
			assertEquals(out + in * domainList.getOutputCardinality(), ji);
			
			Arrays.fill(indices2, -1);
			Arrays.fill(elements2, null);
			domainList.inputIndexToIndices(in, indices2);
			assertEquals(in, domainList.inputIndexFromIndices(indices2));
			domainList.inputIndexToElements(in, elements2);
			assertEquals(in, domainList.inputIndexFromElements(elements2));
			if (domainList.isDirected())
			{
			}
			else
			{
				for (int j : indices2)
				{
					assertEquals(-1, j);
				}
			}
			
			Arrays.fill(indices2, -1);
			Arrays.fill(elements2, null);
			domainList.outputIndexToIndices(out, indices2);
			assertEquals(out, domainList.outputIndexFromIndices(indices2));
			domainList.outputIndexToElements(out, elements2);
			assertEquals(out, domainList.outputIndexFromElements(elements2));
			if (domainList.isDirected())
			{
			}
			else
			{
				for (int j = 0; j < size; ++j)
				{
					assertEquals(indices[j], indices2[j]);
				}
			}
		}
		
		if (domainList.isDirected())
		{
			assertFalse(inSet.intersects(outSet));
			assertEquals(size, inSet.cardinality(), outSet.cardinality());
			assertNotSame(inSet, domainList.getInputSet());
			assertEquals(inSet, domainList.getInputSet());
			assertNotSame(outSet, domainList.getOutputSet());
			assertEquals(outSet, domainList.getOutputSet());
			
			assertEquals(inIndices.length, inSet.cardinality());
			assertEquals(outIndices.length, outSet.cardinality());
			assertEquals(inIndices.length, domainList.getInputSize());
			assertEquals(outIndices.length, domainList.getOutputSize());
			
			for (int j = outIndices.length; --j>=0;)
			{
				assertEquals(outIndices[j], domainList.getOutputIndex(j));
				assertTrue(outSet.get(outIndices[j]));
			}
			for (int j = inIndices.length; --j>=0;)
			{
				assertEquals(inIndices[j], domainList.getInputIndex(j));
				assertTrue(inSet.get(inIndices[j]));
			}
		}
		else
		{
			assertEquals(1, inCardinality);
			assertEquals(0, inSize);
			assertNull(inSet);
			assertNull(outSet);
			assertNull(inIndices);
			assertNull(outIndices);
			
			for (i = 0; i < size; ++i)
			{
				assertEquals(i, domainList.getOutputIndex(i));
			}
			
			try
			{
				domainList.getInputIndex(0);
				fail("should not get here");
			}
			catch (ArrayIndexOutOfBoundsException ex)
			{
			}
			try
			{
				domainList.getOutputIndex(-1);
				fail("should not get here");
			}
			catch (ArrayIndexOutOfBoundsException ex)
			{
			}
			try
			{
				domainList.getOutputIndex(size);
				fail("should not get here");
			}
			catch (ArrayIndexOutOfBoundsException ex)
			{
			}
		}
		
		DiscreteDomainList domainList2 = SerializationTester.clone(domainList);
		assertEquals(domainList, domainList2);
		assertEquals(domainList.hashCode(), domainList2.hashCode());
	}
	
	@Test
	public void testConverter()
	{
		DiscreteDomain d2 = DiscreteDomain.intRangeFromSize(2);
		DiscreteDomain d3 = DiscreteDomain.intRangeFromSize(3);
		DiscreteDomain d4 = DiscreteDomain.intRangeFromSize(4);
		DiscreteDomain d5 = DiscreteDomain.intRangeFromSize(5);
		
		DiscreteDomainList dl2by3 = DiscreteDomainList.create(d2, d3);
		DiscreteDomainList dl3by2 = DiscreteDomainList.create(d3, d2);
		
		// A simple permutation
		DiscreteDomainListConverter dl2by3_to_dl3by2 =
			DiscreteDomainListConverter.createPermuter(dl2by3, null,  dl3by2,  null, new int[] { 1, 0});
		assertSame(dl2by3, dl2by3_to_dl3by2.getFromDomains());
		assertSame(dl3by2, dl2by3_to_dl3by2.getToDomains());
		testInvariants(dl2by3_to_dl3by2);
		assertNotEquals(dl2by3_to_dl3by2, dl2by3_to_dl3by2.getInverse());
		assertNotEquals(dl2by3_to_dl3by2.hashCode(), dl2by3_to_dl3by2.getInverse().hashCode());
		
		DiscreteDomainListConverter dl3by2_to_dl2by3 =
			DiscreteDomainListConverter.createPermuter(dl3by2, null,  dl2by3,  null, new int[] { 1, 0});
		testInvariants(dl3by2_to_dl2by3);
		assertEquals(dl2by3_to_dl3by2, dl3by2_to_dl2by3.getInverse());
		assertEquals(dl3by2_to_dl2by3, dl2by3_to_dl3by2.getInverse());
		assertEquals(dl2by3_to_dl3by2.hashCode(), dl3by2_to_dl2by3.getInverse().hashCode());
		
		// Remove a domain
		DiscreteDomainList dl2 = DiscreteDomainList.create(d2);
		DiscreteDomainListConverter dl2by3_to_dl3 = DiscreteDomainListConverter.createRemover(dl2by3, 0);
		assertSame(dl2by3, dl2by3_to_dl3.getFromDomains());
		assertEquals(dl2, dl2by3_to_dl3.getRemovedDomains());
		testInvariants(dl2by3_to_dl3);
		assertNotEquals(dl2by3_to_dl3by2, dl2by3_to_dl3);
		assertNotEquals(dl2by3_to_dl3by2.hashCode(), dl2by3_to_dl3.hashCode());
		
		DiscreteDomainListConverter dl2by3_to_dl2 = DiscreteDomainListConverter.createRemover(dl2by3, 1);
		assertSame(dl2by3, dl2by3_to_dl2.getFromDomains());
		assertEquals(dl2, dl2by3_to_dl2.getToDomains());
		testInvariants(dl2by3_to_dl2);
		
		DiscreteDomainList dl2by3by4by5 = DiscreteDomainList.create(d2, d3, d4, d5);
		DiscreteDomainListConverter dl2by3by4by5_to_dl2by = DiscreteDomainListConverter.createJoiner(dl2by3by4by5, 1, 2);
		DiscreteDomainList dl2by12by5 = dl2by3by4by5_to_dl2by.getToDomains();
		testInvariants(dl2by3by4by5_to_dl2by);
		assertEquals(3, dl2by12by5.size());
		assertEquals(12, dl2by12by5.getDomainSize(1));
		assertNotEquals(dl2by3_to_dl3by2, dl2by3by4by5_to_dl2by);
		assertNotEquals(dl2by3by4by5_to_dl2by, dl2by3_to_dl3);
		assertNotEquals(dl2by3by4by5_to_dl2by, dl2by3by4by5_to_dl2by.getInverse());
		assertNotEquals(dl2by3by4by5_to_dl2by.hashCode(), dl2by3by4by5_to_dl2by.getInverse().hashCode());
		
		DiscreteDomainListConverter dl2by12by5_to_dl2by3by4by5 = DiscreteDomainListConverter.createSplitter(dl2by12by5, 1);
		testInvariants(dl2by12by5_to_dl2by3by4by5);
		assertEquals(dl2by3by4by5_to_dl2by, dl2by12by5_to_dl2by3by4by5.getInverse());
		
		// Chain
		DiscreteDomainListConverter dl3by2_to_dl3 = dl3by2_to_dl2by3.combineWith(dl2by3_to_dl3);
		testInvariants(dl3by2_to_dl3);
		
		try
		{
			dl3by2_to_dl3.combineWith(dl2by12by5_to_dl2by3by4by5);
			fail("expected exception");
		}
		catch (DimpleException ex)
		{
		}
	}
	
	public void testInvariants(DiscreteDomainListConverter converter)
	{
		testInvariants(converter, true);
	}
	
	private void testInvariants(DiscreteDomainListConverter converter, boolean testInverse)
	{
		assertEquals(converter, converter);

		DiscreteDomainListConverter inverse = converter.getInverse();
		assertEquals(converter, inverse.getInverse());
		
		DiscreteDomainListConverter.Indices indices = converter.getScratch();
		assertSame(converter, indices.converter);
		assertEquals(converter.getFromDomains().size(), indices.fromIndices.length);
		assertEquals(converter.getToDomains().size(), indices.toIndices.length);
		if (converter.getAddedDomains() == null)
		{
			assertSame(ArrayUtil.EMPTY_INT_ARRAY, indices.addedIndices);
		}
		else
		{
			assertEquals(converter.getAddedDomains().size(), indices.addedIndices.length);
		}
		if (converter.getRemovedDomains() == null)
		{
			assertSame(ArrayUtil.EMPTY_INT_ARRAY, indices.removedIndices);
		}
		else
		{
			assertEquals(converter.getRemovedDomains().size(), indices.removedIndices.length);
		}
		
		indices.release();
		assertSame(indices, converter.getScratch());
		assertNotSame(indices, converter.getScratch());
		indices = converter.getScratch();
		
		final int maxFrom = converter.getFromDomains().getCardinality();
		final int maxAdded = converter.getAddedCardinality();
		
		final AtomicInteger removedRef = new AtomicInteger();
		final AtomicInteger removedRef2 = new AtomicInteger();
		final AtomicInteger addedRef = new AtomicInteger();
		
		double[] fromDenseWeights = new double[maxFrom];
		double[] fromDenseEnergies = new double[maxFrom];
		for (int i = 0; i < maxFrom; ++i)
		{
			double w = rand.nextDouble();
			fromDenseWeights[i] = w;
			fromDenseEnergies[i] = weightToEnergy(w);
		}
		
		final int maxTo = converter.getToDomains().getCardinality();
		final int maxRemoved = converter.getRemovedCardinality();
		
		final double[] toDenseWeights = converter.convertDenseWeights(fromDenseWeights);
		assertEquals(maxTo, toDenseWeights.length);
		double[] toDenseEnergies = converter.convertDenseEnergies(fromDenseEnergies);
		assertEquals(maxTo, toDenseEnergies.length);
		
		for (int from = 0; from < maxFrom; ++from)
		{
			double fromWeight = fromDenseWeights[from];
			double fromEnergy = fromDenseEnergies[from];
			
			for (int added = 0; added < maxAdded; ++added)
			{
				int to = converter.convertJointIndex(from,  added, null);
				assertEquals(to, converter.convertJointIndex(from, added));
				assertEquals(to, converter.convertJointIndex(from, added, removedRef));
				
				assertEquals(from, inverse.convertJointIndex(to, removedRef.get(), null));
				assertEquals(from, inverse.convertJointIndex(to, removedRef.get(), addedRef));
				assertEquals(added, addedRef.get());
				
				indices.writeIndices(from, added);
				converter.convertIndices(indices);
				int to2 = indices.readIndices(null);
				assertEquals(to, to2);
				assertEquals(to, indices.readIndices(removedRef2));
				assertEquals(removedRef.get(), removedRef2.get());
				
				if (maxRemoved == 1)
				{
					assertEquals(fromWeight, toDenseWeights[to], 0.0);
					assertEquals(fromEnergy, toDenseEnergies[to], 0.0);
				}
				else
				{
					// Weight must be equal sum of entries mapping to this one
					double weightSum = 0.0;
					for (int removed = 0; removed < maxRemoved; ++removed)
					{
						int fromInverse = inverse.convertJointIndex(to, removed);
						weightSum += fromDenseWeights[fromInverse];
					}
					assertEquals(weightSum, toDenseWeights[to], 1e-12);
					assertEquals(weightToEnergy(weightSum), toDenseEnergies[to], 1e-12);
				}
			}
		}
		
		//
		// Test sparse conversions
		//
		
		// A "dense" sparse to joint index.
		final int[] fromDenseSparseToJoint = new int[maxFrom];
		for (int i = 0; i < maxFrom; ++i)
		{
			fromDenseSparseToJoint[i] = i;
		}
		final int[] toDenseSparseToJoint = converter.convertSparseToJointIndex(fromDenseSparseToJoint);
		assertEquals(maxTo, toDenseSparseToJoint.length);
		for (int i = toDenseSparseToJoint.length; --i>=0;)
		{
			assertEquals(i, toDenseSparseToJoint[i]);
		}
		assertArrayEquals(
			toDenseWeights,
			converter.convertSparseWeights(fromDenseWeights, fromDenseSparseToJoint, toDenseSparseToJoint),
			1e-12);
		assertArrayEquals(
			toDenseEnergies,
			converter.convertSparseEnergies(fromDenseEnergies, fromDenseSparseToJoint, toDenseSparseToJoint),
			1e-12);
		
		// Test a random sparse selection
		BitSet sparseSet = new BitSet(maxFrom);
		for (int i = maxFrom/2; --i>=0;)
		{
			sparseSet.set(rand.nextInt(maxFrom));
		}
		final int[] fromSparseToJoint = new int[sparseSet.cardinality()];
		for (int i = 0, sparseIndex = -1; i < fromSparseToJoint.length; ++i)
		{
			sparseIndex = sparseSet.nextSetBit(sparseIndex+1);
			fromSparseToJoint[i] = sparseIndex;
		}
		final int[] toSparseToJoint = converter.convertSparseToJointIndex(fromSparseToJoint);
		for (int oldSparse : fromSparseToJoint)
		{
			for (int added = 0; added < maxAdded; ++added)
			{
				int newSparse = converter.convertJointIndex(oldSparse, added);
				assertTrue(Arrays.binarySearch(toSparseToJoint, newSparse) >= 0);
			}
		}
		final double[] fromSparseWeights = new double[fromSparseToJoint.length];
		final double[] fromSparseEnergies = new double[fromSparseToJoint.length];
		for (int si = fromSparseToJoint.length; --si>=0;)
		{
			int ji = fromSparseToJoint[si];
			fromSparseWeights[si] = fromDenseWeights[ji];
			fromSparseEnergies[si] = fromDenseEnergies[ji];
		}
		final double[] toSparseWeights =
			converter.convertSparseWeights(fromSparseWeights,  fromSparseToJoint, toSparseToJoint);
		final double[] toSparseEnergies =
			converter.convertSparseEnergies(fromSparseEnergies,  fromSparseToJoint, toSparseToJoint);
		for (int si = toSparseToJoint.length; --si>=0;)
		{
			int ji = toSparseToJoint[si];
			if (maxRemoved == 1)
			{
				assertEquals(toDenseWeights[ji], toSparseWeights[si], 1e-12);
				assertEquals(toDenseEnergies[ji], toSparseEnergies[si], 1e-12);
			}
			else
			{
				// Weight must be equal sum of entries mapping to this one
				double weightSum = 0.0;
				for (int removed = 0; removed < maxRemoved; ++removed)
				{
					int fromInverse = inverse.convertJointIndex(ji, removed);
					if (Arrays.binarySearch(fromSparseToJoint, fromInverse) >= 0)
					{
						weightSum += fromDenseWeights[fromInverse];
					}
				}
				assertEquals(weightSum, toSparseWeights[si], 1e-12);
				assertEquals(weightToEnergy(weightSum), toSparseEnergies[si], 1e-12);
			}
		}
		
		if (testInverse)
		{
			testInvariants(inverse, false);
		}
	}
}