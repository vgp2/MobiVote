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
package ch.bfh.evoting.voterapp.hkrs12.network;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ch.bfh.evoting.alljoyn.BusHandler;
import ch.bfh.evoting.voterapp.hkrs12.R;
import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import ch.bfh.evoting.voterapp.hkrs12.entities.Participant;
import ch.bfh.evoting.voterapp.hkrs12.entities.VoteMessage;
import ch.bfh.evoting.voterapp.hkrs12.network.wifi.WifiAPManager;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
import ch.bfh.evoting.voterapp.hkrs12.util.SerializationUtil;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class AllJoynNetworkInterface extends AbstractNetworkInterface{

	private BusHandler mBusHandler;
	private String groupName;
	private String groupPassword;
	private String saltShortDigest;
	private boolean feedbackReceived;

	public AllJoynNetworkInterface(Context context) {
		super(context);
		HandlerThread busThread = new HandlerThread("BusHandler");
		busThread.start();
		mBusHandler = new BusHandler(busThread.getLooper(), context);

		// Listening for arriving messages
		LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, new IntentFilter("messageArrived"));
		// Listening for group destroy signal
		LocalBroadcastManager.getInstance(context).registerReceiver(mGroupEventReceiver, new IntentFilter("groupDestroyed"));
		// Listening for group destroy signal
		LocalBroadcastManager.getInstance(context).registerReceiver(mNetworkConectionFailedReceiver, new IntentFilter(BroadcastIntentTypes.networkConnectionFailed));

	}

	@Override
	public String getNetworkName() {
		SharedPreferences preferences = context.getSharedPreferences(AndroidApplication.PREFS_NAME, 0);
		return preferences.getString("SSID", "");
	}

	@Override
	public String getGroupName() {
		return groupName;
	}

	@Override
	public String getGroupPassword() {
		return this.groupPassword;
	}
	
	@Override
	public List<String> listAvailableGroups(){
		ArrayList<String> newList = new ArrayList<String>();
		for(String s: mBusHandler.listGroups()){
			newList.add(s.replace("group", ""));
		}
		return newList;
	}

	@Override
	public String getMyUniqueId() {
		return mBusHandler.getIdentification();
	}

	@Override
	public Map<String, Participant> getGroupParticipants() {
		TreeMap<String,Participant> parts = new TreeMap<String,Participant>();
		for(String s : mBusHandler.getParticipants(this.groupName)){
			String wellKnownName = s;
			String publicKey = "";
			if(mBusHandler.getPeerWellKnownName(s)!=null){
				wellKnownName = mBusHandler.getPeerWellKnownName(s);
				publicKey = mBusHandler.getPeerPublicKey(s);
			}
			parts.put(s, new Participant(wellKnownName, s, publicKey, false, false, false));
		}
		return parts;
	}

	@Override
	public void sendMessage(VoteMessage votemessage) {
		votemessage.setSenderUniqueId(getMyUniqueId());
		SerializationUtil su = AndroidApplication.getInstance().getSerializationUtil();
		String string = su.serialize(votemessage);

		//Since message sent through AllJoyn are not sent to the sender, we do it here
		this.transmitReceivedMessage(votemessage);

		Message msg = mBusHandler.obtainMessage(BusHandler.PING);
		Bundle data = new Bundle();
		data.putString("groupName", this.groupName);
		data.putString("pingString", string);
		msg.setData(data);
		mBusHandler.sendMessage(msg);

	}

	@Override
	public void sendMessage(VoteMessage votemessage, String destinationUniqueId) {
		throw new UnsupportedOperationException("Unicast is not supported with AllJoyn Network interface");
	}

	@Override
	public void disconnect() {

		//leave actual group
		Message msg1 = mBusHandler.obtainMessage(BusHandler.LEAVE_GROUP, this.groupName);
		mBusHandler.sendMessage(msg1);

		Message msg2 = mBusHandler.obtainMessage(BusHandler.DESTROY_GROUP, this.groupName);
		mBusHandler.sendMessage(msg2);

		this.groupName = null;

	}

	@Override
	public void joinGroup(String groupName) {
		Log.e("TAG", "group name "+groupName);
		for(String s:this.listAvailableGroups()){
			Log.e("TAG", "group name found"+s);
		}
		
		if(!AndroidApplication.getInstance().isAdmin()){
			if(this.listAvailableGroups().isEmpty() || !this.listAvailableGroups().contains(groupName.replace("group",""))){
				Intent intent = new Intent(BroadcastIntentTypes.networkConnectionFailed);
				intent.putExtra("error", 5);
				LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
				return;
			}
		}

		connectionTimeOut(40000);

		//Close previous connections
		if(this.groupName!=null && this.groupName!=""){
			disconnect();
		}

		if(AndroidApplication.getInstance().isAdmin()){
			//Generate group name
			int groupNumber = 1;
			groupName = "group"+groupNumber;
			while(mBusHandler.listGroups().contains(groupName)){
				groupNumber++;
				groupName = "group"+groupNumber;
			}
			//generate group password
			this.groupPassword = generatePassword();
		}

		this.groupName = groupName;


		boolean apOn = new WifiAPManager().isWifiAPEnabled((WifiManager) context.getSystemService(Context.WIFI_SERVICE));

		if(AndroidApplication.getInstance().isAdmin() || apOn){
			Message msg2 = mBusHandler.obtainMessage(BusHandler.CREATE_GROUP);
			Bundle data = new Bundle();
			data.putString("groupName", this.groupName);
			data.putString("groupPassword", this.groupPassword);
			msg2.setData(data);
			mBusHandler.sendMessage(msg2);
		} else {
			Message msg3 = mBusHandler.obtainMessage(BusHandler.JOIN_GROUP);
			Bundle data = new Bundle();
			data.putString("groupName", this.groupName);
			data.putString("groupPassword", this.groupPassword);
			data.putString("saltShortDigest", this.saltShortDigest);
			msg3.setData(data);
			mBusHandler.sendMessage(msg3);
		}		
	}

	@Override
	public void lockGroup(){
		Message msg = mBusHandler.obtainMessage(BusHandler.LOCK_GROUP, groupName);
		mBusHandler.sendMessage(msg);
	}

	@Override
	public void unlockGroup(){
		Message msg = mBusHandler.obtainMessage(BusHandler.UNLOCK_GROUP, groupName);
		mBusHandler.sendMessage(msg);
	}

	@Override
	public void setGroupName(String groupName){
		this.groupName = groupName;
	}

	@Override
	public void setGroupPassword(String password){
		if(password.length()>3){
			this.saltShortDigest = password.substring(password.length()-3, password.length());
			this.groupPassword = password.substring(0,password.length()-3);
		}
	}

	@Override
	public String getSaltShortDigest(){
		return mBusHandler.getSaltShortDigest();
	}

	/**
	 * this broadcast receiver listens for incoming messages
	 */
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			SerializationUtil su = AndroidApplication.getInstance().getSerializationUtil();
			transmitReceivedMessage((VoteMessage) su.deserialize(intent.getStringExtra("message")));
		}
	};

	/**
	 * this broadcast receiver listens for incoming event indicating that the joined group was destroyed
	 */
	private BroadcastReceiver mGroupEventReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String groupName = intent.getStringExtra("groupName");

			if(AllJoynNetworkInterface.this.groupName!=null && groupName !=null && AllJoynNetworkInterface.this.groupName.equals(groupName)){
				groupName = null;
				Intent i  = new Intent(BroadcastIntentTypes.networkGroupDestroyedEvent);
				LocalBroadcastManager.getInstance(context).sendBroadcast(i);
			}
		}
	};

	/**
	 * this broadcast receiver listens for incoming events notifying that connection to network failed
	 */
	private BroadcastReceiver mNetworkConectionFailedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			feedbackReceived = true;
			groupName = null;

			int status = intent.getIntExtra("error", 0);
			if(status==1){
				for(int i=0; i < 2; i++)
					Toast.makeText(context, context.getString(R.string.toast_join_error_invalid_name), Toast.LENGTH_SHORT).show();
			} else if (status == 2){
				for(int i=0; i < 2; i++)
					Toast.makeText(context, context.getString(R.string.toast_join_error_admin), Toast.LENGTH_SHORT).show();
			} else if (status == 3){
				for(int i=0; i < 2; i++)
					Toast.makeText(context, context.getString(R.string.toast_join_error_voter), Toast.LENGTH_SHORT).show();
			} else if (status == 4){
				for(int i=0; i < 2; i++)
					Toast.makeText(context, context.getString(R.string.toast_join_error_voter_network), Toast.LENGTH_SHORT).show();
			}  else if (status == 5){
				for(int i=0; i < 4; i++)
					Toast.makeText(context, context.getString(R.string.toast_join_error_no_groups), Toast.LENGTH_SHORT).show();
			} else {
				for(int i=0; i < 2; i++)
					Toast.makeText(context, context.getString(R.string.toast_join_error), Toast.LENGTH_SHORT).show();
			}
		}
	};

	/**
	 * Helper method that generates the network password
	 * @return a random string of 10 lower case chars
	 */
	private String generatePassword(){
			//Inspired from: http://stackoverflow.com/questions/5683327/how-to-generate-a-random-string-of-20-characters
			char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
			StringBuilder sb = new StringBuilder();

			SecureRandom random = new SecureRandom();
			for (int i = 0; i < 10; i++) {
				int pos = random.generateSeed(1)[0]%26;
				if(pos<0)pos=pos+26;

				sb.append(chars[pos]);
			}
			return sb.toString();
		
	}

	/**
	 * Helper method simulating a connection time out
	 * @param time timeout time for the connection try
	 */
	public void connectionTimeOut(long time){
		new Handler().postDelayed(new Runnable() {

			public void run() {  
				if(!mBusHandler.getConnected() && !feedbackReceived){
					mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);
					LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("networkConnectionFailed"));
				}
				feedbackReceived = false;
			}
		}, time); 
	}
}
