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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;

/**
 * Class representing the interface between the graphical user interface and the protocol itself
 * @author Philemon von Bergen
 *
 */
public abstract class ProtocolInterface {

	private Context context;
	
	/**
	 * Interface object for protocol implementation
	 * @param context android context
	 */
	public ProtocolInterface(Context context){
		this.context = context;

		// Register a broadcast receiver listening for the poll to review
		LocalBroadcastManager.getInstance(this.context).registerReceiver(
				new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						if(!AndroidApplication.getInstance().isAdmin()){
							Poll poll = (Poll) intent.getSerializableExtra("poll");
							// Poll is not in the DB, so reset the id
							poll.setId(-1);
							handleReceivedPoll(poll, intent.getStringExtra("sender"));
						}
					}
				}, new IntentFilter(BroadcastIntentTypes.pollToReview));
	}

	/**
	 * Method called by the administrator when he want to start the review process
	 * @param poll poll to review
	 */
	public abstract void showReview(Poll poll);
	
	/**
	 * Method called when a message containing the poll to review is received
	 * @param poll poll to review
	 * @param sender sender of the poll
	 */
	protected abstract void handleReceivedPoll(Poll poll, String sender);

	/**
	 * Method called by the administrator when he want to start the voting period
	 * and by the voter when he receives the start message from the administrator
	 * @param poll poll object already received for the review
	 */
	public abstract void beginVotingPeriod(Poll poll);

	/**
	 * Method called by the administrator when he want to end the voting period
	 */
	public abstract void endVotingPeriod();

	/**
	 * Method called when a participant want to want
	 * @param selectedOption the option the participant choose to vote
	 * @param poll the poll object
	 */
	public abstract void vote(Option selectedOption, Poll poll);

	/**
	 * Method called at the end of the vote, when displaying results. This methods
	 * create a file in the app data containing the protocol values. This file can then
	 * be used to verify the vote outside of this app. 
	 * @param file filename
	 * @param poll poll object to export
	 */
	public abstract void exportToXML(File file, Poll poll);

	/**
	 * Method called when the administrator wants to cancel the voting period
	 */
	public abstract void cancelVotingPeriod();

}
