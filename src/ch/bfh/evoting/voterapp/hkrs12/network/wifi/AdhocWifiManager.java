
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
package ch.bfh.evoting.voterapp.hkrs12.network.wifi;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import ch.bfh.evoting.voterapp.hkrs12.R;
import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
import ch.bfh.evoting.voterapp.hkrs12.util.Utility;

/**
 * This class implements methods which are used to adjust the wifi configuration
 * of the device
 * 
 * @author Juerg Ritter (rittj1@bfh.ch)
 */
public class AdhocWifiManager {

	private static final String TAG = AdhocWifiManager.class.getSimpleName();

	private WifiManager wifi;
	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;
	private String SSID;

	/**
	 * Instatiates a new instance
	 * 
	 * @param wifi
	 *            the android wifi manager which should be used
	 * 
	 */
	public AdhocWifiManager(WifiManager wifi) {
		this.wifi = wifi;				
	}

	/**
	 * Connects to a network by applying a WifiConfiguration
	 * 
	 * @param config
	 *            the WifiConfiguration which will be applied
	 * @param context
	 *            the android context from which this functionality is used
	 */
	public void connectToNetwork(WifiConfiguration config, Context context) {
		new ConnectWifiTask(config, context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Connects to a network using an SSID and a password
	 * 
	 * @param ssid
	 *            the ssid as string (without double-quotes) to which will be
	 *            connected
	 * @param password
	 *            the network key
	 * @param context
	 *            the android context from which this functionality is used
	 */
	public void connectToNetwork(final String ssid, final String password,
			final Context context) {
		connectToNetwork(ssid, password, context, true);
	}

	/**
	 * Connects to a network using an SSID and a password
	 * 
	 * @param ssid
	 *            the ssid as string (without double-quotes) to which will be
	 *            connected
	 * @param password
	 *            the network key
	 * @param context
	 *            the android context from which this functionality is used
	 * @param startActivity
	 *            defines whether the NetworkActiveActivity should be started
	 *            after connecting
	 */
	public void connectToNetwork(final String ssid, final String password,
			final Context context, final boolean startActivity) {
		Log.d(TAG, "connect using SSID...");
		new ConnectWifiTask(ssid, password, context, startActivity).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Connects to a network using a predefined networkId.
	 * 
	 * @param networkId
	 *            the id of the predefined (or known) network configuration on
	 *            the device
	 * @param context
	 *            the android context from which this functionality is used
	 */
	public void connectToNetwork(final int networkId, final Context context) {
		Log.d(TAG, "Connect using netid... " + networkId);
		new ConnectWifiTask(networkId, context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Restores the previously configured network configuration
	 * 
	 * @param context
	 *            the android context from which this functionality is used
	 */
	public void restoreWifiConfiguration(Context context) {
		new RestoreWifiConfigurationTask(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * AsyncTask which encapsulates the pretty time consuming logic to connect
	 * to wifi networks into an independetly running task. During execution, a
	 * progress dialog will be displayed on the screen.
	 * 
	 * @author Juerg Ritter (rittj1@bfh.ch)
	 */
	class ConnectWifiTask extends AsyncTask<Void, Void, Void> {

		private Context context;
		private ProgressDialog d;
		private WifiConfiguration config;
		private boolean success;
		private String password;
		private String ssid;
		private int networkId = -1;

		private boolean startActivity = true;

		/**
		 * Initializing the task by defining an ssid and a password
		 * 
		 * @param ssid
		 *            the SSID to which should be connected
		 * @param password
		 *            the password for the network which is used to connect
		 *            securely to a network
		 * @param context
		 *            the android context from which this functionality is used
		 * @param startActivity
		 *            defines whether the NetworkActiveActivity should be
		 *            started after connecting
		 */
		public ConnectWifiTask(String ssid, String password, Context context,
				boolean startActivity) {

			this.ssid = ssid;
			this.password = password;
			this.context = context;
			this.startActivity = startActivity;

			config = new WifiConfiguration();
			config.SSID = ssid;
			d = new ProgressDialog(context);
		}

		/**
		 * Initializing the task by specifying a WifiConfiguration
		 * 
		 * @param config
		 *            the WifiConfiguration which will be applied
		 * @param context
		 *            the android context from which this functionality is used
		 */
		public ConnectWifiTask(WifiConfiguration config, Context context) {

			this.config = config;
			this.context = context;
			this.password = config.preSharedKey;
			this.ssid = config.SSID;

			d = new ProgressDialog(context);
		}

		/**
		 * Initializing the task by specifying a WifiConfiguration
		 * 
		 * @param networkId
		 *            the id of the predefined (or known) network configuration
		 *            on the device
		 * @param context
		 *            the android context from which this functionality is used
		 */
		public ConnectWifiTask(int networkId, Context context) {
			this.networkId = networkId;
			this.context = context;

			// iterate over all the known network configurations in the device
			// and get the configuration with the corresponding networkId
			for (WifiConfiguration config : wifi.getConfiguredNetworks()) {
				if (config.networkId == networkId) {
					this.config = config;
					break;
				}
			}
			this.ssid = config.SSID;


		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			//d.setTitle("Connecting to Network " + ssid + "...");
			d = new ProgressDialog(context);
			d.setMessage("...please wait a moment.");
			d.setProgressStyle(ProgressDialog.THEME_HOLO_LIGHT);
			d.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					Utility.setTextColor(dialog, context.getResources().getColor(R.color.theme_color));
					Log.d(TAG, "Magic hack fired for Progress Dialog");
				}
			});
			d.show();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Void doInBackground(Void... params) {

			// make sure that wifi on the device is enabled
			wifi.setWifiEnabled(true);

			// extract the current networkId and store it in the preferences
			// file
			preferences = context.getSharedPreferences(AndroidApplication.PREFS_NAME, 0);
			editor = preferences.edit();
			editor.putInt("originalNetId", wifi.getConnectionInfo()
					.getNetworkId());
			editor.commit();
			SSID = config.SSID.replace("\"", "");
			if(wifi.getConnectionInfo().getSSID()!=null){
				Log.d(TAG, "SSID to connect to: " +SSID+ " . Currently connected SSID: "+ wifi.getConnectionInfo().getSSID().replace("\"", ""));
				if(SSID.equals(wifi.getConnectionInfo().getSSID().replace("\"", ""))){
					success = true;
					return null;
				}
			}

			Log.d(TAG, "Connect to: " + SSID);
			// handle the strange habit with the double quotes...
			config.SSID = "\"" + config.SSID + "\"";

			if (networkId != -1) {
				// Configuration already exists, no need to create a new one...
				success = wifi.enableNetwork(networkId, true);
				//Sleep in order to let time to disconnect from current wifi and connect to new
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {

				// make sure that we have scan results before we continue
				if (wifi.getScanResults() == null) {
					wifi.startScan();

					// wait until we get scanresults
					int maxLoops = 10;
					int i = 0;
					while (i < maxLoops) {
						if (wifi.getScanResults() != null) {
							break;
						}
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						i++;
					}
					if (wifi.getScanResults() == null) {
						success = false;
						return null;
					}
				}

				// Configuration has to be created and added
				config.allowedAuthAlgorithms.clear();
				config.allowedGroupCiphers.clear();
				config.allowedKeyManagement.clear();
				config.allowedPairwiseCiphers.clear();
				config.allowedProtocols.clear();

				// iterate over the scanresults, extract their properties and
				// create a new WifiConfiguration from these parameters
				List<ScanResult> results = wifi.getScanResults();
				for (ScanResult result : results) {
					if (result.SSID.equals(ssid)) {
						config.hiddenSSID = false;
						config.priority = 10000;
						config.SSID = "\"".concat(result.SSID).concat("\"");

						// handling the different types of security
						if (result.capabilities.contains("WPA")) {
							config.preSharedKey = "\"".concat(password).concat(
									"\"");
							config.allowedGroupCiphers
							.set(WifiConfiguration.GroupCipher.TKIP);
							config.allowedGroupCiphers
							.set(WifiConfiguration.GroupCipher.CCMP);
							config.allowedKeyManagement
							.set(WifiConfiguration.KeyMgmt.WPA_PSK);
							config.allowedPairwiseCiphers
							.set(WifiConfiguration.PairwiseCipher.TKIP);
							config.allowedPairwiseCiphers
							.set(WifiConfiguration.PairwiseCipher.CCMP);
							config.allowedProtocols
							.set(WifiConfiguration.Protocol.RSN);
							config.allowedProtocols
							.set(WifiConfiguration.Protocol.WPA);
						} else if (result.capabilities.contains("WEP")) {
							config.wepKeys[0] = "\"" + password + "\"";
							config.wepTxKeyIndex = 0;
							config.allowedKeyManagement
							.set(WifiConfiguration.KeyMgmt.NONE);
							config.allowedGroupCiphers
							.set(WifiConfiguration.GroupCipher.WEP40);
						} else {
							config.allowedKeyManagement
							.set(WifiConfiguration.KeyMgmt.NONE);
						}
						config.status = WifiConfiguration.Status.ENABLED;
						break;
					}
				}

				// add the network to the known network configuration and enable
				// it
				networkId = wifi.addNetwork(config);
				wifi.saveConfiguration();
				success = wifi.enableNetwork(networkId, true);
				//Sleep in order to let time to disconnect from current wifi and connect to new
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			// wait until the connection is actually established
			ConnectivityManager conn = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo nInfo = null;

			int i = 0;
			int maxLoops = 10;
			while (i < maxLoops) {
				nInfo = conn.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				Log.d(TAG, nInfo.getDetailedState().toString() + "  "
						+ nInfo.getState().toString());
				if (nInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED
						&& nInfo.getState() == NetworkInfo.State.CONNECTED
						&& getBroadcastAddress() != null && nInfo.isConnected()) {
					Log.d(TAG, "Connected!");

					break;
				}
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				i++;
			}
			
			Log.d(TAG, "Will sleep in order to wait until really connected");
			
			//wait until really connected and ready to establish sessions
			try {
				Thread.sleep(8000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			Log.d(TAG, "given SSID " + ssid);
			Log.d(TAG, "SSID from wifiman: " + wifi.getConnectionInfo().getSSID());
			Log.d(TAG, "Equals: " + ssid.equals(wifi.getConnectionInfo().getSSID()));

			// check whether we have been successful
			if (!(nInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED
					&& nInfo.getState() == NetworkInfo.State.CONNECTED && getBroadcastAddress() != null)) {
				success = false;
			} else {
				// start the network service if we were successful
				if (startActivity) {
					success = true;
				}
			}
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		@SuppressLint("ShowToast")
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (startActivity) {
				if (success) {
					// start the service if we were successful
					preferences = context.getSharedPreferences(AndroidApplication.PREFS_NAME, 0);
					editor = preferences.edit();
					editor.putString("SSID", SSID);
					editor.commit();


					//Register listener on group connexion event to dismiss the dialog
					LocalBroadcastManager.getInstance(context).registerReceiver(new BroadcastReceiver() {

						@Override
						public void onReceive(Context arg0, Intent intent) {

							try{
								d.dismiss();
							} catch(Throwable t){
								//do nothing;
							}
						}
					}, new IntentFilter(BroadcastIntentTypes.networkConnectionFailed));
					LocalBroadcastManager.getInstance(context).registerReceiver(new BroadcastReceiver() {

						@Override
						public void onReceive(Context arg0, Intent arg1) {
							try{
								d.dismiss();
							} catch(Throwable t){
								//do nothing;
							}
						}
					}, new IntentFilter(BroadcastIntentTypes.networkConnectionSuccessful));

					Log.d(TAG, "Trying to connect to AllJoyn!");
					String groupName = AndroidApplication.getInstance().getNetworkInterface().getGroupName();
					AndroidApplication.getInstance().getNetworkInterface().joinGroup(groupName);
					

				} else {
					try{
						d.dismiss();
					} catch(Throwable t){
						//do nothing;
					}

					for(int i=0; i<2; i++)
						Toast.makeText(AndroidApplication.getInstance(), R.string.toast_content_connect_failed, Toast.LENGTH_SHORT).show();
				}
			} else {
				d.dismiss();
				Log.e(TAG, "Wireless connection failed");
				Toast.makeText(context, context.getString(R.string.toast_network_connection_error), Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * AsyncTask which restores the restoration of the original network
	 * configuration
	 * 
	 * @author Juerg Ritter (rittj1@bfh.ch)
	 */
	class RestoreWifiConfigurationTask extends AsyncTask<Void, Void, Void> {

		private Context context;

		/**
		 * Initializing the task
		 * 
		 * @param context
		 *            the android context from which this functionality is used
		 */
		public RestoreWifiConfigurationTask(Context context) {
			this.context = context;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Void doInBackground(Void... params) {
			preferences = context.getSharedPreferences(AndroidApplication.PREFS_NAME, 0);
			wifi.enableNetwork(preferences.getInt("originalNetId", 0), true);

			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
		}
	}

	/**
	 * A helper method to find out the broadcast address of the current network
	 * configuration
	 * 
	 * @return the broadcast address
	 */
	public InetAddress getBroadcastAddress() {
		DhcpInfo myDhcpInfo = wifi.getDhcpInfo();
		int broadcast = (myDhcpInfo.ipAddress & myDhcpInfo.netmask)
				| ~myDhcpInfo.netmask;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++) {
			quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
		}
		try {
			return InetAddress.getByAddress(quads);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * A helper method to find out the IP address of the current network
	 * configuration
	 * 
	 * @return the IP address
	 */
	public InetAddress getIpAddress() {
		DhcpInfo myDhcpInfo = wifi.getDhcpInfo();
		int ipaddress = myDhcpInfo.ipAddress;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++) {
			quads[k] = (byte) ((ipaddress >> k * 8) & 0xFF);
		}
		try {
			return InetAddress.getByAddress(quads);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}
}
