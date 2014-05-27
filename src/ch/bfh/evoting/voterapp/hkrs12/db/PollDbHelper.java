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
package ch.bfh.evoting.voterapp.hkrs12.db;

import java.util.ArrayList;
import java.util.List;

import ch.bfh.evoting.voterapp.hkrs12.entities.DatabaseException;
import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Helper class to access to the application database
 * @author Philemon von Bergen
 *
 */
public class PollDbHelper extends SQLiteOpenHelper {
	
	private static final String TAG = PollDbHelper.class.getSimpleName();

	// Basic DB parameters
	private static final String DATABASE_NAME = "poll_options.db";
	private static final int DATABASE_VERSION = 2;

	// Table names
	private static final String TABLE_NAME_POLLS = "poll";
	private static final String TABLE_NAME_OPTIONS = "option";

	// Attributes of the poll table
	private static final String POLL_ID = "id";
	private static final String POLL_QUESTION = "question";
	private static final String POLL_START_TIME = "start_time";
	private static final String POLL_IS_TERMINATED = "is_terminated";
	private static final String POLL_NUMBER_PARTICIPANTS = "number_participants";

	// Attributes of the results table
	private static final String OPTION_ID = "id";
	private static final String OPTION_POLL_ID = "poll_id";
	private static final String OPTION_TEXT = "text";
	private static final String OPTION_NUMBER_OF_VOTES = "nbr_votes";
	private static final String OPTION_PERCENTAGE = "percentage";
	
	private static PollDbHelper instance;

	/**
	 * Private constructor of this helper
	 * @param context
	 */
	private PollDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		
	}

	/**
	 * Return the instance of the DB Helper
	 * @param ctx android context
	 * @return a DB hepler object
	 */
	public static PollDbHelper getInstance(Context ctx) {

		if (instance == null) {
			instance = new PollDbHelper(ctx);
		}
		return instance;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		
		// Create the schema if it is not there
		String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_POLLS
				+ " (" + POLL_ID + " INTEGER PRIMARY KEY, "
				+ POLL_QUESTION + " TEXT, "
				+ POLL_START_TIME + " INTEGER, "
				+ POLL_IS_TERMINATED + " INTEGER, "
				+ POLL_NUMBER_PARTICIPANTS + " INTEGER);";

		db.execSQL(sql);

		sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_OPTIONS + " ("
				+ OPTION_ID + " INTEGER PRIMARY KEY, "
				+ OPTION_POLL_ID + " INTEGER, "
				+ OPTION_TEXT + " TEXT, "
				+ OPTION_NUMBER_OF_VOTES + " INTEGER, "
				+ OPTION_PERCENTAGE + " REAL, "
				+ "FOREIGN KEY(" + OPTION_POLL_ID
				+ ") REFERENCES " + TABLE_NAME_POLLS + "("
				+ POLL_ID + "));";

		db.execSQL(sql);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// drop the schema if there is a new version
		Log.w(this.getClass().getSimpleName(), "DB Upgrade from Version " + oldVersion + " to version "
				+ newVersion);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_POLLS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_OPTIONS);
		onCreate(db);
	}
	
	/**
	 * Get all non-terminated poll
	 * @return a list of non terminated polls
	 */
	public List<Poll> getAllOpenPolls(){
		String sql1 = "SELECT * FROM " + TABLE_NAME_POLLS + " WHERE "+ POLL_IS_TERMINATED +"=0 ORDER BY ID ASC;";
		return this.getListOfPolls(sql1, null);
	}
	
	/**
	 * Get all terminated polls
	 * @return a list of terminated polls
	 */
	public List<Poll> getAllTerminatedPolls(){
		String sql1 = "SELECT * FROM " + TABLE_NAME_POLLS + " WHERE "+ POLL_IS_TERMINATED +"=1 ORDER BY ID ASC;";
		return this.getListOfPolls(sql1, null);
	}

	/**
	 * Get all terminated and non-terminated polls
	 * @return list of all polls
	 */
	public List<Poll> getAllPolls(){
		String sql1 = "SELECT * FROM " + TABLE_NAME_POLLS + " ORDER BY ID ASC;";
		return this.getListOfPolls(sql1, null);
	}
	
	/**
	 * Helper method getting a list of poll satisfying a query
	 * @param rawQuery the select query
	 * @param params the parameters to inject in the query
	 * @return the list of polls satisfying the query
	 */
	private List<Poll> getListOfPolls(String rawQuery, String[] params){
		SQLiteDatabase db = getReadableDatabase();

		Cursor c1 = db.rawQuery(rawQuery, params);

		List<Poll> polls = new ArrayList<Poll>();

		c1.moveToFirst();
		while(!c1.isAfterLast()){
			polls.add(this.extractPoll(c1, db));
			c1.moveToNext();
		}

		c1.close();
		db.close();
		return polls;
	}

	/**
	 * Get the poll with the given id
	 * @param pollId id of the poll wanted
	 * @return the poll object
	 */
	public Poll getPoll(int pollId){
		SQLiteDatabase db = getReadableDatabase();

		String sql1 = "SELECT * FROM " + TABLE_NAME_POLLS + " WHERE " + POLL_ID + "=? ORDER BY ID ASC;";
		Cursor c1 = db.rawQuery(sql1, new String[]{""+pollId});
		
		Poll poll = new Poll();

		c1.moveToFirst();
		while(!c1.isAfterLast()){
			poll = this.extractPoll(c1, db);
			c1.moveToNext();
		}

		c1.close();
		db.close();
		return poll;
	}
	
	/**
	 * Helper method getting a poll out of a cursor
	 * @param c1 cursor containing the result of a select query on polls table
	 * @param db open database
	 * @return the poll contained in the query
	 */
	private Poll extractPoll(Cursor c1, SQLiteDatabase db){
		Poll poll = new Poll();
		poll.setId(c1.getInt(c1.getColumnIndex(POLL_ID)));
		poll.setQuestion(c1.getString(c1.getColumnIndex(POLL_QUESTION)));
		poll.setStartTime(c1.getLong(c1.getColumnIndex(POLL_START_TIME)));
		poll.setTerminated(c1.getInt(c1.getColumnIndex(POLL_IS_TERMINATED)) == 1);
		poll.setNumberOfParticipants(c1.getInt(c1.getColumnIndex(POLL_NUMBER_PARTICIPANTS)));

		String sql2 = "SELECT * FROM " + TABLE_NAME_OPTIONS + " WHERE " + OPTION_POLL_ID + "=? ORDER BY ID ASC;";
		Cursor c2 = db.rawQuery(sql2, new String[]{""+c1.getInt(c1.getColumnIndex(POLL_ID))});

		List<Option> options = new ArrayList<Option>();

		c2.moveToFirst();
		while(!c2.isAfterLast()){
			Option option = new Option();
			option.setId(c2.getInt(c2.getColumnIndex(OPTION_ID)));
			option.setText(c2.getString(c2.getColumnIndex(OPTION_TEXT)));
			option.setPollId(c2.getInt(c2.getColumnIndex(OPTION_POLL_ID)));
			option.setVotes(c2.getInt(c2.getColumnIndex(OPTION_NUMBER_OF_VOTES)));
			option.setPercentage(c2.getDouble(c2.getColumnIndex(OPTION_PERCENTAGE)));
			options.add(option);
			c2.moveToNext();
		}
		c2.close();

		poll.setOptions(options);
		
		return poll;
	}
	
	/**
	 * Save a poll into the database
	 * @param poll the poll to save
	 * @return the id of the row where the poll has been inserted
	 * @throws DatabaseException thrown when an error occurred inserting the record in the db
	 */
	public long savePoll(Poll poll) throws DatabaseException{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues valuesPoll = new ContentValues();
		valuesPoll.put(POLL_QUESTION, poll.getQuestion());
		valuesPoll.put(POLL_START_TIME, poll.getStartTime());
		valuesPoll.put(POLL_IS_TERMINATED, poll.isTerminated());
		valuesPoll.put(POLL_NUMBER_PARTICIPANTS, poll.getNumberOfParticipants());
		
		long rowId = db.insert(TABLE_NAME_POLLS, null, valuesPoll);
		poll.setId((int)(rowId));

		long rowId2 = 0;
		for(Option option : poll.getOptions()){
			ContentValues valuesOption = new ContentValues();
			valuesOption.put(OPTION_POLL_ID, rowId);
			valuesOption.put(OPTION_TEXT, option.getText());
			valuesOption.put(OPTION_NUMBER_OF_VOTES, option.getVotes());
			valuesOption.put(OPTION_PERCENTAGE, option.getPercentage());

			rowId2 = db.insert(TABLE_NAME_OPTIONS, null, valuesOption);
		}
		db.close();

		if(rowId==-1 || rowId2 == -1){
			throw new DatabaseException("Error while saving terminated poll!");
		}
		return rowId;
	}
	
	/**
	 * Update an already existing poll in the DB
	 * @param pollId id of the poll to update
	 * @param poll the poll object containing the data to update
	 * @throws DatabaseException thrown when an error occurred inserting the record in the db
	 */
	public void updatePoll(int pollId, Poll poll) throws DatabaseException{
		SQLiteDatabase db = getWritableDatabase();
		String strFilter = POLL_ID + "=" + pollId;
		ContentValues valuesPoll = new ContentValues();
		valuesPoll.put(POLL_QUESTION, poll.getQuestion());
		valuesPoll.put(POLL_START_TIME, poll.getStartTime());
		valuesPoll.put(POLL_IS_TERMINATED, poll.isTerminated());
		valuesPoll.put(POLL_NUMBER_PARTICIPANTS, poll.getNumberOfParticipants());
		db.update(TABLE_NAME_POLLS, valuesPoll, strFilter, null);
		
		Log.d(TAG, "I have " + poll.getOptions().size() + " Options in the update.");
		Log.d(TAG, "I have " + poll.getNumberOfParticipants() + " Participants in the update.");
		
		//Delete actual options and put the new options
		db.delete(TABLE_NAME_OPTIONS, OPTION_POLL_ID + "=" + pollId, null);
		
		long rowId2 = -1;
		for(Option option : poll.getOptions()){
			
			ContentValues valuesOption = new ContentValues();
			valuesOption.put(OPTION_POLL_ID, pollId);
			valuesOption.put(OPTION_TEXT, option.getText());
			valuesOption.put(OPTION_NUMBER_OF_VOTES, option.getVotes());
			valuesOption.put(OPTION_PERCENTAGE, option.getPercentage());

			rowId2 = db.insert(TABLE_NAME_OPTIONS, null, valuesOption);
		}
		
		if(rowId2 == -1 && poll.getOptions().size()!=0){
			throw new DatabaseException("Error while saving terminated poll!");
		}
		
		db.close();
	}

	/**
	 * Get the number of open polls in the database
	 * @return the number of open polls in the database
	 */
	public int getNumberOfOpenPolls(){
		String countQuery = "SELECT  * FROM " + TABLE_NAME_POLLS + " WHERE "+ POLL_IS_TERMINATED +"=0 ORDER BY ID ASC;";
		return this.count(countQuery, null);
	}
	
	/**
	 * Get the number of terminated poll in the database
	 * @return the number of terminated poll in the database
	 */
	public int getNumberOfTerminatedPolls(){
		String countQuery = "SELECT  * FROM " + TABLE_NAME_POLLS + " WHERE "+ POLL_IS_TERMINATED +"=1 ORDER BY ID ASC;";
		return this.count(countQuery, null);
	}
	
	/**
	 * Get the total number of poll
	 * @return the total number of poll
	 */
	public int getNumberOfPolls(){
		String countQuery = "SELECT  * FROM " + TABLE_NAME_POLLS + " ORDER BY ID ASC;";
		return this.count(countQuery, null);
	}

	/**
	 * Get the number of option for the given poll
	 * @param pollId id of the poll
	 * @return number of option of this poll
	 */
	public int getNumberOfOptionsForPoll(int pollId){
		String countQuery = "SELECT  * FROM " + TABLE_NAME_OPTIONS + " WHERE " + OPTION_POLL_ID + "=? ORDER BY ID ASC;";
		return this.count(countQuery, new String[]{""+pollId});
	}
	
	/**
	 * Helper method executing a count query
	 * @param rawQuery the SQL query
	 * @param params the parameters to inject in the query
	 * @return the number of records matching the query
	 */
	private int count(String rawQuery, String[] params){
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(rawQuery, params);
		int count = cursor.getCount();
		cursor.close();
		db.close();

		return count;
	}
	
	/**
	 * Delete a poll from the db
	 * @param pollId id of the poll to delete
	 */
	public void deletePoll(int pollId){
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.delete(TABLE_NAME_OPTIONS, OPTION_POLL_ID + "=" + pollId, null);
		db.delete(TABLE_NAME_POLLS, POLL_ID + "=" + pollId, null);

		db.close();
	}
}