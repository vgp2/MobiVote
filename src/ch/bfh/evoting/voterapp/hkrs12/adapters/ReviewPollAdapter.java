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
package ch.bfh.evoting.voterapp.hkrs12.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import ch.bfh.evoting.voterapp.hkrs12.R;

import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.evoting.voterapp.hkrs12.entities.Participant;
import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;
import ch.bfh.evoting.voterapp.hkrs12.entities.VoteMessage;
import ch.bfh.evoting.voterapp.hkrs12.util.Separator;

/**
 * List adapter showing a list of the options
 * 
 * @author von Bergen Philémon
 */
public class ReviewPollAdapter extends BaseAdapter {

	private Context context;
	private ArrayList<Object> data = new ArrayList<Object>();
	
	public ReviewPollAdapter(Context context, Poll poll) {
		this.context = context;
		data.add(new Separator("Question"));
		data.add(poll);
		data.add(new Separator("Options"));
		data.addAll(poll.getOptions());
		data.add(new Separator("Participants"));
		data.addAll(poll.getParticipants().values());
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		
		View view = null;
		LayoutInflater inflater = LayoutInflater.from(context);
		
		
		if (this.getItem(position) instanceof Poll){
			view = inflater.inflate(R.layout.list_item_question, parent, false);
			TextView tvSeparator = (TextView) view.findViewById(R.id.textview_question);
			tvSeparator.setText(((Poll)this.getItem(position)).getQuestion());
		}
		else if (this.getItem(position) instanceof Option){
			view = inflater.inflate(R.layout.list_item_option_poll, parent, false);
			TextView tvOption = (TextView)view.findViewById(R.id.textview_poll_option_review);
			tvOption.setText(((Option)this.getItem(position)).getText());
		}
		else if (this.getItem(position) instanceof Participant){
			
			Participant participant = (Participant)this.getItem(position);
			
			view = inflater.inflate(R.layout.list_item_participant_poll, parent, false);
			TextView tvParticipant = (TextView)view.findViewById(R.id.textview_participant_identification);
			tvParticipant.setText(participant.getIdentification());
			TextView tvKey = (TextView)view.findViewById(R.id.textview_participant_key);
			tvKey.setText(participant.getPublicKey());

			ImageView ivAcceptImage = (ImageView)view.findViewById(R.id.imageview_accepted_img);
			ProgressBar pgWaitForAccept = (ProgressBar)view.findViewById(R.id.progress_bar_waitforaccept);
			CheckBox btnValidateReview = (CheckBox)view.findViewById(R.id.button_validate_review);

			
			//set the correct image
			if(participant.hasAcceptedReview()){
				pgWaitForAccept.setVisibility(View.GONE);
				ivAcceptImage.setVisibility(View.VISIBLE);
				btnValidateReview.setVisibility(View.GONE);
			} else {
				if(participant.getUniqueId().equals(AndroidApplication.getInstance().getNetworkInterface().getMyUniqueId())){
					pgWaitForAccept.setVisibility(View.GONE);
					ivAcceptImage.setVisibility(View.GONE);
					btnValidateReview.setVisibility(View.VISIBLE);
					btnValidateReview.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View v) {
							AndroidApplication.getInstance().getNetworkInterface().sendMessage(new VoteMessage(VoteMessage.Type.VOTE_MESSAGE_ACCEPT_REVIEW, ""));
						}
					});
				} else {
					pgWaitForAccept.setVisibility(View.VISIBLE);
					ivAcceptImage.setVisibility(View.GONE);
					btnValidateReview.setVisibility(View.GONE);
				}
			}
		}
		else if (this.getItem(position) instanceof Separator){
			view = inflater.inflate(R.layout.list_item_separator, parent, false);
			TextView tvSeparator = (TextView) view.findViewById(R.id.textview_separator);
			tvSeparator.setText(((Separator)this.getItem(position)).getText());
		}
		else {
			view = inflater.inflate(R.layout.list_item_string, parent, false);
			TextView tvSeparator = (TextView) view.findViewById(R.id.textview_content);
			tvSeparator.setText(this.getItem(position).toString());
		}
		
		return view;
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Object getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
}
