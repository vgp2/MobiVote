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
 * Class representing an option that can be chosen for a poll
 * @author Philemon von Bergen
 *
 */
public class Option implements Serializable {

	private static final long serialVersionUID = 1L;
	private String text;
	private int votes;
	private int pollId;
	private int id;
	private double percentage;
	
	/**
	 * Create an empty Option object
	 */
	public Option(){}
	
	/**
	 * Constructs an Option object
	 * @param text text of the option
	 * @param votes number of votes this option has received
	 * @param percentage percentage of votes this option received
	 * @param id id in the database
	 * @param pollId id of the poll to whom it belongs
	 */
	public Option(String text, int votes, double percentage, int id, int pollId){
		this.text = text;
		this.votes = votes;
		this.id = id;
		this.pollId = pollId;
		this.percentage = percentage;
	}
	
	/**
	 * Get the text of the option
	 * @return the text of the option
	 */
	public String getText() {
		return text;
	}

	/**
	 * Set text of the option
	 * @param text the text of the option
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Get the number of votes this option has received
	 * @return the number of votes this option has received
	 */
	public int getVotes() {
		return votes;
	}

	/**
	 * Set the number of votes this option has received
	 * @param votes the number of votes this option has received
	 */
	public void setVotes(int votes) {
		this.votes = votes;
	}
	
	/**
	 * Get the percentage of votes received in comparison of the total number of votes
	 * @return the percentage of votes received in comparison of the total number of votes (in the form 50,6)
	 */
	public double getPercentage() {
		return percentage;
	}

	/**
	 * Set the percentage of votes received in comparison of the total number of votes
	 * @param percentage the percentage of votes received in comparison of the total number of votes (in the form 50,6)
	 */
	public void setPercentage(double percentage) {
		this.percentage = percentage;
	}
	
	/**
	 * Get the id of the poll to whom this option belongs
	 * @return id of the poll to whom it belongs
	 */
	public int getPollId() {
		return pollId;
	}

	/**
	 * Set the id of the poll to whom this options belongs
	 * @param pollId the id of the poll to whom it belongs
	 */
	public void setPollId(int pollId) {
		this.pollId = pollId;
	}

	/**
	 * Get the id in the database
	 * @return the id in the database
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set the id in the database
	 * @param id the id of this option in the database
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
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
		Option other = (Option) obj;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}
	
}
