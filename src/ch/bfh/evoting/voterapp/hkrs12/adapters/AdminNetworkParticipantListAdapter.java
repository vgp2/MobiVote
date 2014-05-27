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


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ch.bfh.evoting.voterapp.hkrs12.R;
import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import ch.bfh.evoting.voterapp.hkrs12.entities.Participant;
import ch.bfh.evoting.voterapp.hkrs12.entities.VoteMessage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * Adapter listing the participants that are present in the network and if they are included or not in the electorate
 * This class is used in the Android ListView
 * @author Philemon von Bergen
 *
 */
public class AdminNetworkParticipantListAdapter extends ArrayAdapter<Participant> {

	private Context context;
	private List<Participant> values;

	/**
	 * Create an adapter object
	 * @param context android context
	 * @param textViewResourceId id of the layout that must be inflated
	 * @param objects list of participants that have to be displayed
	 */
	public AdminNetworkParticipantListAdapter(Context context, int textViewResourceId, List<Participant> objects) {
		super(context, textViewResourceId, objects);
		this.context=context;
		this.values=objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);

		View view;
		if (null == convertView) {
			//when view is created
			view =  inflater.inflate(R.layout.list_item_participant_network_admin, parent, false);
		} else {
			view = convertView;
		}

		final CheckBox cbInElectorate = (CheckBox) view.findViewById(R.id.checkbox_inelectorate);
		final TextView tvContent = (TextView) view.findViewById(R.id.textview_content);

		//set the participant identification
		tvContent.setText(this.values.get(position).getIdentification());

		view.setTag(position);
		cbInElectorate.setTag(position);
		//set the click listener
		OnClickListener click = new OnClickListener() {

			@Override
			public void onClick(View v) {

				if(!(v instanceof CheckBox))
					cbInElectorate.toggle();

				if(cbInElectorate.isChecked()){
					values.get((Integer)v.getTag()).setSelected(true);
				} else {
					values.get((Integer)v.getTag()).setSelected(false);
				}

				//Send the updated list of participants in the network over the network
				Map<String,Participant> map = new TreeMap<String,Participant>();
				for(Participant p : values){
					map.put(p.getUniqueId(), p);
				}
				VoteMessage vm = new VoteMessage(VoteMessage.Type.VOTE_MESSAGE_ELECTORATE, (Serializable)map);
				AndroidApplication.getInstance().getNetworkInterface().sendMessage(vm);
			}
		};
		view.setOnClickListener(click);
		cbInElectorate.setOnClickListener(click);

		if(values.get(position).isSelected()){
			cbInElectorate.setChecked(true);
		} else {
			cbInElectorate.setChecked(false);
		}

		return view;
	}

	@Override
	public Participant getItem (int position)
	{
		return super.getItem (position);
	}

}
