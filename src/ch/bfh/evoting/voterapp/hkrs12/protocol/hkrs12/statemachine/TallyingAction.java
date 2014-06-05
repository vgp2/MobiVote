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
package ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.statemachine;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import ch.bfh.evoting.voterapp.hkrs12.DisplayResultActivity;
import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.evoting.voterapp.hkrs12.entities.Participant;
import ch.bfh.evoting.voterapp.hkrs12.protocol.HKRS12ProtocolInterface;
import ch.bfh.evoting.voterapp.hkrs12.protocol.ResultComputationWithPrecomputation;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.ProtocolMessageContainer;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.ProtocolOption;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.ProtocolParticipant;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.ProtocolPoll;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.statemachine.StateMachineManager.Round;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
import ch.bfh.evoting.voterapp.hkrs12.util.ObservableTreeMap;
import ch.bfh.unicrypt.math.algebra.general.interfaces.Element;

import com.continuent.tungsten.fsm.core.Entity;
import com.continuent.tungsten.fsm.core.Event;
import com.continuent.tungsten.fsm.core.FiniteStateException;
import com.continuent.tungsten.fsm.core.Transition;
import com.continuent.tungsten.fsm.core.TransitionFailureException;
import com.continuent.tungsten.fsm.core.TransitionRollbackException;

/**
 * Action executed in Tally step
 * @author Philemon von Bergen
 * 
 */
public class TallyingAction extends AbstractAction {

	private ResultComputationWithPrecomputation rc;

	public TallyingAction(Context context, String messageTypeToListenTo,
			final ProtocolPoll poll) {
		super(context, messageTypeToListenTo, poll, 0);

		//start to compute possible results
		createPrecomputationTask();

		((ObservableTreeMap<String, Participant>)poll.getExcludedParticipants()).addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent event) {
				createPrecomputationTask();
			}
		});

		((ObservableTreeMap<String, Participant>)poll.getCompletelyExcludedParticipants()).addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent event) {
				createPrecomputationTask();
			}
		});
	}

	@Override
	public void doAction(Event message, Entity entity, Transition transition,
			int actionType) throws TransitionRollbackException,
			TransitionFailureException {

		Log.d(TAG,"Tally started");
		
		AndroidApplication.getInstance().setVoteRunning(false);

		//compute result
		Element product = poll.getG_q().getElement(BigInteger.valueOf(1));
		Collection<Participant> activeParticipants = new ArrayList<Participant>();
		for(Participant p : poll.getParticipants().values()){
			activeParticipants.add(p);
		}
		activeParticipants.removeAll(poll.getExcludedParticipants().values());
		activeParticipants.removeAll(poll.getCompletelyExcludedParticipants().values());
		for(Participant p : activeParticipants){

			ProtocolParticipant p2 = (ProtocolParticipant)p;
			if(poll.getExcludedParticipants().isEmpty()){
				product=product.apply(p2.getBi());
			} else {
				product=product.apply(p2.getBi());
				product=product.apply(p2.getHiHatPowXi());
			}
		}

		//try to find combination corresponding to the computed result

		Future<int[]> compareFuture = null;
		try {
			compareFuture = rc.compareResults(product);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		int[] result = null;
		try {
			result = compareFuture.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		if(result!=null){
			Log.d(TAG, "Result is "+Arrays.toString(result));
			float sum = arraySum(result);
			int i=0;
			for(Option op : poll.getOptions()){
				op.setVotes(result[i]);
				if(sum!=0){
					op.setPercentage(result[i]/sum*100);
				}
				i++;
			}
		} else {
			Log.e(TAG, "Result not found");
			new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {

					while(!(AndroidApplication.getInstance().getCurrentActivity() instanceof DisplayResultActivity)){
						SystemClock.sleep(300);
					}
					
					LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(BroadcastIntentTypes.resultNotFound));
					
					return null;
				}
			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		}
		
		goToNextState();
	}

	@Override
	public void savedProcessedMessage(Round round, String sender, ProtocolMessageContainer message, boolean exclude){
		//no message processed at this state
	}

	@Override
	protected void goToNextState() {
		poll.setTerminated(true);
		
		//send broadcast to dismiss the wait dialog
		Intent intent2 = new Intent(BroadcastIntentTypes.dismissWaitDialog);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent2);

		//Send a broadcast to start the review activity
		Intent intent = new Intent(BroadcastIntentTypes.showResultActivity);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

		try {
			((HKRS12ProtocolInterface)AndroidApplication.getInstance().getProtocolInterface()).getStateMachineManager().getStateMachine().applyEvent(new ResultComputedEvent(null));
		} catch (FiniteStateException e) {
			Log.e(TAG,e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			Log.e(TAG,e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Create the discrete logarithm pre computation task
	 */
	private void createPrecomputationTask(){
		if(rc!=null) rc.interrupt();
		rc = new ResultComputationWithPrecomputation();
		Element[] possiblePlainTexts = new Element[poll.getOptions().size()];
		int i=0;
		for(Option op:poll.getOptions()){
			ProtocolOption pop = (ProtocolOption)op;
			possiblePlainTexts[i]=pop.getRepresentation();
			i++;
		}
		rc.startComputation(poll.getNumberOfParticipants()-poll.getExcludedParticipants().size()-poll.getCompletelyExcludedParticipants().size(),
				poll.getOptions().size(), possiblePlainTexts, poll.getGenerator(), poll.getZ_q(), poll.getOptions());
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


}
