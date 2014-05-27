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


import ch.bfh.evoting.voterapp.hkrs12.R;

import ch.bfh.evoting.voterapp.hkrs12.adapters.VoteOptionListAdapter;
import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;
import ch.bfh.evoting.voterapp.hkrs12.fragment.HelpDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
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
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Activity displaying the vote options in the voting phase
 * @author Philemon von Bergen
 *
 */
public class VoteActivity extends Activity {

	private NfcAdapter nfcAdapter;
	private boolean nfcAvailable;

	private Poll poll;

	private VoteOptionListAdapter volAdapter;
	private boolean scrolled = false;
	private boolean demoScrollDone = false;

	private ListView lvChoices;
	private PendingIntent pendingIntent;

	private AlertDialog dialogBack;
	private BroadcastReceiver showNextActivityListener;
	private BroadcastReceiver showNextActivityListener2;
	private BroadcastReceiver cancelVotingPeriodListener;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(getResources().getBoolean(R.bool.portrait_only)){
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		AndroidApplication.getInstance().setCurrentActivity(this);

		setContentView(R.layout.activity_vote);

		lvChoices = (ListView)findViewById(R.id.listview_choices);

		//Get the data in the intent
		Intent intent = this.getIntent();
		Poll intentPoll = (Poll) intent.getSerializableExtra("poll");
		if(intentPoll!=null){
			poll = intentPoll;
		}

		//Set the question text
		TextView tvQuestion = (TextView)findViewById(R.id.textview_vote_poll_question);
		tvQuestion.setText(poll.getQuestion());

		//create the list of vote options
		volAdapter = new VoteOptionListAdapter(this, R.layout.list_item_vote, poll.getOptions());
		lvChoices.setAdapter(volAdapter);

		lvChoices.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				//Check if the last view is visible
				if (++firstVisibleItem + visibleItemCount > totalItemCount && demoScrollDone) {
					scrolled=true;
				}
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {}


		});

		lvChoices.post(new Runnable(){

			@Override
			public void run() {
				if(lvChoices.getLastVisiblePosition() < lvChoices.getCount()-1){

					//animate scroll
					new AsyncTask<Object, Object, Object>(){

						@Override
						protected Object doInBackground(Object... params) {
							SystemClock.sleep(500);
							Log.d("VoteActivity", "Doing demo scroll");
							lvChoices.smoothScrollToPositionFromTop(lvChoices.getAdapter().getCount(), 0, 1500);
							SystemClock.sleep(1550);
							lvChoices.smoothScrollToPositionFromTop(0, 0, 1500);
							scrolled = false;
							SystemClock.sleep(1550);
							demoScrollDone = true;
							return null;
						}

					}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

				} else {
					Log.d("VoteActivity", "Demo scroll not needed");
					scrolled = true;
				}
			}

		});


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

		// Subscribing to the showNextActivity request to show WaitForVotesActivity
		showNextActivityListener = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				
				LocalBroadcastManager.getInstance(VoteActivity.this).unregisterReceiver(this);

				//Start activity waiting for other participants to vote
				//If is admin, returns to admin app wait activity
				if(AndroidApplication.getInstance().isAdmin()){
					Intent i = new Intent(context, WaitForVotesAdminActivity.class);
					i.putExtras(intent.getExtras());
					startActivity(i);
				} else {
					Intent i = new Intent(context, WaitForVotesVoterActivity.class);
					i.putExtras(intent.getExtras());
					startActivity(i);
				}
			}
		};
		LocalBroadcastManager.getInstance(this).registerReceiver(showNextActivityListener, new IntentFilter(BroadcastIntentTypes.showNextActivity));

		// Subscribing to the showNextActivity request to show ResultActivity
		showNextActivityListener2 = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				LocalBroadcastManager.getInstance(VoteActivity.this).unregisterReceiver(this);

				//start Review activity
				Intent i = new Intent(context, DisplayResultActivity.class);
				if(intent.getExtras()!=null)
					i.putExtras(intent.getExtras());
				i.putExtra("saveToDb", true);
				startActivity(i);
			}
		};
		LocalBroadcastManager.getInstance(this).registerReceiver(showNextActivityListener2, new IntentFilter(BroadcastIntentTypes.showResultActivity));

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (nfcAvailable) {
			nfcAdapter.disableForegroundDispatch(this);
		}
		LocalBroadcastManager.getInstance(VoteActivity.this).unregisterReceiver(cancelVotingPeriodListener);

	}

	@Override
	protected void onResume() {
		super.onResume();
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

		// Subscribing to the cancel vote event
		cancelVotingPeriodListener = new BroadcastReceiver() {
			private AlertDialog dialogCancel;

			@Override
			public void onReceive(Context context, Intent intent) {
				LocalBroadcastManager.getInstance(VoteActivity.this).unregisterReceiver(this);

				AlertDialog.Builder builder = new AlertDialog.Builder(VoteActivity.this);
				// Add the buttons
				builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						AndroidApplication.getInstance().getProtocolInterface().cancelVotingPeriod();
						
						Intent i = new Intent(VoteActivity.this, MainActivity.class);
						startActivity(i);
					}
				});

				builder.setTitle(R.string.dialog_title_poll_canceled);
				builder.setMessage(R.string.dialog_poll_canceled);

				// Create the AlertDialog
				dialogCancel = builder.create();

				dialogCancel.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialog) {
						Utility.setTextColor(dialog, getResources().getColor(R.color.theme_color));
						dialogCancel.getButton(AlertDialog.BUTTON_NEUTRAL).setBackgroundResource(
								R.drawable.selectable_background_votebartheme);
					}
				});
				dialogCancel.show();

			}
		};
		LocalBroadcastManager.getInstance(this).registerReceiver(cancelVotingPeriodListener, new IntentFilter(BroadcastIntentTypes.cancelVote));

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
				startActivity(new Intent(VoteActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
			}
		});
		builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
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
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.help:
			HelpDialogFragment hdf = HelpDialogFragment.newInstance( getString(R.string.help_title_vote), getString(R.string.help_text_vote) );
			hdf.show( getFragmentManager( ), "help" );
			return true;
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.vote, menu);
		return true;
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

	/**
	 * Method called when cast button is clicked
	 */
	public void castBallot(){
		Option selectedOption = volAdapter.getItemSelected();
		AndroidApplication.getInstance().getProtocolInterface().vote(selectedOption, poll);
	}

	/**
	 * Indicate if the user has scrolled over all possible options
	 * @return true if user has scrolled, false otherwise
	 */
	public boolean getScrolled(){
		return scrolled;
	}


}
