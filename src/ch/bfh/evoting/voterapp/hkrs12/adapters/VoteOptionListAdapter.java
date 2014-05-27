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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import ch.bfh.evoting.voterapp.hkrs12.R;

import ch.bfh.evoting.voterapp.hkrs12.VoteActivity;
import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.evoting.voterapp.hkrs12.util.Utility;

/**
 * Adapter listing the different vote options that can be chosen in the vote
 * This class is used in the Android ListView
 * @author Philemon von Bergen
 *
 */
public class VoteOptionListAdapter extends ArrayAdapter<Option> {

	private Context context;
	private List<Option> values;
	private int selected = -1;

	private AlertDialog dialogConfirmVote = null;


	/**
	 * Create an adapter object
	 * @param context android context
	 * @param textViewResourceId id of the layout that must be inflated
	 * @param objects list of options that can be chosen in the vote
	 */
	public VoteOptionListAdapter(Context context, int textViewResourceId, List<Option> objects) {
		super(context, textViewResourceId, objects);
		this.context=context;
		this.values=objects;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);

		View view;
		if (null == convertView) {
			//when view is created
			view =  inflater.inflate(R.layout.list_item_vote, parent, false);			
		} else {
			view = convertView;
		}

		final RadioButton rbChoice = (RadioButton)view.findViewById(R.id.radiobutton_choice);
		final TextView tvContent = (TextView) view.findViewById(R.id.textview_content);

		tvContent.setText(this.values.get(position).getText());
		view.setTag(position);
		rbChoice.setTag(position);

		if (position == selected) {
			rbChoice.setChecked(true);
		} else {
			rbChoice.setChecked(false);
		}

		// set the click listener
		OnClickListener click = new OnClickListener() {
			@Override
			public void onClick(View v) {
				selected = position;
				VoteOptionListAdapter.this.notifyDataSetChanged();

				final VoteActivity activity = (VoteActivity)context;
				
				if(!activity.getScrolled()){
					for(int i=0; i < 2; i++)
						Toast.makeText(context, context.getString(R.string.toast_scroll), Toast.LENGTH_SHORT).show();
				} else if (getSelectedPosition() == -1){
					for(int i=0; i < 2; i++)
						Toast.makeText(context, context.getString(R.string.toast_choose_one_option), Toast.LENGTH_SHORT).show();
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					// Add the buttons
					builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							activity.castBallot();
							dialogConfirmVote.dismiss();
						}
					});
					builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialogConfirmVote.dismiss();
						}
					});

					Option op = getItemSelected();

					builder.setTitle(R.string.dialog_title_confirm_vote);
					builder.setMessage(Html.fromHtml(context.getString(R.string.dialog_confirm_vote, op.getText())));

					// Create the AlertDialog
					dialogConfirmVote = builder.create();
					
					dialogConfirmVote.setOnShowListener(new DialogInterface.OnShowListener() {
						@Override
						public void onShow(DialogInterface dialog) {
							Utility.setTextColor(dialog, context.getResources().getColor(R.color.theme_color));
							dialogConfirmVote.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundResource(
									R.drawable.selectable_background_votebartheme);
							dialogConfirmVote.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundResource(
									R.drawable.selectable_background_votebartheme);
							
						}
					});
					
					dialogConfirmVote.show();
				}
			}
		};

		view.setOnClickListener(click);

		return view;
	}

	@Override
	public Option getItem(int position) {
		if (position >= this.getCount() || position < 0)
			return null;
		return super.getItem(position);
	}

	public Option getItemSelected(){
		return this.values.get(selected);
	}

	public int getSelectedPosition () {
		return selected;
	}
}
