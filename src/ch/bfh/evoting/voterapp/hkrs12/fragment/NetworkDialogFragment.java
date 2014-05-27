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

import ch.bfh.evoting.voterapp.hkrs12.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.view.View;

public class NetworkDialogFragment extends DialogFragment {
	
	private AlertDialog dialog;
	
	// Factory method to create a new EditTextDialogFragment 
    public static NetworkDialogFragment newInstance() {
    	NetworkDialogFragment frag = new NetworkDialogFragment();
    	return frag;
    }
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		
		
		View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_network_information, null);

		AlertDialog.Builder builder = new AlertDialog.Builder( getActivity( ) )
        .setView(view)
        .setIcon( android.R.drawable.ic_dialog_info )
        .setNeutralButton(R.string.close, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				FragmentTransaction ft = NetworkDialogFragment.this.getFragmentManager().beginTransaction();
				ft.remove(getFragmentManager().findFragmentByTag("networkInformation"));
                ft.commit();
				dismiss();
			}
        	
        });
		
		dialog = builder.create();

		dialog.setOnShowListener(new OnShowListener() {

			public void onShow(DialogInterface dialog) {
				((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL)
						.setBackgroundResource(
								R.drawable.selectable_background_votebartheme);
			}
		});

        return dialog;
    }
	
}
