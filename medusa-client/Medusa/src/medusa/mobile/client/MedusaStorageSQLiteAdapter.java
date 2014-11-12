/**
 * 'MedusaStorageSQLiteAdapter'
 *
 * - This class implements SQLite-specific functions 
 * 	 primarily for MedusaStorageManager.
 *   
 * - Current Scope of Realization
 * 		2011.02.02. - basic function support. (create db/table, basic insert/select test)
 * 		
 * @created : Jan. 26th 2011
 * @modified : Dec. 14th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

public class MedusaStorageSQLiteAdapter 
{
	private static final String TAG = "MedusaSQLiteAdapter";

	/* inner class starts (instance of SQLiteOpenHelper) */
	private class MedusaSQLiteDBHelper extends SQLiteOpenHelper {
		
		public String SQL_CREATE_MEDIA_METADATA_TABLE = "create table if not exists mediameta ("
				+ " uid integer primary key,"
				+ " path text unique, "
				+ " type text, "
				+ " fsize text, "
				+ " lat text, "
				+ " lng text, "
				+ " ctime text, "
				+ " mtime text, "
				+ " review text"
				+ " ); ";
				
		public MedusaSQLiteDBHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase _db) {
			
			/* Create internal tables here */
			_db.execSQL(SQL_CREATE_MEDIA_METADATA_TABLE);
			//_db.execSQL(SQL_CREATE_RESOURCE_LIMIT_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Called when there is a database to conform to the new version.
			// In other words, it will be called due to the db version mismatch.
			Log.d(TAG, "* Currently empty since we are not using db versioning..");
		}
	}
	/* inner class ends */
	
	private String dbname; 	/* ex) mydb.db */
	private String tbname; 	/* ex) mainTable */

	private SQLiteDatabase db;
	private final Context context;
	private MedusaSQLiteDBHelper dbHelper;

	/* constructor */
	public MedusaStorageSQLiteAdapter(Context in_context, String in_dbname) 
	{
		context = in_context;
		dbname = in_dbname;
		dbHelper = new MedusaSQLiteDBHelper(context, dbname, null, 1);
	}

	public MedusaStorageSQLiteAdapter open() throws SQLiteException 
	{
		db = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		db.close();
	}

	public Cursor runSQL(String sqlstmt) 
	{
		if (sqlstmt.startsWith("select") == true) {
			Cursor rcursor = db.rawQuery(sqlstmt, null);
			Log.v(TAG, "* sqlstmt: " + sqlstmt + " cnt= " + rcursor.getCount());

			return rcursor;
		} else {
			Log.v(TAG, "* sqlstmt: " + sqlstmt);
			try {
				db.execSQL(sqlstmt);
			}
			catch (SQLiteConstraintException e) {
				;
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}
	}
}
