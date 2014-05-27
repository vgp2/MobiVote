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

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import ch.bfh.evoting.voterapp.hkrs12.protocol.hkrs12.ProtocolPoll;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;

import com.continuent.tungsten.fsm.core.EntityAdapter;
import com.continuent.tungsten.fsm.core.EventTypeGuard;
import com.continuent.tungsten.fsm.core.FiniteStateException;
import com.continuent.tungsten.fsm.core.Guard;
import com.continuent.tungsten.fsm.core.State;
import com.continuent.tungsten.fsm.core.StateMachine;
import com.continuent.tungsten.fsm.core.StateTransitionMap;
import com.continuent.tungsten.fsm.core.StateType;

/**
 * Creation of the State Machine managing the flow of the protocol
 * @author Philemon von Bergen
 *
 */
public class StateMachineManager implements Runnable {

	private static final String TAG = StateMachineManager.class.getSimpleName();
	private SetupRoundAction setupRoundAction;
	private CommitmentRoundAction commitmentRoundAction;
	private VotingRoundAction votingRoundAction;
	private TallyingAction tallyingAction;
	private RecoveryRoundAction recoveryRoundAction;
	private Context context;
	private ProtocolPoll poll;
	private StateMachine sm;
	
	enum Round{
		setup, commit, voting, recovery;
	}

	/**
	 * Create an object managing the state machine
	 * @param context Android context
	 * @param poll Poll to fill in the actions of the state machine
	 */
	public StateMachineManager(Context context, ProtocolPoll poll) {
		this.context = context;
		this.poll = poll;
	}

	/**
	 * Create and run the state machine
	 */
	@Override
	public void run() {

		/*Create the state machine*/
		StateTransitionMap stmap = new StateTransitionMap();
		
		/*Define actions*/
		setupRoundAction = new SetupRoundAction(context, BroadcastIntentTypes.setupMessage, poll);
		commitmentRoundAction = new CommitmentRoundAction(context, BroadcastIntentTypes.commitMessage, poll);
		votingRoundAction = new VotingRoundAction(context, BroadcastIntentTypes.newVote, poll);
		tallyingAction = new TallyingAction(context, "nothing", poll);
		recoveryRoundAction = new RecoveryRoundAction(context, BroadcastIntentTypes.recoveryMessage, poll);

		/*Define states*/
		State begin = new State("begin", StateType.START, null, null);
		State setup = new State("setup", StateType.ACTIVE, setupRoundAction, null);
		State commit = new State("commit", StateType.ACTIVE, commitmentRoundAction, null);
		State vote = new State("vote", StateType.ACTIVE, votingRoundAction, null);
		State tally = new State("tally", StateType.ACTIVE, tallyingAction, null);
		State recovery = new State("recover", StateType.ACTIVE, recoveryRoundAction, null);
		State exit = new State("exit", StateType.END, new ExitAction(), null);

		
		/*Define Guards (=conditions) for transitions*/
		Guard startProtocol = new EventTypeGuard(StartProtocolEvent.class);
		Guard allSetupMessagesReceived = new EventTypeGuard(AllSetupMessagesReceivedEvent.class);
		Guard allCommitMessagesReceived = new EventTypeGuard(AllCommitMessagesReceivedEvent.class);
		Guard allVotingMessagesReceived = new EventTypeGuard(AllVotingMessagesReceivedEvent.class);
		Guard allRecoveryMessagesReceived = new EventTypeGuard(AllRecoveringMessagesReceivedEvent.class);
		Guard notAllMessageReceived = new EventTypeGuard(NotAllMessageReceivedEvent.class);
		Guard resultComputed = new EventTypeGuard(ResultComputedEvent.class);
		
		try {
			/*Add states*/
			stmap.addState(begin);
			stmap.addState(setup);
			stmap.addState(commit);
			stmap.addState(vote);
			stmap.addState(tally);
			stmap.addState(recovery);			
			stmap.addState(exit);
		
			/*Add transitions*/

			//Transition of state begin
			stmap.addTransition("begin-setup", startProtocol, begin, null, setup);
			//Transition of state setup
			stmap.addTransition("setup-commit", allSetupMessagesReceived, setup, null, commit);
			//Transition of state commit
			stmap.addTransition("commit-vote", allCommitMessagesReceived, commit, null, vote);
			//Transition of state vote
			stmap.addTransition("vote-tally", allVotingMessagesReceived, vote, null, tally);
			stmap.addTransition("vote-recovery", notAllMessageReceived, vote, null, recovery);
			//Transition of state recovery
			stmap.addTransition("recovery-tally", allRecoveryMessagesReceived, recovery, null, tally);
			//Transition of state tally
			stmap.addTransition("tally-exit", resultComputed, tally, null, exit);
		
			/*Build map*/
			stmap.build();

		} catch (FiniteStateException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}

		/*Start state machine*/
		sm = new StateMachine(stmap, new EntityAdapter(this));
		try {
			//wait a little time to ensure that all other participant had time to create their state machine
			//and register the needed listeners
			SystemClock.sleep(2000);
			sm.applyEvent(new StartProtocolEvent(null));
		} catch (FiniteStateException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the state machine
	 * @return the state machine
	 */
	public StateMachine getStateMachine(){
		return sm;
	}
	
	/**
	 * Reset all actions
	 */
	public void reset(){
		this.setupRoundAction.reset();
		this.commitmentRoundAction.reset();
		this.votingRoundAction.reset();
		this.recoveryRoundAction.reset();
		this.tallyingAction.reset();
	}
	
}
