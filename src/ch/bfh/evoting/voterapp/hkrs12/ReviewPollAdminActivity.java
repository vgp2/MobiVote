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
import java.util.Collection;

import ch.bfh.evoting.voterapp.hkrs12.R;

import ch.bfh.evoting.voterapp.hkrs12.entities.Participant;
import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;
import ch.bfh.evoting.voterapp.hkrs12.fragment.HelpDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.fragment.PollReviewFragment;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
import ch.bfh.evoting.voterapp.hkrs12.util.Utility;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

/**
 * Class displaying the activity that allows the user to check if the poll is
 * correct
 * 
 * @author Philemon von Bergen
 * 
 */
public class ReviewPollAdminActivity extends Activity implements OnClickListener {

	private NfcAdapter nfcAdapter;
	private boolean nfcAvailable;
	private PendingIntent pendingIntent;

	private Poll poll;

	private Button btnStartPollPeriod;

	private String sender;

	private PollReviewFragment fragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getResources().getBoolean(R.bool.portrait_only)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		setContentView(R.layout.activity_review_poll);
		setupActionBar();

		if (getResources().getBoolean(R.bool.display_bottom_bar) == false) {
			findViewById(R.id.layout_bottom_bar).setVisibility(View.GONE);
		}

		AndroidApplication.getInstance().setCurrentActivity(this);
		AndroidApplication.getInstance().getNetworkInterface().lockGroup();

		btnStartPollPeriod = (Button) findViewById(R.id.button_start_poll_period);
		btnStartPollPeriod.setOnClickListener(this);

		if(savedInstanceState!=null){
			poll = (Poll) savedInstanceState.getSerializable("poll");
			sender = savedInstanceState.getString("sender");
		}

		Poll intentPoll = (Poll) getIntent().getSerializableExtra("poll");
		if (intentPoll != null) {
			poll = intentPoll;
			sender = getIntent().getStringExtra("sender");
		}

		FragmentManager fm = getFragmentManager();
		fragment = new PollReviewFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable("poll", poll);
		bundle.putString("sender", sender);
		fragment.setArguments(bundle);

		fm.beginTransaction().replace(R.id.fragment_container, fragment, "review").commit();

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

	// Subscribing to the showNextActivity request
	private BroadcastReceiver showNextActivityListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			LocalBroadcastManager.getInstance(ReviewPollAdminActivity.this).unregisterReceiver(this);

			Poll poll = (Poll)intent.getSerializableExtra("poll");

			if (isContainedInParticipants(AndroidApplication.getInstance()
					.getNetworkInterface().getMyUniqueId(), poll.getParticipants().values())) {
				Intent i = new Intent(context, VoteActivity.class);
				i.putExtras(intent.getExtras());
				AndroidApplication.getInstance().getCurrentActivity().startActivity(i);
			} else {
				Intent i = new Intent(context, WaitForVotesAdminActivity.class);
				i.putExtras(intent.getExtras());
				AndroidApplication.getInstance().getCurrentActivity().startActivity(i);
			}
		}
	};

	@Override
	protected void onPause() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(showNextActivityListener);
		super.onPause();
		if (nfcAvailable) {
			nfcAdapter.disableForegroundDispatch(this);
		}
	}

	@Override
	protected void onResume() {
		AndroidApplication.getInstance().setCurrentActivity(this);
		LocalBroadcastManager.getInstance(this).registerReceiver(showNextActivityListener, new IntentFilter(BroadcastIntentTypes.showNextActivity));

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
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putSerializable("poll", poll);
		savedInstanceState.putString("sender", sender);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		poll = (Poll) savedInstanceState.getSerializable("poll");
		sender = savedInstanceState.getString("sender");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.review_poll, menu);

		if(getResources().getBoolean(R.bool.display_bottom_bar)){
			menu.findItem(R.id.action_start_voteperiod).setVisible(false);
		}

		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent i = new Intent(this, ElectorateActivity.class);
			i.putExtra("poll", (Serializable) poll);
			NavUtils.navigateUpTo(this, i);
			return true;
		case R.id.help:
			HelpDialogFragment hdf = HelpDialogFragment.newInstance(
					getString(R.string.help_title_review),
					getString(R.string.help_text_review_admin));
			hdf.show(getFragmentManager(), "help");
			return true;
		case R.id.action_start_voteperiod:
			startVotePeriod();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View view) {
		if (view == btnStartPollPeriod) {
			startVotePeriod();
			SharedPreferences preferences = getSharedPreferences(AndroidApplication.PREFS_NAME, 0);
			preferences.edit().remove("poll").commit();
		}
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

	/**
	 * Start the vote phase
	 */
	private void startVotePeriod() {
		for (Participant p : poll.getParticipants().values()) {
			if (!p.hasAcceptedReview()) {
				for(int i=0; i < 2; i++)
					Toast.makeText(this, R.string.toast_not_everybody_accepted,	Toast.LENGTH_LONG).show();
				return;
			}
		}
		poll.setStartTime(System.currentTimeMillis());

		AndroidApplication.getInstance().getProtocolInterface().beginVotingPeriod(poll);
	}

	/**
	 * Indicate if the peer identified with the given string is contained in the list of participants
	 * @param uniqueId identifier of the peer
	 * @return true if it is contained in the list of participants, false otherwise
	 */
	private boolean isContainedInParticipants(String uniqueId, Collection<Participant> participants) {
		for (Participant p : participants) {
			if (p.getUniqueId().equals(uniqueId)) {
				return true;
			}
		}
		return false;
	}
}
