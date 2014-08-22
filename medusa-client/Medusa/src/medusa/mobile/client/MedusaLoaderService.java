/**
 * 'MedusaLoaderService'
 *
 * - This is the 'Loader' class for medusalet management.
 *
 * @created: May. 3rd 2011
 * @modified : Dec. 14th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import medusa.mobile.client.R;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.provider.MediaStore;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;


public class MedusaLoaderService extends Service {

	public static final String TAG = "MedusaLoaderService";
	
	public static final String CMD_START_AQUALET = "start_medusalet";
	public static final String CMD_STOP_AQUALET = "stop_medusalet";
	public static final String CMD_START_GROUP_AQUALETS = "start_medusaletgrp";
	public static final String CMD_LAUNCH_REVIEW_DIALOG = "launch_review_dialog";
	public static final String CMD_RESUME_UPLOAD = "resume_upload";
	public static final String CMD_UPDATE_REVIEW = "update_review";
	public static final String CMD_RETRY_SMSREAD = "retry_sms_reading";
	public static final String CMD_TOAST_NOTIFICATION = "toast_notification";
	public static final String CMD_SCAN_BLUETOOTH = "scan_bluetooth";
	public static final String CMD_START_VIDEO_CAPTURE = "start_video_capture";
	public static final String CMD_START_IMAGE_CAPTURE = "start_image_capture";
	
	private HashMap<String, ArrayList<Bundle>> groupTriggerMap;
	
	@Override
	public void onCreate() 
	{
		super.onCreate();

		G.initialize( getApplicationContext() );
		MedusaletRunner.initializeMedusaletRunner();
		
		Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

		/*
		 * Put the service in a foreground state, where the system considers it
		 * to be something the user is actively aware of and thus not a
		 * candidate for killing when low on memory.
		 */
		startForeground(R.string.app_name, new Notification());
		
		/* 
		 * Logging 
		 */
		Log.d(TAG, "MedusaLoaderService created.");
		Toast.makeText(this, "MedusaLoader Service Created", Toast.LENGTH_LONG).show();
		
		groupTriggerMap = new HashMap<String, ArrayList<Bundle>>();
	}

	@Override
	public void onStart(Intent intent, int start_id) 
	{
		super.onStart(intent, start_id);
		
		Bundle params = intent.getExtras();
		if (params == null) return;
		
		String md_cmd, md_name;
		String md_pid, md_qid, md_amazonid, md_instruction;
		String md_config_params, md_config_input;
		String md_review_method, md_review_options, md_preview_opt;
		String mid = null, dlimit = null;
		MedusaletBase md_inst = null;
		
		md_cmd = params.getString("cmd");
		md_name = params.getString("medusalet_name");
		mid = params.getString("mid");
		
		MedusaUtil.log(TAG, "* cmd=" + md_cmd + " name=" + md_name + " mid=" + mid);
		
		if (md_cmd == null || md_cmd.equals(CMD_START_AQUALET) == true) {
			/* create medusalet */
			String mtot = params.getString("mtot");
			if (mid != null && mtot != null) {
				ArrayList<Bundle> bl = groupTriggerMap.get(mid);
				params.putString("mid", null);
				if (bl == null) {
					bl = new ArrayList<Bundle>();
					bl.add(params);
					groupTriggerMap.put(mid, bl);
				}
				else {
					if (bl.size() < Integer.parseInt(mtot)) {
						bl.add(params);
					}
					
					if (bl.size() == Integer.parseInt(mtot)) {
						CharSequence content_title = "Group Trigger GID=[" + mid + "]";
						CharSequence content_text = params.getString("mtot") + " medusalets are pending";
						Bundle bundle = new Bundle();
						bundle.putString("cmd", CMD_START_GROUP_AQUALETS);
						bundle.putString("mid", mid);
						
						MedusaUtil.createCrowdSensingNotification(G.STATUSBAR_GROUP_TRIGGER_NID, content_title, content_text, bundle);
					}
				}
			}
			else if (md_name != null) {
				/* load medusalet */
				MedusaUtil.makeMedusaletFileReady(G.appCtx, md_name);
				md_inst = MedusaUtil.loadMedusalet(G.appCtx, md_name);
				
				/* 
				 * run the medusalet, now it will have dedicated thread 
				 * and all-related callback functions will run on the same thread.
				 * hopefully this makes many things easier.
				 */
				if (md_inst != null) {
					/* retrieve parameter values. */
					md_pid = params.getString("pid");
					md_qid = params.getString("qid");
					md_amazonid = params.getString("amtid");
					md_instruction = params.getString("inst");
					md_review_method = params.getString("review");
					md_review_options = params.getString("reviewopt");
					md_preview_opt = params.getString("preview");
					md_config_params = params.getString("config_params");
					md_config_input = params.getString("config_input");
					dlimit = params.getString("dlimit");
					
					/* set relevant parameter set for the medusalet instance. */
					if (md_pid != null) {
						md_inst.setPid(Integer.parseInt(md_pid));
						if (dlimit != null) {
							MedusaResourceMonitor.setDataLimit(md_pid, dlimit);
						}
					}
					if (md_qid != null) md_inst.setQid(Integer.parseInt(md_qid));
					if (md_review_method != null) md_inst.setReviewMethod(md_review_method);
					if (md_review_method != null) md_inst.setReviewOptions(md_review_options);
					if (md_amazonid != null) G.amazonUID = md_amazonid;
					if (md_preview_opt != null) md_inst.setPreviewOpt(md_preview_opt);
					
					/* these two should be at the end. */
					if (md_config_params != null) md_inst.setConifgParams(md_config_params);
					if (md_config_input != null) md_inst.setConfigInput(md_config_input);
					
					Log.d(TAG, "* pid=" + md_pid + " qid=" + md_qid + " AMT-ID=" + md_amazonid);
					
					/* TTS Instruction */
					if (md_instruction != null) MedusaUtil.talkByTTS(md_instruction);
					
					createMedusaletRunner(md_name, md_inst);
					MedusaUtil.syncENVVars(md_pid, md_qid, md_name);
					
					/* notification */
					Toast.makeText(this, 
							"[" + md_name + "] started w/" + 
								" pid=" + md_pid + 
								" qid=" + md_qid + 
								(md_review_method != null ? " review=" + md_review_method : "") +
								(md_amazonid != null ? " amtid=" + md_amazonid : "")
							, Toast.LENGTH_LONG).show();
				}
				else {	/* (md_inst == null) */
					Toast.makeText(this, "! invalid or missing md_name=[" + md_name + "]", Toast.LENGTH_LONG).show();
				}
			}
		}
		else if (md_cmd.equals(CMD_STOP_AQUALET) == true) {
			MedusaletRunner.exitMedusalet( params.getString("medusalet_runner_name") );
		}
		else if (md_cmd.equals(CMD_START_GROUP_AQUALETS) == true) {
			ArrayList<Bundle> bl = groupTriggerMap.get(mid);
			if (bl == null) {
				Log.e(TAG, "! no such group ID to trigger mid=" + mid);
			}
			else {
				Iterator<Bundle> itr = bl.iterator();
				while(itr.hasNext()) {
					Bundle bundle = itr.next();
					MedusaUtil.sendIntentToMedusaServiceByBundle(G.appCtx, bundle);
				}
				groupTriggerMap.remove(mid);
				
				MedusaUtil.log(TAG, "* MID=" + mid + " triggered");
			}
		}
		else if (md_cmd.equals(CMD_LAUNCH_REVIEW_DIALOG) == true) {
			Intent di = new Intent();
	        di.setClass(G.appCtx, MedusaDataReviewDialog.class);
	        di.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
	        di.putExtras(params);
	        G.appCtx.startActivity(di);
		}
		else if (md_cmd.equals(CMD_RESUME_UPLOAD) == true) {
			MedusaTransferManager.requestUploadAfterReview(params);
		}
		else if (md_cmd.equals(CMD_RETRY_SMSREAD) == true) {
			Log.d(TAG, "* retry to retrieve sms commanding msg again.");
			try {
				( new MedusaSMSReceiver() ).parseWAPPush(G.appCtx, null);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		else if (md_cmd.equals(CMD_TOAST_NOTIFICATION) == true) {
			Toast.makeText(this, params.getString("msg"), Toast.LENGTH_LONG).show();
		}
		else if (md_cmd.equals(CMD_SCAN_BLUETOOTH) == true) {
			MedusaTransferBluetoothAdapter.startScan();
		}
		else if (md_cmd.equals(CMD_START_VIDEO_CAPTURE) == true) {
			Intent itnt = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
			itnt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			G.appCtx.startActivity(itnt);
		}
		else if (md_cmd.equals(CMD_START_IMAGE_CAPTURE) == true) {
			Intent itnt = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			Time t = new Time();
			t.setToNow();
			/* 
			 * EXTRA_OUPUT field is mandatory due to the Android bug.
			 * 		- http://stackoverflow.com/questions/1910608/android-action-image-capture-intent 
			 */
			itnt.putExtra(MediaStore.EXTRA_OUTPUT
					, Uri.fromFile( new File(G.PATH_SDCARD + "/DCIM/Camera/IMG_CAPTURE_SERVICE_" + t.format("%Y%m%d_%H%M%S") + ".jpg") ));
			itnt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			G.appCtx.startActivity(itnt);
		}
		else if (md_cmd.equals(CMD_UPDATE_REVIEW) == true) {
			String path = params.getString("paths");
			String uid = params.getString("uids");
			String rcontent = params.getString("review_content");
			
			MedusaStorageManager.updateReviewColumnOnDB(path, uid, rcontent);
		}
		else {
			/* for future use, not implemented. */
			Toast.makeText(this, "WHAT?!..:( MSG=" + md_cmd, Toast.LENGTH_LONG).show();
		}
		
		System.gc();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		onStart(intent, startId);
		return START_STICKY;
	}

	@Override
	public void onDestroy() 
	{
		super.onDestroy();

		G.finalise();
		
		stopForeground(true);
		Toast.makeText(this, "MedusaLoaderService destroyed", Toast.LENGTH_LONG).show();
		
		System.gc();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	private boolean createMedusaletRunner(String medusalet_name, MedusaletBase medusalet_instance)
	{
		String proc_name;
		MedusaletRunner runner;
		boolean b_ret = true;
		int aid = G.getMedusaletId();
		
		proc_name = medusalet_name.replace(".apk", ":") + aid;
		runner = new MedusaletRunner(proc_name, medusalet_instance);
		
		medusalet_instance.setRunner(runner);
		MedusaUtil.log(TAG, "[overhead] stagetracker delay on medusalet initialization: " + G.getElapsedTime());
		runner.runMedusaletMain(medusalet_instance);
		
		/* 
		 * POST PROCESSING 
		 */
		/* 1. Stop notification entry. */
		String stop_cond = medusalet_instance.getConfigParams("-s");
		if (stop_cond != null && stop_cond.equals("notification") == true) {
			MedusaUtil.makeMedusaletExitNotification(medusalet_instance);
		}
		
		/* 2. Watchdog parameter setup. */ 
		String watchdog_timeout = medusalet_instance.getConfigParams("-w"); 
		if (watchdog_timeout != null) { 
			int to = Integer.parseInt(watchdog_timeout); 
			MedusaWatchdogManager.addStageTimeout(proc_name, to); 
			
			MedusaUtil.log(TAG, "* watchdog timeout: medusalet=[" + proc_name + "], to=" + to); 
		}
		
		/* 3. Report to the server */
		MedusaUtil.reportState("startMedusalet", "name=" + medusalet_name + " aid=" + aid, 
					medusalet_instance.getPid() + "", medusalet_instance.getQid() + "", medusalet_name);
		
		return b_ret;
	}
}


