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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import ch.bfh.evoting.voterapp.hkrs12.R;

import ch.bfh.evoting.voterapp.hkrs12.adapters.ResultAdapter;
import ch.bfh.evoting.voterapp.hkrs12.db.PollDbHelper;
import ch.bfh.evoting.voterapp.hkrs12.entities.DatabaseException;
import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;
import ch.bfh.evoting.voterapp.hkrs12.fragment.HelpDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.fragment.ResultChartFragment;
import ch.bfh.evoting.voterapp.hkrs12.protocol.HKRS12ProtocolInterface;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
import ch.bfh.evoting.voterapp.hkrs12.util.Utility;

/**
 * Activity displaying the results of a poll
 * 
 * @author Philemon von Bergen
 * 
 */
public class DisplayResultActivity extends Activity implements OnClickListener {

	private static final String TAG = DisplayResultActivity.class.getSimpleName();

	private NfcAdapter nfcAdapter;
	private boolean nfcAvailable;
	private PendingIntent pendingIntent;

	private int pollId;
	private boolean saveToDbNeeded;
	private Poll poll;

	private Button btnRedo;
	private Button btnExport;

	private boolean exported = false;

	private AlertDialog dialogNoResult;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getResources().getBoolean(R.bool.portrait_only)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		setContentView(R.layout.activity_display_result);
		setupActionBar();

		if (getResources().getBoolean(R.bool.display_bottom_bar) == false) {
			findViewById(R.id.layout_bottom_bar).setVisibility(View.GONE);
		}

		AndroidApplication.getInstance().setCurrentActivity(this);
		AndroidApplication.getInstance().setVoteRunning(false);
		if (AndroidApplication.getInstance().isAdmin()) {
			AndroidApplication.getInstance().getNetworkInterface()
			.unlockGroup();
		}

		if( findViewById(R.id.chartfragment_container) != null){
			ResultChartFragment newFragment = new ResultChartFragment();
			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.add(R.id.chartfragment_container, newFragment);
			transaction.commit();
		}

		ListView lv = (ListView) findViewById(android.R.id.list);

		// Get the data in the intent
		Poll intentPoll = (Poll) this.getIntent().getSerializableExtra("poll");
		if (intentPoll != null) {
			this.poll = intentPoll;
		} else {
			//the poll was just finished and not transmitted in the extras
			//so we have to get it
			this.poll = ((HKRS12ProtocolInterface)AndroidApplication.getInstance().getProtocolInterface()).getRunningPoll();
		}

		saveToDbNeeded = this.getIntent().getBooleanExtra("saveToDb", false);
		if (poll.getId() >= 0) {
			pollId = poll.getId();
		} else {
			pollId = -1;
		}

		if (!AndroidApplication.getInstance().isAdmin() && saveToDbNeeded) {
			LinearLayout ll = (LinearLayout) findViewById(R.id.layout_bottom_bar);
			ll.findViewById(R.id.button_redo_poll).setVisibility(View.GONE);
		} else {

			// Set a listener on the redo button
			btnRedo = (Button) findViewById(R.id.button_redo_poll);
			btnRedo.setOnClickListener(this);
			// Poll is just finished
			if (saveToDbNeeded) {
				btnRedo.setText(R.string.action_redo_poll);
			} else {
				btnRedo.setText(R.string.action_clone_poll);
			}
		}
		
		
		//Message if result is not computed because person was not in electorate
		if(this.getIntent().getBooleanExtra("resultNotComputable", false)){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			// Add the buttons
			builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialogNoResult.dismiss();
				}
			});

			builder.setTitle(R.string.dialog_title_no_result);
			builder.setMessage(R.string.dialog_no_result);


			dialogNoResult = builder.create();
			dialogNoResult.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					Utility.setTextColor(dialog, getResources().getColor(R.color.theme_color));
					dialogNoResult.getButton(AlertDialog.BUTTON_NEUTRAL).setBackgroundResource(
							R.drawable.selectable_background_votebartheme);
				}
			});

			// Create the AlertDialog
			dialogNoResult.show();
		}
		
		// Set a listener on the redo button
		btnExport = (Button) findViewById(R.id.button_export);
		btnExport.setOnClickListener(this);

		lv.setAdapter(new ResultAdapter(this, poll));

		// Save the poll to the DB if needed
		if (saveToDbNeeded) {
			try {
				if (pollId >= 0) {
					PollDbHelper.getInstance(this).updatePoll(pollId, poll);
				} else {
					int newPollId = (int) PollDbHelper.getInstance(this)
							.savePoll(poll);
					this.pollId = newPollId;
					poll.setId(newPollId);
				}
			} catch (DatabaseException e) {
				Log.e(TAG,
						"DB error: " + e.getMessage());
				e.printStackTrace();
			}
			File directory = new File(Environment.getExternalStorageDirectory() + AndroidApplication.FOLDER);
			directory.mkdirs();
		    final File file = new File(Environment.getExternalStorageDirectory() + AndroidApplication.FOLDER + poll.getStartTime()+ AndroidApplication.EXTENSION);
		    new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					AndroidApplication.getInstance().getProtocolInterface().exportToXML(file, poll);
				    exported = true;
				    Log.d(TAG, "XML file saved under "+file.getAbsolutePath());
					return null;
				}
			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		}
		
		
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
	public void onBackPressed() {
		if (!this.saveToDbNeeded) {
			// if consulting an archive
			startActivity(new Intent(this,
					ListTerminatedPollsActivity.class));
		} else {
			Intent i = new Intent(this, MainActivity.class);
			startActivity(i);
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
		AndroidApplication.getInstance().setVoteRunning(false);
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
	public void onSaveInstanceState(Bundle savedInstanceState) {
		try {
			super.onSaveInstanceState(savedInstanceState);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		savedInstanceState.putSerializable("poll", poll);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		poll = (Poll) savedInstanceState.getSerializable("poll");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case android.R.id.home:
			if (saveToDbNeeded) {
				Intent i = new Intent(DisplayResultActivity.this,
						MainActivity.class);
				startActivity(i);

			} else {
				// if consulting an archive
				startActivity(new Intent(this,
						ListTerminatedPollsActivity.class));
			}
			return true;
		case R.id.help:
			HelpDialogFragment hdf = HelpDialogFragment.newInstance(
					getString(R.string.help_title_display_results),
					getString(R.string.help_text_display_results));
			hdf.show(getFragmentManager(), "help");
			return true;
		case R.id.action_redo_vote:
			redoVote();
			return true;
		case R.id.action_export:
			export();
			return true;
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.display_results, menu);

		if (saveToDbNeeded) {
			menu.findItem(R.id.action_redo_vote).setTitle(R.string.action_redo_poll);
			menu.findItem(R.id.action_redo_vote).setVisible(
					AndroidApplication.getInstance().isAdmin());
		} else {
			menu.findItem(R.id.action_redo_vote).setTitle(R.string.action_clone_poll);
		}

		if (getResources().getBoolean(R.bool.display_bottom_bar)) {
			menu.findItem(R.id.action_redo_vote).setVisible(false);
			menu.findItem(R.id.action_export).setVisible(false);
		}

		return true;
	}

	@Override
	public void onClick(View view) {
		if (view == btnRedo) {
			redoVote();
		} else if (view == btnExport){
			export();
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
	 * Method called when the admin want to repeat the poll directly after its
	 * finition or if someone want to clone a poll
	 */
	private void redoVote() {
		Poll newPoll = new Poll();
		newPoll.setQuestion(poll.getQuestion());
		List<Option> newOptions = new ArrayList<Option>();
		for (Option op : poll.getOptions()) {
			Option newOp = new Option();
			newOp.setText(op.getText());
			newOptions.add(newOp);
		}
		newPoll.setOptions(newOptions);

		PollDbHelper pollDbHelper = PollDbHelper
				.getInstance(DisplayResultActivity.this);
		try {
			int pollId = (int) pollDbHelper.savePoll(newPoll);
			Intent i = new Intent(DisplayResultActivity.this,
					PollDetailActivity.class);
			i.putExtra("pollid", pollId);
			startActivity(i);
		} catch (DatabaseException e) {
			for (int i = 0; i < 2; i++)
				Toast.makeText(DisplayResultActivity.this,
						getString(R.string.toast_redo_impossible), Toast.LENGTH_LONG)
						.show();
			e.printStackTrace();
		}
	}
	
	private void export(){
		if(!exported && saveToDbNeeded){
			Toast.makeText(this, getString(R.string.dialog_wait_wifi), Toast.LENGTH_SHORT).show();
			return;
		}
	    File file = new File(Environment.getExternalStorageDirectory() + AndroidApplication.FOLDER + poll.getStartTime()+ AndroidApplication.EXTENSION);
	    Log.d(TAG, "Looking for xml file under "+file.getAbsolutePath());
	    if (!file.exists()) {
	    	for(int i=0; i<2;i++)
	    		Toast.makeText(this, getString(R.string.toast_export_failed), Toast.LENGTH_SHORT).show();
	        return;
	    }
	    for(int i=0; i<2;i++)
			Toast.makeText(this, getString(R.string.toast_file_exported, file.getAbsolutePath()), Toast.LENGTH_SHORT).show();
		
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.setType("text/xml");
		sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
		startActivity(Intent.createChooser(sendIntent, getString(R.string.action_export)));
	}
}
