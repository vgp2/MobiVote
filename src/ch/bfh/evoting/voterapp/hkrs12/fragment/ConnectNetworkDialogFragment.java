

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

import java.util.List;

import ch.bfh.evoting.voterapp.hkrs12.R;
import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

/**
 * Fragment which is included in the the Dialog which is shown after clicking on
 * a network in the main screen.
 * 
 * @author Juerg Ritter (rittj1@bfh.ch)
 * 
 */
@SuppressLint("ValidFragment")
public class ConnectNetworkDialogFragment extends DialogFragment implements
OnClickListener, TextWatcher {

	/*
	 * The activity that creates an instance of this dialog fragment must
	 * implement this interface in order to receive event callbacks. Each method
	 * passes the DialogFragment in case the host needs to query it.
	 */
	public interface NoticeDialogListener {
		public void onDialogPositiveClick(DialogFragment dialog);

		public void onDialogNegativeClick(DialogFragment dialog);
	}

	private EditText txtPassword;
	private EditText txtNetworkKey;

	private String password;
	private String networkKey;
	private String groupName;
	
	private Button buttonJoin;
	private Button buttonCancel;


	private boolean showNetworkKeyField;

	private AlertDialog dialog;
	private Spinner groupsSpinner;
	private BroadcastReceiver groupsBroadcastReceiver;


	/**
	 * @param showNetworkKeyField
	 *            this boolean defines whether the network key field should be
	 *            displayed or not
	 */
	public ConnectNetworkDialogFragment(boolean showNetworkKeyField) {
		this.showNetworkKeyField = showNetworkKeyField;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.DialogFragment#onCreateDialog(android.os.Bundle)
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		LayoutInflater inflater = getActivity().getLayoutInflater();

		// applying the layout
		View view = inflater.inflate(R.layout.dialog_join_network, null);

		// extract the controls of the layout
		groupsSpinner = (Spinner) view.findViewById(R.id.groups_spinner);
		List<String> list = AndroidApplication.getInstance().getNetworkInterface().listAvailableGroups();
		if(list.isEmpty()){
			list.add(getString(R.string.no_group_available));
		}
		final ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this.getActivity(),
			android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		groupsSpinner.setAdapter(dataAdapter);
				
		//register a broadcast receiver for updating the list when list of groups changed
		groupsBroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context c, Intent intent) {
				dataAdapter.clear();
				List<String> list = AndroidApplication.getInstance().getNetworkInterface().listAvailableGroups();
				if(list.isEmpty()){
					list.add(getString(R.string.no_group_available));
				}
				dataAdapter.addAll(list);
				dataAdapter.notifyDataSetChanged();
			}
		};
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(groupsBroadcastReceiver, new IntentFilter(BroadcastIntentTypes.advertisedGroupChange));

		txtPassword = (EditText) view.findViewById(R.id.edittext_password);
		txtPassword.addTextChangedListener(this);

		txtNetworkKey = (EditText) view.findViewById(R.id.edittext_networkkey);
		txtNetworkKey.addTextChangedListener(this);
		
		

		if (!showNetworkKeyField) {
			txtNetworkKey.setVisibility(View.INVISIBLE);
			view.findViewById(R.id.textview_wlan_key_desc).setVisibility(View.INVISIBLE);
		}

		if(AndroidApplication.getInstance().isAdmin()){
			txtPassword.setVisibility(View.INVISIBLE);
			groupsSpinner.setVisibility(View.INVISIBLE);
			view.findViewById(R.id.textview_group_name_desc).setVisibility(View.INVISIBLE);
			view.findViewById(R.id.textview_group_password_desc).setVisibility(View.INVISIBLE);
		}
		
		if(txtPassword.getVisibility() == View.INVISIBLE &&
				groupsSpinner.getVisibility() == View.INVISIBLE &&
				txtNetworkKey.getVisibility() == View.INVISIBLE){
			saveData();
		}

		// Inflate and set the layout for the dialog
		// Pass null as the parent view because its going in the dialog layout
		builder.setView(view);
		// Add action buttons
		builder.setPositiveButton(R.string.join,
				new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				saveData();

			}
		});

		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				password = txtPassword.getText().toString();
				groupName = String.valueOf(groupsSpinner.getSelectedItem());//txtGroupName.getText().toString();
				networkKey = txtNetworkKey.getText().toString();
				getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, getActivity().getIntent());
			}
		});

		//builder.setTitle(R.string.network_password);



		dialog = builder.create();

		

		// always disable the Join button since the key is always empty and
		// therefore we are not ready to connect yet
		dialog.setOnShowListener(new OnShowListener() {

			public void onShow(DialogInterface dialog) {
					
					buttonJoin = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
					buttonCancel = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
					
					buttonJoin.setBackgroundResource(R.drawable.selectable_background_votebartheme);
					buttonCancel.setBackgroundResource(R.drawable.selectable_background_votebartheme);
				
					buttonJoin.setEnabled(false);
			}
		});

		return dialog;
	}
	

	private void saveData(){
		password = txtPassword.getText().toString();
		groupName = "group"+String.valueOf(groupsSpinner.getSelectedItem());//txtGroupName.getText().toString();
		networkKey = txtNetworkKey.getText().toString();
		
		if(!AndroidApplication.getInstance().isAdmin()){
			
			AndroidApplication.getInstance().getNetworkInterface().setGroupName(groupName);
			AndroidApplication.getInstance().getNetworkInterface().setGroupPassword(password);
		}
		
		getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, getActivity().getIntent());
	}

	/**
	 * Returns the network key which is defined in the textfield
	 * 
	 * @return the network key
	 */
	public String getNetworkKey() {
		return networkKey;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.text.TextWatcher#afterTextChanged(android.text.Editable)
	 */
	public void afterTextChanged(Editable s) {

		// handling the activation of the buttons
		if (showNetworkKeyField) {
			// activate only if there is at least one character in the password
			// field and 8 characters in the network key field
			if (txtPassword.getText().toString().length() < 1
					|| txtNetworkKey.getText().toString().length() < 8) {
				buttonJoin.setEnabled(false);
			} else {
				buttonJoin.setEnabled(true);
			}
		} else {
			if(AndroidApplication.getInstance().isAdmin()){
				buttonJoin.setEnabled(true);
			} else {
				// activate only if there is at least one character in the password
				// field
				if (txtPassword.getText().toString().length() < 1) {
					buttonJoin.setEnabled(false);
				} else {
					buttonJoin.setEnabled(true);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.text.TextWatcher#beforeTextChanged(java.lang.CharSequence,
	 * int, int, int)
	 */
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.text.TextWatcher#onTextChanged(java.lang.CharSequence, int,
	 * int, int)
	 */
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View view) {

	}

	@Override
	public void dismiss() {
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(groupsBroadcastReceiver);
		super.dismiss();
	}
	
	
}
