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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import ch.bfh.evoting.voterapp.hkrs12.entities.Participant;
import ch.bfh.evoting.voterapp.hkrs12.entities.VoteMessage;
import ch.bfh.evoting.voterapp.hkrs12.entities.VoteMessage.Type;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.ProtocolMessageContainer;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.ProtocolParticipant;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.ProtocolPoll;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.statemachine.StateMachineManager.Round;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;

import com.continuent.tungsten.fsm.core.Action;
import com.continuent.tungsten.fsm.core.Entity;
import com.continuent.tungsten.fsm.core.Event;
import com.continuent.tungsten.fsm.core.Transition;
import com.continuent.tungsten.fsm.core.TransitionFailureException;
import com.continuent.tungsten.fsm.core.TransitionRollbackException;

/**
 * Abstract class representing the action done at a step of the protocol
 * This class implement the logic of receiving and sending a message
 * @author Philemon von Bergen
 *
 */
public abstract class AbstractAction implements Action {

	protected String TAG;

	protected Map<String,ProtocolMessageContainer> messagesReceived;
	protected int numberMessagesReceived = 0;
	protected int numberOfProcessedMessages;


	private boolean timerIsRunning = false;
	private Timer timer;
	private TaskTimer timerTask;

	protected ProtocolPoll poll;

	protected Context context;

	protected boolean actionTerminated;

	protected ProtocolParticipant me;

	private long timeOut;

	public AbstractAction(){};
	
	/**
	 * Create an  Action object
	 * 
	 * @param context Android context
	 * @param messageTypeToListenTo the type of message that concern this action
	 * @param poll the poll to fill in this action
	 * @param timeOut the maximum time in millisecond that this action can last
	 * 
	 */
	public AbstractAction(Context context, String messageTypeToListenTo, ProtocolPoll poll, long timeOut) {

		TAG = this.getClass().getSimpleName();

		//Map of message received
		this.messagesReceived = new HashMap<String,ProtocolMessageContainer>();

		//Store some important entities that will be used in the actions
		this.context = context;
		this.poll = poll;
		this.timeOut = timeOut;

		for(Participant p:poll.getParticipants().values()){
			if(p.getUniqueId().equals(AndroidApplication.getInstance().getNetworkInterface().getMyUniqueId())){
				this.me = (ProtocolParticipant)p;
			}
		}
		if(messageTypeToListenTo!=null){
			// Subscribing to the messageArrived events to update immediately
			LocalBroadcastManager.getInstance(context).registerReceiver(
					this.voteMessageReceiver, new IntentFilter(messageTypeToListenTo));
		}
	}

	/**
	 * Method containing stuff to do in the current state 
	 */
	@Override
	public void doAction(Event arg0, Entity arg1, Transition arg2, int arg3)
			throws TransitionRollbackException, TransitionFailureException,
			InterruptedException {
		LocalBroadcastManager.getInstance(context).registerReceiver(
				this.participantsLeaved, new IntentFilter(BroadcastIntentTypes.participantStateUpdate));
		if(timeOut>0){
			this.startTimer(timeOut);
		}
	}


	/**
	 * Store the received messages if they are from the interesting type for this step 
	 */
	protected BroadcastReceiver voteMessageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			if(actionTerminated) return;

			//Get the message
			ProtocolMessageContainer message = (ProtocolMessageContainer) intent.getSerializableExtra("message");
			String sender = intent.getStringExtra("sender");

			if (messagesReceived.get(sender)!=null && messagesReceived.get(sender).equals(message)){
				//when resending message, check if message differs from one received previously
				Log.w(TAG, "Seems to receive a different message from same source !!!");
				return;
			}

			//When a new message arrives, we re-execute the action of the step
			if(actionTerminated){
				Log.w(TAG,"Action was called by an incoming message, but was already terminated");
				return;
			}			

			//Ignore messages from excluded participant
			if(poll.getExcludedParticipants().containsKey(sender)){
				Log.w(TAG, "Ignoring message from previously excluded participant!");
				return;
			}

			if(sender.equals(me.getUniqueId())){
				Log.d(TAG, "Message received from myself, not needed to process it.");
				if(!messagesReceived.containsKey(sender)){
					messagesReceived.put(sender, message);
					Log.d(TAG, "Message received from "+sender);
				}

				if(readyToGoToNextState()) goToNextState();
				return;
			}

			numberMessagesReceived++;
			
			Log.d(TAG, "Message received from "+poll.getParticipants().get(sender).getIdentification()+" ("+sender+"). Going to process it");

			Round round = null;

			if(AbstractAction.this instanceof SetupRoundAction){
				round = Round.setup;
			} else if(AbstractAction.this instanceof CommitmentRoundAction){
				round = Round.commit;
				poll.getParticipants().get(sender).setHasVoted(true);
				//notify UI about new incomed vote
				Intent i = new Intent(BroadcastIntentTypes.newIncomingVote);
				i.putExtra("votes", numberMessagesReceived);
				i.putExtra("options", (Serializable)poll.getOptions());
				i.putExtra("participants", (Serializable)poll.getParticipants());
				LocalBroadcastManager.getInstance(context).sendBroadcast(i);
			} else if(AbstractAction.this instanceof VotingRoundAction){
				round = Round.voting;
			} else if(AbstractAction.this instanceof RecoveryRoundAction){
				round = Round.recovery;
			}

			Log.d(TAG, "Sending to processing service");
			Intent intent2 = new Intent(context, ProcessingService.class);
			intent2.putExtra("round", (Serializable) round);
			intent2.putExtra("message", (Serializable) message);
			intent2.putExtra("sender", sender);
			context.startService(intent2);

		}
	};

	/**
	 * if the state of a participant has been
	 * changed and she has leaved the discussion, we can put her in the excluded participants
	 */
	protected BroadcastReceiver participantsLeaved = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getStringExtra("action");
			if(action==null || action.equals("")){
				return;
			} else if (action.equals("left")){
				Participant p = poll.getParticipants().get(intent.getStringExtra("id"));
				if(p!=null){
					if(AbstractAction.this instanceof SetupRoundAction){
						poll.getCompletelyExcludedParticipants().put(p.getUniqueId(), p);
						//TODO notify exclusion
						Log.w(TAG, "Participant "+p.getIdentification()+" ("+p.getUniqueId()+") went out of the network before submitting the setup value, so he was completely excluded (also from recovery).");
					} else {
						poll.getExcludedParticipants().put(p.getUniqueId(), p);
						//TODO notify exclusion
						Log.w(TAG, "Participant "+p.getIdentification()+" ("+p.getUniqueId()+") was added to excluded list since he went out of the network.");
					}
				}
			}

			if(readyToGoToNextState() && !actionTerminated){
				goToNextState();
			}
		}
	};

	/**
	 * Method called when a received message has been processed and values contained in the message must be saved
	 * @param round Round to which the received message belongs
	 * @param sender the sender of the message
	 * @param message the message received
	 * @param exclude if the processing of the message done imply the exclusion of the participant
	 */
	public void savedProcessedMessage(Round round, String sender, ProtocolMessageContainer message, boolean exclude){
		this.numberOfProcessedMessages++;
		if(!messagesReceived.containsKey(sender)){
			messagesReceived.put(sender, message);
			Log.d(TAG, "Message received from "+poll.getParticipants().get(sender).getIdentification()+" ("+sender+") back from processing.");
		}
	}

	/**
	 * Indicate if conditions to go to next step are fulfilled
	 * @return true if conditions are fulfilled, false otherwise
	 */
	protected boolean readyToGoToNextState(){
		Collection<Participant> activeParticipants = new ArrayList<Participant>();
		for(Participant p : poll.getParticipants().values()){
			activeParticipants.add(p);
		}
		activeParticipants.removeAll(poll.getExcludedParticipants().values());
		activeParticipants.removeAll(poll.getCompletelyExcludedParticipants().values());
		for(Participant p: activeParticipants){
			if(!this.messagesReceived.containsKey(p.getUniqueId())){
				Log.w(TAG, "Message from "+p.getUniqueId()+" ("+p.getIdentification()+") not received");
				return false;
			}
		}
		return true;
	}

	/**
	 * Implement logic before going and requesting to go to next state
	 */
	protected void goToNextState(){
		this.stopTimer();
		this.actionTerminated = true;
		LocalBroadcastManager.getInstance(context).unregisterReceiver(voteMessageReceiver);
		LocalBroadcastManager.getInstance(context).unregisterReceiver(participantsLeaved);
	}


	/**
	 * Helper method used to send a message
	 * 
	 * @param messageContent content of the message to send
	 * @param type message type
	 * @param IP address to sent the message to if it has to be sent as unicast
	 */
	protected void sendMessage(ProtocolMessageContainer message, Type type) {
		VoteMessage m = new VoteMessage(type, (Serializable)message);
		AndroidApplication.getInstance().getNetworkInterface().sendMessage(m);
	}

	/* Timer methods */

	/**
	 * Start the timer used as time out
	 * @param time time out time in milliseconds
	 * @param numberOfResend number of resend request to send before forcing transition to next step
	 */
	protected void startTimer(long time) {
		if (!timerIsRunning) {
			timer = new Timer();

			timerTask = new TaskTimer();
			timer.schedule(timerTask, time);
			timerIsRunning = true;
		}
	}

	/**
	 * Stop the timer used as time out
	 */
	private void stopTimer() {
		if(timer!=null){
			timerTask.cancel();
			timer.cancel();
			timer.purge();
			timerIsRunning = false;
		}
	}

	/**
	 * Task run on timer tick
	 * 
	 */
	private class TaskTimer extends TimerTask {

		@Override
		public void run() {
			if(actionTerminated) return;
			
			stopTimer();
			if(numberOfProcessedMessages < numberMessagesReceived){
				startTimer(10000);
				Log.d(TAG, "Timer timed out by not all messages were processed. So restarting time out timer");
				return;
			}
			
			for(Participant p:poll.getParticipants().values()){
				if(AbstractAction.this instanceof SetupRoundAction){
					if(!messagesReceived.containsKey(p.getUniqueId())){
						//TODO notify exclusion
						poll.getCompletelyExcludedParticipants().put(p.getUniqueId(), p);
					}
					Log.w(TAG, "Participant "+p.getIdentification()+" ("+p.getUniqueId()+") did not responde before timed out for submitting the setup value, so he was completely excluded (also from recovery).");
				} else if(!messagesReceived.containsKey(p.getUniqueId()) && !poll.getCompletelyExcludedParticipants().containsKey(p.getUniqueId())){
					poll.getExcludedParticipants().put(p.getUniqueId(), p);
					//TODO notify exclusion
					Log.w(TAG, "Excluding participant "+p.getIdentification()+" ("+p.getUniqueId()+") because not sending his message.");
				}
			}

			goToNextState();
		}
	};

	public ProtocolPoll getPoll(){
		return this.poll;
	}

	/**
	 * Unregister LocalBoradcastReceivers
	 */
	public void reset(){
		LocalBroadcastManager.getInstance(context).unregisterReceiver(voteMessageReceiver);
		LocalBroadcastManager.getInstance(context).unregisterReceiver(participantsLeaved);
	}
}
