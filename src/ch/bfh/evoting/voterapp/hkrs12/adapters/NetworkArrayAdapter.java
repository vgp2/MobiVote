
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
import java.util.HashMap;

import ch.bfh.evoting.voterapp.hkrs12.R;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Implements an ArrayAdapter which maps the values of the Hashmaps stored in
 * the ArrayList to the views in the layout displaying all the available WLAN
 * networks
 * 
 * @author Juerg Ritter (rittj1@bfh.ch)
 */
public class NetworkArrayAdapter extends ArrayAdapter<HashMap<String, Object>> {

	private ArrayList<HashMap<String, Object>> items;
	private Context context;
	private String capabilities;
	
	private boolean hideCreateNetwork = false;

	
	/**
	 * @param context
	 *            The context from which it has been created
	 * @param textViewResourceId
	 *            The id of the item layout
	 * @param items
	 *            The ArrayList with the values
	 */
	public NetworkArrayAdapter(Context context, int textViewResourceId,
			ArrayList<HashMap<String, Object>> items) {
		super(context, textViewResourceId, items);
		this.context = context;
		this.items = items;
	}
	
	
	/**
	 * @param context
	 *            The context from which it has been created
	 * @param textViewResourceId
	 *            The id of the item layout
	 * @param items
	 *            The ArrayList with the values
	 * @param hideCreateNetwork
	 * 			  Indicates whether the the last item should have a router icon or not, default is false
	 */
	public NetworkArrayAdapter(Context context, int textViewResourceId,
			ArrayList<HashMap<String, Object>> items, boolean hideCreateNetwork) {
		super(context, textViewResourceId, items);
		this.context = context;
		this.items = items;
		
		this.hideCreateNetwork = hideCreateNetwork;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.list_item_network, null);
		}

		// extract the Hashmap at the according postition of the ArrayList
		HashMap<String, Object> item = items.get(position);
		if (item != null) {
			// extract the views in the layout
			TextView content = (TextView) view.findViewById(R.id.textview_content);
			TextView description = (TextView) view
					.findViewById(R.id.textview_description);
			ImageView icon = (ImageView) view.findViewById(R.id.imageview_icon);

			if (this.getCount() - 1 == position && !hideCreateNetwork) {
				// the last item is for the "Create Network" item, so the icon
				// needs to be the router if we don't hide the createnetwork option
				icon.setImageResource(R.drawable.glyphicons_046_router);
				icon.setBackgroundColor(context.getResources().getColor(
						R.color.hotspot));
				description.setText("");
			} else {
				// set the values of the labels accordingly
				icon.setImageResource(R.drawable.glyphicons_032_wifi_alt);
				icon.setBackgroundColor(context.getResources().getColor(
						R.color.network));
				capabilities = (String) item.get("capabilities");
				if (capabilities == null) {
					description.setText("");
				} else if (capabilities.contains("WPA")) {
					description.setText(R.string.wpa_secure);
				} else if (capabilities.contains("WEP")) {
					description.setText(R.string.wep_secure);
				} else {
					description.setText(R.string.unsecure);
				}
			}
			content.setText((String) item.get("SSID"));

		}
		return view;
	}
	
	
}
