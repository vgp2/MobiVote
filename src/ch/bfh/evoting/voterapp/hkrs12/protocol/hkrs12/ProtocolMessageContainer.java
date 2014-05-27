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
package ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12;

import java.io.Serializable;

import ch.bfh.unicrypt.math.algebra.general.interfaces.Element;

/**
 * Class representing a message send or received inside the protocol
 * @author Philemon von Bergen
 *
 */
public class ProtocolMessageContainer implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Element value;
	private Element proof;
	private Element complementaryValue;

	/**
	 * Constructs a message
	 * @param value value to be sent
	 * @param proof proof corresponding to the value sent
	 */
	public ProtocolMessageContainer(Element value, Element proof){
		this.value = value;
		this.proof = proof;
	}
	
	/**
	 * Constructs a message
	 * @param value value to be sent
	 * @param proof proof corresponding to the value sent
	 * @param complementaryValue additional value needed to verify the proof
	 */
	public ProtocolMessageContainer(Element value, Element proof, Element complementaryValue){
		this.value = value;
		this.proof = proof;
		this.complementaryValue = complementaryValue;
	}

	/**
	 * Get the value stored in the message
	 * @return the value stored in the message
	 */
	public Element getValue() {
		return value;
	}

	/**
	 * Get the proof stored in the message
	 * @return the proof stored in the message
	 */
	public Element getProof() {
		return proof;
	}
	
	/**
	 * Get the additional value stored in the message
	 * @return the addtitional value stored in the message
	 */
	public Element getComplementaryValue() {
		return complementaryValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((complementaryValue == null) ? 0 : complementaryValue.getValue()
						.hashCode());
		result = prime * result + ((proof == null) ? 0 : proof.getValue().hashCode());
		result = prime * result + ((value == null) ? 0 : value.getValue().hashCode());
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
		ProtocolMessageContainer other = (ProtocolMessageContainer) obj;
		if (complementaryValue == null) {
			if (other.complementaryValue != null)
				return false;
		} else if (!complementaryValue.getValue().equals(other.complementaryValue.getValue()))
			return false;
		if (proof == null) {
			if (other.proof != null)
				return false;
		} else if (!proof.getValue().equals(other.proof.getValue()))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.getValue().equals(other.value.getValue()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ProtocolMessageContainer [value=" + value + ", proof=" + proof
				+ ", complementaryValue=" + complementaryValue + "]";
	}
	
}
