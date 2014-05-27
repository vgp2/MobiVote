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

import java.math.BigInteger;

import ch.bfh.evoting.voterapp.hkrs12.entities.Participant;
import ch.bfh.unicrypt.math.algebra.concatenative.classes.ByteArrayElement;
import ch.bfh.unicrypt.math.algebra.concatenative.classes.ByteArrayMonoid;
import ch.bfh.unicrypt.math.algebra.dualistic.classes.N;
import ch.bfh.unicrypt.math.algebra.general.classes.Tuple;
import ch.bfh.unicrypt.math.algebra.general.interfaces.Element;

public class ProtocolParticipant extends Participant {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int protocolParticipantIndex;
	
	//transient fields are not serialized
	private transient Element xi = null;
	private Element ai = null;
	private Element proofForXi = null;
	private Element hi = null;
	private Element bi = null;
	private Element proofValidVote = null;
	private Element hiHat = null;
	private Element hiHatPowXi = null;
	private Element proofForHiHat = null;

	/**
	 * Create a ProtocolParticipant by giving a Participant object
	 * @param p the participant object to use in this ProtocolParticipant
	 */
	public ProtocolParticipant(Participant p){
		super(p.getIdentification(), p.getUniqueId(), p.getPublicKey(), p.isSelected(), p.hasVoted(), p.hasAcceptedReview());
	}
	
	/**
	 * Get the index of the participant in the protocol
	 * @return identification integer
	 */
	public int getProtocolParticipantIndex() {
		return protocolParticipantIndex;
	}

	/**
	 * Set the index of the participant in the protocol
	 * @param protocolParticipantIndex the index of the participant in the protocol
	 */
	public void setProtocolParticipantIndex(int protocolParticipantIndex) {
		this.protocolParticipantIndex = protocolParticipantIndex;
	}

	/**
	 * Get value x of the protocol for this participant
	 * @return value x of the protocol for this participant
	 */
	public Element getXi() {
		return xi;
	}

	/**
	 * Set value x of the protocol for this participant
	 * @param xi value x of the protocol for this participant
	 */
	public void setXi(Element xi) {
		this.xi = xi;
	}
	
	/**
	 * Get value a of the protocol for this participant
	 * @return value a of the protocol for this participant
	 */
	public Element getAi() {
		return ai;
	}

	/**
	 * Set value a of the protocol for this participant
	 * @param ai value a of the protocol for this participant
	 */
	public void setAi(Element ai) {
		this.ai = ai;
	}

	/**
	 * Get the ZK proof for knowledge of x
	 * @return the ZK proof for knowledge of x
	 */
	public Element getProofForXi() {
		return proofForXi;
	}

	/**
	 * Set the ZK proof for knowledge of x
	 * @param proofForXi the ZK proof for knowledge of x
	 */
	public void setProofForXi(Element proofForXi) {
		this.proofForXi = proofForXi;
	}

	/**
	 * Get value h of the protocol for this participant
	 * @return value h of the protocol for this participant
	 */
	public Element getHi() {
		return hi;
	}

	/**
	 * Set value h of the protocol for this participant
	 * @param hi value h of the protocol for this participant
	 */
	public void setHi(Element hi) {
		this.hi = hi;
	}

	/**
	 * Get value b of the protocol for this participant
	 * @return value b of the protocol for this participant
	 */
	public Element getBi() {
		return bi;
	}

	/**
	 * Set value b of the protocol for this participant
	 * @param bi value b of the protocol for this participant
	 */
	public void setBi(Element bi) {
		this.bi = bi;
	}

	/**
	 * Get the validity proof for the vote
	 * @return the validity proof for the vote
	 */
	public Element getProofValidVote() {
		return proofValidVote;
	}

	/**
	 * Set the validity proof for the vote
	 * @param proofValidVote the validity proof for the vote
	 */
	public void setProofValidVote(Element proofValidVote) {
		this.proofValidVote = proofValidVote;
	}
	
	/**
	 * Get value h hat of the protocol for this participant
	 * @return value h hat of the protocol for this participant
	 */
	public Element getHiHat() {
		return hiHat;
	}

	/**
	 * Set value h hat of the protocol for this participant
	 * @param hiHat value h hat of the protocol for this participant
	 */
	public void setHiHat(Element hiHat) {
		this.hiHat = hiHat;
	}

	/**
	 * Get value (h hat)^x of the protocol for this participant
	 * @return value (h hat)^x of the protocol for this participant
	 */
	public Element getHiHatPowXi() {
		return hiHatPowXi;
	}

	/**
	 * Set value (h hat)^x of the protocol for this participant
	 * @param hiHatPowXi value (h hat)^x of the protocol for this participant
	 */
	public void setHiHatPowXi(Element hiHatPowXi) {
		this.hiHatPowXi = hiHatPowXi;
	}

	/**
	 * Get the Equality between logs ZK proof 
	 * @return the Equality between logs ZK proof 
	 */
	public Element getProofForHiHat() {
		return proofForHiHat;
	}

	/**
	 * Set the Equality between logs ZK proof
	 * @param proofForHiHat the Equality between logs ZK proof 
	 */
	public void setProofForHiHat(Element proofForHiHat) {
		this.proofForHiHat = proofForHiHat;
	}
	
	/**
	 * Return all the important data of this object that should be used in the hash used in ZK proofs
	 * @return all the important data of this object that should be used in the hash used in ZK proofs
	 */
	public Tuple getDataToHash(){

		Element index = N.getInstance().getElement(BigInteger.valueOf(this.getProtocolParticipantIndex()));
		ByteArrayElement proverId = ByteArrayMonoid.getInstance().getElement(this.getUniqueId().getBytes());

		return Tuple.getInstance(index, proverId);
	}

}
