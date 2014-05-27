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

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import ch.bfh.evoting.voterapp.hkrs12.R;

import ch.bfh.evoting.voterapp.hkrs12.adapters.PollAdapter;
import ch.bfh.evoting.voterapp.hkrs12.db.PollDbHelper;
import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;
import ch.bfh.evoting.voterapp.hkrs12.fragment.HelpDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.fragment.NetworkDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
import ch.bfh.evoting.voterapp.hkrs12.util.Utility;

/**
 * Class displaying all the available polls
 * 
 */
public class PollActivity extends Activity implements OnClickListener,
		OnItemClickListener {
	
	private NfcAdapter nfcAdapter;
	private boolean nfcAvailable;
	private PendingIntent pendingIntent;

	private ListView lvPolls;

	private PollDbHelper pollDbHelper;

	private Button btnCreateVote;
	private Button btnCreateVoteEmpty;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getResources().getBoolean(R.bool.portrait_only)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		final FrameLayout overlayFramelayout = new FrameLayout(this);
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT,
				FrameLayout.LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(
				getResources().getDimensionPixelSize(
						R.dimen.activity_horizontal_margin),
				0,
				getResources().getDimensionPixelSize(
						R.dimen.activity_horizontal_margin), 0);
		overlayFramelayout.setLayoutParams(layoutParams);

		View view = getLayoutInflater().inflate(R.layout.activity_poll,
				overlayFramelayout, false);
		overlayFramelayout.addView(view);

		final SharedPreferences settings = getSharedPreferences(
				AndroidApplication.PREFS_NAME, MODE_PRIVATE);

		if (settings.getBoolean("first_run", true)) {
			final View overlay_view = getLayoutInflater().inflate(
					R.layout.overlay_parent_button, null, false);
			overlayFramelayout.addView(overlay_view);

			overlay_view.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					overlayFramelayout.removeView(overlay_view);
					settings.edit().putBoolean("first_run", false).commit();
				}
			});
		}
		setContentView(overlayFramelayout);

		// Show the Up button in the action bar.
		setupActionBar();

		AndroidApplication.getInstance().setCurrentActivity(this);
		AndroidApplication.getInstance().setIsAdmin(true);

		pollDbHelper = PollDbHelper.getInstance(this);

		lvPolls = (ListView) findViewById(R.id.listview_polls);
		List<Poll> polls = pollDbHelper.getAllOpenPolls();

		View footer = LayoutInflater.from(this).inflate(
				R.layout.footer_create_vote, null);
		btnCreateVote = (Button) footer.findViewById(R.id.button_create_vote);
		btnCreateVote.setOnClickListener(this);
		
		btnCreateVoteEmpty = (Button) findViewById(R.id.button_create_vote);
		btnCreateVoteEmpty.setOnClickListener(this);

		lvPolls.addFooterView(footer);
		lvPolls.setAdapter(new PollAdapter(this, R.layout.list_item_poll, polls));
		lvPolls.setEmptyView(findViewById(R.id.layout_empty));
		lvPolls.setOnItemClickListener(this);
		
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
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.poll, menu);
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
		case R.id.action_network_info:

			// Network interface can be null since it is created in an async
			// task, so we wait until the task is completed
			if (AndroidApplication.getInstance().getNetworkInterface() == null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.dialog_wait_wifi);
				final AlertDialog waitDialog = builder.create();
				waitDialog.show();

				new AsyncTask<Object, Object, Object>() {

					@Override
					protected Object doInBackground(Object... params) {
						while (AndroidApplication.getInstance()
								.getNetworkInterface() == null) {
							// wait
						}
						waitDialog.dismiss();
						showNetworkInfoDialog();
						return null;
					}

				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				return true;
			}

			showNetworkInfoDialog();

			return true;
		case R.id.help:
			HelpDialogFragment hdf = HelpDialogFragment.newInstance(
					getString(R.string.help_title_poll),
					getString(R.string.help_text_poll));
			hdf.show(getFragmentManager(), "help");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onItemClick(AdapterView<?> listview, View view, int position,
			long id) {

		Intent intent = new Intent(this, PollDetailActivity.class);
		intent.putExtra("pollid", view.getId());
		startActivity(intent);

	}
	
	@Override
	public void onClick(View view) {
		if (view == btnCreateVote || view == btnCreateVoteEmpty) {
			Intent intent = new Intent(this, PollDetailActivity.class);
			startActivity(intent);
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
	 * Shows the dialog containing the network informations
	 */
	private void showNetworkInfoDialog() {
		NetworkDialogFragment ndf = NetworkDialogFragment.newInstance();
		ndf.show(getFragmentManager(), "networkInfo");
	}
}
