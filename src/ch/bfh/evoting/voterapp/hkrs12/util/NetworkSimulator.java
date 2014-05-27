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
package ch.bfh.evoting.voterapp.hkrs12.util;
/*
 * This class was used at the beginning of the project to simulate the network
 * It is no more needed but we keep it for development reasons
 */
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.evoting.voterapp.hkrs12.entities.Participant;
import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;
import ch.bfh.evoting.voterapp.hkrs12.entities.VoteMessage;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class NetworkSimulator {

	private Context ctx;

	private int excludeParticipant = 0;

	public NetworkSimulator(Context ctx){
		this.ctx = ctx;

		String packageName = ctx.getPackageName();
		if(packageName.equals("ch.bfh.evoting.voterapp")){
			simulateAdmin();
			simulateVoter();
		} else if (packageName.equals("ch.bfh.evoting.adminapp")){
			simulateVoter();
		}
	}


	private void simulateVoter(){
		//Simulate votes arrive each second
		new Thread(){

			@Override
			public void run() {
				while(true){
					SystemClock.sleep(1000);
					
					Poll poll = NetworkSimulator.this.createDummyPoll();
					
					//creation of vote and sender ip
					int random = (int)(Math.random() * (poll.getOptions().size()-0)) + 0;
					Option vote = poll.getOptions().get(random);
					int i=0;
					int random2 = (int)(Math.random() * (AndroidApplication.getInstance().getNetworkInterface().getGroupParticipants().size()-0)) + 0;
					String senderAddress = "0.0.0.0";
					for(Participant p:poll.getParticipants().values()){
						if(i==random2){
							senderAddress = p.getUniqueId();
							break;
						} else {
							i++;
						}
					}
					//creation of vote message
					VoteMessage vm = new VoteMessage(VoteMessage.Type.VOTE_MESSAGE_VOTE, vote);
					vm.setSenderUniqueId(senderAddress);

					//simulate the entry of the created message
					Intent intent = new Intent("messageArrived");
					intent.putExtra("message", vm);
					LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
				}	
			}
		}.start();

		//Simulate update in participants
		new Thread(){

			@Override
			public void run() {
				while(true){
					SystemClock.sleep(6000);
					//choose a participant to exclude temporarily
					excludeParticipant = (int)(Math.random() * (15-1)) + 1;
					Log.w("NetworkSimulator", "Participant "+excludeParticipant+" was excluded this time!");
					//simulate the entry of the created message
					Intent intent = new Intent(BroadcastIntentTypes.participantStateUpdate);
					LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
				}	
			}
		}.start();
	}

	private void simulateAdmin(){
		//Simulate incoming of a message containing the electorate
		new Thread(){

			@Override
			public void run() {
				while(true){
					SystemClock.sleep(10000);
					//creation of sender ip
					String senderAddress = "192.168.1.1";

					//creation of vote message
					Serializable content = (Serializable)NetworkSimulator.this.createDummyParticipants();
					VoteMessage vm = new VoteMessage(VoteMessage.Type.VOTE_MESSAGE_ELECTORATE, content);
					vm.setSenderUniqueId(senderAddress);
					
					//simulate the entry of the created message
					Intent intent = new Intent("messageArrived");
					intent.putExtra("message", vm);
					LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);

				}	
			}
		}.start();

		//Simulate incoming of a message containing the poll
		new Thread(){

			@Override
			public void run() {
				while(true){
					SystemClock.sleep(15000);
					//creation of sender ip
					String senderAddress = "192.168.1.1";

					//creation of vote message
					Serializable content = NetworkSimulator.this.createDummyPoll();
					VoteMessage vm = new VoteMessage(VoteMessage.Type.VOTE_MESSAGE_POLL_TO_REVIEW, content);
					vm.setSenderUniqueId(senderAddress);
					
					//simulate the entry of the created message
					Intent intent = new Intent("messageArrived");
					intent.putExtra("message", vm);
					LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);

				}	
			}
		}.start();

		//Simulate incoming of a message with start poll order
		new Thread(){

			@Override
			public void run() {
				while(true){
					SystemClock.sleep(15000);
					//creation of sender ip
					String senderAddress = "192.168.1.1";

					//creation of vote message
					String content = "START POLL";
					VoteMessage vm = new VoteMessage(VoteMessage.Type.VOTE_MESSAGE_START_POLL, content);
					vm.setSenderUniqueId(senderAddress);

					//simulate the entry of the created message
					Intent intent = new Intent("messageArrived");
					intent.putExtra("message", vm);
					LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);

				}	
			}
		}.start();

		//Simulate incoming of a message with stop poll order
		new Thread(){

			@Override
			public void run() {
				while(true){
					SystemClock.sleep(15000);
					//creation of sender ip
					String senderAddress = "192.168.1.1";

					//creation of vote message
					String content = "STOP POLL";
					VoteMessage vm = new VoteMessage(VoteMessage.Type.VOTE_MESSAGE_STOP_POLL, content);
					vm.setSenderUniqueId(senderAddress);
												
					//simulate the entry of the created message
					Intent intent = new Intent("messageArrived");
					intent.putExtra("message", vm);
					LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);

				}	
			}
		}.start();
	}


	public Map<String,Participant> createDummyParticipants(){
		List<Participant> participants = new ArrayList<Participant>();
		if(excludeParticipant != 1){
			Participant p1 = new Participant("Administrator 1 with very very very very very very very very very very very very long name", "192.168.1.1", "",false, false, false);
			participants.add(p1);
		}
		if(excludeParticipant != 2){
			Participant p2 = new Participant("Participant 2 with very very very very very very very very very very very very long name", "192.168.1.2", "",false, false, false);
			participants.add(p2);
		}
		if(excludeParticipant != 3){
			Participant p3 = new Participant("Participant 3 with very very very very very very very very very very very very long name", "192.168.1.3", "",false, false, false);
			participants.add(p3);
		}
		if(excludeParticipant != 4){
			Participant p4 = new Participant("Participant 4 with very very very very very very very very very very very very long name", "192.168.1.4", "",false, false, false);
			participants.add(p4);
		}
		if(excludeParticipant != 5){
			Participant p5 = new Participant("Participant 5 with very very very very very very very very very very very very long name", "192.168.1.5", "",false, false, false);
			participants.add(p5);
		}
		if(excludeParticipant != 6){
			Participant p6 = new Participant("Participant 6 with very very very very very very very very very very very very long name", "192.168.1.6", "",false, false, false);
			participants.add(p6);
		}
		if(excludeParticipant != 7){
			Participant p7 = new Participant("Participant 7 with very very very very very very very very very very very very long name", "192.168.1.7", "",false, false, false);
			participants.add(p7);
		}
		if(excludeParticipant != 8){
			Participant p8 = new Participant("Participant 8 with very very very very very very very very very very very very long name", "192.168.1.8", "",false, false, false);
			participants.add(p8);
		}
		if(excludeParticipant != 9){
			Participant p9 = new Participant("Participant 9 with very very very very very very very very very very very very long name", "192.168.1.9", "",false, false, false);
			participants.add(p9);
		}
		if(excludeParticipant != 10){
			Participant p10 = new Participant("Participant 10 with very very very very very very very very very very very very long name", "192.168.1.10", "",false, false, false);
			participants.add(p10);
		}
		if(excludeParticipant != 11){
			Participant p11 = new Participant("Participant 11 with very very very very very very very very very very very very long name", "192.168.1.11", "",false, false, false);
			participants.add(p11);
		}
		if(excludeParticipant != 12){
			Participant p12 = new Participant("Participant 12 with very very very very very very very very very very very very long name", "192.168.1.12", "",false, false, false);
			participants.add(p12);
		}
		if(excludeParticipant != 13){
			Participant p13 = new Participant("Participant 13 with very very very very very very very very very very very very long name", "192.168.1.13", "",false, false, false);
			participants.add(p13);
		}
		if(excludeParticipant != 14){
			Participant p14 = new Participant("Participant 14 with very very very very very very very very very very very very long name", "192.168.1.14", "",false, false, false);
			participants.add(p14);
		}
		if(excludeParticipant != 15){
			Participant p15 = new Participant("Participant 15 with very very very very very very very very very very very very long name", "192.168.1.15", "",false, false, false);
			participants.add(p15);
		}	

		Map<String,Participant> map = new TreeMap<String,Participant>(new UniqueIdComparator());
		for(Participant p:participants){
			map.put(p.getUniqueId(), p);
		}
		return map;
	}

	private Poll createDummyPoll(){

		//Create the option
		String question = "What do you think very very very long question very very very long question very very very long question very very very long question?";
		Option yes = new Option();
		yes.setText("Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes 1");
		Option no = new Option();
		no.setText("No No No No No No No No No No No No No No No No No No No NoNo No No No No No No No No No No No 2");
		Option yes1 = new Option();
		yes1.setText("Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes 3");
		Option no1 = new Option();
		no1.setText("No No No No No No No No No No No No No No No No No No No NoNo No No No No No No No No No No No 4");
		Option yes2 = new Option();
		yes2.setText("Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes 5");
		Option no2 = new Option();
		no2.setText("No No No No No No No No No No No No No No No No No No No NoNo No No No No No No No No No No No 6");
		Option yes3 = new Option();
		yes3.setText("Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes 7");
		Option no3 = new Option();
		no3.setText("No No No No No No No No No No No No No No No No No No No NoNo No No No No No No No No No No No 8");
		Option yes4 = new Option();
		yes4.setText("Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes 9");
		Option no4 = new Option();
		no4.setText("No No No No No No No No No No No No No No No No No No No NoNo No No No No No No No No No No No 10");
		Option yes5 = new Option();
		yes5.setText("Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes 11");
		Option no5 = new Option();
		no5.setText("No No No No No No No No No No No No No No No No No No No NoNo No No No No No No No No No No No 12");
		Option yes6 = new Option();
		yes6.setText("Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes 13");
		Option no6 = new Option();
		no6.setText("No No No No No No No No No No No No No No No No No No No NoNo No No No No No No No No No No No 14");
		Option yes7 = new Option();
		yes7.setText("Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes Yes 15");
		Option no7 = new Option();
		no7.setText("No No No No No No No No No No No No No No No No No No No NoNo No No No No No No No No No No No 16");

		List<Option> options = new ArrayList<Option>();
		options.add(yes);
		options.add(no);
		options.add(yes1);
		options.add(no1);
		options.add(yes2);
		options.add(no2);
		options.add(yes3);
		options.add(no3);
		options.add(yes4);
		options.add(no4);
		options.add(yes5);
		options.add(no5);
		options.add(yes6);
		options.add(no6);
		options.add(yes7);
		options.add(no7);

		Map<String,Participant> participantsMap = this.createDummyParticipants();
		List<Participant> participants = new ArrayList<Participant>(participantsMap.values());
		participants.get(0).setSelected(true);
		participants.get(2).setSelected(true);
		participants.get(4).setSelected(true);
		participants.get(6).setSelected(true);
		participants.get(7).setSelected(true);
		participants.get(9).setSelected(true);
		participants.get(10).setSelected(true);
		participants.get(11).setSelected(true);
		participants.get(13).setSelected(true);

		Poll poll = new Poll();
		poll.setOptions(options);
		poll.setParticipants(participantsMap);
		poll.setQuestion(question);
		poll.setTerminated(false);

		return poll;
	}
}
