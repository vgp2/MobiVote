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
package ch.bfh.evoting.voterapp.hkrs12.util.xml;


public class XMLParticipant {

	private String identification;
	private String uniqueId;

//	private XMLGqElement ai = null;
//	private XMLKnowledgeProof proofForXi = null;
//	private XMLGqElement hi = null;
//	private XMLGqElement bi = null;
//	private XMLValidityProof proofValidVote = null;
//	private XMLGqElement hiHat = null;
//	private XMLGqElement hiHatPowXi = null;
//	private XMLEqualityProof proofForHiHat = null;
	private int protocolParticipantIndex;

	private XMLElement ai = null;
	private XMLElement proofForXi = null;
	private XMLElement hi = null;
	private XMLElement bi = null;
	private XMLElement proofValidVote = null;
	private XMLElement hiHat = null;
	private XMLElement hiHatPowXi = null;
	private XMLElement proofForHiHat = null;
	
	public XMLParticipant(){
		
	}
	
	public XMLParticipant(String identification, String uniqueId, XMLElement ai,
			XMLElement proofForXi, XMLElement hi, XMLElement bi, XMLElement proofValidVote,
			XMLElement hiHat, XMLElement hiHatPowXi, XMLElement proofForHiHat, int protocolParticipantIndex) {
		super();
		this.identification = identification;
		this.uniqueId = uniqueId;
		this.ai = ai;
		this.proofForXi = proofForXi;
		this.hi = hi;
		this.bi = bi;
		this.proofValidVote = proofValidVote;
		this.hiHat = hiHat;
		this.hiHatPowXi = hiHatPowXi;
		this.proofForHiHat = proofForHiHat;
		this.protocolParticipantIndex = protocolParticipantIndex;
	}
	
	public String getIdentification() {
		return identification;
	}
	public void setIdentification(String identification) {
		this.identification = identification;
	}
	public String getUniqueId() {
		return uniqueId;
	}
	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}
	public XMLElement getAi() {
		return ai;
	}
	public void setAi(XMLElement ai) {
		this.ai = ai;
	}
	public XMLElement getProofForXi() {
		return proofForXi;
	}
	public void setProofForXi(XMLElement proofForXi) {
		this.proofForXi = proofForXi;
	}
	public XMLElement getHi() {
		return hi;
	}
	public void setHi(XMLElement hi) {
		this.hi = hi;
	}
	public XMLElement getBi() {
		return bi;
	}
	public void setBi(XMLElement bi) {
		this.bi = bi;
	}
	public XMLElement getProofValidVote() {
		return proofValidVote;
	}
	public void setProofValidVote(XMLElement proofValidVote) {
		this.proofValidVote = proofValidVote;
	}
	public XMLElement getHiHat() {
		return hiHat;
	}
	public void setHiHat(XMLElement hiHat) {
		this.hiHat = hiHat;
	}
	public XMLElement getHiHatPowXi() {
		return hiHatPowXi;
	}
	public void setHiHatPowXi(XMLElement hiHatPowXi) {
		this.hiHatPowXi = hiHatPowXi;
	}
	public XMLElement getProofForHiHat() {
		return proofForHiHat;
	}
	public void setProofForHiHat(XMLElement proofForHiHat) {
		this.proofForHiHat = proofForHiHat;
	}

	public int getProtocolParticipantIndex() {
		return protocolParticipantIndex;
	}

	public void setProtocolParticipantIndex(int protocolParticipantIndex) {
		this.protocolParticipantIndex = protocolParticipantIndex;
	}
	
	
}
