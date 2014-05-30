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

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.evoting.voterapp.hkrs12.entities.Participant;
import ch.bfh.evoting.voterapp.hkrs12.entities.VoteMessage.Type;
import ch.bfh.evoting.voterapp.hkrs12.protocol.HKRS12ProtocolInterface;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.ProtocolMessageContainer;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.ProtocolOption;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.ProtocolParticipant;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.ProtocolPoll;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.statemachine.StateMachineManager.Round;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
import ch.bfh.unicrypt.crypto.proofsystem.classes.ElGamalEncryptionValidityProofSystem;
import ch.bfh.unicrypt.crypto.schemes.encryption.classes.ElGamalEncryptionScheme;
import ch.bfh.unicrypt.math.algebra.general.classes.Subset;
import ch.bfh.unicrypt.math.algebra.general.classes.Tuple;
import ch.bfh.unicrypt.math.algebra.general.interfaces.Element;

import com.continuent.tungsten.fsm.core.Entity;
import com.continuent.tungsten.fsm.core.Event;
import com.continuent.tungsten.fsm.core.FiniteStateException;
import com.continuent.tungsten.fsm.core.Transition;
import com.continuent.tungsten.fsm.core.TransitionFailureException;
import com.continuent.tungsten.fsm.core.TransitionRollbackException;

/**
 * Action executed in Commit step
 * @author Philemon von Bergen
 * 
 */
public class CommitmentRoundAction extends AbstractAction {



	private BroadcastReceiver voteDone;
	//	private AsyncTask<Object, Object, Object> sendVotesTask;
	private BroadcastReceiver stopReceiver;
	@SuppressWarnings("unused")
	private AsyncTask<Object, Object, Object> sendVotesTask;
	public boolean stopSendingUpdateVote = false;
	private Timer timer;
	private TaskTimer timerTask;

	public CommitmentRoundAction(final Context context, String messageTypeToListenTo, final ProtocolPoll poll) {
		super(context, messageTypeToListenTo, poll, 0);

		voteDone = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, final Intent intent) {
				new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						executeCallback(intent);
						return null;
					}
				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		};
		LocalBroadcastManager.getInstance(context).registerReceiver(voteDone, new IntentFilter(BroadcastIntentTypes.vote));

		//Register a BroadcastReceiver on stop poll order events
		stopReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context arg0, Intent intent) {
				new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						//send broadcast to dismiss the wait dialog
						Intent intent1 = new Intent(BroadcastIntentTypes.showWaitDialog);
						LocalBroadcastManager.getInstance(context).sendBroadcast(intent1);
						
						//Wait for other voters to submit their votes
						SystemClock.sleep(20000);
						Log.d(TAG, "Ending voting phase.");

						for(Participant p:poll.getParticipants().values()){
							if(!messagesReceived.containsKey(p.getUniqueId()) && !poll.getCompletelyExcludedParticipants().containsKey(p.getUniqueId())){
								poll.getExcludedParticipants().put(p.getUniqueId(), p);
								//notify exclusion
								LocalBroadcastManager.getInstance(AndroidApplication.getInstance())
								.sendBroadcast(new Intent(BroadcastIntentTypes.proofVerificationFailed)
									.putExtra("type", 2)
									.putExtra("participant", p.getIdentification()));
								Log.w(TAG, "Excluding "+p.getIdentification()+"("+p.getUniqueId()+") at end of voting period");
							}
						}
						goToNextState();
						return null;
					}
				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

			}
		};
		LocalBroadcastManager.getInstance(context).registerReceiver(stopReceiver, new IntentFilter(BroadcastIntentTypes.stopVote));

	}

	@Override
	public void doAction(Event message, Entity entity, Transition transition,
			int actionType) throws TransitionRollbackException,
			TransitionFailureException, InterruptedException {

		super.doAction(message, entity, transition, actionType);
		Log.d(TAG,"Commitment round started");

		//send broadcast to dismiss the wait dialog
		Intent intent1 = new Intent(BroadcastIntentTypes.showWaitDialog);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent1);

		Element productNumerator = poll.getG_q().getElement(BigInteger.valueOf(1));
		Element productDenominator = poll.getG_q().getElement(BigInteger.valueOf(1));

		Collection<Participant> activeParticipants = new ArrayList<Participant>();
		for(Participant p : poll.getParticipants().values()){
			activeParticipants.add(p);
		}
		activeParticipants.removeAll(poll.getCompletelyExcludedParticipants().values());
		for(Participant p : activeParticipants){
			ProtocolParticipant p2 = (ProtocolParticipant) p;
			if(p2.getProtocolParticipantIndex()>me.getProtocolParticipantIndex()){
				productDenominator = productDenominator.apply(p2.getAi());
			} else if(me.getProtocolParticipantIndex()>p2.getProtocolParticipantIndex()){
				productNumerator = productNumerator.apply(p2.getAi());
			}
		}

		me.setHi(productNumerator.applyInverse(productDenominator));

		//send broadcast to dismiss the wait dialog
		Intent intent2 = new Intent(BroadcastIntentTypes.dismissWaitDialog);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent2);
		
	}

	@Override
	public void savedProcessedMessage(Round round, String sender, ProtocolMessageContainer message, boolean exclude){
		if(round!=Round.commit){
			Log.w(TAG, "Not saving value of processed message since they are value of a previous state.");
		}

		ProtocolParticipant senderParticipant = (ProtocolParticipant) poll.getParticipants().get(sender);
		senderParticipant.setProofValidVote(message.getProof());
				
		if(exclude){
			Log.w(TAG, "Excluding participant "+senderParticipant.getIdentification()+" ("+sender+") because of a message processing problem.");
			poll.getExcludedParticipants().put(sender, senderParticipant);
		} else {
			Log.w(TAG, "Saving message for participant "+senderParticipant.getIdentification()+" ("+sender+").");
		}
		super.savedProcessedMessage(round, sender, message, exclude);

	}

	@Override
	protected void goToNextState() {
		LocalBroadcastManager.getInstance(context).unregisterReceiver(voteDone);
		super.goToNextState();
		try {
			((HKRS12ProtocolInterface)AndroidApplication.getInstance().getProtocolInterface()).getStateMachineManager().getStateMachine().applyEvent(new AllCommitMessagesReceivedEvent(null));
		} catch (FiniteStateException e) {
			Log.e(TAG,e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			Log.e(TAG,e.getMessage());
			e.printStackTrace();
		}
		timer = new Timer();
		timerTask = new TaskTimer();
		timer.schedule(timerTask, 5000);
	}
	
	/**
	 * Task run on timer tick
	 * 
	 */
	private class TaskTimer extends TimerTask {

		@Override
		public void run() {
			stopSendingUpdateVote = true;
		}
	};

	/**
	 * Method executed when user has selected an option
	 * @param data intent received in the broadcast receiver
	 */
	public void executeCallback(Intent data) {

		me.setHasVoted(true);
		numberMessagesReceived++;
		//inform GUI about new vote message received
		sendVotesTask = new AsyncTask<Object, Object, Object>(){

			@Override
			protected Object doInBackground(Object... arg0) {
				while(!stopSendingUpdateVote){

					//notify UI about new incomed vote
					Intent i = new Intent(BroadcastIntentTypes.newIncomingVote);
					i.putExtra("votes", numberMessagesReceived);
					i.putExtra("options", (Serializable)poll.getOptions());
					i.putExtra("participants", (Serializable)poll.getParticipants());
					LocalBroadcastManager.getInstance(context).sendBroadcast(i);
					SystemClock.sleep(1000);
				}
				return null;
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		ProtocolOption option = (ProtocolOption)data.getSerializableExtra("option");
		int index = data.getIntExtra("index", -1);
		
		me.setBi(me.getHi().selfApply(me.getXi()).apply(poll.getGenerator().selfApply(option.getRepresentation())));

		//compute validity proof
		Element[] possibleVotes = new Element[poll.getOptions().size()];
		int i=0;
		for(Option op:poll.getOptions()){
			possibleVotes[i] = poll.getGenerator().selfApply(((ProtocolOption)op).getRepresentation());
			i++;
		}

		ElGamalEncryptionScheme ees = ElGamalEncryptionScheme.getInstance(poll.getGenerator());
		Tuple otherInput = Tuple.getInstance(me.getDataToHash(), poll.getDataToHash());

		
		Subset possibleVotesSet = Subset.getInstance(poll.getG_q(), possibleVotes);
		ElGamalEncryptionValidityProofSystem vpg = ElGamalEncryptionValidityProofSystem.getInstance(ees, me.getHi(), possibleVotesSet, otherInput);

		//simulate the ElGamal cipher text (a,b) = (ai,bi);
		Tuple publicInput = Tuple.getInstance(me.getAi(), me.getBi());
		Tuple privateInput = vpg.createPrivateInput(me.getXi(), index);
		me.setProofValidVote(vpg.generate(privateInput, publicInput));
		
		ProtocolMessageContainer m = new ProtocolMessageContainer(null, me.getProofValidVote(), null);
		sendMessage(m, Type.VOTE_MESSAGE_COMMIT);

		if(readyToGoToNextState()){
			goToNextState();
		}

	}
	
	@Override
	public void reset(){
		if(timer!=null)
			timer.cancel();
		if(timerTask!=null)
			timerTask.cancel();
		stopSendingUpdateVote = true;
		super.reset();
	}

}
