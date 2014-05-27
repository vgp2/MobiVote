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

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import ch.bfh.evoting.voterapp.hkrs12.R;

import ch.bfh.evoting.voterapp.hkrs12.adapters.NetworkParticipantListAdapter;
import ch.bfh.evoting.voterapp.hkrs12.entities.Participant;
import ch.bfh.evoting.voterapp.hkrs12.fragment.HelpDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.fragment.NetworkDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
import ch.bfh.evoting.voterapp.hkrs12.util.Utility;

/**
 * Class displaying the activity where the user can see which persons are
 * present in the network and if they are included in the electorate
 * 
 * @author Philemon von Bergen
 * 
 */
public class CheckElectorateActivity extends ListActivity {

	private NfcAdapter nfcAdapter;
	private boolean nfcAvailable;
	private PendingIntent pendingIntent;

	private BroadcastReceiver networkParticipantUpdater;
	private BroadcastReceiver electorateReceiver;

	private AlertDialog dialogBack;
	private BroadcastReceiver showNextActivityListener;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getResources().getBoolean(R.bool.portrait_only)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}

		AndroidApplication.getInstance().setCurrentActivity(this);
		AndroidApplication.getInstance().setVoteRunning(true);

		setContentView(R.layout.activity_check_electorate);

		setupActionBar();

		Map<String, Participant> participants = new TreeMap<String, Participant>();

		if (this.getIntent().getSerializableExtra("participants") == null) {
			participants = AndroidApplication.getInstance()
					.getNetworkInterface().getGroupParticipants();
			if (participants.size() == 0)
				participants.put("", new Participant("Please wait...", "","",
						false, false,false));
		} else {
			participants = (Map<String, Participant>) this.getIntent()
					.getSerializableExtra("participants");
		}

		final NetworkParticipantListAdapter npa = new NetworkParticipantListAdapter(
				CheckElectorateActivity.this,
				R.layout.list_item_participant_network,
				new ArrayList<Participant>(participants.values()));
		setListAdapter(npa);

		// Until the electorate is received from the administrator, the list is filled
		// with the participant in the network
		networkParticipantUpdater = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {

				Map<String, Participant> participants = AndroidApplication
						.getInstance().getNetworkInterface()
						.getGroupParticipants();
				npa.clear();
				npa.addAll(participants.values());
				npa.notifyDataSetChanged();
			}
		};
		LocalBroadcastManager.getInstance(this).registerReceiver(
				networkParticipantUpdater,
				new IntentFilter(BroadcastIntentTypes.participantStateUpdate));

		// broadcast receiving the participants
		electorateReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {

				Map<String, Participant> participants = (Map<String, Participant>) intent
						.getSerializableExtra("participants");
				npa.clear();
				npa.addAll(participants.values());
				npa.notifyDataSetChanged();

			}
		};
		LocalBroadcastManager.getInstance(this).registerReceiver(
				electorateReceiver,
				new IntentFilter(BroadcastIntentTypes.electorate));

		// Subscribing to the showNextActivity request
		showNextActivityListener = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				LocalBroadcastManager.getInstance(CheckElectorateActivity.this).unregisterReceiver(this);
				LocalBroadcastManager.getInstance(CheckElectorateActivity.this).unregisterReceiver(electorateReceiver);
				LocalBroadcastManager.getInstance(CheckElectorateActivity.this).unregisterReceiver(networkParticipantUpdater);
				//start Review activity
				Intent i = new Intent(context, ReviewPollVoterActivity.class);
				i.putExtras(intent.getExtras());
				startActivity(i);
			}
		};
		LocalBroadcastManager.getInstance(this).registerReceiver(showNextActivityListener, new IntentFilter(BroadcastIntentTypes.showNextActivity));


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
	protected void onPause() {
		super.onPause();
		if (nfcAvailable) {
			nfcAdapter.disableForegroundDispatch(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		AndroidApplication.getInstance().setVoteRunning(true);
		AndroidApplication.getInstance().setCurrentActivity(this);

		if (nfcAdapter != null && nfcAdapter.isEnabled()) {
			nfcAvailable = true;
		}

		// make sure that this activity is the first one which can handle the
		// NFC tags
		if (nfcAvailable) {
			nfcAdapter.enableForegroundDispatch(this, pendingIntent,
					Utility.getNFCIntentFilters(), null);
		}

	}

	@Override
	public void onBackPressed() {
		// Show a dialog to ask confirmation to quit vote
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// Add the buttons
		builder.setPositiveButton(R.string.yes,
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialogBack.dismiss();
				startActivity(new Intent(CheckElectorateActivity.this,
						MainActivity.class).addFlags(
								Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(
										Intent.FLAG_ACTIVITY_CLEAR_TASK));
			}
		});
		builder.setNegativeButton(R.string.no,
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialogBack.dismiss();
			}
		});

		builder.setTitle(R.string.dialog_title_back);
		builder.setMessage(this.getString(R.string.dialog_back));

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
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.check_electorate, menu);
		return true;
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
			ndf.show(getFragmentManager(), "networkInfo");
			return true;
		case R.id.help:
			HelpDialogFragment hdf = HelpDialogFragment.newInstance(
					getString(R.string.help_title_electorate),
					getString(R.string.help_text_electorate_voter));
			hdf.show(getFragmentManager(), "help");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	public void onNewIntent(Intent intent) {
		Intent broadcastIntent = new Intent(BroadcastIntentTypes.nfcTagTapped);
		broadcastIntent.putExtra(NfcAdapter.EXTRA_TAG, intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
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
}
