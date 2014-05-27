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
package ch.bfh.evoting.voterapp.hkrs12.fragment;


import ch.bfh.evoting.voterapp.hkrs12.R;

import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import ch.bfh.evoting.voterapp.hkrs12.NetworkConfigActivity;
import ch.bfh.evoting.voterapp.hkrs12.network.wifi.AdhocWifiManager;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * Fragment displaying the review of a poll
 * 
 */
public class NetworkOptionsFragment extends Fragment {

	public static final int DIALOG_FRAGMENT = 1;
	private static final String TAG = NetworkListFragment.class.getSimpleName();

	private NetworkConfigActivity activity;

	private ConnectNetworkDialogFragment dialogFragment;
	private BroadcastReceiver ssidChangeReceiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		this.activity = (NetworkConfigActivity)this.getActivity();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.fragment_network_options, container, false);

		final Button btnUseActualNetwork = (Button)v.findViewById(R.id.button_use_actual_ssid);
		String ssid = AndroidApplication.getInstance().getNetworkMonitor().getConnectedSSID();
		ssid = ssid.replace("\"", "");
		btnUseActualNetwork.setText(getString(R.string.button_use_actual_ssid, ssid));
		Button btnScanQrCode = (Button)v.findViewById(R.id.button_capture_qrcode);
		Button btnScanNFC = (Button)v.findViewById(R.id.button_scan_nfc);
		Button btnAdvancedConfig = (Button)v.findViewById(R.id.button_advanced_network_config);

		LinearLayout layout = (LinearLayout)v.findViewById(R.id.layout_network_options);

		if(AndroidApplication.getInstance().isAdmin()){
			layout.removeView(btnScanQrCode);
			layout.removeView(btnScanNFC);
		}

		if(!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC) && btnScanNFC!=null){
			layout.removeView(btnScanNFC);
		}

		btnUseActualNetwork.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//check if an identification is defined
				if(!checkIdentification()) return;

				if(!AndroidApplication.getInstance().getNetworkMonitor().isConnected()){
					for(int i=0; i<2; i++)
						Toast.makeText(NetworkOptionsFragment.this.getActivity(), getString(R.string.toast_wifi_is_not_connected), Toast.LENGTH_SHORT).show();
					return;
				}

				dialogFragment = new ConnectNetworkDialogFragment(false);

				dialogFragment.setTargetFragment(NetworkOptionsFragment.this, DIALOG_FRAGMENT);
				dialogFragment.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Dialog);
				dialogFragment.show(getFragmentManager(), TAG);
			}

		});

		if(btnScanQrCode!=null){
			btnScanQrCode.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					//check if an identification is defined
					if(!checkIdentification()) return;
					if(!AndroidApplication.getInstance().getNetworkMonitor().isWifiEnabled()){
						for(int i=0; i<2; i++)
							Toast.makeText(NetworkOptionsFragment.this.getActivity(), getString(R.string.toast_wifi_is_disabled), Toast.LENGTH_SHORT).show();
						return;
					}

					Intent intent = new Intent("com.google.zxing.client.android.SCAN");
					intent.setPackage(activity.getApplication().getPackageName());
					intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
					startActivityForResult(intent, 0);

				}
			});
		}

		if(btnScanNFC!=null){
			btnScanNFC.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					//check if an identification is defined
					if(!checkIdentification()) return;
					if(!AndroidApplication.getInstance().getNetworkMonitor().isWifiEnabled()){
						for(int i=0; i<2; i++)
							Toast.makeText(NetworkOptionsFragment.this.getActivity(), getString(R.string.toast_wifi_is_disabled), Toast.LENGTH_SHORT).show();
						return;
					}

					// Create new fragment and transaction
					Fragment nfcFragment = new NFCFragment();
					FragmentTransaction transaction = getFragmentManager().beginTransaction();

					// Replace whatever is in the fragment_container view with this fragment,
					// and add the transaction to the back stack
					transaction.replace(R.id.fragment_container, nfcFragment);
					transaction.addToBackStack(null);

					// Commit the transaction
					transaction.commit();

				}
			});
		}

		btnAdvancedConfig.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//check if an identification is defined
				if(!checkIdentification()) return;
				if(!AndroidApplication.getInstance().getNetworkMonitor().isWifiEnabled()){
					for(int i=0; i<2; i++)
						Toast.makeText(NetworkOptionsFragment.this.getActivity(), getString(R.string.toast_wifi_is_disabled), Toast.LENGTH_SHORT).show();
					return;
				}

				// Create new fragment and transaction
				Fragment advancedNetworkFragment = new NetworkListFragment();
				FragmentTransaction transaction = getFragmentManager().beginTransaction();

				// Replace whatever is in the fragment_container view with this fragment,
				// and add the transaction to the back stack
				transaction.replace(R.id.fragment_container, advancedNetworkFragment);
				transaction.addToBackStack(null);

				// Commit the transaction
				transaction.commit();

			}
		});

		//broadcast receiving SSID changes updates and changes the text on the first button
		ssidChangeReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String ssid = AndroidApplication.getInstance().getNetworkMonitor().getConnectedSSID();
				ssid = ssid.replace("\"", "");
				btnUseActualNetwork.setText(getString(R.string.button_use_actual_ssid, ssid));
			}
		};
		LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(ssidChangeReceiver, new IntentFilter(BroadcastIntentTypes.networkSSIDUpdate));

		return v;
	}
	
	

	@Override
	public void onDetach() {
		super.onDetach();
		LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(ssidChangeReceiver);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(ssidChangeReceiver);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode){

		case DIALOG_FRAGMENT:

			WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
			AdhocWifiManager adhoc = new AdhocWifiManager(wifiManager);

			if (resultCode == Activity.RESULT_OK) {

				adhoc.connectToNetwork(this.getCurrentSsid(this.getActivity()),
						((ConnectNetworkDialogFragment) dialogFragment).getNetworkKey(),
						getActivity());


				dialogFragment.dismiss();
			} else if (resultCode == Activity.RESULT_CANCELED){
				dialogFragment.dismiss();
			}

			break;
		default:
			activity.onActivityResult(requestCode, resultCode, data);
		}
	}

	/*--------------------------------------------------------------------------------------------
	 * Helper Methods
	--------------------------------------------------------------------------------------------*/

	/**
	 * Controls if identification is empty and shows a dialog
	 * @return true if identification was filled, false otherwise
	 */
	private boolean checkIdentification() {
		if(((NetworkConfigActivity)this.getActivity()).getIdentification().equals("")){
			
			for(int i=0; i<2; i++)
				Toast.makeText(this.getActivity(), R.string.toast_no_identification, Toast.LENGTH_SHORT).show();

			return false;
		}
		return true;
	}

	/**
	 * Return the ssid of the currently connected network
	 * @param context android context
	 * @return the ssid of the currently connected network
	 */
	public String getCurrentSsid(Context context) {
		String ssid = null;
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (networkInfo.isConnected()) {
			final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
			if (connectionInfo != null && !connectionInfo.getSSID().equals("")) {
				ssid = connectionInfo.getSSID();
			}
		}
		return ssid;
	}
}
