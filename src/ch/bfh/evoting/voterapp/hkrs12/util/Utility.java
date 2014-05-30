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
package ch.bfh.evoting.voterapp.hkrs12.util;

import java.lang.reflect.Field;

import ch.bfh.evoting.voterapp.hkrs12.AndroidApplication;
import de.mindpipe.android.logging.log4j.LogConfigurator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Utility class 
 * @author Philemon von Bergen
 *
 */
public class Utility {

	
	/**
	 * Configure Log4J to also log in LogCat
	 */
	public static void initialiseLogging() {
		final LogConfigurator logConfigurator = new LogConfigurator();

		logConfigurator.setFileName(AndroidApplication.getInstance().getFilesDir() + "/evotingcircle.log");
		logConfigurator.setRootLevel(AndroidApplication.LEVEL);
		
		// max 3 rotated log files
		logConfigurator.setMaxBackupSize(3);
		// Max 500ko per file
		logConfigurator.setMaxFileSize(500000);
		logConfigurator.configure();
	}
	
	/**
	 * 
	 * Hack to change the color of the separator and the title text in the dialog.
	 * Many thanks to David Wasser
	 * http://stackoverflow.com/questions/14770400/android-alertdialog-styling
	 * 
	 * @param alert dialog to modify
	 * @param color color to attribute to the title and the separator
	 */
	public static void setTextColor(DialogInterface alert, int color) {
	    try {
	        Class<?> c = alert.getClass();
	        Field mAlert = c.getDeclaredField("mAlert");
	        mAlert.setAccessible(true);
	        Object alertController = mAlert.get(alert);
	        c = alertController.getClass();
	        Field mTitleView = c.getDeclaredField("mTitleView");
	        mTitleView.setAccessible(true);
	        Object dialogTitle = mTitleView.get(alertController);
	        TextView dialogTitleView = (TextView)dialogTitle;
	        // Set text color on the title
	        dialogTitleView.setTextColor(color);
	        // To find the horizontal divider, first
	        //  get container around the Title
	        ViewGroup parent = (ViewGroup)dialogTitleView.getParent();
	        // Then get the container around that container
	        parent = (ViewGroup)parent.getParent();
	        for (int i = 0; i < parent.getChildCount(); i++) {
	            View v = parent.getChildAt(i);
	            if (v instanceof View) {
	            	if (v.getHeight() < 5){
	            		v.setBackgroundColor(color);
	            	}
	            }
	        }
	    } catch (Exception e) {
	        // Ignore any exceptions, either it works or it doesn't
	    }
	}
	
	public static IntentFilter[] getNFCIntentFilters() {
		IntentFilter nfcIntentFilter = new IntentFilter();
		nfcIntentFilter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
		nfcIntentFilter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
		return new IntentFilter[] { nfcIntentFilter };
	}
}
