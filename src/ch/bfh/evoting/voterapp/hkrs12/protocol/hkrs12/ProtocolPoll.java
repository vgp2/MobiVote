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

import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.evoting.voterapp.hkrs12.entities.Participant;
import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;
import ch.bfh.evoting.voterapp.hkrs12.util.ObservableTreeMap;
import ch.bfh.unicrypt.crypto.random.classes.PseudoRandomOracle;
import ch.bfh.unicrypt.crypto.random.classes.ReferenceRandomByteSequence;
import ch.bfh.unicrypt.math.algebra.concatenative.classes.ByteArrayElement;
import ch.bfh.unicrypt.math.algebra.concatenative.classes.ByteArrayMonoid;
import ch.bfh.unicrypt.math.algebra.dualistic.classes.ZMod;
import ch.bfh.unicrypt.math.algebra.general.classes.FiniteByteArrayElement;
import ch.bfh.unicrypt.math.algebra.general.classes.Tuple;
import ch.bfh.unicrypt.math.algebra.general.interfaces.Element;
import ch.bfh.unicrypt.math.algebra.multiplicative.classes.GStarMod;
import ch.bfh.unicrypt.math.algebra.multiplicative.classes.GStarModElement;
import ch.bfh.unicrypt.math.algebra.multiplicative.classes.GStarModSafePrime;
import ch.bfh.unicrypt.math.helper.ByteArray;
import ch.bfh.unicrypt.math.helper.factorization.SafePrime;

public class ProtocolPoll extends Poll {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//when updating this, do not forget to also update serializedSafePrimeofP variable
	private static BigInteger p = new BigInteger("24421817481307177449647246484681681783337829412862177682538435312071281569646025606745584903210775224457523457768723824442724616998787110952108654428565400454402598245210227144929556256697593550903247924055714937916514526166092438066936693296218391429342957961400667273342778842895486447440287639065428393782477303395870298962805975752198304889507138990179204870133839847367098792875574662446712567387387134946911523722735147628746206081844500879809860996360597720571611720620174658556850893276934140542331691801045622505813030592119908356317756153773900818965668280464355085745552657819811997912683349698802670648319"); 
	//this correspond to the SafePrime object containing p in a serialized form
	private String serializedSafePrimeofP = "rO0ABXNyADNjaC5iZmgudW5pY3J5cHQubWF0aC5oZWxwZXIuZmFjdG9yaXphdGlvbi5TYWZlUHJpbWWyvqMSqeOumgIAAHhyAC9jaC5iZmgudW5pY3J5cHQubWF0aC5oZWxwZXIuZmFjdG9yaXphdGlvbi5QcmltZWp9MF748cJqAgAAeHIAPmNoLmJmaC51bmljcnlwdC5tYXRoLmhlbHBlci5mYWN0b3JpemF0aW9uLlNwZWNpYWxGYWN0b3JpemF0aW9u38aui3+qN3cCAAB4cgA3Y2guYmZoLnVuaWNyeXB0Lm1hdGguaGVscGVyLmZhY3Rvcml6YXRpb24uRmFjdG9yaXphdGlvbi+9CK5/FWTKAgADWwAJZXhwb25lbnRzdAACW0lbAAxwcmltZUZhY3RvcnN0ABdbTGphdmEvbWF0aC9CaWdJbnRlZ2VyO0wABXZhbHVldAAWTGphdmEvbWF0aC9CaWdJbnRlZ2VyO3hyACRjaC5iZmgudW5pY3J5cHQubWF0aC5oZWxwZXIuVW5pQ3J5cHQAAAAAAAAAAQIAAHhwdXIAAltJTbpgJnbqsqUCAAB4cAAAAAEAAAABdXIAF1tMamF2YS5tYXRoLkJpZ0ludGVnZXI7DnzbRuA6YMYCAAB4cAAAAAFzcgAUamF2YS5tYXRoLkJpZ0ludGVnZXKM/J8fqTv7HQMAAkkABnNpZ251bVsACW1hZ25pdHVkZXQAAltCeHIAEGphdmEubGFuZy5OdW1iZXKGrJUdC5TgiwIAAHhwAAAAAXVyAAJbQqzzF/gGCFTgAgAAeHAAAAEAwXVDXuuKegbZYUnx0w9Zrh5w8EANrSiQfZH3BSNa5mpixdJdpogbAtWcmCctxJgDlJ9ZIfmB8QCoGHe6Ie7E8Tih8QfyCjBxruHuu29S7ryuNcmSH+gHYDXQQ1hrgn2ri8i5NKUoN6167nyQX8VSq0cOapxqX54im5yqhSvBAw2X3fRoOcVuel2oV189MjdvhDIM/+1g4jwyV9UwyA1s/bX4nuSeeaNNyHKPKJL9598XoLaK0sUI5AAVzF7IOZ4dV0mvWGJ+YSbc6+DaYvGRUvHhVWEYQG98aAPp2Rz73vjVFKZRauKxVnEGvBWHQUxLifziEf9Wx1at+0/pvlw7/3hxAH4AEA==";
	
	private GStarModSafePrime G_q;
	private ZMod Z_q;
	private GStarModElement generator;

	private Map<String, Participant> excludedParticipants = new ObservableTreeMap<String,Participant>();
	private Map<String, Participant> completelyExcludedParticipants = new ObservableTreeMap<String,Participant>();


	public ProtocolPoll(Poll poll){
		super(poll.getId(), poll.getQuestion(), poll.getStartTime(), poll.getOptions(), poll.getParticipants(), poll.isTerminated());
		SafePrime sp = (SafePrime)AndroidApplication.getInstance().getSerializationUtil().deserialize(serializedSafePrimeofP);
		G_q = GStarModSafePrime.getInstance(sp);
		Z_q = G_q.getZModOrder();
	}

	/**
	 * Create a generator used in the protocol that is dependent on the text of the question, the text of the options
	 * and the representation of the option
	 */
	public void generateGenerator(){

		//computes a commitment to the text of the poll and use this commitment as generator
		String texts = this.getQuestion();
		Element[] representations = new Element[this.getOptions().size()];
		int i=0;
		for(Option op:this.getOptions()){
			texts += op.getText();
			representations[i]=((ProtocolOption)op).getRepresentation();
			i++;
		}
		
		Tuple tuple = Tuple.getInstance(representations);
		FiniteByteArrayElement representationsElement = tuple.getHashValue();
		ByteArrayElement textElement = ByteArrayMonoid.getInstance().getElement(texts.getBytes());

		ByteBuffer buffer = ByteBuffer.allocate(textElement.getValue().getLength()+representationsElement.getValue().getLength());
		buffer.put(textElement.getValue().getAll());
		buffer.put(representationsElement.getValue().getAll());
		buffer.flip(); 
		
		ReferenceRandomByteSequence rrs = PseudoRandomOracle.getInstance().getReferenceRandomByteSequence(ByteArray.getInstance(buffer.array()));
		generator = G_q.getIndependentGenerator(1, rrs);

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
