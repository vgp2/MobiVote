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
 *   a written agreement between you and Bern University of Applied Sciences (BFH), Research Institute for
 *   Security in the Information Society (RISIS), E-Voting Group (EVG)
 *   Quellgasse 21, CH-2501 Biel, Switzerland.
 * 
 *
 *   For further information contact us: http://e-voting.bfh.ch/
 * 
 *
 * Redistributions of files must retain the above copyright notice.
 */
package ch.bfh.evoting.voterapp.hkrs12;

import org.apache.log4j.Level;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import ch.bfh.evoting.voterapp.hkrs12.R;
import ch.bfh.evoting.voterapp.hkrs12.network.AllJoynNetworkInterface;
import ch.bfh.evoting.voterapp.hkrs12.network.NetworkInterface;
import ch.bfh.evoting.voterapp.hkrs12.network.NetworkMonitor;
import ch.bfh.evoting.voterapp.hkrs12.protocol.HKRS12ProtocolInterface;
import ch.bfh.evoting.voterapp.hkrs12.protocol.ProtocolInterface;
import ch.bfh.evoting.voterapp.hkrs12.util.BroadcastIntentTypes;
import ch.bfh.evoting.voterapp.hkrs12.util.JavaSerialization;
import ch.bfh.evoting.voterapp.hkrs12.util.SerializationUtil;
import ch.bfh.evoting.voterapp.hkrs12.util.Utility;

/**
 * Class representing the application. This class is used to do some initializations and to share data.
 * @author Philemon von Bergen
 *
 */
public class AndroidApplication extends Application {

	public static final String PREFS_NAME = "network_preferences";
	public static final Level LEVEL = Level.DEBUG;
	public static final String FOLDER = "/MobiVote/";
	public static final String EXTENSION = ".mobix";


	private static AndroidApplication instance;
	private SerializationUtil su;
	private NetworkInterface ni;
	private ProtocolInterface pi;
	private Activity currentActivity = null;
	private boolean isAdmin = false;
	private boolean voteRunning;


	private AlertDialog dialogNetworkLost;
	private AlertDialog dialogWrongKey;
	private NetworkMonitor networkMonitor;
	private AlertDialog waitDialog;
	private boolean dialogShown;


	/**
	 * Return the single instance of this class
	 * @return the single instance of this class
	 */
	public static AndroidApplication getInstance() {
		return instance;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		//Force to show help overlays
		//		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		//		settings.edit().putBoolean("first_run_ReviewPollVoterActivity", true).commit();
		//		settings.edit().putBoolean("first_run_NetworkConfigActivity", true).commit();
		//		settings.edit().putBoolean("first_run", true).commit();

		WifiManager wm = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		if(!wm.isWifiEnabled()){
			wm.setWifiEnabled(true);
		}

		instance = this;
		instance.initializeInstance();
		Utility.initialiseLogging();

		//wifi event listener
		IntentFilter filters = new IntentFilter();
		filters.addAction("android.net.wifi.STATE_CHANGED");
		filters.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		networkMonitor = new NetworkMonitor(this);
		this.registerReceiver(networkMonitor, filters);
		registerActivityLifecycleCallbacks(new AndroidApplicationActivityLifecycleCallbacks());


		LocalBroadcastManager.getInstance(this).registerReceiver(mGroupEventReceiver, new IntentFilter(BroadcastIntentTypes.networkGroupDestroyedEvent));
		LocalBroadcastManager.getInstance(this).registerReceiver(mAttackDetecter, new IntentFilter(BroadcastIntentTypes.attackDetected));
		LocalBroadcastManager.getInstance(this).registerReceiver(startPollReceiver, new IntentFilter(BroadcastIntentTypes.electorate));
		LocalBroadcastManager.getInstance(this).registerReceiver(wrongDecryptionKeyReceiver, new IntentFilter(BroadcastIntentTypes.probablyWrongDecryptionKeyUsed));
		LocalBroadcastManager.getInstance(this).registerReceiver(waitDialogDismiss, new IntentFilter(BroadcastIntentTypes.dismissWaitDialog));
		LocalBroadcastManager.getInstance(this).registerReceiver(waitDialogShow, new IntentFilter(BroadcastIntentTypes.showWaitDialog));
		LocalBroadcastManager.getInstance(this).registerReceiver(differentProtocolsReceiver, new IntentFilter(BroadcastIntentTypes.differentProtocols));
		LocalBroadcastManager.getInstance(this).registerReceiver(differentPollsReceiver, new IntentFilter(BroadcastIntentTypes.differentPolls));
		LocalBroadcastManager.getInstance(this).registerReceiver(resultComputationFailedReceiver, new IntentFilter(BroadcastIntentTypes.resultNotFound));
		LocalBroadcastManager.getInstance(this).registerReceiver(proofVerificationFailedReceiver, new IntentFilter(BroadcastIntentTypes.proofVerificationFailed));

	}

	@Override
	public void onTerminate() {
		if(this.ni!=null)
			this.ni.disconnect();
		this.unregisterReceiver(networkMonitor);
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel("mobivote", 1);
		super.onTerminate();
	}



	/*--------------------------------------------------------------------------------------------
	 * Helper Methods
	--------------------------------------------------------------------------------------------*/

	/**
	 * Initialize the Serialization method and the Network Component to use
	 */
	private void initializeInstance() {

		new AsyncTask<Object, Object, Object>() {

			@Override
			protected Object doInBackground(Object... params) {

				su = new SerializationUtil(new JavaSerialization());
				ni = new AllJoynNetworkInterface(AndroidApplication.this.getApplicationContext());///* new InstaCircleNetworkInterface(this.getApplicationContext());*/new SimulatedNetworkInterface(AndroidApplication.this.getApplicationContext());
				pi = new HKRS12ProtocolInterface(AndroidApplication.this.getApplicationContext());

				return null;
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

	}




	/*--------------------------------------------------------------------------------------------
	 * Getters/Setters
	--------------------------------------------------------------------------------------------*/

	/**
	 * Get the serialization helper
	 * @return the serialization helper
	 */
	public SerializationUtil getSerializationUtil(){
		return su;
	}

	/**
	 * Get the network component
	 * @return the network component
	 */
	public NetworkInterface getNetworkInterface(){
		return ni;
	}

	/**
	 * Get the protocol component
	 * @return the protocol component
	 */
	public ProtocolInterface getProtocolInterface(){
		return pi;
	}

	/**
	 * Get the network monitor receiving wifi events
	 * @return the network monitor receiving wifi events
	 */
	public NetworkMonitor getNetworkMonitor(){
		return this.networkMonitor;
	}

	/**
	 * Get the activity that is currently running
	 * @return the activity that is currently running, null if none is running
	 */
	public Activity getCurrentActivity(){
		return currentActivity;
	}

	/**
	 * Set the activity that is currently running
	 * @param currentActivity the activity that is currently running
	 */
	public void setCurrentActivity(Activity currentActivity){
		this.currentActivity = currentActivity;

		if(isVoteRunning()){
			// Create a pending intent which will be invoked after tapping on the
			// Android notification
			Intent notificationIntent = new Intent(this,
					currentActivity.getClass());
			notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent pendingNotificationIntent = PendingIntent.getActivity(
					this, 0, notificationIntent, 0);

			// Setting up the notification which is being displayed
			Notification.Builder notificationBuilder = new Notification.Builder(
					this);
			notificationBuilder.setContentTitle(getResources().getString(
					R.string.voter_app_app_name));
			notificationBuilder
			.setContentText(getResources().getString(R.string.notification));
			notificationBuilder
			.setSmallIcon(R.drawable.ic_launcher);
			notificationBuilder.setContentIntent(pendingNotificationIntent);
			notificationBuilder.setOngoing(true);
			@SuppressWarnings("deprecation")
			Notification notification = notificationBuilder.getNotification();

			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

			notificationManager.notify("mobivote", 1, notification);
		} else {
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancel("mobivote", 1);
		}
	}

	/**
	 * Set a flag indicating that a vote session is running
	 * @param running true if a vote session is running, false otherwise
	 */
	public void setVoteRunning(boolean running){
		this.voteRunning = running;
	}

	/**
	 * Indicate if a vote session is running
	 * @return true if yes, false otherwise
	 */
	public boolean isVoteRunning(){
		return voteRunning;
	}

	/**
	 * Indicate if this user is the administrator of the vote
	 * @return true if yes, false otherwise
	 */
	public boolean isAdmin() {
		return isAdmin;
	}

	/**
	 * Set if this user is the administrator of the vote 
	 * @param isAdmin true if this user is the administrator of the vote, false otherwise
	 */
	public void setIsAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}



	/*--------------------------------------------------------------------------------------------
	 * Broadcast receivers
	--------------------------------------------------------------------------------------------*/

	/**
	 * this broadcast receiver listens for information about the network group destruction
	 */
	private BroadcastReceiver mGroupEventReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(ni==null) return;
			if(currentActivity!=null && ni.getNetworkName()!=null){
				if(voteRunning){
					AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
					// Add the buttons
					builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Intent i = new Intent(AndroidApplication.this, MainActivity.class);
							currentActivity.startActivity(i);
						}
					});

					builder.setTitle(R.string.dialog_title_network_lost);
					builder.setMessage(R.string.dialog_network_lost);


					dialogNetworkLost = builder.create();
					dialogNetworkLost.setOnShowListener(new DialogInterface.OnShowListener() {
						@Override
						public void onShow(DialogInterface dialog) {
							Utility.setTextColor(dialog, getResources().getColor(R.color.theme_color));
							dialogNetworkLost.getButton(AlertDialog.BUTTON_NEUTRAL).setBackgroundResource(
									R.drawable.selectable_background_votebartheme);
						}
					});

					// Create the AlertDialog
					dialogNetworkLost.show();
				} else {
					for(int i=0; i < 2; i++)
						Toast.makeText(currentActivity, getResources().getString(R.string.toast_network_lost), Toast.LENGTH_SHORT).show();
				}
				ni.disconnect();
			}
		}
	};

	/**
	 * this broadcast receiver listens for information about an attack
	 */
	private BroadcastReceiver mAttackDetecter = new BroadcastReceiver() {
		private AlertDialog dialogAttack;

		@Override
		public void onReceive(Context context, Intent intent) {
			if(currentActivity!=null && voteRunning){
				AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
				// Add the buttons
				builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Intent i = new Intent(AndroidApplication.this, MainActivity.class);
						currentActivity.startActivity(i);
					}
				});

				builder.setTitle(R.string.dialog_title_attack_detected);
				if(intent.getIntExtra("type", 0)==1){
					builder.setMessage(R.string.dialog_attack_impersonalization);
				} else if(intent.getIntExtra("type", 0)==2){
					builder.setMessage(R.string.dialog_attack_different_senders);
				} else if(intent.getIntExtra("type", 0)==3){
					builder.setMessage(R.string.dialog_attack_different_salts);
				}

				// Create the AlertDialog
				dialogAttack = builder.create();

				dialogAttack.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialog) {
						Utility.setTextColor(dialog, getResources().getColor(R.color.theme_color));
						dialogAttack.getButton(AlertDialog.BUTTON_NEUTRAL).setBackgroundResource(
								R.drawable.selectable_background_votebartheme);
					}
				});
				dialogAttack.show();
			}
		}
	};

	/**
	 * this broadcast receiver listen for broadcasts containing the electorate. So, if the user is member
	 * of a session, when the admin sends the electorate, the user is redirected to the correct activity, wherever
	 * he is.
	 */
	private BroadcastReceiver startPollReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			if(!isAdmin && !(currentActivity instanceof CheckElectorateActivity) && currentActivity!=null){
				Intent i = new Intent(AndroidApplication.this, CheckElectorateActivity.class);
				i.putExtra("participants", intent.getSerializableExtra("participants"));
				currentActivity.startActivity(i);
			}
		}
	};

	/**
	 * this broadcast receiver listens for messages indicating that many decryptions failed
	 */
	private BroadcastReceiver wrongDecryptionKeyReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(currentActivity!=null){
				AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);

				// Add the buttons
				builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});

				builder.setTitle(R.string.dialog_title_wrong_key);
				builder.setMessage(R.string.dialog_wrong_key_pwd);

				// Create the AlertDialog
				dialogWrongKey = builder.create(); 

				dialogWrongKey.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialog) {
						Utility.setTextColor(dialog, getResources().getColor(R.color.theme_color));
						dialogWrongKey.getButton(AlertDialog.BUTTON_NEUTRAL).setBackgroundResource(
								R.drawable.selectable_background_votebartheme);
					}
				});
				dialogWrongKey.show();
			}
		}
	};

	/**
	 * this broadcast receiver listens for messages indicating that different protocols are used by different participants
	 */
	private BroadcastReceiver differentProtocolsReceiver = new BroadcastReceiver() {
		private AlertDialog dialogDiffProtocols;

		@Override
		public void onReceive(Context context, Intent intent) {
			if(currentActivity!=null){
				AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);

				// Add the buttons
				builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
						Intent i = new Intent(AndroidApplication.this, MainActivity.class);
						currentActivity.startActivity(i);
					}
				});

				builder.setTitle(R.string.dialog_title_different_protocols);
				builder.setMessage(R.string.dialog_different_protocols);

				// Create the AlertDialog
				dialogDiffProtocols = builder.create(); 

				dialogDiffProtocols.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialog) {
						Utility.setTextColor(dialog, getResources().getColor(R.color.theme_color));
						dialogDiffProtocols.getButton(AlertDialog.BUTTON_NEUTRAL).setBackgroundResource(
								R.drawable.selectable_background_votebartheme);
					}
				});
				dialogDiffProtocols.show();
			}
		}
	};

	/**
	 * this broadcast receiver listens for messages indicating that different polls are used by different participants
	 */
	private BroadcastReceiver differentPollsReceiver = new BroadcastReceiver() {
		private AlertDialog dialogDiffPolls;

		@Override
		public void onReceive(Context context, Intent intent) {
			if(currentActivity!=null){
				AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);

				// Add the buttons
				builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
						Intent i = new Intent(AndroidApplication.this, MainActivity.class);
						currentActivity.startActivity(i);
					}
				});

				builder.setTitle(R.string.dialog_title_different_polls);
				builder.setMessage(R.string.dialog_different_polls);

				// Create the AlertDialog
				dialogDiffPolls = builder.create(); 

				dialogDiffPolls.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialog) {
						Utility.setTextColor(dialog, getResources().getColor(R.color.theme_color));
						dialogDiffPolls.getButton(AlertDialog.BUTTON_NEUTRAL).setBackgroundResource(
								R.drawable.selectable_background_votebartheme);
					}
				});
				dialogDiffPolls.show();
			}
		}
	};

	/**
	 * this broadcast receiver listens for messages indicating that the result could not be computed
	 */
	private BroadcastReceiver resultComputationFailedReceiver = new BroadcastReceiver() {
		private AlertDialog dialogResultComputed;

		@Override
		public void onReceive(Context context, Intent intent) {

			AlertDialog.Builder builder = new AlertDialog.Builder(AndroidApplication.getInstance().getCurrentActivity());
			// Add the buttons
			builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int id) {
					dialogResultComputed.dismiss();
					Intent i = new Intent(AndroidApplication.this, MainActivity.class);
					currentActivity.startActivity(i);
				}
			});

			builder.setTitle(R.string.dialog_title_result_not_computed);
			builder.setMessage(R.string.dialog_result_not_computed);


			dialogResultComputed = builder.create();
			dialogResultComputed.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					Utility.setTextColor(dialog, AndroidApplication.getInstance().getResources().getColor(R.color.theme_color));
					dialogResultComputed.getButton(AlertDialog.BUTTON_NEUTRAL).setBackgroundResource(
							R.drawable.selectable_background_votebartheme);
				}
			});

			// Create the AlertDialog
			dialogResultComputed.show();
		}
	};

	/**
	 * this broadcast receiver listens for messages indicating that a proof verification failed
	 */
	private BroadcastReceiver proofVerificationFailedReceiver = new BroadcastReceiver() {
		private AlertDialog dialogProof;

		@Override
		public void onReceive(Context context, Intent intent) {

			AlertDialog.Builder builder = new AlertDialog.Builder(AndroidApplication.getInstance().getCurrentActivity());
			// Add the buttons
			builder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int id) {
					dialogProof.dismiss();
				}
			});

			builder.setTitle(R.string.dialog_title_excluded);
			int type = intent.getIntExtra("type", 0);
			switch(type){
			case 1:
				builder.setMessage(getString(R.string.dialog_proof_verification_failed, intent.getStringExtra("participant")));
				break;
			case 2:
				builder.setMessage(getString(R.string.dialog_exclusion_time_out, intent.getStringExtra("participant")));
				break;
			case 3:
				builder.setMessage(getString(R.string.dialog_exclusion_participant_left, intent.getStringExtra("participant")));
				break;
			default:
				builder.setMessage(getString(R.string.dialog_exclusion_default, intent.getStringExtra("participant")));
				break;
			}
			
			dialogProof = builder.create();
			dialogProof.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialog) {
					Utility.setTextColor(dialog, AndroidApplication.getInstance().getResources().getColor(R.color.theme_color));
					dialogProof.getButton(AlertDialog.BUTTON_NEUTRAL).setBackgroundResource(
							R.drawable.selectable_background_votebartheme);
				}
			});

			// Create the AlertDialog
			dialogProof.show();
		}
	};

	/**
	 * this broadcast receiver listens for commands to show a wait dialog on the current activity
	 */
	private BroadcastReceiver waitDialogDismiss = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			dismissDialog();
		}
	};

	/**
	 * this broadcast receiver listens for commands to remove a wait dialog on the current activity
	 */
	private BroadcastReceiver waitDialogShow = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			showDialog(currentActivity);
		}
	};

	public boolean backupDialogShown;

	/**
	 * Shows a wait dialog on the given activity
	 * @param activity where to show the wait dialog
	 */
	private void showDialog(Activity activity){
		if(waitDialog!=null && waitDialog.isShowing()) return;
		//Prepare wait dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setMessage(R.string.dialog_wait_wifi);
		waitDialog = builder.create();
		waitDialog.setCancelable(false);
		waitDialog.show();
		dialogShown = true;
	}

	/**
	 * Removes a wait dialog on the given activity
	 * @param activity where to remove the wait dialog
	 */
	private void dismissDialog(){
		if(waitDialog!=null && waitDialog.isShowing()){
			try{
				waitDialog.dismiss();
			} catch (IllegalArgumentException e){
				//some time dismissing a dialog that is no more attached to the view
				e.printStackTrace();
			}
			dialogShown = false;
		}
	}


	/**
	 * Android callback class
	 * This class is used to manage the displaying of the wait dialog when activity changes occur 
	 */
	private class AndroidApplicationActivityLifecycleCallbacks implements ActivityLifecycleCallbacks {

		private String connectedSSID = "";

		public void onActivityCreated(Activity activity, Bundle bundle) {
			if(isVoteRunning()){
				activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
				activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			} else {
				activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
				activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}
			if(dialogShown){
				showDialog(activity);
			}
		}


		public void onActivityDestroyed(Activity activity) {
		}

		public void onActivityPaused(Activity activity) {
			this.connectedSSID = networkMonitor.getConnectedSSID();
			if(dialogShown){
				backupDialogShown = true;
				dismissDialog();
			}
		}

		public void onActivityResumed(Activity activity) {
			if(isVoteRunning()){
				activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
				activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			} else {
				activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
				activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}
			if(backupDialogShown){
				showDialog(activity);
				backupDialogShown=false;
			}
			if(this.connectedSSID == null) return;
			if(!this.connectedSSID.equals(networkMonitor.getConnectedSSID())){
				Intent intent = new Intent(BroadcastIntentTypes.networkGroupDestroyedEvent);
				LocalBroadcastManager.getInstance(AndroidApplication.this).sendBroadcast(intent);
			}

		}

		public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
		}

		public void onActivityStarted(Activity activity) {
		}

		public void onActivityStopped(Activity activity) {
		}
	}
}
