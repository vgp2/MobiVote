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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.util.Log;
import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.evoting.voterapp.hkrs12.entities.Participant;
import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;
import ch.bfh.evoting.voterapp.hkrs12.util.ObservableTreeMap;
import ch.bfh.unicrypt.helper.array.ByteArray;
import ch.bfh.unicrypt.helper.factorization.SafePrime;
import ch.bfh.unicrypt.helper.hash.HashAlgorithm;
//import ch.bfh.unicrypt.crypto.random.classes.PseudoRandomOracle;
//import ch.bfh.unicrypt.crypto.random.classes.ReferenceRandomByteSequence;
import ch.bfh.unicrypt.math.algebra.concatenative.classes.ByteArrayElement;
import ch.bfh.unicrypt.math.algebra.concatenative.classes.ByteArrayMonoid;
import ch.bfh.unicrypt.math.algebra.dualistic.classes.ZMod;
import ch.bfh.unicrypt.math.algebra.general.classes.FiniteByteArrayElement;
import ch.bfh.unicrypt.math.algebra.general.classes.Tuple;
import ch.bfh.unicrypt.math.algebra.general.interfaces.Element;
import ch.bfh.unicrypt.math.algebra.multiplicative.classes.GStarMod;
import ch.bfh.unicrypt.math.algebra.multiplicative.classes.GStarModElement;
import ch.bfh.unicrypt.math.algebra.multiplicative.classes.GStarModSafePrime;
import ch.bfh.unicrypt.random.classes.PseudoRandomOracle;
import ch.bfh.unicrypt.random.classes.ReferenceRandomByteSequence;

public class ProtocolPoll extends Poll {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//when updating this, do not forget to also update serializedSafePrimeofP variable
	private static BigInteger p = new BigInteger("24421817481307177449647246484681681783337829412862177682538435312071281569646025606745584903210775224457523457768723824442724616998787110952108654428565400454402598245210227144929556256697593550903247924055714937916514526166092438066936693296218391429342957961400667273342778842895486447440287639065428393782477303395870298962805975752198304889507138990179204870133839847367098792875574662446712567387387134946911523722735147628746206081844500879809860996360597720571611720620174658556850893276934140542331691801045622505813030592119908356317756153773900818965668280464355085745552657819811997912683349698802670648319"); 
//	private static BigInteger p = new BigInteger("1187");
	//this correspond to the SafePrime object containing p in a serialized form
	private String serializedSafePrimeofP = "rO0ABXNyAC5jaC5iZmgudW5pY3J5cHQuaGVscGVyLmZhY3Rvcml6YXRpb24uU2FmZVByaW1lzEYaSXqxXPACAAB4cgAqY2guYmZoLnVuaWNyeXB0LmhlbHBlci5mYWN0b3JpemF0aW9uLlByaW1lC09p/ri/FwsCAAB4cgA5Y2guYmZoLnVuaWNyeXB0LmhlbHBlci5mYWN0b3JpemF0aW9uLlNwZWNpYWxGYWN0b3JpemF0aW9uSQgdmBj1KJgCAAB4cgAyY2guYmZoLnVuaWNyeXB0LmhlbHBlci5mYWN0b3JpemF0aW9uLkZhY3Rvcml6YXRpb252Jhq+x1QMUQIAA1sACWV4cG9uZW50c3QAAltJWwAMcHJpbWVGYWN0b3JzdAAXW0xqYXZhL21hdGgvQmlnSW50ZWdlcjtMAAV2YWx1ZXQAFkxqYXZhL21hdGgvQmlnSW50ZWdlcjt4cgAfY2guYmZoLnVuaWNyeXB0LmhlbHBlci5VbmlDcnlwdAAAAAAAAAABAgAAeHB1cgACW0lNumAmduqypQIAAHhwAAAAAQAAAAF1cgAXW0xqYXZhLm1hdGguQmlnSW50ZWdlcjsOfNtG4DpgxgIAAHhwAAAAAXNyABRqYXZhLm1hdGguQmlnSW50ZWdlcoz8nx+pO/sdAwACSQAGc2lnbnVtWwAJbWFnbml0dWRldAACW0J4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAABdXIAAltCrPMX+AYIVOACAAB4cAAAAQDBdUNe64p6BtlhSfHTD1muHnDwQA2tKJB9kfcFI1rmamLF0l2miBsC1ZyYJy3EmAOUn1kh+YHxAKgYd7oh7sTxOKHxB/IKMHGu4e67b1LuvK41yZIf6AdgNdBDWGuCfauLyLk0pSg3rXrufJBfxVKrRw5qnGpfniKbnKqFK8EDDZfd9Gg5xW56XahXXz0yN2+EMgz/7WDiPDJX1TDIDWz9tfie5J55o03Ico8okv3n3xegtorSxQjkABXMXsg5nh1XSa9YYn5hJtzr4Npi8ZFS8eFVYRhAb3xoA+nZHPve+NUUplFq4rFWcQa8FYdBTEuJ/OIR/1bHVq37T+m+XDv/eHEAfgAQ";
	
	private GStarModSafePrime G_q;
	private ZMod Z_q;
	private GStarModElement generator;

	private Map<String, Participant> excludedParticipants = new ObservableTreeMap<String,Participant>();
	private Map<String, Participant> completelyExcludedParticipants = new ObservableTreeMap<String,Participant>();


	public ProtocolPoll(Poll poll){
		super(poll.getId(), poll.getQuestion(), poll.getStartTime(), poll.getOptions(), poll.getParticipants(), poll.isTerminated());
		SafePrime sp = /*SafePrime.getInstance(p);*/(SafePrime)AndroidApplication.getInstance().getSerializationUtil().deserialize(serializedSafePrimeofP);
		G_q = GStarModSafePrime.getInstance(sp);
		Z_q = G_q.getZModOrder();
	}

	/**
	 * Create a generator used in the protocol that is dependent on the text of the question, the text of the options
	 * and the representation of the option
	 */
	public void generateGenerator(){
		
		//Since SHA256 MessageDigest of OpenSSL is not serializable this cannot be used anymore
		//This is however already covered with the otherInputs of the proofs
		//computes a commitment to the text of the poll and use this commitment as generator
//		String texts = this.getQuestion();
//		Element[] representations = new Element[this.getOptions().size()];
//		int i=0;
//		for(Option op:this.getOptions()){
//			texts += op.getText();
//			representations[i]=((ProtocolOption)op).getRepresentation();
//			i++;
//		}
//		
//		Tuple tuple = Tuple.getInstance(representations);
//		ByteArray representationsElement = tuple.getByteArray();//getHashValue();
//		ByteArray textElement = ByteArray.getInstance(texts.getBytes());
//
//		ByteBuffer buffer = ByteBuffer.allocate(textElement.getLength()+representationsElement.getLength());
//		buffer.put(textElement.getAll());
//		buffer.put(representationsElement.getAll());
//		buffer.flip(); 
//		
//		ReferenceRandomByteSequence rrs = PseudoRandomOracle.getInstance().getReferenceRandomByteSequence(ByteArray.getInstance(buffer.array()));
		generator = G_q.getDefaultGenerator();
	}

	/**
	 * Get the value of prime p
	 * @return the value of prime p
	 */
	public BigInteger getP() {
		return p;
	}

	/**
	 * Get the safe prime group used in the protocol
	 * @return the safe prime group used in the protocol
	 */
	public GStarMod getG_q() {
		return G_q;
	}

	/**
	 * Get the additive group used in the protocol
	 * @return the additive group used in the protocol
	 */
	public ZMod getZ_q() {
		return Z_q;
	}

	/**
	 * Get the generator of the group Gq used in the protocol
	 * @return the generator of the group Gq used in the protocol
	 */
	public Element getGenerator() {
		return generator;
	}

	/**
	 * Get the map containing all participant excluded during the protocol
	 * The recovery round will allow to recover them
	 * @return the map containing all participant excluded during the protocol
	 */
	public Map<String, Participant> getExcludedParticipants() {
		return excludedParticipants;
	}


	/**
	 * Get the map containing all participant excluded before the protocol
	 * The recovery round will NOT allow to recover them
	 * @return the map containing all participant excluded before the protocol
	 */
	public Map<String, Participant> getCompletelyExcludedParticipants() {
		return completelyExcludedParticipants;
	}

	/**
	 * Return all the important data of this object that should be used in the hash used in ZK proofs
	 * @return all the important data of this object that should be used in the hash used in ZK proofs
	 */
	public Tuple getDataToHash(){

		String otherHashInputString = this.getQuestion();
		List<Element> optionsRepresentations = new ArrayList<Element>();
		for(Option op:this.getOptions()){
			otherHashInputString += op.getText();
			optionsRepresentations.add(((ProtocolOption)op).getRepresentation());
		}
		for(Participant p:this.getParticipants().values()){
			otherHashInputString += p.getUniqueId()+p.getIdentification();
		}
		
		ByteArrayElement otherHashInput = ByteArrayMonoid.getInstance().getElement(otherHashInputString.getBytes());
		Tuple optionsRepresentationsTuple = Tuple.getInstance(optionsRepresentations.toArray(new Element[optionsRepresentations.size()]));

		return Tuple.getInstance(optionsRepresentationsTuple, otherHashInput, this.generator);

	}


}
