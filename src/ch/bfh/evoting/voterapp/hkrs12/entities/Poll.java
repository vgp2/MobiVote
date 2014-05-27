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
import java.util.List;
import java.util.Map;


/**
 * Class representing a poll
 * @author Philemon von Bergen
 *
 */
public class Poll implements Serializable {

	private static final long serialVersionUID = 1L;
	private int id = -1;
	private String question;
	private List<Option> options;
	private Map<String,Participant> participants;
	private int numberParticipants;
	private long startTime;
	private boolean isTerminated;
	
	/**
	 * Construct an empty Poll object
	 */
	public Poll(){}
	
	/**
	 * Constructs a poll object
	 * @param id id defined in the database
	 * @param question text of the poll's question
	 * @param startTime time when the poll has begun
	 * @param options list of the possible options for the voter
	 * @param participants list of participants to the poll
	 * @param isTerminated true if Poll is terminated
	 */
	public Poll(int id, String question, long startTime, List<Option> options, Map<String,Participant> participants, boolean isTerminated){
		this.id = id;
		this.question = question;
		this.startTime = startTime;
		this.options = options;
		this.participants = participants;
		this.isTerminated = isTerminated;
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
	 * @param id the id in the database
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Get the poll's question
	 * @return the poll's question
	 */
	public String getQuestion() {
		return question;
	}

	/**
	 * Set the poll's question
	 * @param question the poll's question
	 */
	public void setQuestion(String question) {
		this.question = question;
	}

	/**
	 * Get the list of options for the voter
	 * @return list of Option objects
	 */
	public List<Option> getOptions() {
		return options;
	}

	/**
	 * Set the list of options for the voter
	 * @param options list of Option objects
	 */
	public void setOptions(List<Option> options) {
		this.options = options;
	}
	
	/**
	 * Get the participants to the poll
	 * @return a list of participant objects
	 */
	public Map<String,Participant> getParticipants() {
		return participants;
	}

	/**
	 * Set the participants to the poll. 
	 * Be careful, the participants are not stored in the DB since they are dependent on the network
	 * @param participants a list of participant objects
	 */
	public void setParticipants(Map<String,Participant> participants) {
		this.participants = participants;
		this.numberParticipants = this.participants.size();
	}
	
	/**
	 * Get the number of participants of the poll
	 * @return the number of participants of the poll
	 */
	public int getNumberOfParticipants() {
		return numberParticipants;
	}

	/**
	 * Set the number of participants of the poll
	 * @param numberParticipants the number of participants of the poll
	 */
	public void setNumberOfParticipants(int numberParticipants) {
		this.numberParticipants = numberParticipants;
	}

	/**
	 * Get the time when the poll has begun
	 * @return the time when the poll has begun in milliseconds
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * Set the time when the poll has begun
	 * @param startTime the time when the poll has begun in milliseconds
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	/**
	 * Ask if poll is terminated
	 * @return true if Poll is terminated else otherwise
	 */
	public boolean isTerminated() {
		return isTerminated;
	}

	/**
	 * Set if poll is terminated
	 * @param isTerminated true if Poll is terminated else otherwise
	 */
	public void setTerminated(boolean isTerminated) {
		this.isTerminated = isTerminated;
	}
	
}
