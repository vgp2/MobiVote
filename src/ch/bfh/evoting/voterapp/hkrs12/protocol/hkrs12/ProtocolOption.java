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

import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.unicrypt.math.algebra.general.interfaces.Element;

public class ProtocolOption extends Option {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Element representation;

	/**
	 * Create a ProtocolOption by giving an Option object
	 * @param op the option object to use in this ProtocolOption
	 */
	public ProtocolOption(Option op){
		super(op.getText(),op.getVotes(), op.getPercentage(), op.getId(), op.getPollId());
	}
	
	/**
	 * Get the cryptographic representation of the option
	 * @return the cryptographic representation of the option
	 */
	public Element getRepresentation() {
		return representation;
	}
	
	/**
	 * Set the cryptographic representation of the option
	 * @param representation the cryptographic representation of the option
	 */
	public void setRepresentation(Element representation) {
		this.representation = representation;
	}

}
