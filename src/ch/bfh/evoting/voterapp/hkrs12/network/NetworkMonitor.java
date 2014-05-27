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
package ch.bfh.evoting.voterapp.hkrs12.network;

import ch.bfh.evoting.voterapp.hkrs12.network.wifi.WifiAPManager;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Broadcast receiver listening for events concerning the wifi network
 * @author Philemon von Bergen
 *
 */
public class NetworkMonitor extends BroadcastReceiver {

	private Context context;
	private boolean wifiEnabled;
	private boolean connected;
	private String ssid;
	private WifiManager wifi;
	private ConnectivityManager connManager;

	/**
	 * Create an object
	 * @param context android apllication context
	 */
	public NetworkMonitor(Context context){
		this.context=context;
		wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	    connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		checkConnectivity();
	}
	
	/**
	 * Helper method checking the state of the connectivity
	 */
	private void checkConnectivity(){
		String backupSSID = this.ssid;
		WifiAPManager wifiAp = new WifiAPManager();
	    if (wifi.isWifiEnabled()==true || wifiAp.isWifiAPEnabled(wifi)) {
	      wifiEnabled = true;
	    } else {
	    	wifiEnabled = false;
	    }
	    NetworkInfo netInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    if(netInfo.isConnected()){
	    	connected = true;
	    	ssid = wifi.getConnectionInfo().getSSID();
		} else {
			connected = false;
			ssid = "";
		}
	    if(wifi.getWifiState() == WifiManager.WIFI_STATE_DISABLING){
	    	this.onLosingConnection();
	    }
	    if(WifiInfo.getDetailedStateOf(wifi.getConnectionInfo().getSupplicantState())==NetworkInfo.DetailedState.DISCONNECTING){
	    	this.onLosingConnection();
	    }
	    if(!this.ssid.equals(backupSSID)){
	    	Intent intent = new Intent(BroadcastIntentTypes.networkSSIDUpdate);
			LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	    }
	}
	
	/**
	 * Helper method called when connectivity has been lost
	 */
	private void onLosingConnection() {
		Intent intent = new Intent(BroadcastIntentTypes.networkGroupDestroyedEvent);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
	
	/**
	 * Indicate if wifi is enabled
	 * @return true if yes, false otherwise
	 */
	public boolean isWifiEnabled(){
		checkConnectivity();
		return wifiEnabled;
	}
	
	/**
	 * Indicate if the device is connected to a network
	 * @return true if yes, false otherwise
	 */
	public boolean isConnected(){
		checkConnectivity();
		return connected;
	}
	
	/**
	 * Get the currently connected SSID
	 * @return the currently connected SSID
	 */
	public String getConnectedSSID(){
		checkConnectivity();
		return ssid;
	}
}
