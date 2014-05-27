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
package ch.bfh.evoting.voterapp.hkrs12.entities;

import java.io.Serializable;


/**
 * Class representing a vote message
 *
 */
public class VoteMessage implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private Type messageType;
	private String senderUniqueId;
	private Serializable messageContent;
	
	/**
	 * Construct an empty message object
	 */
	public VoteMessage(){}
	
	/**
	 * Construct a message object
	 * @param messageType type of the message's content
	 * @param messageContent content of the message
	 */
	public VoteMessage(Type messageType, Serializable messageContent){
		this.messageType = messageType;
		this.messageContent = messageContent;
	}
	
	/**
	 * Get the sender unique identifier 
	 * @return the unique identifier of the sender of this message
	 */
	public String getSenderUniqueId () {
		return senderUniqueId;		
	}
	
	/**
	 * Set the sender unique identifier
	 * @param senderUniqueId the unique identifier of the sender of this message
	 */
	public void setSenderUniqueId(String senderUniqueId) {
		this.senderUniqueId = senderUniqueId;
	}

	/**
	 * Get the content of the message
	 * @return the content of the message
	 */
	public Serializable getMessageContent() {
		return messageContent;
	}

	/**
	 * Set the content of the message
	 * @param messageContent the content of the message
	 */
	public void setMessageContent(Serializable messageContent) {
		this.messageContent = messageContent;
	}

	/**
	 * Get the type of the message
	 * @return the type of the message
	 */
	public Type getMessageType() {
		return messageType;
	}
	
	/**
	 * Set the type of the message
	 * @param messageType the type of the message
	 */
	public void setMessageType(Type messageType) {
		this.messageType = messageType;
	}
	
	/**
	 * Type of vote messages
	 *
	 */
	public enum Type {
		VOTE_MESSAGE_ELECTORATE,
		VOTE_MESSAGE_POLL_TO_REVIEW,
		VOTE_MESSAGE_ACCEPT_REVIEW,
		VOTE_MESSAGE_START_POLL,
		VOTE_MESSAGE_STOP_POLL, 
		VOTE_MESSAGE_CANCEL_POLL,
		VOTE_MESSAGE_SETUP,
		VOTE_MESSAGE_COMMIT,
		VOTE_MESSAGE_VOTE,
		VOTE_MESSAGE_RECOVERY;
	}
}
