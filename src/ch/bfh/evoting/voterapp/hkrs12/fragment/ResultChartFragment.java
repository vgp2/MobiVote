/**
 * 
 */
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
package ch.bfh.evoting.voterapp.hkrs12.fragment;

import org.achartengine.GraphicalView;

import ch.bfh.evoting.voterapp.hkrs12.R;

import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;
import ch.bfh.evoting.voterapp.hkrs12.protocol.HKRS12ProtocolInterface;
import ch.bfh.evoting.voterapp.hkrs12.util.PieChartView;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class ResultChartFragment extends Fragment {
	
	String [] labels;
	float [] values;
	
	private Poll poll;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		poll = (Poll) getActivity().getIntent().getSerializableExtra("poll");
		if(poll==null){
			poll = ((HKRS12ProtocolInterface)AndroidApplication.getInstance().getProtocolInterface()).getRunningPoll();
		}
		
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_result_chart, container,
				false);
		
		// Displaying the graph
		LinearLayout layoutGraph = (LinearLayout) v.findViewById(R.id.layout_result_chart);
		//values = calculateData(values);
		GraphicalView chartView = PieChartView.getNewInstance(getActivity(), poll);
		chartView.setClickable(true);
		
		
		chartView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (getFragmentManager().findFragmentByTag("resultChartDialog") == null){
					ResultChartDialogFragment.newInstance().show(getFragmentManager(), "resultChartDialog");
				}
			}
		});
		
		layoutGraph.addView(chartView);
		
		return v;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		try {
	        getFragmentManager().beginTransaction().remove(this).commit();
	    } catch (IllegalStateException e) {
	       
	    }
	}

}
