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

import java.util.concurrent.Callable;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
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
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import ch.bfh.evoting.voterapp.hkrs12.R;

import ch.bfh.evoting.voterapp.hkrs12.fragment.HelpDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.fragment.IdentificationWlanKeyDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.fragment.NetworkDialogFragment;
import ch.bfh.evoting.voterapp.hkrs12.network.wifi.AdhocWifiManager;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
import ch.bfh.evoting.voterapp.hkrs12.util.Utility;

/**
 * First activity, displaying the buttons for the different actions
 * 
 * @author Philemon von Bergen
 * 
 */
public class MainActivity extends Activity implements OnClickListener, IdentificationWlanKeyDialogFragment.NoticeDialogListener {

	private NfcAdapter nfcAdapter;
	private boolean nfcAvailable;
	private PendingIntent pendingIntent;

	private BroadcastReceiver serviceStartedListener;

	private Button btnSetupNetwork;
	private Button btnPollArchive;
	private Button btnPolls;

	private Parcelable[] rawMsgs;
	private String[] config;

	private SharedPreferences preferences;

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

		setContentView(R.layout.activity_main);
		AndroidApplication.getInstance().setVoteRunning(false);
		AndroidApplication.getInstance().setCurrentActivity(this);

		AndroidApplication.getInstance().setIsAdmin(false);

		btnSetupNetwork = (Button) findViewById(R.id.button_joinnetwork);
		btnPolls = (Button) findViewById(R.id.button_polls);
		btnPollArchive = (Button) findViewById(R.id.button_archive);

		btnSetupNetwork.setOnClickListener(this);
		btnPolls.setOnClickListener(this);
		btnPollArchive.setOnClickListener(this);

		preferences = getSharedPreferences(AndroidApplication.PREFS_NAME, 0);

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

			rawMsgs = null;
			rawMsgs = getIntent().getParcelableArrayExtra(
					NfcAdapter.EXTRA_NDEF_MESSAGES);
			// see whether we got launched with an NFC tag

			if (rawMsgs != null) {
				NdefMessage msg = (NdefMessage) rawMsgs[0];

				config = new String(msg.getRecords()[0].getPayload())
				.split("\\|\\|");

				// saving the values that we got
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString("SSID", ssid);
				editor.commit();

				this.waitForNetworkInterface(new Callable<Void>() {
					public Void call() {
						Log.d("lkjdsafl", "call called");
						return joinNetwork();
					}
				});

			}
		}

		serviceStartedListener = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				LocalBroadcastManager.getInstance(MainActivity.this)
				.unregisterReceiver(this);
				startActivity(new Intent(MainActivity.this,
						CheckElectorateActivity.class));
			}
		};
		LocalBroadcastManager.getInstance(this).registerReceiver(
				serviceStartedListener,
				new IntentFilter(
						BroadcastIntentTypes.networkConnectionSuccessful));
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
		AndroidApplication.getInstance().setIsAdmin(false);
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
	public void onClick(View view) {
		if (view == btnSetupNetwork) {
			if (!AndroidApplication.getInstance().getNetworkMonitor()
					.isWifiEnabled()) {
				for (int i = 0; i < 2; i++)
					Toast.makeText(this,
							getString(R.string.toast_wifi_is_disabled),
							Toast.LENGTH_SHORT).show();
				return;
			}
			this.waitForNetworkInterface(new Callable<Void>() {
				public Void call() {
					return goToNetworkConfig();
				}
			});

		} else if (view == btnPolls) {
			Intent intent = new Intent(this, PollActivity.class);
			startActivity(intent);
		} else if (view == btnPollArchive) {
			Intent intent = new Intent(this, ListTerminatedPollsActivity.class);
			startActivity(intent);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.network_info:
			this.waitForNetworkInterface(new Callable<Void>() {
				public Void call() {
					return showNetworkInfoDialog();
				}
			});
			return true;
		case R.id.help:
			HelpDialogFragment hdf = HelpDialogFragment.newInstance(
					getString(R.string.help_title_main),
					getString(R.string.help_text_main));
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

		if (intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) != null) {

			Intent broadcastIntent = new Intent(
					BroadcastIntentTypes.nfcTagTapped);
			broadcastIntent.putExtra(NfcAdapter.EXTRA_TAG,
					intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
			LocalBroadcastManager.getInstance(this).sendBroadcast(
					broadcastIntent);

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

				this.waitForNetworkInterface(new Callable<Void>() {
					public Void call() {
						return joinNetwork();
					}
				});
			}
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Helper Methods
	--------------------------------------------------------------------------------------------*/

	private void waitForNetworkInterface(final Callable<Void> methodToExecute) {
		// Network interface can be null since it is created in an async task,
		// so we wait until the task is completed
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
					return null;
				}

				@Override
				protected void onPostExecute(Object result) {
					super.onPostExecute(result);
					waitDialog.dismiss();
					try {
						methodToExecute.call();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			return;
		}
		// then start next activity
		try {
			methodToExecute.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Void goToNetworkConfig() {
		// then start next activity
		if (AndroidApplication.getInstance().getNetworkInterface()
				.getGroupName() == null) {
			Intent intent = new Intent(this, NetworkConfigActivity.class);
			intent.putExtra("hideCreateNetwork", true);
			startActivity(intent);
		} else {
			Intent i = new Intent(this, NetworkInformationActivity.class);
			startActivity(i);
		}
		return null;
	}

	private Void joinNetwork() {
		// then start next activity
		AndroidApplication.getInstance().getNetworkInterface()
		.setGroupName(config[1]);
		AndroidApplication.getInstance().getNetworkInterface()
		.setGroupPassword(config[2]);

		Log.d("Join Network", AndroidApplication.getInstance()
				.getNetworkInterface().getGroupName());
		Log.d("Join Network", AndroidApplication.getInstance()
				.getNetworkInterface().getGroupPassword());

		// connect to the network
		connect(config, MainActivity.this);

		return null;
	}

	private Void showNetworkInfoDialog() {
		NetworkDialogFragment ndf = NetworkDialogFragment.newInstance();
		ndf.show(getFragmentManager(), "networkInfo");
		return null;
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
		identificationWlanKeyDialogFragment.dismiss();
	}

}
