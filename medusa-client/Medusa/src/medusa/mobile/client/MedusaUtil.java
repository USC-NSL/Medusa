/**
 * 'MedusaUtil'
 *
 * - Global utility functions.  
 *   
 * @modified : Dec. 14th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.text.format.Time;
import android.util.Base64;
import android.util.Log;
import dalvik.system.DexClassLoader;

public class MedusaUtil 
{
	public static String TAG = "MedusaUtil";
	
	public static void invokeMedusalet(String in_msg)
	{
		HashMap<String, String> sms_xml_map = new HashMap<String, String>();
		int start_idx, end_idx;
		
		Log.d(TAG, "* Received XML: " + in_msg);
		
		start_idx = in_msg.indexOf("<xml>");
		end_idx = in_msg.lastIndexOf("</xml>");
		
		if (start_idx == -1 || end_idx == -1) {
			/* 
			 * This part is hack. The code implements a failover mechanism 
			 * for T-Mobile client. Suspect broadcast message prioritization problem.
			 */
			CharSequence content_title = "Commanding Error";
			CharSequence content_text = "Press this to retry";
			Bundle bd = new Bundle();
			bd.putString("cmd", MedusaLoaderService.CMD_RETRY_SMSREAD);
			bd.putString("medusalet_name", "unknown");
							
			MedusaUtil.createCrowdSensingNotification( G.STATUSBAR_RETRY_AQUALET_NID
													, content_title, content_text, bd );
		}
		else {
			/* parse xml */
			String xml_msg = in_msg.substring(start_idx, end_idx) + "</xml>";
			String medusalet_name = null, trigger = null;
			String mstr = null;

			if (MedusaUtil.parseXMLMessage(xml_msg, sms_xml_map) == true) {
				medusalet_name = sms_xml_map.get("stage_binary");
				trigger = sms_xml_map.get("stage_trigger");
				mstr = sms_xml_map.get("multi");
				
				/* logging and reporting */
				MedusaUtil.log(TAG, "[overhead] SMS interpretation latency: " + G.getElapsedTime());
				MedusaUtil.reportState("smsReceived", "Received", sms_xml_map.get("pid"), sms_xml_map.get("qid"), medusalet_name);
			}
			else {
				Log.e(TAG, "! parsing failed: XML message format may contain error(s).");
			}
			
			if (mstr != null) {
				/* group trigger */
				Bundle bundle = MedusaUtil.getStartMedusaletBundleByXML(xml_msg); 
				bundle.putString("mid", mstr.split("\\|")[0]); 
				bundle.putString("midx", mstr.split("\\|")[1].split("/")[0]);
				bundle.putString("mtot", mstr.split("\\|")[1].split("/")[1]);
				
				MedusaUtil.log(TAG, "* mid=" + mstr.split("\\|")[0] 
				                + " midx=" + mstr.split("\\|")[1].split("/")[0]
				                + " mtot=" + mstr.split("\\|")[1].split("/")[1]);
				MedusaUtil.sendIntentToMedusaServiceByBundle(G.appCtx, bundle);
			}
			else if (trigger == null || 
					trigger.equals("none") == true || 
					trigger.equals("immediate") == true) {
				/* start medusalet */
				MedusaUtil.startMedusaletByXML(G.appCtx, xml_msg);
			}
			else if (trigger.equals("custom") == true ||	/* deprecated */
					 trigger.equals("user-initiated") == true) {
				CharSequence content_title = "Pending Medusalet [" + medusalet_name + "]";
				CharSequence content_text = "Press this when you are ready to start";
								
				MedusaUtil.createCrowdSensingNotification( G.STATUSBAR_START_AQUALET_NID
									, content_title, content_text, MedusaUtil.getStartMedusaletBundleByXML(xml_msg) );
			}
			else if (trigger.startsWith("location")) {
				String[] words = trigger.split(","); 		/* words[0] would be location-related.. */
				String[] keyval = words[0].split("=");
				String[] tmp_set = keyval[1].split("\\|");
				if (tmp_set.length != 3) {
					/* latitude|longitude|accuracy */
					Log.e(TAG, "! Unknown triggering command: " + trigger);
					return;
				}
				
				try {
					double lat = Double.parseDouble(tmp_set[0]);
					double lng = Double.parseDouble(tmp_set[1]);
					double dis = Double.parseDouble(tmp_set[2]);
					
					if  (lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) {
						String[] strs = trigger.trim().split(",");
						String loc_part = strs[0] + ",";
						String new_trigger = trigger.replace(loc_part, "").trim();
						if (strs.length == 1) {
							/* no secondary trigger, hence none */
							MedusaGpsManager.requestLocationTrigger(lat, lng, dis, xml_msg.replace(trigger, "none"));
						}
						else {
							MedusaGpsManager.requestLocationTrigger(lat, lng, dis, xml_msg.replace(trigger, new_trigger));
						}
						MedusaUtil.log(TAG, "* [Trigger] prev_trigger=" + trigger + ", new_trigger=" + new_trigger);
					} else {
						Log.e(TAG, "! Incorrect values of latitude (-90~90) or longitude (-180~180): " + trigger);
						return;
					}
				} 
				catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG, "! Wrong format in location trigger: " + trigger);
				}
				
			}
			else {
				Log.e(TAG, "! Unknown triggering command: " + trigger);
			}
		}
		
		sms_xml_map = null;
	}
	
	/*
	 * start medusalet using the simplest parameter: medusalet_name only.
	 */
	public static void startMedusaletByName(Context ctx, String medusalet_name)
	{
		Bundle bundle = new Bundle();
		String aqname = medusalet_name;
	
		if (aqname.indexOf(".apk") == -1) aqname = aqname.concat(".apk");
		bundle.putString("cmd", MedusaLoaderService.CMD_START_AQUALET);
		bundle.putString("medusalet_name", aqname);
		
		MedusaUtil.sendIntentToMedusaServiceByBundle(ctx, bundle);
	}
	
	/* kill a medusalet, given pid ... */
	public static void exitMedusalet(String pid)
	{
		for (Entry<String, MedusaletRunner> cursor : MedusaletRunner.medusaletRunnerMap.entrySet()) 
		{
			if (cursor.getValue().getMedusaletInstance().getPid() == Integer.parseInt(pid))
			{
				cursor.getValue().getMedusaletInstance().quitThisMedusalet();
				MedusaTransferManager.getInstance().deleteServicesOfPid(pid);
				Log.i(TAG, "* Medusa: Killing Medusalet of pid=" + pid);
				return;
			}
		}	
	}
	
	/*
	 * start medusalet using xml configuration information 
	 * in the crowd-sensing framework.
	 */
	public static Bundle getStartMedusaletBundleByXML(String xml_msg)
	{
		Bundle bundle = new Bundle();
		HashMap<String, String> xml_map = new HashMap<String, String>();
		
		bundle.putString("cmd", MedusaLoaderService.CMD_START_AQUALET);
		
		if (MedusaUtil.parseXMLMessage(xml_msg, xml_map) == true) {
			String aqname = xml_map.get("stage_binary");
			if (aqname == null) return null;
			if (aqname.indexOf(".apk") == -1) aqname = aqname.concat(".apk");
			bundle.putString("medusalet_name", aqname);
			bundle.putString("pid", xml_map.get("pid"));
			bundle.putString("qid", xml_map.get("qid"));
			bundle.putString("amtid", xml_map.get("amtid"));
			bundle.putString("dlimit", xml_map.get("dlimit"));
			bundle.putString("inst", xml_map.get("stage_inst"));
			bundle.putString("review", xml_map.get("stage_review"));
			bundle.putString("reviewopt", xml_map.get("stage_reviewopt"));
			bundle.putString("preview", xml_map.get("stage_preview"));
			bundle.putString("config_params", xml_map.get("stage_config_params"));
			bundle.putString("config_input", xml_map.get("stage_config_input"));
			bundle.putString("config_output", xml_map.get("stage_config_output"));
		}
		
		xml_map = null;
		
		return bundle;
	}

	public static void startMedusaletByXML(Context ctx, String xml_msg)
	{
		MedusaUtil.sendIntentToMedusaServiceByBundle( ctx, MedusaUtil.getStartMedusaletBundleByXML(xml_msg) );
	}
	
	/*
	 * start medusalet using a bundle object.
	 */
	public static void sendIntentToMedusaServiceByBundle(Context ctx, Bundle params)
	{
		if (params != null) {
			Intent service = new Intent(G.AQUA_SERVICE_NAME);
			service.putExtras(params);
			
			if (ctx == null) G.appCtx.startService(service);
			else ctx.startService(service);
		}
	}
	
	public static void stopMedusaLoaderService()
	{
		Intent intent = new Intent();
		intent.setAction(G.AQUA_SERVICE_NAME);
		G.appCtx.stopService(intent);
	}
	
	/*
	 * copy the apk file in asset directory to sdcard. this is just for our
	 * convenience for now.
	 */
	public static boolean copyFileFromAssetToSdcard(Context ctx, String filename) 
	{
		boolean cpResult = false;

		try {
			InputStream in = ctx.getAssets().open(filename);
			FileOutputStream out = new FileOutputStream("/sdcard/" + filename);
			int b;
			while ((b = in.read()) != -1)
				out.write(b);
			in.close();
			out.close();
			cpResult = true;
		} 
		catch (Exception e) {
			Log.e(TAG, "! Error copying asset file to sdcard: " + filename);
		}
		
		return cpResult;
	}

	public static boolean makeMedusaletFileReady(Context ctx, String filename) 
	{
		boolean cpResult = false;
		
		try {
			/* check if it is already in the internal storage. */
			File fp = new File(ctx.getFilesDir().getAbsolutePath(), filename);
			
			if (fp.exists() == false) {	
				InputStream in = ctx.getAssets().open(filename);
				FileOutputStream out = ctx.openFileOutput(filename, Context.MODE_PRIVATE);
				int b;
				while ((b = in.read()) != -1) out.write(b);
				in.close();
				out.close();
			}
		
			fp = null;
			cpResult = true;
		} 
		catch (Exception e) {
			MedusaUtil.log(TAG, "* Medusalet [" + filename + "] is not on the device. Should download from Stage Library.");
			/* MRA: download code should be placed here. */
		}
		
		return cpResult;
	}
	
	/*
	 * Instantiate the medusalet class and launch it.
	 */
	@SuppressLint("NewApi")
	public static MedusaletBase loadMedusalet(Context ctx, String medusalet) 
	{
		Pattern p = Pattern.compile("medusalet_(\\w+)_(\\d*)\\.apk");
		Matcher m = p.matcher(medusalet);
		String appname = null;
		int ver = -1;

		if (m.find()) {
			appname = m.group(1);
			ver = Integer.parseInt(m.group(2));
			Log.i(TAG, "* Loading Medusalet " + medusalet + " - " + appname
					+ ", ver " + ver);
		} 
		else {
			p = Pattern.compile("medusalet_(\\w+)\\.apk");
			m = p.matcher(medusalet);
			if (m.find()) {
				appname = m.group(1);
				Log.i(TAG, "* Medusa: Loading Medusalet " + medusalet + " - "
						+ appname + ", no version # ");
			} else {
				Log.e(TAG, "! Error, cannot parse medusalet filename.");
				return null;
			}
		}

		try {
			String medusalet_path = ctx.getFilesDir().getAbsolutePath() + "/" + medusalet;
			DexClassLoader dcl = new DexClassLoader(medusalet_path,
													ctx.getFilesDir().getAbsolutePath(), null,
													ctx.getClassLoader());
			MedusaletBase medusalet_object = (MedusaletBase) dcl.loadClass(
					"medusa.medusalet." + appname + ".MedusaletMain").newInstance();
			
			return medusalet_object;
		} 
		catch (Exception e) {
			Log.e(TAG, "! Error(s) in running medusalet. " + e.getClass().getName());
		}
		
		return null;
	}
	
	/*
	 * simplest form of xml parsing function. 
	 */
	public static boolean parseXMLMessage(String msg, HashMap<String, String> strMap) 
	{
		boolean bRet = true;

		// parse xml document.
		XmlPullParserFactory factory;
		XmlPullParser parser;
		
		try {
			factory = XmlPullParserFactory.newInstance();
			parser = factory.newPullParser();
			Stack eleStack = new Stack();
			int parserEvent;
	
			parser.setInput(new StringReader(msg));
			parserEvent = parser.getEventType();
	
			while (parserEvent != XmlPullParser.END_DOCUMENT) {
				switch (parserEvent) {
				case XmlPullParser.START_TAG:
					String newtag = parser.getName();
					if (newtag.compareTo("xml") != 0) {
						eleStack.push(newtag);
					}
					break;
				case XmlPullParser.END_TAG:
					if (parser.getName().compareTo("xml") != 0) {
						eleStack.pop();
					}
					break;
				case XmlPullParser.TEXT:
					String tagkey = "";
					for (int i = 0; i < eleStack.size(); i++) {
						tagkey += eleStack.elementAt(i);
						if (i < eleStack.size() - 1)
							tagkey += "_";
					}
					strMap.put(tagkey, parser.getText());
					break;
				default:
					break;
				}
	
				parserEvent = parser.next();
			}
			
			eleStack = null;
			parser = null;
			factory = null;
		} 
		catch (Exception e) {
			e.printStackTrace();
			bRet = false;
		}
		
		return bRet;
	}
	
	public static void createCrowdSensingNotification(int nid, CharSequence content_title, CharSequence content_text, Bundle extras)
	{
		/* send pending medusalet run to the status bar notification */
		NotificationManager mNotificationManager = (NotificationManager)G.appCtx.
													getSystemService(Context.NOTIFICATION_SERVICE);
		int icon = R.drawable.icon;
		CharSequence ticker_text = "Pending Crowd-Sensing Task..";
		long when = System.currentTimeMillis();

		Intent serviceIntent = new Intent(G.AQUA_SERVICE_NAME);
		//serviceIntent.addCategory(""+nid);
		serviceIntent.putExtras( extras );
		PendingIntent content_intent = PendingIntent.getService(G.appCtx, nid, serviceIntent, PendingIntent.FLAG_ONE_SHOT);

		// Instantiate the Notification
		Notification nt = new Notification(icon, ticker_text, when);
		nt.flags |= Notification.DEFAULT_VIBRATE;
		nt.flags |= Notification.FLAG_AUTO_CANCEL;	//nt.flags |= Notification.FLAG_INSISTENT;
		nt.setLatestEventInfo(G.appCtx, content_title, content_text, content_intent);
		
		// Pass the Notification to the NotificationManager
		mNotificationManager.notify(nid, nt);
		
		Log.i("MedusaNotification", "* Notify nid=" + nid + " title=" + content_title + " text=" + content_text);
		Log.i("MedusaNotification", "* PendingIntent hashId=" + content_intent.hashCode());
	}
	
	/*
	 * Toast notification interface.
	 */
	public static void toastNotifiationViaMedusaService(Context ctx, String msg)
	{
		Bundle bundle = new Bundle();
		bundle.putString("cmd", MedusaLoaderService.CMD_TOAST_NOTIFICATION);
		bundle.putString("medusalet_name", "N/A");
		bundle.putString("msg", msg);
		
		Intent service = new Intent(G.AQUA_SERVICE_NAME);
		service.putExtras(bundle);
		
		if (ctx == null) G.appCtx.startService(service);
		else ctx.startService(service);
	}
	
	/*
	 * Send log to debug console activity.
	 */
	public static void log(String tag, String msg)
	{
		/* temporarily blocked */
		if (msg.contains("[overhead]") == true) return;
		
		Bundle bundle = new Bundle();
		bundle.putString("tag", tag);
		bundle.putString("msg", msg);
		
		Intent logIntent = new Intent(G.AQUA_DEBUG_LOG);
		logIntent.putExtras(bundle);
		
		G.appCtx.sendBroadcast(logIntent);
		Log.d(tag, msg);
	}
	
	/*
	 * Send state information to the server.
	 */
	public static void reportState(String action, String msg, String pid, String qid, String aqname)
	{
		String uri = G.URIBASE_REPORT + "?action=" + action + "&custom=" + MedusaUtil.base64Encode(msg) 
							+ "&pid=" + pid + "&qid=" + qid + "&qtype=" + aqname;
		
		MedusaTransferManager.requestHttpReq("MedusaStateReport", "GET", uri, null);
		MedusaUtil.log(TAG, "[overhead] reportState delay: " + G.getElapsedTime() + ", action=" + action);
	}
	
	/*
	 * ENV var sync.
	 */
	public static void syncENVVars(String pid, String qid, String aqname)
	{
		String uri = G.URIBASE_REPORT + "?action=syncENV&pid=" + pid + "&qid=" + qid + "&qtype=" + aqname
					+ "&wrid=" + MedusaUtil.base64Encode(G.AMT_WRID) 
					+ "&wrkey=" + MedusaUtil.base64Encode(G.AMT_WRKEY) 
					+ "&wwid=" + MedusaUtil.base64Encode(G.amazonUID);
		
		MedusaTransferManager.requestHttpReq("MedusaStateReport", "GET", uri, null);
		
		MedusaUtil.log(TAG, "* sent AMT credential to Medusa server.");
	}
	
	public static void syncC2DMVars(String key, String val)
	{
		String uri = G.URIBASE_REPORT + "?action=syncC2DM&pid=c2dm&qid=c2dm&qtype=c2dm&wid=" 
					+ key + "&regid=" + MedusaUtil.base64Encode(val);
	
    	MedusaTransferManager.requestHttpReq("MedusaStateReport", "GET", uri, null);
	
	    MedusaUtil.log(TAG, "* sent C2DM credential to Medusa server.");
	}
	
	/*
	 * Request General User Input.
	 */
	public static void requestUserInput(MedusaletBase medusalet, String tid, String msg)
	{
		Bundle extras = new Bundle();
		extras.putString("cmd", MedusaLoaderService.CMD_LAUNCH_REVIEW_DIALOG);
		extras.putString("medusalet_name", medusalet.getName());
		extras.putString("tid", "" + tid);
		extras.putString("pid", "" + medusalet.getPid());
		extras.putString("qid", "" + medusalet.getQid());
		extras.putString("type", "userinput");
		extras.putString("msg", msg);
		
		MedusaUtil.createCrowdSensingNotification((int)(G.STATUSBAR_REVIEW_NID_BASE + G.getMedusaletId())
										, "Waiting for your input", msg, extras);
	}

	/*
	 * Data Review Dialog
	 */
	public static void requestReviewForRawData(MedusaletBase medusalet, String paths, String uids)
	{
		String rtype = medusalet.getReviewMethod();
		
		if (rtype.equals(MedusaletBase.REVIEW_YESNO) == true ||
				rtype.equals(MedusaletBase.REVIEW_LABELING) == true ||
				rtype.equals(MedusaletBase.REVIEW_TEXTDESC) == true) {
			
			Bundle extras = new Bundle();
			extras.putString("cmd", MedusaLoaderService.CMD_LAUNCH_REVIEW_DIALOG);
			extras.putString("medusalet_name", medusalet.getName());
			extras.putString("type", "review");
			extras.putString("pid", "" + medusalet.getPid());
			extras.putString("qid", "" + medusalet.getQid());
			extras.putString("review_type", rtype);
			extras.putString("review_options", medusalet.getReviewOptions());
			extras.putString("paths", paths);
			extras.putString("uids", uids);
			
			MedusaUtil.createCrowdSensingNotification((int)(G.STATUSBAR_REVIEW_NID_BASE + G.getMedusaletId())
								, "Review request", "New file(s) [" + paths + "] have been created", extras);
		}
		else if (rtype.equals(MedusaletBase.REVIEW_NONE) == true) {
			// skipping.
			;
		}
		else {
			Log.e(TAG, "! unknown review type: " + medusalet.getReviewMethod());
		}
	}

	/*
	 * Preview Dialog
	 */
	public static Bundle requestPreviewForUpload(MedusaletBase medusalet, String paths, String uids, String reviews, boolean useNotif)
	{
		Bundle extras = new Bundle();
		extras.putString("cmd", MedusaLoaderService.CMD_LAUNCH_REVIEW_DIALOG);
		extras.putString("medusalet_name", medusalet.getName());
		extras.putString("type", "preview");
		extras.putString("pid", "" + medusalet.getPid());
		extras.putString("qid", "" + medusalet.getQid());
		extras.putString("review_type", MedusaletBase.REVIEW_YESNO);
		extras.putString("review_options", medusalet.getReviewOptions());
		extras.putString("paths", paths);
		extras.putString("uids", uids);
		extras.putString("tagged_reviews", reviews);
		
		if (useNotif == true) {
			MedusaUtil.createCrowdSensingNotification((int)(G.STATUSBAR_REVIEW_NID_BASE + G.getMedusaletId())
							, "Preview on outgoing data", "the data to be uploaded are waiting to preview", extras);
		}
		
		return extras;
	}
	
	public static String base64Encode(String msg)
	{
		return Base64.encodeToString(msg.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
	}
	
	public static String base64Decode(String msg)
	{
		return new String(Base64.decode(msg, Base64.DEFAULT));
	}
	
	/*
	 * call custom commands to the MedusaLoaderService..
	 */
	public static void passCommandToService(Context ctx, String cmd, String args)
	{
		Bundle bundle = new Bundle();
		
		bundle.putString("cmd", cmd);
		bundle.putString("args", args);
		
		MedusaUtil.sendIntentToMedusaServiceByBundle(ctx, bundle);
	}
	
	/*
	 * ensureDirectoryPath()
	 * 		- this function will see if the path exists. 
	 * 		  if not creates it.
	 */
	public static boolean ensureDirectoryPath(String path)
	{
		File pathFile = new File(path);
		if (pathFile.exists() == false) {
			pathFile.mkdir();
		}
		
		return true;
	}
	
	/* Make ease of sensing jobs: e.g.) medusalet_mediagen */
	public static void invokeSensingApp(String type) {
		if (type.equals("video") == true || type.equals("audio") == true) {
			/* invoke camera app. */
			MedusaUtil.passCommandToService(G.appCtx, MedusaLoaderService.CMD_START_VIDEO_CAPTURE, "N/A"); 
		}
		else if (type.equals("media") == true || type.equals("image") == true 
				|| type.equals("camera") == true) {
			/* voice recorder app. (if exists) */
			MedusaUtil.passCommandToService(G.appCtx, MedusaLoaderService.CMD_START_IMAGE_CAPTURE, "N/A");
		}
		else {
			Log.e(TAG, "! No App for the type: " + type);
		}
	}
	
	public static void makeMedusaletExitNotification(MedusaletBase medusalet) {
		/* runner's name has a format like [medusalet_name]:[nid] */
		String proc_name = medusalet.getRunner().getName();
		
		if (proc_name != null) {
			String medusalet_name = proc_name.split(":")[0];	
			int nid = G.STATUSBAR_STOP_AQUALET_NID_BASE + Integer.parseInt( proc_name.split(":")[1] ); 
				
			CharSequence content_title = "Done for Medusalet [" + medusalet_name + "]";
			CharSequence content_text = "Press when you've finished the job";
			
			Bundle bundle = new Bundle();
			bundle.putString("cmd", MedusaLoaderService.CMD_STOP_AQUALET);
			bundle.putString("medusalet_name", medusalet_name);
			bundle.putString("nid", nid + "");
			bundle.putString("medusalet_runner_name", proc_name);
							
			MedusaUtil.createCrowdSensingNotification( nid, content_title, content_text, bundle );
		}
		else {
			Log.e(TAG, "! Wrong notification request.");
		}
	}
	
	public static long convertStrTimeToLong(String time_str) {
		/* input time string should have 
		 * 	- RFC 2445 DATETIM type: "%Y%m%dT%H%M%S"
		 */
		Time time = new Time();
		time.parse(time_str);
		return time.normalize(false);
	}
	
	public static String getRootPath() {
		return Environment.getExternalStorageDirectory().getAbsolutePath();
	}

	public static void talkByTTS(String speech) {
        //int ret = G.tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
		AudioManager audio_mgr = (AudioManager) G.appCtx.getSystemService(Context.AUDIO_SERVICE);
		//int cur_volume = audio_mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
		audio_mgr.setStreamVolume(AudioManager.STREAM_MUSIC
								, audio_mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
		
		int ret = G.tts.speak(speech, TextToSpeech.QUEUE_ADD, null);
		MedusaUtil.log("TTS", "* TTS Content=" + speech + ", RetVal=" + ret);
	}
}


