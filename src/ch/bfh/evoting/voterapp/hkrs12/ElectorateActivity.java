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
package ch.bfh.evoting.voterapp.hkrs12;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ch.bfh.evoting.voterapp.hkrs12.R;

import ch.bfh.evoting.voterapp.hkrs12.adapters.AdminNetworkParticipantListAdapter;
import ch.bfh.evoting.voterapp.hkrs12.entities.Participant;
import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;
import ch.bfh.evoting.voterapp.hkrs12.entities.VoteMessage;
import ch.bfh.evoting.voterapp.hkrs12.fragment.HelpDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.fragment.NetworkDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
import ch.bfh.evoting.voterapp.hkrs12.util.UniqueIdComparator;
import ch.bfh.evoting.voterapp.hkrs12.util.Utility;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Class displaying the activity that allows the administrator to select which participants to include in the electorate
 * @author Philemon von Bergen
 *
 */
public class ElectorateActivity extends Activity implements OnClickListener {

	private NfcAdapter nfcAdapter;
	private boolean nfcAvailable;
	private PendingIntent pendingIntent;

	private Poll poll;
	private Map<String,Participant> participants;
	private AdminNetworkParticipantListAdapter npa;
	private boolean active;

	private Button btnNext;
	private ListView lvElectorate;

	private AsyncTask<Object, Object, Object> resendElectorate;
	private BroadcastReceiver participantsDiscoverer;
	private AlertDialog dialogBack;
	private BroadcastReceiver showNextActivityListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(getResources().getBoolean(R.bool.portrait_only)){
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		setContentView(R.layout.activity_electorate);
		setupActionBar();

		if(getResources().getBoolean(R.bool.display_bottom_bar) == false){
			findViewById(R.id.layout_bottom_bar).setVisibility(View.GONE);
		}

		AndroidApplication.getInstance().setCurrentActivity(this);
		AndroidApplication.getInstance().setVoteRunning(true);
		AndroidApplication.getInstance().getNetworkInterface().unlockGroup();

		btnNext = (Button) findViewById(R.id.button_next);
		btnNext.setOnClickListener(this);

		lvElectorate = (ListView) findViewById(R.id.listview_electorate);

		//if extra is present, it has priority
		Intent intent = getIntent();
		Poll serializedPoll = (Poll)intent.getSerializableExtra("poll");
		if(serializedPoll!=null){
			poll = serializedPoll;
		} else if (poll==null){
			SharedPreferences preferences = getSharedPreferences(AndroidApplication.PREFS_NAME, 0);
			poll = (Poll) AndroidApplication.getInstance().getSerializationUtil().deserialize(preferences.getString("poll", ""));
		}

		participants = AndroidApplication.getInstance().getNetworkInterface().getGroupParticipants();
		npa = new AdminNetworkParticipantListAdapter(this, R.layout.list_item_participant_network_admin, new ArrayList<Participant>(participants.values()));
		lvElectorate.setAdapter(npa);

		// Subscribing to the participantStateUpdate events
		participantsDiscoverer = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				updateFromNetwork();

				//Send the updated list of participants in the network over the network
				VoteMessage vm = new VoteMessage(VoteMessage.Type.VOTE_MESSAGE_ELECTORATE, (Serializable)participants);
				AndroidApplication.getInstance().getNetworkInterface().sendMessage(vm);
			}
		};

		// Subscribing to the showNextActivity request
		showNextActivityListener = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				LocalBroadcastManager.getInstance(ElectorateActivity.this).unregisterReceiver(this);

				//start Review activity
				Intent i = new Intent(context, ReviewPollAdminActivity.class);
				i.putExtras(intent.getExtras());
				startActivity(i);
			}
		};
		LocalBroadcastManager.getInstance(this).registerReceiver(showNextActivityListener, new IntentFilter(BroadcastIntentTypes.showNextActivity));

		active = true;
		startPeriodicSend();


		//Send the list of participants in the network over the network
		VoteMessage vm = new VoteMessage(VoteMessage.Type.VOTE_MESSAGE_ELECTORATE, (Serializable)participants);
		AndroidApplication.getInstance().getNetworkInterface().sendMessage(vm);

		// Is NFC available on this device?
		nfcAvailable = this.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_NFC);

		if (nfcAvailable) {

			nfcAdapter = NfcAdapter.getDefaultAdapter(this);

			if (nfcAdapter.isEnabled()) {

				// Setting up a pending intent that is invoked when an NFC tag
				// is tapped on the back
				pendingIntent = PendingIntent.getActivity(this, 0, new Intent(
						this, getClass())
				.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
			} else {
				nfcAvailable = false;
			}
		}

	}

	@Override
	protected void onNewIntent(Intent intent) {

		//if extra is present, it has priority on the saved poll
		Poll serializedPoll = (Poll)intent.getSerializableExtra("poll");
		if(serializedPoll!=null){
			poll = serializedPoll;
		}

		if (intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) != null){
			Intent broadcastIntent = new Intent(BroadcastIntentTypes.nfcTagTapped);
			broadcastIntent.putExtra(NfcAdapter.EXTRA_TAG, intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
			LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
		}

		super.onNewIntent(intent);
	}

	@Override
	protected void onResume() {

		active = true;

		AndroidApplication.getInstance().setCurrentActivity(this);
		AndroidApplication.getInstance().setVoteRunning(true);

		LocalBroadcastManager.getInstance(this).registerReceiver(participantsDiscoverer, new IntentFilter(BroadcastIntentTypes.participantStateUpdate));

		updateFromNetwork();

		//Send the updated list of participants in the network over the network
		VoteMessage vm = new VoteMessage(VoteMessage.Type.VOTE_MESSAGE_ELECTORATE, (Serializable)participants);
		AndroidApplication.getInstance().getNetworkInterface().sendMessage(vm);

		startPeriodicSend();

		if (nfcAdapter != null && nfcAdapter.isEnabled()) {
			nfcAvailable = true;
		}

		// make sure that this activity is the first one which can handle the
		// NFC tags
		if (nfcAvailable) {
			nfcAdapter.enableForegroundDispatch(this, pendingIntent,
					Utility.getNFCIntentFilters(), null);
		}

		super.onResume();
	}

	@Override
	protected void onPause() {
		active = false;
		LocalBroadcastManager.getInstance(this).unregisterReceiver(participantsDiscoverer);

		super.onPause();

		if (nfcAvailable) {
			nfcAdapter.disableForegroundDispatch(this);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putSerializable("poll", poll);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		poll = (Poll)savedInstanceState.getSerializable("poll");
	}

	@Override
	public void onBackPressed() {
		//Show a dialog to ask confirmation to quit vote 
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// Add the buttons
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialogBack.dismiss();
				ElectorateActivity.super.onBackPressed();
			}
		});
		builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialogBack.dismiss();
			}
		});

		builder.setTitle(R.string.dialog_title_back);
		builder.setMessage(this.getString(R.string.dialog_back_admin));

		// Create the AlertDialog
		dialogBack = builder.create();


		dialogBack.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Utility.setTextColor(dialog, getResources().getColor(R.color.theme_color));
				dialogBack.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundResource(
						R.drawable.selectable_background_votebartheme);
				dialogBack.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundResource(
						R.drawable.selectable_background_votebartheme);

			}
		});

		dialogBack.show();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.network_info:
			NetworkDialogFragment ndf = NetworkDialogFragment.newInstance();			
			ndf.show( getFragmentManager( ), "networkInfo" );
			return true;
		case R.id.help:
			HelpDialogFragment hdf = HelpDialogFragment.newInstance( getString(R.string.help_title_electorate), getString(R.string.help_text_electorate_admin) );
			hdf.show( getFragmentManager( ), "help" );
			return true;
		case R.id.action_next:
			next();
			return true;
		}
		return super.onOptionsItemSelected(item); 
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.electorate, menu);

		if(getResources().getBoolean(R.bool.display_bottom_bar)){
			menu.findItem(R.id.action_next).setVisible(false);
		}

		return true;
	}

	@Override
	public void onClick(View view) {
		if (view == btnNext){
			next();
		}	
	}


	/*--------------------------------------------------------------------------------------------
	 * Helper Methods
	--------------------------------------------------------------------------------------------*/

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	/**
	 * Method called when a modification has occured in the list of participants in the group
	 */
	private void updateFromNetwork(){
		Map<String,Participant> newReceivedMapOfParticipants = AndroidApplication.getInstance().getNetworkInterface().getGroupParticipants();
		for(String ip : newReceivedMapOfParticipants.keySet()){
			if(!participants.containsKey(ip)){
				//Participant is not already know
				//we add it
				participants.put(ip, newReceivedMapOfParticipants.get(ip));
			} else if (!participants.get(ip).getIdentification().equals(newReceivedMapOfParticipants.get(ip).getIdentification())) {
				//There is already a participant registered with this ip,
				//but the identification in the new set is not the same
				//so we delete the old and put the new
				participants.remove(ip);
				participants.put(ip, newReceivedMapOfParticipants.get(ip));
			}
		}

		List<String> toRemove = new ArrayList<String>();
		for(String ip : participants.keySet()){
			if(!newReceivedMapOfParticipants.containsKey(ip)){
				//participant is no more in the new set
				//we delete it
				toRemove.add(ip);
			}
		}
		for(String ip : toRemove){
			participants.remove(ip);
		}

		npa.clear();
		npa.addAll(participants.values());
		npa.notifyDataSetChanged();
	}

	/**
	 * Method called to initialize a periodic send of the list of participants in the group 
	 */
	private void startPeriodicSend(){

		if(resendElectorate!=null && !resendElectorate.isCancelled()){
			return;
		}

		resendElectorate = new AsyncTask<Object, Object, Object>(){

			@Override
			protected Object doInBackground(Object... arg0) {

				while(active){
					Log.d("ElectorateActivity", "sending electorate "+participants);
					//Send the list of participants in the network over the network
					VoteMessage vm = new VoteMessage(VoteMessage.Type.VOTE_MESSAGE_ELECTORATE, (Serializable)participants);
					AndroidApplication.getInstance().getNetworkInterface().sendMessage(vm);
					SystemClock.sleep(5000);

				}
				return null;
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Method called when pushing on the Next button
	 */
	private void next() {
		Map<String,Participant> finalParticipants = new TreeMap<String,Participant>(new UniqueIdComparator());
		for(Participant p: participants.values()){
			if(p.isSelected()){
				finalParticipants.put(p.getUniqueId(),p);
			}
		}
		if(finalParticipants.size()<2){
			for(int i=0; i < 2; i++)
				Toast.makeText(this, R.string.toast_not_enough_participant_selected, Toast.LENGTH_SHORT).show();
			return;
		}
		poll.setParticipants(finalParticipants);

		//if this is a modification of the poll, reset all the acceptations received
		for(Participant p: poll.getParticipants().values()){
			p.setHasAcceptedReview(false);
		}

		active = false;
		resendElectorate.cancel(true);

		AndroidApplication.getInstance().getProtocolInterface().showReview(poll);

		LocalBroadcastManager.getInstance(this).unregisterReceiver(participantsDiscoverer);
	}

}

