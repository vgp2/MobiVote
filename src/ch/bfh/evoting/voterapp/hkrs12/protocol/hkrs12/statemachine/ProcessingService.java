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
package ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.statemachine;

import com.continuent.tungsten.fsm.core.StateMachine;

import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.evoting.voterapp.hkrs12.protocol.HKRS12ProtocolInterface;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.ProtocolMessageContainer;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.ProtocolOption;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.ProtocolParticipant;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.ProtocolPoll;
import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.statemachine.StateMachineManager.Round;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
import ch.bfh.unicrypt.crypto.proofgenerator.challengegenerator.classes.StandardNonInteractiveSigmaChallengeGenerator;
import ch.bfh.unicrypt.crypto.proofgenerator.challengegenerator.interfaces.SigmaChallengeGenerator;
import ch.bfh.unicrypt.crypto.proofgenerator.classes.ElGamalEncryptionValidityProofGenerator;
import ch.bfh.unicrypt.crypto.proofgenerator.classes.PreimageEqualityProofGenerator;
import ch.bfh.unicrypt.crypto.proofgenerator.classes.PreimageProofGenerator;
import ch.bfh.unicrypt.crypto.schemes.commitment.classes.StandardCommitmentScheme;
import ch.bfh.unicrypt.crypto.schemes.encryption.classes.ElGamalEncryptionScheme;
import ch.bfh.unicrypt.math.algebra.general.classes.Subset;
import ch.bfh.unicrypt.math.algebra.general.classes.Tuple;
import ch.bfh.unicrypt.math.algebra.general.interfaces.Element;
import ch.bfh.unicrypt.math.function.classes.ProductFunction;
import ch.bfh.unicrypt.math.function.interfaces.Function;
import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * This class is a service receiving messages to process in a queue
 * It processes them one after the other
 * @author Philemon von Bergen
 *
 */
public class ProcessingService extends IntentService {

	private static final String TAG = "ProcessingService";
	private StateMachine sm;

	public ProcessingService(){
		super(TAG);
		sm = ((HKRS12ProtocolInterface)AndroidApplication.getInstance().getProtocolInterface()).getStateMachineManager().getStateMachine();
	};


	@Override
	protected void onHandleIntent(Intent intent) {
				
		boolean exclude = false;
		Round round = (Round) intent.getSerializableExtra("round");
		String sender = intent.getStringExtra("sender");
		ProtocolMessageContainer message = (ProtocolMessageContainer) intent.getSerializableExtra("message");

		Log.d(TAG, "Processing service called for "+round);

		AbstractAction action = ((AbstractAction)sm.getState().getEntryAction());
		if(action==null || action instanceof ExitAction) return; //state machine was already terminated

		//Get the poll for read only actions
		ProtocolPoll poll = action.getPoll();

		ProtocolParticipant senderParticipant = (ProtocolParticipant)poll.getParticipants().get(sender);

		Log.d(TAG, "Processing message for "+senderParticipant.getIdentification());


		switch(round){
		case setup:

			//Verify proof of knowledge of xi

			StandardCommitmentScheme csSetup = StandardCommitmentScheme.getInstance(poll.getGenerator());

			//Generator and index of the participant has also to be hashed in the proof
			Tuple otherInput = Tuple.getInstance(senderParticipant.getDataToHash(), poll.getDataToHash());
			
			SigmaChallengeGenerator scg = StandardNonInteractiveSigmaChallengeGenerator.getInstance(csSetup.getCommitmentFunction(), otherInput);

			PreimageProofGenerator spg = PreimageProofGenerator.getInstance(scg, csSetup.getCommitmentFunction());

			//if proof is false, exclude participant
			if(!spg.verify(message.getProof(), message.getValue()).getValue()){
				exclude = true;
				Log.w(TAG, "Proof of knowledge for xi was false for participant "+senderParticipant.getIdentification()+" ("+sender+")");
			}

			break;
		case commit:
			//Nothing specific to do, only save values
			break;
		case voting:
			//processing of voting messages directly depend on values received in commitment round, so we have to wait until commitment round is finished
			if(!(action instanceof VotingRoundAction)){
				//put the message at the end of the queue
				startService(intent);
				return;
			}

			if(senderParticipant.getProofValidVote()==null) return;
			
			//Verify validity proof
			Log.d(TAG, "Start verification of validity proof");
			Element[] possibleVotes = new Element[poll.getOptions().size()];
			int i=0;
			for(Option op:poll.getOptions()){
				possibleVotes[i] = poll.getGenerator().selfApply(((ProtocolOption)op).getRepresentation());
				i++;
			}
			
			ElGamalEncryptionScheme ees = ElGamalEncryptionScheme.getInstance(poll.getGenerator());

			Tuple otherInput2 = Tuple.getInstance(senderParticipant.getDataToHash(), poll.getDataToHash());
			SigmaChallengeGenerator scg2 = ElGamalEncryptionValidityProofGenerator.createNonInteractiveChallengeGenerator(ees, possibleVotes.length, otherInput2);
			Subset possibleVotesSet = Subset.getInstance(poll.getG_q(), possibleVotes);

			ElGamalEncryptionValidityProofGenerator vpg = ElGamalEncryptionValidityProofGenerator.getInstance(
					scg2, ees, message.getComplementaryValue(), possibleVotesSet);

			//simulate the ElGamal cipher text (a,b) = (ai,bi);
			Tuple publicInput = Tuple.getInstance(senderParticipant.getAi(), message.getValue());

			if(!vpg.verify(senderParticipant.getProofValidVote(), publicInput).getValue()){
				exclude = true;
				Log.w(TAG, "Proof of validity was false for participant "+senderParticipant.getIdentification()+" ("+sender+")");
			}

			break;
		case recovery:

			//verify proof

			//Function g^r
			StandardCommitmentScheme cs3 = StandardCommitmentScheme.getInstance(poll.getGenerator());
			Function f1 = cs3.getCommitmentFunction();

			//Function h_hat^r
			StandardCommitmentScheme cs4 = StandardCommitmentScheme.getInstance(message.getComplementaryValue());
			Function f2 = cs4.getCommitmentFunction();
			
			ProductFunction f3 = ProductFunction.getInstance(f1, f2);

			Tuple otherInput3 = Tuple.getInstance(senderParticipant.getDataToHash(), poll.getDataToHash());

			SigmaChallengeGenerator scg3 = StandardNonInteractiveSigmaChallengeGenerator.getInstance(f3, otherInput3);

			PreimageEqualityProofGenerator piepg = PreimageEqualityProofGenerator.getInstance(scg3, f1,f2);

			Tuple publicInput3 = Tuple.getInstance(senderParticipant.getAi(), message.getValue());

			if(!piepg.verify(message.getProof(), publicInput3).getValue()){
				Log.w(TAG, "Proof of equality between discrete logs was false for participant "+senderParticipant.getIdentification()+" ("+sender+")");
				exclude = true;
			}
			break;
		default:
			break;
		}

		Log.d(TAG, "Processing done");
		if(exclude){
			LocalBroadcastManager.getInstance(AndroidApplication.getInstance())
				.sendBroadcast(new Intent(BroadcastIntentTypes.proofVerificationFailed)
					.putExtra("participant", senderParticipant.getIdentification()));
		}

		//notify the action that it message has been processed and pass the result to it
		action.savedProcessedMessage(round, sender, message, exclude);

		if(action.readyToGoToNextState()){
			action.goToNextState();
		}


	}

}
