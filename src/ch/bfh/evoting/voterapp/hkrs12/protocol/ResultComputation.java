/* 
 * MobiVote
 * 
 *  MobiVote: Mobile application for boardroom voting
 *  Copyright (C) 2014 Bern
 *  University of Applied Sciences (BFH), Research Institute for Security
 *  in the Information Society (RISIS), E-Voting Group (EVG) Quellgasse 21,
 *  CH-2501 Biel, Switzerland
 * 
 *  Licensed under Dual License consisting of:
 *  1. GNU Affero General Public License (AGPL) v3
 *  and
 *  2. Commercial license
 * 
 *
 *  1. This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *
 *  2. Licensees holding valid commercial licenses for MobiVote may use this file in
 *   accordance with the commercial license agreement provided with the
 *   Software or, alternatively, in accordance with the terms contained in
 *   a written agreement between you and Bern University of Applied Sciences (BFH), 
 *   Research Institute for Security in the Information Society (RISIS), E-Voting Group (EVG)
 *   Quellgasse 21, CH-2501 Biel, Switzerland.
 * 
 *
 *   For further information contact us: http://e-voting.bfh.ch/
 * 
 *
 * Redistributions of files must retain the above copyright notice.
 */
package ch.bfh.evoting.voterapp.hkrs12.protocol;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.util.Log;

import ch.bfh.unicrypt.math.algebra.dualistic.classes.ZMod;
import ch.bfh.unicrypt.math.algebra.general.interfaces.Element;

/**
 * This class is responsible for the computation of the result
 * @author Philemon von Bergen
 *
 */
public class ResultComputation {

	private static final String TAG = ResultComputation.class.getSimpleName();
	private Thread t;
	private boolean computationTerminated;
	private Map<Element, int[]> resultMap = new ConcurrentHashMap<Element, int[]>();
	private final ExecutorService pool = Executors.newFixedThreadPool(1);
	private boolean interrupt;
	private Element searchedResult = null;
	private int numberOfCombination = 0;

	/**
	 * Start the computation of the discrete logarithm
	 * @param maxVotes total number of votes submitted
	 * @param nbrOptions number of possible options
	 * @param possiblePlainTexts the possible plain texts corresponding to the options
	 * @param generator the generator used as base of the discrete logarithm
	 * @param Z_q the Zq group in which the number of votes must be represented
	 */
	public void startComputation(final int maxVotes, final int nbrOptions, final Element[] possiblePlainTexts, final Element generator, final ZMod Z_q){
		numberOfCombination=0;
		t = new Thread(){

			public void run() {
				Log.d(TAG,"Starting computing possible results");
				long time0 = System.currentTimeMillis();
				computePossibleResults(maxVotes,nbrOptions, possiblePlainTexts, generator, Z_q);
				long time1 = System.currentTimeMillis();
				computationTerminated = true;
				synchronized (ResultComputation.this) {
					ResultComputation.this.notifyAll();
				}
				if(interrupt){
					Log.d(TAG,"Computation interrupted after "+numberOfCombination+" combinations and "+(time1-time0)+" ms");
				} else {
					Log.d(TAG,"Computation terminated in "+(time1-time0)+" ms for "+numberOfCombination+" combinations");
				}
			}
		};
		t.start();
	}

	
	
	/**
	 * Method returning a future that can be used to get the result when it is computed
	 * @param searchedResult discrete logarithm to compute
	 * @return the array representing the votes for the options
	 * @throws IOException if an error occurred with the future
	 */
	public Future<int[]> compareResults(final Element searchedResult) throws IOException {
	    return pool.submit(new Callable<int[]>() {
	        @Override
	        public int[] call() throws Exception {
				Log.d(TAG,"Setting searched discrete logarithm");
	        	ResultComputation.this.searchedResult = searchedResult;
	        	while(true){
	        		int[] result = resultMap.get(searchedResult);
	        		if(result!=null){
	    	        	pool.shutdown();
	        			return result;
	        		} else if (computationTerminated) {
	    	        	pool.shutdown();
	        			return null;
	        		} else {
	        			synchronized (ResultComputation.this) {
	        				Log.d(TAG,"Waiting as searched discrete logarithm not already comuted");
	        				ResultComputation.this.wait();
	        				Log.d(TAG,"Notified that searched discrete logarithm was just comuted");
						}
	        		}
	        	}
	        }
	    });
	}
	
	/**
	 * Compute all combination of divide up MAX votes between Array.length candidate 
	 * Optimized for most of the votes in first columns
	 * @param array Array containing the possible combination
	 * @param resting Resting number of vote to divide up
	 * @param max Number of vote to divide up in total
	 * @param idx Index of the column (start with 0)
	 * @param result Obtained result of votes to find
	 */	
	private boolean computePossibleResults(int numberOfVotes, int numberOfOptions, Element[] possiblePlainTexts, Element generator, ZMod Z_q) {

		//initialize array containing possible combination and the array receiving the result
		int[] voteForCandidates = new int[numberOfOptions];
		for(int i=0; i<voteForCandidates.length;i++){
			voteForCandidates[i]=0;
		}

		computePossibleResultsRecursion(voteForCandidates,numberOfVotes,numberOfVotes,0, possiblePlainTexts, generator, Z_q);
		return true;
	}

	/**
	 * Compute all combination of divide up MAX votes between Array.length candidate 
	 * Optimized for most of the votes in first columns
	 * @param array Array containing the possible combination
	 * @param resting Resting number of vote to divide up
	 * @param max Number of vote to divide up in total
	 * @param idx Index of the column (start with 0)
	 * @param result Obtained result of votes to find
	 */	
	private void computePossibleResultsRecursion(int[] array, int resting, int max, int idx,  Element[] possiblePlainTexts, Element generator, ZMod Z_q) {
		if(interrupt) return;
		
		//stop condition for the recursion
		//we have reached the last column
		if (idx == array.length) {
			//if the number of votes attributed < max => not interesting for us
			if(arraySum(array)<max)return;
			//compare combination and result of tally
			Element tempResult = Z_q.getElement(BigInteger.ZERO);
			for(int j=0;j<array.length;j++){
				tempResult = tempResult.apply(possiblePlainTexts[j].selfApply(Z_q.getElement(BigInteger.valueOf(array[j]))));
			}
			resultMap.put(generator.selfApply(tempResult), array.clone());
		
			numberOfCombination++;
			
			//if searched result is set, look if it is already found
			if(searchedResult!=null){
				if(resultMap.containsKey(searchedResult)){
					synchronized (ResultComputation.this) {
						ResultComputation.this.notifyAll();
					}
					interrupt = true;
				} else {
					//reinit
					resultMap.clear();
				}
			}
			return;
		}
		//else put a value at the index and call recursion for the other columns
		for (int i = resting; i >= 0; i--) {
			if(interrupt) break;
			array[idx] = i;
			computePossibleResultsRecursion(array, resting-i, max, idx+1, possiblePlainTexts, generator, Z_q);
		}
		return;
	}

	/**
	 * Make the sum of each element of the array
	 * Warning: this method is sensible to integer overflow if the number a great enough
	 * @param array the array to sum up
	 * @return the sum
	 */
	private int arraySum(int[] array){
		int sum=0;
		for(int i=0;i<array.length;i++){
			sum+=array[i];
		}
		return sum;
	}

	/**
	 * Method called when computation process must be interrupted
	 */
	public void interrupt() {
		Log.d(TAG,"Interrupt discrete logarithm computation process");
		this.interrupt = true;
		pool.shutdown();
	}
}
