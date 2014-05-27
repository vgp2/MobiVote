
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

import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import ch.bfh.evoting.voterapp.hkrs12.R;

import ch.bfh.evoting.voterapp.hkrs12.network.wifi.WifiAPManager;
import ch.bfh.evoting.voterapp.hkrs12.util.Utility;

/**
 * Activity which provides functionality to set up a new Wifi access point
 * 
 * @author Juerg Ritter (rittj1@bfh.ch)
 * 
 */
public class CreateNetworkActivity extends Activity implements OnClickListener,
TextWatcher {
	
	private WifiAPManager wifiapman;
	private WifiManager wifiman;
	private Button btnCreateNetwork;

	private EditText txtNetworkName;
	private EditText txtNetworkPIN;
	private AlertDialog dialogUseAP;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// apply the layout
		setContentView(R.layout.activity_create_network);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// extract the control elements from the layout
		txtNetworkName = (EditText) findViewById(R.id.edittext_network_name);
		txtNetworkPIN = (EditText) findViewById(R.id.edittext_network_pin);

		btnCreateNetwork = (Button) findViewById(R.id.create_network_button);
		btnCreateNetwork.setOnClickListener(this);

		wifiapman = new WifiAPManager();
		wifiman = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		WifiConfiguration config = wifiapman.getWifiApConfiguration(wifiman);
		txtNetworkName.setText(config.SSID);
		txtNetworkPIN.addTextChangedListener(this);
		txtNetworkPIN.setText(config.preSharedKey);

		// make a suggestion of a password
		if (config.preSharedKey == null || config.preSharedKey.length() < 8) {
			txtNetworkPIN.setText(UUID.randomUUID().toString().substring(0, 8));
		}

		// asking if we should use the already running access point
		if (wifiapman.isWifiAPEnabled(wifiman)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getResources().getString(R.string.dialog_title_access_point));
			builder.setMessage(getResources().getString(R.string.dialog_content_access_point));
			builder.setPositiveButton(getResources().getString(R.string.yes),
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {

					AndroidApplication.getInstance().getNetworkInterface().joinGroup(null);

					CreateNetworkActivity.this.finish();
				}
			});
			builder.setNegativeButton(getResources().getString(R.string.no),
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					return;
				}
			});
			dialogUseAP = builder.create();
			
			dialogUseAP.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					Utility.setTextColor(dialog, getResources().getColor(R.color.theme_color));
					dialogUseAP.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundResource(
							R.drawable.selectable_background_votebartheme);
					dialogUseAP.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundResource(
							R.drawable.selectable_background_votebartheme);
				}
			});
			
			dialogUseAP.show();
		}

		// enable the create button only if the key has a sufficient length
		if (txtNetworkPIN.getText().toString().length() < 8) {
			btnCreateNetwork.setEnabled(false);
		} else {
			btnCreateNetwork.setEnabled(true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_create_network, menu);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View view) {
		if (view == btnCreateNetwork) {
			// setting up the the configuration
			if (wifiapman.isWifiAPEnabled(wifiman)) {
				wifiapman.disableHotspot(wifiman, this);
			}

			WifiConfiguration wificonfig = new WifiConfiguration();
			wificonfig.SSID = txtNetworkName.getText().toString();
			wificonfig.preSharedKey = txtNetworkPIN.getText().toString();

			wificonfig.hiddenSSID = false;
			wificonfig.status = WifiConfiguration.Status.ENABLED;

			wificonfig.allowedGroupCiphers
			.set(WifiConfiguration.GroupCipher.TKIP);
			wificonfig.allowedGroupCiphers
			.set(WifiConfiguration.GroupCipher.CCMP);
			wificonfig.allowedKeyManagement
			.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			wificonfig.allowedPairwiseCiphers
			.set(WifiConfiguration.PairwiseCipher.TKIP);
			wificonfig.allowedPairwiseCiphers
			.set(WifiConfiguration.PairwiseCipher.CCMP);
			wificonfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

			SharedPreferences preferences = this.getSharedPreferences(AndroidApplication.PREFS_NAME, 0);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString("wlan_key", txtNetworkPIN.getText().toString());
			editor.commit();
			
			// enabling the configuration
			wifiapman.enableHotspot(wifiman, wificonfig, this);
			
			
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.text.TextWatcher#onTextChanged(java.lang.CharSequence, int,
	 * int, int)
	 */
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.text.TextWatcher#afterTextChanged(android.text.Editable)
	 */
	public void afterTextChanged(Editable s) {
		// always check after editing the password field whether it has a
		// sufficient length and set the status of the create network button
		// accordingly
		if (txtNetworkPIN.getText().toString().length() < 8) {
			btnCreateNetwork.setEnabled(false);
		} else {
			btnCreateNetwork.setEnabled(true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.text.TextWatcher#beforeTextChanged(java.lang.CharSequence,
	 * int, int, int)
	 */
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

	}
	
}
