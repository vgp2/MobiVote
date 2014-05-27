
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

import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;

import android.net.wifi.WifiConfiguration;

/**
 * This is a wrapper class which is used to serialize a WifiConfiguration
 * object. This is used for backing up the wifi AP configuration.
 * 
 * @author Juerg Ritter (rittj1@bfh.ch)
 */
public class SerializableWifiConfiguration implements Serializable {

	private static final long serialVersionUID = 1L;

	public String BSSID;
	public String SSID;
	public BitSet allowedAuthAlgorithms;
	public BitSet allowedGroupCiphers;
	public BitSet allowedKeyManagement;
	public BitSet allowedPairwiseCiphers;
	public BitSet allowedProtocols;
	public boolean hiddenSSID;
	public String preSharedKey;
	public int priority;
	public int status;
	public String[] wepKeys;
	public int wepTxKeyIndex;

	/**
	 * @param config
	 *            The WifiConfiguration object which should be transfered into a
	 *            serializable object.
	 */
	public SerializableWifiConfiguration(WifiConfiguration config) {
		this.BSSID = config.BSSID;
		this.SSID = config.SSID;
		this.allowedAuthAlgorithms = config.allowedAuthAlgorithms;
		this.allowedGroupCiphers = config.allowedGroupCiphers;
		this.allowedKeyManagement = config.allowedKeyManagement;
		this.allowedPairwiseCiphers = config.allowedPairwiseCiphers;
		this.allowedProtocols = config.allowedProtocols;
		this.hiddenSSID = config.hiddenSSID;
		this.preSharedKey = config.preSharedKey;
		this.priority = config.priority;
		this.wepKeys = config.wepKeys;
		this.wepTxKeyIndex = config.wepTxKeyIndex;
	}

	/**
	 * Transforms the object back into a WifiConfiguration object.
	 * 
	 * @return the newly created WifiConfiguration object
	 */
	public WifiConfiguration getWifiConfiguration() {
		WifiConfiguration config = new WifiConfiguration();
		if (this.BSSID == null) {
			config.BSSID = "";
		} else {
			config.BSSID = this.BSSID;
		}
		config.SSID = this.SSID;
		config.allowedAuthAlgorithms = this.allowedAuthAlgorithms;
		config.allowedGroupCiphers = this.allowedGroupCiphers;
		config.allowedKeyManagement = this.allowedKeyManagement;
		config.allowedPairwiseCiphers = this.allowedPairwiseCiphers;
		config.allowedProtocols = this.allowedProtocols;
		config.hiddenSSID = this.hiddenSSID;
		config.preSharedKey = this.preSharedKey;
		config.priority = this.priority;
		config.wepKeys = this.wepKeys;
		config.wepTxKeyIndex = this.wepTxKeyIndex;

		return config;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SerializableWifiConfiguration [BSSID=" + BSSID + ", SSID="
				+ SSID + ", allowedAuthAlgorithms=" + allowedAuthAlgorithms
				+ ", allowedGroupCiphers=" + allowedGroupCiphers
				+ ", allowedKeyManagement=" + allowedKeyManagement
				+ ", allowedPairwiseCiphers=" + allowedPairwiseCiphers
				+ ", allowedProtocols=" + allowedProtocols + ", hiddenSSID="
				+ hiddenSSID + ", preSharedKey=" + preSharedKey + ", priority="
				+ priority + ", status=" + status + ", wepKeys="
				+ Arrays.toString(wepKeys) + ", wepTxKeyIndex=" + wepTxKeyIndex
				+ "]";
	}
}
