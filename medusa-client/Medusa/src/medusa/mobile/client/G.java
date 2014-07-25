/**
 * 'G'
 *
 * - The class for the Global Information/Functions.
 *
 * @created : Feb. 19th 2011
 * @modified : June. 19th 2012
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.util.ArrayList;
import java.util.HashMap;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.util.Log;
//import org.opencv.objdetect.CascadeClassifier;

public class G 
{
	public static final String TAG = "Medusa";
	/* 
	 * *************************************************************************************
	 * Configurable Parameters. 
	 * 		- You may need to change below settings appropriately.
	 * *************************************************************************************
	 */
	/* Medusa cloud configuration */
	public static final String SERVER_URL = "http://128.xxx.xxx.xxx";	/* Task tracker location */
	public static final String URIBASE_UPLOAD = SERVER_URL + "/Medusa/medusa-cloud/tasktracker/web_tt_upload.php";
	public static final String URIBASE_REPORT = SERVER_URL + "/Medusa/medusa-cloud/tasktracker/web_tt_service.php";
	/* For using reverse incentives */
	public static final String AMT_WRID = "AKIAJ4DYABCDEFGHIJKL";	/* Worker's AMT Requestor ID */		 
	public static final String AMT_WRKEY = "oyg+tsrPO3cQ85abcdefghijklmnopqrstuvwxyz";	/* Worker's AMT Requestor Key. */
	/*
	 * *************************************************************************************
	 */
	
	//GCM
	public static final String SENDER_ID = "GCM_SENDER_ID_HERE";
	public static final String ACTION_ON_REGISTERED = "medusa.mobile.client.ON_REGISTERED";
	public static final String FIELD_REGISTRATION_ID = "registration_id";
	public static final String FIELD_MESSAGE = "msg";
	/* Global Constants: in the medusa.mobile package */
	public static final String GPREF_NAME = "MEDUSA_PREF";
	/* C2DM Id */
	public static String C2DM_ID = "medusa";
	/* AMT Worker ID */
	public static String AMT_WWID = "DEFAULT";			
	/* GCM Id*/
	public static String GCM_ID = "medusagcm";
	/* Web Socket Server*/
	public static String WSS_HOSTNAME = "128.xxx.xxx.xxx";
	public static String WSS_PORT = "9002";

	public static final String NAME = "Medusa";
	public static final String VERSION = "0.9.4";
	public static final int	STATUSBAR_START_AQUALET_NID = 786;	/* no reason for this number, just need to be unique in the process. */
	public static final int	STATUSBAR_RETRY_AQUALET_NID = 787;
	public static final int	STATUSBAR_GROUP_TRIGGER_NID = 800;
	public static final int	STATUSBAR_STOP_AQUALET_NID_BASE = 1000;
	public static final int	STATUSBAR_REVIEW_NID_BASE = 4000;
	
	public static final String AQUA_SERVICE_NAME = "medusa.mobile.client.SERVICE";
	public static final String AQUA_REVIEW_DIALOG_NAME = "medusa.mobile.client.REVIEW";
	public static final String AQUA_DEBUG_LOG = "medusa.mobile.client.DEBUG_LOG";
	public static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	public static final String WAP_PUSH = "android.provider.Telephony.WAP_PUSH_RECEIVED";
	
	/* Global Constants: public */
	public static final String PATH_SDCARD = Environment.getExternalStorageDirectory().toString();
	
	/* Global Pool */
	private static HashMap<String, ArrayList<BroadcastReceiver>> receiverMap;	// BroadcastReceiver Map.
	private static HashMap<String, String> receiverTagMap;						// BroadcastReceiver Map.
	private static HashMap<String, String> directoryMap; 
	
	/* Public variables */
	public static Context appCtx;
	
	public static String tpVer;		/* platform version */
	public static String tpIMEI;
	public static String tpModel;
	public static String tpPhoneType;
	public static String tpNetworkOperatorName;
	public static String tpPhoneNumber;
	
	public static String amazonUID;
	public static int medusaletCounter = 100;
	
	public static TextToSpeech tts;
	//public static CascadeClassifier  mCascade;
	/* private variables */
	private static boolean bInit = false;

	public static void initialize(Context context) 
	{
		if (bInit == false) 
		{/*
			 mCascade = new CascadeClassifier("/sdcard/cascade.xml");
		      if (mCascade.empty()) {
		    	  Log.d("cascade", "cascade xml load failure!!");
		    	  mCascade = null;
		      } else
		      {
		    	  Log.d("cascade", "cascade xml load success!!");
		      }*/
			appCtx = context;
			tts = null;

			/* For broadcastMgr instance management */
			receiverMap = new HashMap<String, ArrayList<BroadcastReceiver>>();
			receiverTagMap = new HashMap<String, String>();
			
			/* Directories that will be observed. (metadata will be generated) */
			directoryMap = new HashMap<String, String>();
			/* 1. camera directory */
			putDirEntry("camera", "/DCIM/Camera");
			/* 2. text logs directory: these would go inside medusa(3 below) dir later.. */
			putDirEntry("textroot", "/medusa_text_file/");
			putDirEntry("bin", "/medusa_text_file/bin/");
			putDirEntry("netscan", "/medusa_text_file/network_scan/");
			putDirEntry("genmetadata", "/medusa_text_file/generate_metadata/");
			/* 3. medusa-related directory */
			putDirEntry("medusaroot", "/medusa/");
			putDirEntry("medusatmp", "/medusa/videosummary/");
			putDirEntry("videosummary", "/medusa/videosummary/");
			putDirEntry("facedetect", "/medusa/facedetect/");
			
			/*
			 * TelephonyManager information -
			 * http://developer.android.com/reference/android/telephony/TelephonyManager.html
			 */
			TelephonyManager telephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

			tpIMEI = telephonyMgr.getDeviceId();
			switch (telephonyMgr.getPhoneType()) {
			case TelephonyManager.PHONE_TYPE_GSM:
				tpPhoneType = "GSM";
				break;
			case TelephonyManager.PHONE_TYPE_CDMA:
				tpPhoneType = "CDMA";
				break;
			case TelephonyManager.PHONE_TYPE_NONE:
			default:
				tpPhoneType = "N/A";
				break;
			}
			tpNetworkOperatorName = telephonyMgr.getNetworkOperatorName();
			tpPhoneNumber = telephonyMgr.getLine1Number();
			tpVer = Build.VERSION.RELEASE + "-" + Build.VERSION.SDK;
			tpModel = Build.MODEL;
			
			amazonUID = "N/A";

			MedusaResourceMonitor.initialize();
			
			/* 
			 * Enable observer manager instance so that 
			 * metadata would be automatically filled the database. 
			 */
			MedusaObserverManager.getInstance();
			MedusaWatchdogChecker.getInstance().checkWatchDogStatus();
			
			/* C2DM registration */
			registerC2DM();
			
			/* Restore preferences */
			restorePrefs();
			
			G.bInit = true;
			
			MedusaUtil.log("GInfo", "IMEI: " + tpIMEI + " Model: " + tpModel 
					+ " C2DM-ID: " + C2DM_ID + " MedusaVer: " + VERSION);
		}
	}
	
	public static void registerC2DM() 
	{
        MedusaUtil.log("MedusaGInfo", "* C2DM Service Started");
	}
	
	public static void finalise()
	{
		/*
		 * Terminate storage manager last so that other managers can finish
		 * writing at terminate
		 */
		MedusaWatchdogChecker.terminate();
		MedusaWatchdogManager.terminate();
		MedusaSoundFrameManager.terminate();
		MedusaGpsManager.terminate();
		MedusaAccelManager.terminate();
		MedusaOrientationManager.terminate();
		MedusaObserverManager.terminate();
		MedusaPollManager.terminate();
		MedusaTransferManager.terminate();
		MedusaTransformManager.terminate();

		MedusaletRunner.terminateAll();

		/* should be the last */
		MedusaStorageManager.terminate();
	}
	
	/*
	 * These two functions, registerReceiver and unregisterReceiver
	 * is for the manager thread to register broadcast receiver.
	 * They provide medusalet_name based management.
	 */
	public static void registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String medusalet_name, String tag)
	{
		/* check if it is already registered */
		if (receiverTagMap.get(medusalet_name) != null) {
			Log.d(medusalet_name, "! already registered receiver. ignored.");
			return;
		}
		
		/* put it into the map */
		ArrayList<BroadcastReceiver> br_list = receiverMap.get(medusalet_name);
		if (br_list != null) {
			br_list.add(receiver);
		}
		else {
			br_list = new ArrayList<BroadcastReceiver>();
			br_list.add(receiver);
			receiverMap.put(medusalet_name, br_list);
		}
		
		receiverTagMap.put(medusalet_name, tag);
		appCtx.registerReceiver(receiver, filter);
	}
	
	public static void unregisterReceiver(String medusalet_name)
	{
		ArrayList<BroadcastReceiver> br_list = receiverMap.get(medusalet_name);
		if (br_list == null) {
			MedusaUtil.log(medusalet_name, "* nothing to unregister for " + medusalet_name);
		}
		else {
			for (BroadcastReceiver br : br_list) {
				appCtx.unregisterReceiver(br);
				MedusaUtil.log(medusalet_name, "* unregistered receiver: " + br.toString());
			}
		}
		
		receiverMap.remove(medusalet_name);
		receiverTagMap.remove(medusalet_name);
	}
	
	public static int getMedusaletId()
	{
		return medusaletCounter++;
	}
	
	public static String getUID()
	{
		if (amazonUID.equals("N/A") == true) return tpIMEI;
		else return amazonUID;
	}
	
	public static void putDirEntry(String tag, String path) {
		directoryMap.put(tag, path);
		MedusaUtil.ensureDirectoryPath(G.PATH_SDCARD + path);	/* if not exists, create it. */
	}
	
	public static String getDirectoryPathByTag(String tag)
	{
		return directoryMap.get(tag);
	}
	
	/* These functions are for overhead calculations */
	private static long baseTime;
	
	public static long setBaseTime() {
		baseTime = System.currentTimeMillis();
		return baseTime;
	}
	
	public static long getElapsedTime() {
		long now = System.currentTimeMillis();
		long diff = now - baseTime;
		baseTime = now;
		return diff;
	}
	
	//update GCM registration function here
	public static void updateRegistration(String rid)
	{
		MedusaUtil.log("MedusaGCM", "* GCM RegID:" + rid);
		MedusaUtil.syncC2DMVars(G.C2DM_ID, rid);
	}

	public static void saveToPrefs(String idpref, String valpref) {
		SharedPreferences settings = G.appCtx.getSharedPreferences(G.GPREF_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(idpref, valpref);
		editor.commit();
	}	
	
	public static void restorePrefs() {
		SharedPreferences settings = appCtx.getSharedPreferences(GPREF_NAME, 0);
		//AMT_WWID = settings.getString("AMT_WWID", AMT_WWID);
		C2DM_ID = settings.getString("C2DM_ID", C2DM_ID);
	}
		
	public static String getPrimaryEmailAccount() {
		AccountManager mgr = AccountManager.get(G.appCtx);
		Account[] accts = mgr.getAccounts();
		
		for (Account account : accts) {
			if (account != null) {
				return account.name;
			}
		}
		
		return null;
	}
}


