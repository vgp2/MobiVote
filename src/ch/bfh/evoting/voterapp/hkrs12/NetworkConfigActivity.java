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

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import ch.bfh.evoting.voterapp.hkrs12.R;

import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;
import ch.bfh.evoting.voterapp.hkrs12.fragment.HelpDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.fragment.IdentificationWlanKeyDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.fragment.NetworkDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.fragment.NetworkOptionsFragment;
import ch.bfh.evoting.voterapp.hkrs12.network.wifi.AdhocWifiManager;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
import ch.bfh.evoting.voterapp.hkrs12.util.Utility;

/**
 * Activity displaying the available networks
 * 
 * @author Philemon von Bergen
 * 
 */
public class NetworkConfigActivity extends Activity implements TextWatcher, IdentificationWlanKeyDialogFragment.NoticeDialogListener {

	private NfcAdapter nfcAdapter;
	private boolean nfcAvailable;
	private PendingIntent pendingIntent;

	private SharedPreferences preferences;
	private EditText etIdentification;
	private BroadcastReceiver serviceStartedListener;

	private boolean active;
	private Poll poll;

	private String[] config;

	private AsyncTask<Object, Object, Object> rescanWifiTask;

	private WifiManager wifi;
	private AdhocWifiManager adhoc;
		
	private String identification;
	private String ssid;
	private boolean identificationMissing = false;
	private boolean wlanKeyMissing = false;
	
	private int networkId;
	
	private IdentificationWlanKeyDialogFragment identificationWlanKeyDialogFragment;
	public static final int DIALOG_FRAGMENT = 1;

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

		View view = getLayoutInflater().inflate(
				R.layout.activity_network_config, overlayFramelayout, false);
		overlayFramelayout.addView(view);

		final SharedPreferences settings = getSharedPreferences(
				AndroidApplication.PREFS_NAME, MODE_PRIVATE);

		if (settings.getBoolean("first_run", true)) {
			// Show General Help Overlay
			final View overlay_view = getLayoutInflater().inflate(
					R.layout.overlay_parent_button, null, false);
			overlayFramelayout.addView(overlay_view);
			overlay_view.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					overlayFramelayout.removeView(overlay_view);
					settings.edit().putBoolean("first_run", false).commit();
					// Show Help Overlay for this activity
					if (settings.getBoolean("first_run_"
							+ NetworkConfigActivity.this.getClass()
									.getSimpleName(), true)) {
						final View overlay_view = getLayoutInflater().inflate(
								R.layout.overlay_network_config, null, false);
						overlayFramelayout.addView(overlay_view);
						overlay_view
								.setOnClickListener(new View.OnClickListener() {

									@Override
									public void onClick(View v) {
										overlayFramelayout
												.removeView(overlay_view);
										settings.edit()
												.putBoolean(
														"first_run_"
																+ NetworkConfigActivity.this
																		.getClass()
																		.getSimpleName(),
														false).commit();
									}
								});
					}
				}
			});
		} else if (settings.getBoolean("first_run_"
				+ this.getClass().getSimpleName(), true)) {
			// Show Help Overlay for this activity
			final View overlay_view = getLayoutInflater().inflate(
					R.layout.overlay_network_config, null, false);
			overlayFramelayout.addView(overlay_view);
			overlay_view.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					overlayFramelayout.removeView(overlay_view);
					settings.edit()
							.putBoolean(
									"first_run_"
											+ NetworkConfigActivity.this
													.getClass().getSimpleName(),
									false).commit();
				}
			});
		}
		setContentView(overlayFramelayout);

		AndroidApplication.getInstance().setCurrentActivity(this);

		Fragment fg = new NetworkOptionsFragment();
		// adding fragment to relative layout by using layout id
		getFragmentManager().beginTransaction()
				.add(R.id.fragment_container, fg).commit();

		// Show the Up button in the action bar.
		setupActionBar();

		Poll serializedPoll = (Poll) getIntent().getSerializableExtra("poll");
		if (serializedPoll != null) {
			poll = serializedPoll;
		}

		// reading the identification from the preferences, if it is not there
		// it will try to read the name of the device owner
		preferences = getSharedPreferences(AndroidApplication.PREFS_NAME, 0);
		String identification = preferences.getString("identification", "");

		// if (identification.equals("")) {
		// identification = readOwnerName();
		// // saving the identification field
		// SharedPreferences.Editor editor = preferences.edit();
		// editor.putString("identification", identification);
		// editor.commit();
		// }

		etIdentification = (EditText) findViewById(R.id.edittext_identification);
		etIdentification.setText(identification);

		etIdentification.addTextChangedListener(this);

		serviceStartedListener = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				active = false;
				rescanWifiTask.cancel(true);
				LocalBroadcastManager.getInstance(NetworkConfigActivity.this)
						.unregisterReceiver(this);
				if (AndroidApplication.getInstance().isAdmin()) {
					Intent i = new Intent(NetworkConfigActivity.this,
							NetworkInformationActivity.class);
					i.putExtra("poll", poll);
					startActivity(i);
				} else {
					startActivity(new Intent(NetworkConfigActivity.this,
							CheckElectorateActivity.class));
				}
			}
		};

		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		active = true;
		rescanWifiTask = new AsyncTask<Object, Object, Object>() {

			@Override
			protected Object doInBackground(Object... arg0) {

				while (active) {
					SystemClock.sleep(5000);
					wifi.startScan();
				}
				return null;
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

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
		// if extra is present, it has priority on the saved poll
		Poll serializedPoll = (Poll) intent.getSerializableExtra("poll");
		if (serializedPoll != null) {
			poll = serializedPoll;
		}

		if (intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) != null) {
			Intent broadcastIntent = new Intent(
					BroadcastIntentTypes.nfcTagTapped);
			broadcastIntent.putExtra(NfcAdapter.EXTRA_TAG,
					intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
			LocalBroadcastManager.getInstance(this).sendBroadcast(
					broadcastIntent);
		}

		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		Ndef ndef = Ndef.get(tag);

		if (ndef == null) {
			Toast.makeText(this,
					getResources().getText(R.string.toast_nfc_tag_read_failed),
					Toast.LENGTH_LONG).show();
		} else {
			NdefMessage msg;
			msg = ndef.getCachedNdefMessage();
			config = new String(msg.getRecords()[0].getPayload())
					.split("\\|\\|");

			// saving the values that we got
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString("SSID", ssid);
			editor.commit();
			
			if (checkIdentification()){
				connect(config, this);
			}
		}

		super.onNewIntent(intent);
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putSerializable("poll", poll);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		poll = (Poll) savedInstanceState.getSerializable("poll");
	}

	@Override
	protected void onPause() {
		active = false;
		rescanWifiTask.cancel(true);

		if (nfcAvailable) {
			nfcAdapter.disableForegroundDispatch(this);
		}

		super.onPause();
	}

	@Override
	protected void onResume() {
		AndroidApplication.getInstance().setCurrentActivity(this);
		LocalBroadcastManager
				.getInstance(NetworkConfigActivity.this)
				.registerReceiver(
						serviceStartedListener,
						new IntentFilter(
								BroadcastIntentTypes.networkConnectionSuccessful));

		active = true;
		rescanWifiTask = new AsyncTask<Object, Object, Object>() {

			@Override
			protected Object doInBackground(Object... arg0) {

				while (active) {
					SystemClock.sleep(5000);
					wifi.startScan();
				}
				return null;
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

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
		getMenuInflater().inflate(R.menu.network_config, menu);
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
			// NavUtils.navigateUpFromSameTask(this);
			super.onBackPressed();
			return true;
		case R.id.network_info:
			NetworkDialogFragment ndf = NetworkDialogFragment.newInstance();
			ndf.show(getFragmentManager(), "networkInfo");
			return true;
		case R.id.help:
			HelpDialogFragment hdf = HelpDialogFragment.newInstance(
					getString(R.string.help_title_network_config),
					getString(R.string.help_text_network_config));
			hdf.show(getFragmentManager(), "help");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void afterTextChanged(Editable s) {
		// saving the identification field
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("identification", etIdentification.getText()
				.toString());
		editor.commit();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

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

	// Commented out in order to be able to remove the permissions
	// /**
	// * This method is used to extract the name of the device owner
	// *
	// * @return the name of the device owner
	// */
	// private String readOwnerName() {
	//
	// Cursor c = getContentResolver().query(
	// ContactsContract.Profile.CONTENT_URI, null, null, null, null);
	// if (c.getCount() == 0) {
	// return "";
	// }
	// c.moveToFirst();
	// String displayName = c.getString(c.getColumnIndex("display_name"));
	// c.close();
	//
	// return displayName;
	//
	// }

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 0) {
			if (resultCode == RESULT_OK) {
				String[] config = intent.getStringExtra("SCAN_RESULT").split(
						"\\|\\|");

				// saving the values that we got
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString("SSID", config[0]);
				editor.commit();

				AndroidApplication.getInstance().getNetworkInterface()
						.setGroupName(config[1]);
				AndroidApplication.getInstance().getNetworkInterface()
						.setGroupPassword(config[2]);

				if (checkIdentification()){
				// connect to the network
				connect(config, this);
				}

			} else if (resultCode == RESULT_CANCELED) {
				// Handle cancel
			}
		}
	}

	public String getIdentification() {
		Log.d("NetworkConfigActivity", "identification is "
				+ this.etIdentification.toString());
		return this.etIdentification.getText().toString();
	}

	/**
	 * This method initiates the connect process
	 * 
	 * @param config
	 *            an array containing the SSID and the password of the network
	 * @param context
	 *            android context
	 */
	public void connect(String[] config, Context context) {
		
		identificationMissing = false;
		wlanKeyMissing = false;

		wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		adhoc = new AdhocWifiManager(wifi);
		ssid = config[0];
		
		identification = preferences.getString("identification", "");
		
		if (identification.equals("")){
			identificationMissing  = true;
		}

		boolean connectedSuccessful = false;
		
		// check whether the network is already known, i.e. the password is
		// already stored in the device
		for (WifiConfiguration configuredNetwork : wifi.getConfiguredNetworks()) {
			if (configuredNetwork.SSID.equals("\"".concat(ssid).concat(
					"\""))) {
				connectedSuccessful = true;
				networkId = configuredNetwork.networkId;
				break;
			}
		}
		
		if (!connectedSuccessful) {
			for (ScanResult result : wifi.getScanResults()) {
				if (result.SSID.equals(ssid)) {
					connectedSuccessful = true;
					
					if (result.capabilities.contains("WPA")
							|| result.capabilities.contains("WEP")) {
						wlanKeyMissing = true;
					} 
					break;
				}
			}
		}
		
		if (connectedSuccessful){
		
			if (identificationMissing || wlanKeyMissing){
				identificationWlanKeyDialogFragment = new IdentificationWlanKeyDialogFragment(identificationMissing, wlanKeyMissing);
				identificationWlanKeyDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog);
				identificationWlanKeyDialogFragment.show(getFragmentManager(), "identificationWlanKeyDialogFragment");
			}
			else {
				adhoc.connectToNetwork(networkId, this);
			}
		}
		else {
			for(int i=0; i<2; i++)
				Toast.makeText(this, getString(R.string.toast_network_not_found_text, ssid), Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onDialogPositiveClick(DialogFragment dialog) {
		if (identificationMissing){
    		SharedPreferences.Editor editor = preferences.edit();
			editor.putString("identification", ((IdentificationWlanKeyDialogFragment)dialog).getIdentification());
			editor.commit();
    	}
    	
    	if (wlanKeyMissing){
    		adhoc.connectToNetwork(ssid, ((IdentificationWlanKeyDialogFragment)dialog).getWlanKey(), this);
    	}
    	else {
    		adhoc.connectToNetwork(networkId, this);
    	}

    	dialog.dismiss();
	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
		dialog.dismiss();
	}
	
	/**
	 * Controls if identification is empty and shows a dialog
	 * @return true if identification was filled, false otherwise
	 */
	private boolean checkIdentification() {
		if(this.getIdentification().equals("")){
					
			for(int i=0; i<2; i++)
				Toast.makeText(this, R.string.toast_no_identification, Toast.LENGTH_SHORT).show();

			return false;
		}
		return true;
	}

}
