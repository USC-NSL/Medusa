/**
 * 'MedusaStorageManager'
 *
 * - This class implements MedusaStorageManager.
 *   Its super class is MedusaServiceManager and 
 *   has designed so that it can support multiple types 
 *   of data abstractions.
 *   
 * - Current Scope of Realization
 *         2011.01.26. - basic sqlite function support. (MedusaStorageSQLiteAdapter)
 *         
 * @created  : Jan. 26th 2011
 * @modified : Dec. 2nd 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu), Bin Liu (binliu@usc.edu), Jeongyeup Paek (jpaek@usc.edu)
 **/

package medusa.mobile.client;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;

public class MedusaStorageManager extends MedusaManagerBase {

	private static String TAG = "MedusaStorageMgr";
	
	/*
	 * Static methods for enforcing singleton pattern.
	 */
	private static MedusaStorageManager manager = null;

	public static MedusaStorageManager getInstance() {
		if (manager == null) {
			manager = new MedusaStorageManager();
			manager.init("MedusaStorageManager");
		}

		return manager;
	}

	public static void terminate() {
		if (manager != null) {
			manager.markExit();
			manager = null;
		}
	}

	/*
	 * Custom requestService wrapper for MedusaStorageManager.
	 */
	public static void requestService(String medusaletname,
			MedusaletCBBase listener, String type, String content,
			String cond, int n, String unit) {
		MedusaServiceE ele = new MedusaServiceE();
		StringBuilder b = new StringBuilder();

		b.append("<xml>");
			b.append("<medusaletname>"); b.append(medusaletname); b.append("</medusaletname>");
			b.append("<storage>");
				b.append("<type>");	b.append(type);	b.append("</type>");
				b.append("<content>"); b.append(content); b.append("</content>");
				b.append("<cond>");	b.append(cond);	b.append("</cond>");
				b.append("<time>");	
					b.append("<num>");	b.append(n); b.append("</num>");
					b.append("<unit>");	b.append(unit);	b.append("</unit>");
				b.append("</time>");
			b.append("</storage>");
		b.append("</xml>");

		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = listener;

		try {
			MedusaStorageManager.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Subscribe Function.
	 */
	public static void requestServiceSubscribe(String medusaletname, MedusaletCBBase listener, String content) {
		MedusaStorageManager.requestService(medusaletname, listener, "subscribe", content, null, 0, null);
	}

	public static void requestServiceUnsubscribe(String medusaletname, MedusaletCBBase listener, String content) {
		
		List<MedusaletCBBase> cbAggList = null;
		cbAggList = getInstance().subscriberMap.get(content);
		
		if (cbAggList != null) {
			Iterator<MedusaletCBBase> aitr = cbAggList.iterator();
			MedusaletCBBase cb;
			while (aitr.hasNext() == true) {
				cb = aitr.next();				
				if (cb == listener) {
					aitr.remove();
					Log.d(TAG,	"* DataType [" + content + "] is unsubscribed by Medusalet ["
							+ medusaletname + "]");
					break;
				}
			}
		}		
	}
	
	/*
	 * Wrapper for camera query.
	 */
	public static void requestServiceCamera(String medusaletname,	MedusaletCBBase listener, String content, int n, String unit) {
		MedusaStorageManager.requestService(medusaletname, listener, "camera", content, "time", n, unit);
	}

	/*
	 * Wrapper for text file read/write.
	 */
	public static void requestServiceTextFile(String medusaletname,
			MedusaletCBBase listener, String subtype, String fnamehead, String content, String operation) 
	{
		MedusaServiceE ele = new MedusaServiceE();
		StringBuilder b = new StringBuilder();

		b.append("<xml>");
			b.append("<medusaletname>"); b.append(medusaletname); b.append("</medusaletname>");
			b.append("<storage>");
				b.append("<type>text</type>");
				b.append("<subtype>"); 
					b.append(subtype); // current, "learning"
				b.append("<subtype>");
				b.append("<fnamehead>"); b.append(fnamehead); b.append("</fnamehead>");
				b.append("<content>"); b.append(content); b.append("</content>");
				b.append("<operation>"); b.append(operation); b.append("</operation>");
			b.append("</storage>");
		b.append("</xml>");

		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = listener;

		try {
			MedusaStorageManager.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Wrapper for text file .
	 */
	public static void requestServiceSQLite(String medusaletname,
			MedusaletCBBase listener, String dbname, String sqlstmt) 
	{
		MedusaServiceE ele = new MedusaServiceE();
		StringBuilder b = new StringBuilder();

		b.append("<xml>");
			b.append("<medusaletname>"); b.append(medusaletname); b.append("</medusaletname>");
			b.append("<storage>");
				b.append("<type>sqlite</type>");
				b.append("<dbname>"); b.append(dbname); b.append("</dbname>");
				b.append("<operation>"); 
					b.append("<sqlstmt>"); b.append(MedusaUtil.base64Encode(sqlstmt)); b.append("</sqlstmt>");
				b.append("</operation>");
			b.append("</storage>");
		b.append("</xml>");

		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = listener;

		try {
			MedusaStorageManager.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}	
	
	/* 
	 * blocking operations. 
	 */
	public static Cursor requestServiceSqliteB(String dbname, String sqlstmt) 
	{
		try {
			MedusaStorageSQLiteAdapter dbAdapter = (new MedusaStorageSQLiteAdapter(G.appCtx, dbname)).open();
			Cursor dbCursor = dbAdapter.runSQL(sqlstmt);
			dbAdapter.close();
			
			return dbCursor;
		}
		catch (Exception e) {
			return null;
		}
	}
	
	public static boolean isExistInDB(String dbname, String sqlstmt) 
	{
		int ret = 0;
		
		MedusaStorageSQLiteAdapter dbAdapter = (new MedusaStorageSQLiteAdapter(G.appCtx, dbname)).open();
		Cursor dbCursor = dbAdapter.runSQL(sqlstmt);
		ret = dbCursor.getCount();
		dbCursor.close();
		dbAdapter.close();
		
		return ret > 0 ? true : false;
	}
	
	HashMap<String, List<MedusaletCBBase>> subscriberMap;

	@Override
	public void init(String name) 
	{
		super.init(name);
		subscriberMap = new HashMap<String, List<MedusaletCBBase>>();
	}

	@Override
	protected void execute(MedusaServiceE ele) 
	{
		Log.v(TAG, "* " + mgrName + "'s execute() is running.");
		//Log.d(TAG, "* medusaletname -> [" + configMap.get("medusaletname") + "]");

		/*
		 * XML format hierarchy.
		 * 
		 * - medusaletname - storage - type [type == sqlite] - dbname - operation
		 * - sqlstmt [type == camera] - content
		 */

		if (configMap.get("storage_type").compareTo("camera") == 0
				&& configMap.get("storage_content") != null) {
			/*
			 * storage_type == "camera" : (MRA) I believe this has been
			 * deprecated.
			 */
			String fname = configMap.get("storage_content");
			String cond = configMap.get("storage_cond");

			// check the condition to retrieve data
			if (cond != null) {
				String condstr = configMap.get("storage_" + cond);
				int within = 0;

				if (condstr != null && condstr.compareTo("time") == 0) {
					/* time constraint */
					int num = Integer.valueOf(configMap.get("storage_cond_time_num"));
					String unit = configMap.get("storage_cond_time_unit");
					within = num;

					if (unit.compareTo("hour") == 0) {
						within = within * 60 * 60;
					} else if (unit.compareTo("min") == 0) {
						within = within * 60;
					}
				}
			}

			// retrieve media directory.
			if (fname.contains(".jpg") == true) {
				// File ipath =
				// Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/Camera");
				File ipath = new File(Environment.getExternalStorageDirectory()
						.getAbsolutePath() + "/DCIM/Camera");
				Log.d(TAG, "* DCIM path: " + ipath.toString());
				File fs[] = ipath.listFiles();

				if (fs != null) {
					for (int i = 0; i < fs.length; i++) {
						Log.d(TAG,	"* entry " + i + " " + fs[i].getName());
					}
					ele.seCBListner.callCBFunc(fs, "OK: " + fs.length
							+ " images");
				} else {
					Log.d(TAG, "! no such files..(image)");
				}
			} else if (fname.contains(".3gp") == true
					|| fname.contains(".mp4") == true) {
				String mpath = "/sdcard/DCIM/Camera";
				File f = new File(mpath, fname);
				File fs[] = f.listFiles();

				if (fs != null) {
					ele.seCBListner.callCBFunc(fs, "OK: " + fs.length + " videos");
				} else {
					Log.d(TAG, "! no such files..(video)");
				}
			}
		} 
		else if (configMap.get("storage_type").compareTo("text") == 0
				&& configMap.get("storage_subtype") != null 
				&& configMap.get("storage_fnamehead") !=null) {
				
				String file_path = configMap.get("storage_subtype");
				if (file_path.equals("learning")) {
					file_path = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/medusa_text_file/collaborative_learning/";
				}			
				String file_name = file_path + configMap.get("storage_fnamehead");
				
				if (configMap.get("storage_operation").compareTo("read") == 0) {
										
					if (MedusaStorageTextFileAdapter.exist(file_name)) {						
						if (ele.seCBListner != null) {
							ele.seCBListner.callCBFunc(MedusaStorageTextFileAdapter.read(file_name), "");	
						}
					} else {					
						if (ele.seCBListner != null) {
							ele.seCBListner.callCBFunc(null, "text file " + configMap.get("fnamehead")
									+ " does not exist!");
						}
					}
					
				} else if (configMap.get("storage_operation").compareTo("write") == 0) {
					
					file_name = generateTimestampFilename(file_name);
					String content = configMap.get("storage_content");
					
					if (ele.seCBListner != null) {
						
						ele.seCBListner.callCBFunc(MedusaStorageTextFileAdapter.write(file_name, content, false), file_name);
					}
										
				} else if (configMap.get("storage_operation").compareTo("append") == 0) {
					
					file_name = generateTimestampFilename(file_name);
					String content = configMap.get("storage_content");
					
					if (ele.seCBListner != null) {
						
						ele.seCBListner.callCBFunc(MedusaStorageTextFileAdapter.write(file_name, content, true), file_name);
					}					
					
				}
			
		} 
		else if (configMap.get("storage_type").compareTo("sqlite") == 0
				&& configMap.get("storage_dbname") != null) {
			/*
			 * storage_type == "sqlite" : general sqlite operations.
			 */
			String dbname = configMap.get("storage_dbname");
			MedusaStorageSQLiteAdapter dbAdapter = (new MedusaStorageSQLiteAdapter(G.appCtx, dbname)).open();

			String sqlstmt = configMap.get("storage_operation_sqlstmt");
			sqlstmt = MedusaUtil.base64Decode(sqlstmt);
			Cursor dbCursor = dbAdapter.runSQL(sqlstmt);

			dbAdapter.close();

			if (ele.seCBListner != null) {
				ele.seCBListner.callCBFunc(dbCursor, "* sql [" + sqlstmt + "] OK");
			}
			
			/* 
			 * Since callback call becomes asynchrounous,
			 * cursor should be closed in the callback function.
			 */
			//if (dbCursor != null) dbCursor.close();
			
		} 
		else if (configMap.get("storage_type").compareToIgnoreCase("subscribe") == 0) {
			/*
			 * storage_type == "subscribe" : Subscribe specific types of data. -
			 * Possible types: media, txt, etc..
			 */
			String content = configMap.get("storage_content");
			List<MedusaletCBBase> cbList = subscriberMap.get(content);

			if (content.compareToIgnoreCase(content) == 0) {
				if (cbList == null) {
					cbList = new LinkedList<MedusaletCBBase>();
					cbList.add(ele.seCBListner);
					subscriberMap.put(content, cbList);
				} else {
					if (ele.seCBListner == null) {
						Log.e(TAG, "! callback function is null for the subscriber?");
					}
					else {
						cbList.add(ele.seCBListner);
					}
				}

				Log.d(TAG,	"* DataType [" + content + "] is subscribed by Medusalet ["
										+ configMap.get("medusaletname") + "]");
			}
		} 
		else {
			if (ele.seCBListner != null) {
				ele.seCBListner.callCBFunc(null, "! storage_type " + configMap.get("storage_type")
													+ " is wrong or not yet implemented.");
			}
		}
	}

	public void announceToSubscribers(String dtype, String path, String msg) 
	{
		/*
		 * [Exact Match] call individual subscribers.
		 */
		
		List<MedusaletCBBase> cbList = subscriberMap.get(dtype);
		
		if (cbList != null) {
			Iterator<MedusaletCBBase> itr = cbList.iterator();
			MedusaletCBBase cb;
			while (itr.hasNext() == true) {
				cb = itr.next();
				cb.callCBFunc(path, "* Announcement to [" + dtype + "] subscribers.");
			}
		}

		/*
		 * [Category Match] process aggregated terms.
		 */
		if (dtype.compareTo("image") == 0 || dtype.compareTo("video") == 0
				|| dtype.compareTo("audio") == 0 || dtype.compareTo("flv") == 0) {
			List<MedusaletCBBase> cbAggList = subscriberMap.get("media");
			if (cbAggList != null) {
				Iterator<MedusaletCBBase> aitr = cbAggList.iterator();
				MedusaletCBBase cb;
				while (aitr.hasNext() == true) {
					cb = aitr.next();
					cb.callCBFunc(path, "* Announcement to media subscribers: " + dtype);
				}
			}
		}
		
	}

	/*
	 * [Static Functions] Other StorageManager Services.
	 */
	public static void addFileToDb(String medusaletname, String path, long size, long time) 
	{
		Cursor cr = MedusaStorageManager.requestServiceSqliteB("medusadata.db", "select type,fsize from mediameta where path='" + path + "'");
		
		StringBuilder sb = new StringBuilder();
		String dtype = "";

		/*
		 * check extension and let subscribers know.
		 */
		if (path.contains("faces_") == true) {
			dtype = "face";
		}
		else if (path.endsWith(".jpg") == true || path.endsWith(".gif") == true) {
			dtype = "image";
		} 
		else if (path.endsWith(".3gp") == true
				|| path.endsWith(".mp4") == true) {
			dtype = "video";
		} 
		else if (path.endsWith(".ac4") == true
				|| path.endsWith(".mp3") == true) {
			dtype = "audio";
		} 
		else if (path.endsWith(".txt") == true) {
			dtype = "text";
		} 
		else if (path.endsWith(".flv") == true) {
			dtype = "flv";
		}

		if (cr.getCount() == 0) {
			sb.append("INSERT into mediameta(path,type,fsize,mtime) VALUES('");
			sb.append(path + "','" + dtype + "','" + size + "','" + time + "');");
			
			Log.v(TAG, "* adding new entry to metadata table fname=" + sb.toString() + ", type=" + dtype);
			MedusaStorageManager.requestServiceSQLite(medusaletname, null, "medusadata.db", sb.toString());
			
			MedusaGpsManager.requestUpdateGpsOnMetaTable(medusaletname, path, 60000 /* default timeout: 1000 minutes */);
			
			// Announce new events to the subscribers.
			MedusaStorageManager.getInstance().announceToSubscribers(dtype, path, "* FILE CREATED");
		}
		else {
			/* data is already existed */
			if (dtype.equals("image") == true || dtype.equals("face") == true) {
				/* 
				 * In YX setting, image file could be overwritten by the software
				 * to reduce the file size.
				 */
				if (cr.moveToFirst()) {
	    			do {
	    				String ftype = cr.getString(0 /* column index */);
	    				String fsize = cr.getString(1 /* column index */);
	    				
	    				if (dtype.equals(ftype) == true && size != Long.parseLong(fsize)) {
		    				StringBuilder strb = new StringBuilder();
		    				strb.append("UPDATE mediameta set fsize='" + fsize + "' where path='" + path + "'");
							MedusaStorageManager.requestServiceSQLite(TAG, null, "medusadata.db", strb.toString());
	    				}
	    				
	    			} while(cr.moveToNext());
	    			
	    			cr.close();
				}
			} 
			else {
				Log.v(TAG, "* ignored. already existed in the mediameta table.");
			}
		}
		
		cr.close();
		cr = null;
	}

	public static void addFileToDb(String medusaletname, String path) 
	{
		File fpath = new File(path);
		addFileToDb(medusaletname, path, fpath.length(), fpath.lastModified());
	}

	/* create filename */
	public static String generateTimestampFilename(String fnamehead) 
	{
		// set current time (default TimeZone is GMT)
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		String timeString = String.format("%1$tF_%1$tH_%1$tM_%1$tS", calendar);
		String stringwt = fnamehead + "_" + timeString + ".txt";
		
		return stringwt;
	}
	
	public static void updateReviewColumnOnDB(String path, String uid, String review)
	{
		String sqlstmt = "update mediameta set review=\"" + review + 
						"\" where path=\"" + path + "\" and uid='" + uid + "'";
		MedusaStorageManager.requestServiceSQLite("ReviewUpdate", null, "medusadata.db", sqlstmt);
	}
}
