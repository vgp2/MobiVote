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

import java.util.List;

import ch.bfh.evoting.voterapp.hkrs12.R;

import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * List adapter showing a list of the options
 * 
 * @author von Bergen Philémon
 */
public class PollOptionAdapter extends ArrayAdapter<Option> {

	private Context context;
	private Poll poll;
	private List<Option> values;

	/**
	 * Create an adapter object
	 * 
	 * @param context
	 *            android context
	 * @param textViewResourceId
	 *            id of the layout that must be inflated
	 * @param poll
	 *            poll from which the options should be listed
	 */
	public PollOptionAdapter(Context context, int textViewResourceId,
			Poll poll) {
		super(context, textViewResourceId, poll.getOptions());
		this.context = context;
		this.poll = poll;
		this.values = poll.getOptions();
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);

		View view;
		ImageButton btnDelete;
		if (null == convertView) {
			view = inflater.inflate(R.layout.list_item_polloption, parent,false);
		} else {
			view = convertView;
			if(view.findViewById(R.id.button_deleteoption)==null){
				view = inflater.inflate(R.layout.list_item_polloption, parent,false);
			}
		}
		btnDelete = (ImageButton) view.findViewById(R.id.button_deleteoption);

		TextView tvContent = (TextView) view
				.findViewById(R.id.textview_content);
		tvContent.setText(this.poll.getOptions().get(position).getText());

		if(this.poll.getOptions().get(position).getText().equals(this.getContext().getString(R.string.empty_vote))){
			LinearLayout ll = (LinearLayout)btnDelete.getParent();
			ll.removeView(btnDelete);
			((LinearLayout)ll.getParent()).setBackgroundColor(Color.parseColor("lightgray"));
		} else {
			LinearLayout ll = (LinearLayout)btnDelete.getParent();
			((LinearLayout)ll.getParent()).setBackgroundResource(android.R.color.transparent);
			btnDelete.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					values.remove(position);
					poll.setOptions(values);
					notifyDataSetChanged();
				}

			});
		}
		
		return view;
	}
}
