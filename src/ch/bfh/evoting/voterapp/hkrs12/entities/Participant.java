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
 * Class representing a participant to the poll
 * @author Philemon von Bergen
 *
 */
public class Participant implements Serializable{

	private static final long serialVersionUID = 1L;
	private String identification;
	private String uniqueId;
	private boolean hasVoted;
	private boolean isSelected;
	private boolean hasAcceptedReview;
	private String publicKey;
	
	/**
	 * Construct a Participant object
	 * @param identification the identification of the participant
	 * @param uniqueId the IP address of the participant
	 * @param isSelected indicate if the participant in the network is selected as member of the electorate
	 * @param hasVoted indicate if the participant already has submitted a vote
	 * @param hasAcceptedReview indicate if the participant already has accepted the review
	 */
	public Participant(String identification, String uniqueId, String publicKey, boolean isSelected, boolean hasVoted, boolean hasAcceptedReview){
		this.identification = identification;
		this.uniqueId = uniqueId;
		this.hasVoted = hasVoted;
		this.isSelected = isSelected;
		this.hasAcceptedReview = hasAcceptedReview;
		this.publicKey = publicKey;
	}
	
	/**
	 * Get the identification the identification of the participant
	 * @return the identification the identification of the participant
	 */
	public String getIdentification() {
		return identification;
	}

	/**
	 * Set the identification the identification of the participant
	 * @param identification the identification the identification of the participant
	 */
	public void setIdentification(String identification) {
		this.identification = identification;
	}

	/**
	 * Get the unique identifier of the participant
	 * @return the unique identifier of the participant
	 */
	public String getUniqueId() {
		return uniqueId;
	}

	/**
	 * Set the IP address of the participant
	 * @param uniqueId the unique identifier of the participant
	 */
	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}
	
	/**
	 * Get the public key used by this participant for the authentication of the messages	
	 * @return the public key used by this participant for the authentication of the messages
	 */
	public String getPublicKey() {
		return publicKey;
	}

	/**
	 * Indicates if the participant has already cast her ballot
	 * @return true if casted
	 */
	public boolean hasVoted() {
		return hasVoted;
	}

	/**
	 * Set if the participant has already cast her ballot
	 * @param hasVoted true if casted
	 */
	public void setHasVoted(boolean hasVoted) {
		this.hasVoted = hasVoted;
	}

	/**
	 * Indicate if the participant in the network is selected as member of the electorate
	 * @return true if she belongs to the electorate
	 */
	public boolean isSelected() {
		return isSelected;
	}

	/**
	 * Set if the participant in the network is selected as member of the electorate
	 * @param isSelected true if she belongs to the electorate
	 */
	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}
	
	/**
	 * Indicate if the participant has accepted the review of the poll
	 * @return true if participant has accepted, else otherwise
	 */
	public boolean hasAcceptedReview() {
		return hasAcceptedReview;
	}

	/**
	 * Set the flag indicating if the participant has accepted the review of the poll
	 * @param hasAcceptedReview true if participant has accepted, else otherwise
	 */
	public void setHasAcceptedReview(boolean hasAcceptedReview) {
		this.hasAcceptedReview = hasAcceptedReview;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((identification == null) ? 0 : identification.hashCode());
		result = prime * result
				+ ((uniqueId == null) ? 0 : uniqueId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Participant other = (Participant) obj;
		if (identification == null) {
			if (other.identification != null)
				return false;
		} else if (!identification.equals(other.identification))
			return false;
		if (uniqueId == null) {
			if (other.uniqueId != null)
				return false;
		} else if (!uniqueId.equals(other.uniqueId))
			return false;
		return true;
	}

	
}
