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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;
import ch.bfh.evoting.voterapp.hkrs12.entities.VoteMessage;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;

/**
 * This class implements a dummy protocol without security
 * @author Philemon von Bergen
 *
 */
public class DummyProtocolInterface extends ProtocolInterface {

	private Context context;

	public DummyProtocolInterface(Context context) {
		super(context);
		this.context = context;
	}

	@Override
	public void showReview(Poll poll) {
		//Add protocol specific stuff to the poll

		//Send poll to other participants
		VoteMessage vm = new VoteMessage(VoteMessage.Type.VOTE_MESSAGE_POLL_TO_REVIEW, (Serializable)poll);
		AndroidApplication.getInstance().getNetworkInterface().sendMessage(vm);

		//Send a broadcast to start the next activity
		Intent intent = new Intent(BroadcastIntentTypes.showNextActivity);
		intent.putExtra("poll", (Serializable)poll);
		intent.putExtra("sender", AndroidApplication.getInstance().getNetworkInterface().getMyUniqueId());
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);	
	}

	@Override
	public void beginVotingPeriod(Poll poll) {

		if(AndroidApplication.getInstance().isAdmin()){
			//called when admin want to begin voting period
			
			// Send start poll signal over the network
			VoteMessage vm = new VoteMessage(VoteMessage.Type.VOTE_MESSAGE_START_POLL, null);
			AndroidApplication.getInstance().getNetworkInterface().sendMessage(vm);

			//Do some protocol specific stuff

			//start service listening to incoming votes and stop voting period events
			context.startService(new Intent(context, VoteService.class).putExtra("poll", poll));

			//Send a broadcast to start the review activity
			Intent intent = new Intent(BroadcastIntentTypes.showNextActivity);
			intent.putExtra("poll", (Serializable)poll);
			LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
		} else {
			//called when start message received from admin
			
			//Do some protocol specific stuff
			
			//start service listening to incoming votes and stop voting period events
			context.startService(new Intent(context, VoteService.class).putExtra("poll", poll));
		}
	}

	@Override
	public void endVotingPeriod() {
		//Send stop signal over the network
		VoteMessage vm = new VoteMessage(VoteMessage.Type.VOTE_MESSAGE_STOP_POLL, null);
		AndroidApplication.getInstance().getNetworkInterface().sendMessage(vm);

		//send broadcast containing the stop voting period event
		Intent i = new Intent(BroadcastIntentTypes.stopVote);
		LocalBroadcastManager.getInstance(context).sendBroadcast(i);
		//The VoteService listens to this broadcast and a calls the computeResult method
	}

	@Override
	public void vote(Option selectedOption, Poll poll) {
		//do some protocol specific stuff

		//send the vote over the network
		AndroidApplication.getInstance().getNetworkInterface().sendMessage(new VoteMessage(VoteMessage.Type.VOTE_MESSAGE_VOTE, selectedOption));

		//Send a broadcast to start the review activity
		Intent intent = new Intent(BroadcastIntentTypes.showNextActivity);
		intent.putExtra("poll", (Serializable)poll);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}

	/**
	 * Method called when the result must be computed (all votes received or stop asked by the admin)
	 * @param poll poll object
	 */
	public void computeResult(Poll poll){

		context.stopService(new Intent(context, VoteService.class));

		//do some protocol specific stuff
		//go through compute result and set percentage result
		List<Option> options = poll.getOptions();
		int votesReceived = 0;
		for(Option option : options){
			votesReceived += option.getVotes();
		}
		for(Option option : options){
			if(votesReceived!=0){
				option.setPercentage(option.getVotes()*100/votesReceived);
			} else {
				option.setPercentage(0);
			}
		}

		poll.setTerminated(true);

		//Send a broadcast to start the review activity
		Intent intent = new Intent(BroadcastIntentTypes.showResultActivity);
		intent.putExtra("poll", (Serializable)poll);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}

	@Override
	protected void handleReceivedPoll(Poll poll, String sender) {
		//do some protocol specific stuff

		//Send a broadcast to start the review activity
		Intent intent = new Intent(BroadcastIntentTypes.showNextActivity);
		intent.putExtra("poll", (Serializable) poll);
		intent.putExtra("sender", sender);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}

	@Override
	public void exportToXML(File file, Poll poll){
		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(file);
			outputStream.write("toto\ntoto2".getBytes());
			outputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void cancelVotingPeriod() {
		Intent i = new Intent(context, VoteService.class);
		context.stopService(i);
	}





}
