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

import org.simpleframework.xml.Element;

//Proof of equality
//1 ProductGroupTuple[
//                1.1 GStarModElement[10],
//                1.2 ZModElement[99], 
//                1.3 ZModElement[391]]

public class XMLKnowledgeProof {

	@Element
	private XMLGqElement value11;
	@Element
	private XMLZqElement value12;
	@Element
	private XMLZqElement value13;
	
	public XMLKnowledgeProof(){}

	public XMLKnowledgeProof(XMLGqElement value11, XMLZqElement value12,
			XMLZqElement value13) {
		super();
		this.value11 = value11;
		this.value12 = value12;
		this.value13 = value13;
	}

	public XMLGqElement getValue11() {
		return value11;
	}

	public void setValue11(XMLGqElement value11) {
		this.value11 = value11;
	}

	public XMLZqElement getValue12() {
		return value12;
	}

	public void setValue12(XMLZqElement value12) {
		this.value12 = value12;
	}

	public XMLZqElement getValue13() {
		return value13;
	}

	public void setValue13(XMLZqElement value13) {
		this.value13 = value13;
	}
	
	
}
