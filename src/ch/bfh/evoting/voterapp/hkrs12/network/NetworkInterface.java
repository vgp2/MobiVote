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

import java.util.List;
import java.util.Map;

import ch.bfh.evoting.voterapp.hkrs12.entities.Participant;
import ch.bfh.evoting.voterapp.hkrs12.entities.VoteMessage;

/**
 * Interface to the network component
 * @author Philemon von Bergen
 *
 */
public interface NetworkInterface {
		
	/**
	 * Join a group
	 * @param groupName name of the group to join
	 */
	public void joinGroup(String groupName);
		
	/**
	 * Get the name of the network in which the group exists
	 * @return the name of the network in which the group exists
	 */
	public String getNetworkName();

	/**
	 * Get the name of the group
	 * @return the name of the group
	 */
	public String getGroupName();

	/**
	 * Get the password of the group
	 * @return the password of the group
	 */
	public String getGroupPassword();
	
	/**
	 * Get the truncated digest of the salt used to derive the symmetric encryption key encrypting the network communication
	 * @return the truncated digest of the salt
	 */
	public String getSaltShortDigest();
	
	/**
	 * Get the unique identifier of myself
	 * @return the unique identifier of myself
	 */
	public String getMyUniqueId();
	
	/**
	 * Get the participants in the group
	 * @return a map (<uniqueId, Participant Object>) of the participants in the group 
	 */
	public Map<String,Participant> getGroupParticipants();
	
	/**
	 * Set the name of the group to connect to
	 * @param groupName the name of the group to connect to
	 */
	public void setGroupName(String groupName);
	
	/**
	 * Set the password of the group to connect to
	 * @param password the password of the group to connect to
	 */
	public void setGroupPassword(String password);
	
	/**
	 * Lock the group
	 * Works only if the caller of this method is also the creator of the group
	 */
	public void lockGroup();
	
	/**
	 * Unlock the group
	 * Works only if the caller of this method is also the creator of the group
	 */
	public void unlockGroup();
	
	/**
	 * List all groups found on the network
	 * @return the list of groups found on the network
	 */
	public List<String> listAvailableGroups();
	
	/**
	 * Leave the currently connected group and destroy it if the caller of this method is the creator of the group
	 */
	public void disconnect();
	
	/**
	 * This method can be used to send a broadcast message
	 * 
	 * @param votemessage The votemessage which should be sent
	 */
	public void sendMessage(VoteMessage votemessage);
	
	/**
	 * This method signature can be used to send unicast message to a specific ip address
	 * 
	 * 
	 * @param votemessage The votemessage which should be sent
	 * @param destinationUniqueId The destination of the message
	 */
	public void sendMessage(VoteMessage votemessage, String destinationUniqueId);		
}

