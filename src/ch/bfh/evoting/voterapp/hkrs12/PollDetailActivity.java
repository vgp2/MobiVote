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
import java.util.concurrent.Callable;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import ch.bfh.evoting.voterapp.hkrs12.R;

import ch.bfh.evoting.voterapp.hkrs12.adapters.PollOptionAdapter;
import ch.bfh.evoting.voterapp.hkrs12.db.PollDbHelper;
import ch.bfh.evoting.voterapp.hkrs12.entities.DatabaseException;
import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;
import ch.bfh.evoting.voterapp.hkrs12.fragment.HelpDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.fragment.NetworkDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
import ch.bfh.evoting.voterapp.hkrs12.util.Utility;
/**
 * Class displaying the activity that show the details of a poll
 *
 */
public class PollDetailActivity extends Activity implements OnClickListener, TextWatcher {
	
	private NfcAdapter nfcAdapter;
	private boolean nfcAvailable;
	private PendingIntent pendingIntent;

	private ListView lv;
	private PollOptionAdapter adapter;
	ArrayList<Option> options;

	private ImageButton btnAddOption;
	private Button btnStartPoll;
	private EditText etOption;
	private EditText etQuestion;
	private CheckBox cbEmptyVote;

	private Poll poll;
	private Poll savedPoll;

	private PollDbHelper pollDbHelper;

	private AlertDialog dialogSave;
	private AlertDialog dialogAddOption;

	private boolean changesMade = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(getResources().getBoolean(R.bool.portrait_only)){
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		setContentView(R.layout.activity_poll_detail);

		// Show the Up button in the action bar.
		setupActionBar();

		if(getResources().getBoolean(R.bool.display_bottom_bar) == false){
			findViewById(R.id.layout_bottom_bar).setVisibility(View.GONE);
		}

		AndroidApplication.getInstance().setCurrentActivity(this);
		AndroidApplication.getInstance().setVoteRunning(false);
		AndroidApplication.getInstance().setIsAdmin(true);

		pollDbHelper = PollDbHelper.getInstance(this);

		if (getIntent().getIntExtra("pollid", -1) == -1 && savedPoll == null && poll==null){
			// we didn't get a poll id, so let's create a new poll.
			poll = new Poll();
			options = new ArrayList<Option>();
			poll.setOptions(options);
		} else if (savedPoll != null) {
			poll = savedPoll;
			options = (ArrayList<Option>) poll.getOptions();
		} else if(poll != null){
			options = (ArrayList<Option>) poll.getOptions();
		}
		else {
			poll = pollDbHelper.getPoll(getIntent().getIntExtra("pollid", -1));
			options = (ArrayList<Option>) poll.getOptions();
		}

		lv = (ListView) findViewById(R.id.listview_pollquestions);
		btnAddOption = (ImageButton) findViewById(R.id.button_addoption);
		btnStartPoll = (Button) findViewById(R.id.button_start_poll);
		etOption = (EditText) findViewById(R.id.edittext_option);
		etQuestion = (EditText) findViewById(R.id.edittext_question);
		etQuestion.setText(poll.getQuestion());
		etQuestion.addTextChangedListener(this);

		btnAddOption.setOnClickListener(this);
		btnStartPoll.setOnClickListener(this);

		adapter = new PollOptionAdapter(this, R.id.listview_pollquestions, poll);

		lv.setAdapter(adapter);
		lv.setEmptyView(findViewById(R.id.textview_empty));


		adapter.registerDataSetObserver(new DataSetObserver() {

			@Override
			public void onChanged() {
				changesMade = true;
				super.onChanged();
			}

		});

		cbEmptyVote = (CheckBox)findViewById(R.id.checkbox_emptyvote);
		cbEmptyVote.setText(R.string.allow_empty_vote);
		cbEmptyVote.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(cbEmptyVote.isChecked()){
					poll.getOptions().add(new Option(getString(R.string.empty_vote),0,0,0,0));
				} else {
					for(Option o:poll.getOptions()){
						if(o.getText().equals(getString(R.string.empty_vote))){
							poll.getOptions().remove(o);
							break;
						}
					}
				}

				adapter.notifyDataSetChanged();
				lv.invalidate();
			}
		});

		cbEmptyVote.setChecked(false);
		for(Option o:poll.getOptions()){
			if(o.getText().equals(getString(R.string.empty_vote))){
				boolean backupChangesMade = changesMade;
				cbEmptyVote.setChecked(true);
				//toogle the checkbox to add the empty vote at the end of the list
				cbEmptyVote.performClick();
				cbEmptyVote.performClick();
				changesMade = backupChangesMade;
				break;
			}
		}

		etOption.setOnEditorActionListener(new OnEditorActionListener() {


			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
					btnAddOption.performClick();
					btnAddOption.animate().start();
				}    
				return false;
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
		AndroidApplication.getInstance().setVoteRunning(false);
		
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
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		savedPoll = (Poll)savedInstanceState.getSerializable("poll");
	}

	@Override
	public void onBackPressed() {
		askToSave();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		getMenuInflater().inflate(R.menu.poll_detail, menu);
		
		if(getResources().getBoolean(R.bool.display_bottom_bar)){
			menu.findItem(R.id.action_start_vote).setVisible(false);
	    }
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onClick(View view) {	
		if (view == btnAddOption){ 
			if (!etOption.getText().toString().equals("")){
				Option option = new Option();
				option.setText(etOption.getText().toString());
				if(cbEmptyVote.isChecked()){
					options.add(options.size()-1,option);
				} else {
					options.add(option);
				}
				poll.setOptions(options);
				adapter.notifyDataSetChanged();
				etOption.setText("");
			}
		}

		if (view == btnStartPoll){
			if(!AndroidApplication.getInstance().getNetworkMonitor().isWifiEnabled()){
				for(int i=0; i<2; i++)
					Toast.makeText(this, getString(R.string.toast_wifi_is_disabled), Toast.LENGTH_SHORT).show();
				return;
			}
			String pollB64 = AndroidApplication.getInstance().getSerializationUtil().serialize(poll);
			SharedPreferences preferences = getSharedPreferences(AndroidApplication.PREFS_NAME, 0);
			preferences.edit().putString("poll", pollB64).commit();
			startVote();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			askToSave();
			return true;
		case R.id.action_network_info:
			//Network interface can be null since it is created in an async task, so we wait until the task is completed
			this.waitForNetworkInterface(new Callable<Void>() {
				public Void call() {
					return showNetworkInfoDialog();
				}
			});
			return true;
		case R.id.help:
			HelpDialogFragment hdf = HelpDialogFragment.newInstance( getString(R.string.help_title_poll_details), getString(R.string.help_text_poll_details) );
			hdf.show( getFragmentManager( ), "help" );
			return true;
		case R.id.action_start_vote:
			startVote();
		}
		return super.onOptionsItemSelected(item); 
	}

	@Override
	public void afterTextChanged(Editable edit) {
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		changesMade = true;
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
	 * Show the dialog asking if the user want to add the option in edition if there is one
	 * and ask to save if changes have been made
	 */
	private void askToSave(){
		if(!etOption.getText().toString().equals("")){
			AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
			// Add the buttons
			builder1.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					btnAddOption.performClick();
					dialogAddOption.dismiss();
				}
			});
			builder1.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					etOption.setText("");
					dialogAddOption.dismiss();
				}
			});

			builder1.setTitle(R.string.dialog_title_add_option);
			builder1.setMessage(R.string.dialog_add_option);

			// Create the AlertDialog
			dialogAddOption = builder1.create();

			dialogAddOption.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					Utility.setTextColor(dialog, getResources().getColor(R.color.theme_color));
					dialogAddOption.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundResource(
							R.drawable.selectable_background_votebartheme);
					dialogAddOption.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundResource(
							R.drawable.selectable_background_votebartheme);

				}
			});

			dialogAddOption.show();
			return;
		}

		if(changesMade){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			// Add the buttons
			builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (poll.getId()>-1){
						updatePoll();
					} else {
						savePoll();
					}
					dialogSave.dismiss();
					NavUtils.navigateUpFromSameTask(PollDetailActivity.this);
				}
			});
			builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialogSave.dismiss();
					NavUtils.navigateUpFromSameTask(PollDetailActivity.this);
				}
			});

			builder.setTitle(R.string.dialog_title_save_poll);
			builder.setMessage(R.string.dialog_save_poll);

			// Create the AlertDialog
			dialogSave = builder.create();

			dialogSave.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					Utility.setTextColor(dialog, getResources().getColor(R.color.theme_color));
					dialogSave.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundResource(
							R.drawable.selectable_background_votebartheme);
					dialogSave.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundResource(
							R.drawable.selectable_background_votebartheme);
				}
			});

			dialogSave.show();
		} else {
			NavUtils.navigateUpFromSameTask(PollDetailActivity.this);
		}
	}

	/**
	 * Save the poll in the database
	 */
	private void savePoll() {
		poll.setQuestion(etQuestion.getText().toString());
		poll.setOptions(options);
		try {
			long id = pollDbHelper.savePoll(poll);
			poll.setId((int)id);
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Update the poll in the database
	 */
	private void updatePoll() {
		poll.setQuestion(etQuestion.getText().toString());
		poll.setOptions(options);
		try {
			pollDbHelper.updatePoll(poll.getId(), poll);
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Network interface can be null since it is created in an async task, so we wait until the task is completed
	 * @param methodToExecute the callback to execute when the network information is created
	 */
	private void waitForNetworkInterface(final Callable<Void> methodToExecute){
		//Network interface can be null since it is created in an async task, so we wait until the task is completed
		if(AndroidApplication.getInstance().getNetworkInterface()==null){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.dialog_wait_wifi);
			final AlertDialog waitDialog = builder.create();
			waitDialog.show();

			new AsyncTask<Object, Object, Object>(){

				@Override
				protected Object doInBackground(Object... params) {
					while(AndroidApplication.getInstance().getNetworkInterface()==null){
						//wait
					}
					waitDialog.dismiss();
					try {
						methodToExecute.call();
					} catch (Exception e) {
						e.printStackTrace();
					}
					return null;
				}

			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			return;
		}
		//then start next activity
		try {
			methodToExecute.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Shows the Network Configuration or Network Information activity
	 * @return
	 */
	private Void goToNetworkConfig(){
		//then start next activity
		if(AndroidApplication.getInstance().getNetworkInterface().getGroupName()==null){
			Intent i = new Intent(this, NetworkConfigActivity.class);
			i.putExtra("poll", (Serializable)this.poll);
			startActivity(i);
		} else {
			Intent i = new Intent(this, NetworkInformationActivity.class);
			i.putExtra("poll", (Serializable)this.poll);
			startActivity(i);
		}
		return null;
	}

	/**
	 * Show the dialog containing the network informations
	 * @return
	 */
	private Void showNetworkInfoDialog(){
		NetworkDialogFragment ndf = NetworkDialogFragment.newInstance();			
		ndf.show( getFragmentManager( ), "networkInfo" );
		return null;
	}

	/**
	 * Start the vote phase
	 */
	private void startVote() {
		if(!etOption.getText().toString().equals("")){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			// Add the buttons
			builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					btnAddOption.performClick();
					btnStartPoll.performClick();
					dialogAddOption.dismiss();
					return;
				}
			});
			builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					etOption.setText("");
					btnStartPoll.performClick();
					dialogAddOption.dismiss();
					return;
				}
			});

			builder.setTitle(R.string.dialog_title_add_option);
			builder.setMessage(R.string.dialog_add_option);

			// Create the AlertDialog
			dialogAddOption = builder.create();
			dialogAddOption.show();
			return;
		}

		//save the poll
		if (poll.getId()>-1){
			updatePoll();
		}
		else {
			savePoll();
		}

		//Check if it is complete
		if(poll.getQuestion()==null || poll.getQuestion().equals("")){
			for(int i=0; i < 2; i++)
				Toast.makeText(this, getString(R.string.toast_question_empty), Toast.LENGTH_SHORT).show();
			return;
		}
		if(poll.getOptions().size()<2){
			for(int i=0; i < 2; i++)
				Toast.makeText(this, getString(R.string.toast_not_enough_options), Toast.LENGTH_SHORT).show();
			return;
		}
		for(Option o : poll.getOptions()){
			if(o.getText()==null || o.getText().equals("")){
				for(int i=0; i < 2; i++)
					Toast.makeText(this, getString(R.string.toast_option_empty), Toast.LENGTH_SHORT).show();
				return;
			}
		}

		//Network interface can be null since it is created in an async task, so we wait until the task is completed
		this.waitForNetworkInterface(new Callable<Void>() {
			public Void call() {
				return goToNetworkConfig();
			}
		});
	}
}
