/**
 * 'MedusaTransformManager'
 *
 * - This class is an implementation of transformation manager.  
 *   
 * @created  : Sept. 11th 2011
 * @modified : Dec. 1st 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/
package medusa.mobile.client;

import java.io.File;

import android.util.Log;

public class MedusaTransformManager extends MedusaManagerBase {
	
	private static String TAG = "MedusaTransformMgr";
	
	public static final String TF_TYPE_SUMMARY_VIDEO = "summary_video";
	public static final String TF_TYPE_SUMMARY_FACES = "summary_faces";
	public static final String TF_TYPE_FEATURE = "feature";
	/*
	 * Static methods for enforcing singleton pattern.
	 */
	private static MedusaTransformManager manager = null;

	public static MedusaTransformManager getInstance() {
		if (manager == null) {
			manager = new MedusaTransformManager();
			manager.init(TAG);
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
	 * Synchronous Service Request (Blocking Call)
	 * 	- MRA: 	This function is not desirable. 
	 * 			Should be changed to asynchronous call. 
	 */
	public static String requestSummaryB(String path) 
	{
		/* Summary parameters. */
		int summaryDuration = 40;
		int summaryResolX = 320, summaryResolY = 240;
		int ret = 0;
		double summaryInFPS = 0.2;
		double summaryOutFPS = 2.0;
		String args = "";
		String srcFilePath = path; /* full path for the source video file. */
		String vcapsPath = "/sdcard/vcaps/";
		String inPath = "/sdcard/vcaps/interim/"; /*
												 * the place for the interim
												 * files; extracted image
												 * frames.
												 */
		String imgName = ""; /* interim image file name. */
		String smPath = "/sdcard/vcaps/summary/"; /*
												 * the place that summary video
												 * will be created.
												 */
		String smName = ""; /* the name of the summary video file. */
		String summaryPath = "";

		/* Fill missing variables. */
		File vcapsPathFile = new File(vcapsPath);
		if (vcapsPathFile.exists() == false) {
			vcapsPathFile.mkdir();
		}
		File inPathFile = new File(inPath);
		if (inPathFile.exists() == false) {
			inPathFile.mkdir();
		}
		File smPathFile = new File(smPath);
		if (smPathFile.exists() == false) {
			smPathFile.mkdir();
		}
		File srcFile = new File(path);
		imgName = srcFile.getName().replace(".3gp", ".jpg");
		smName = imgName.replace(".jpg", ".flv");

		inPathFile = null;
		smPathFile = null;
		srcFile = null;

		Log.i(TAG, "* making summary video from [" + path + "] summary: [" + smName + "]");

		/* Directly call ffmpeg summary call */
		MedusaTransformFFmpegAdapter.initialize();
		MedusaTransformFFmpegAdapter adapter = new MedusaTransformFFmpegAdapter();

		/* Extract Image Frames from the Video */
		args = "-y -i " + srcFilePath + " -ss 00:00:01 -r " + summaryInFPS
				+ " -t " + summaryDuration + " -f image2 -s " + summaryResolX
				+ "x" + summaryResolY + " " + inPath + "%03d_" + imgName;

		Log.d(TAG, "* ffmpeg args: " + args);
		System.gc();
		ret = adapter.runFFmpeg(args, "/sdcard/ffmpeg_log1.txt");

		if (ret == 0) {
			System.gc();

			/* Make a FLV summary video */
			args = "-y -f image2 -r " + summaryOutFPS + " -i " + inPath
					+ "%03d_" + imgName + " -r " + +summaryOutFPS + " "
					+ smPath + smName;
			
			Log.d(TAG, "* ffmpeg args2: " + args);
			System.gc();
			adapter.runFFmpeg(args, "/sdcard/ffmpeg_log2.txt");

			summaryPath = smPath + smName;
		} else {
			Log.e(TAG, "! ffmpeg error, ret code= [" + ret + "]");
		}

		return summaryPath;
	}

	/*
	 * Asynchronous Service Request for Video Feature extraction
	 */
	/*
	public static void requestFeature(String aqname, String type, String path, int video_length, String uid, MedusaletCBBase listener)
	{
		String adapter, config, param;
		
		if (type.equals(TF_TYPE_FEATURE) == true) {
			adapter = MedusaTransformFFmpegAdapter2.TAG;
			config = MedusaTransformFFmpegAdapter2.CONFIG_FEATURE_ON_VIDEO;
			param = path;	
			requestService(aqname, listener, adapter, config, param, uid);
		}
		else {
			Log.e(TAG, "! Unknown summary request: " + type);
		}
	}
	*/
	/* Asynchronous Service Request */
	public static void requestSummary(String aqname, String type, String path, String uid, MedusaletCBBase listener)
	{
		String adapter, config, param;
		
		if (type.equals(TF_TYPE_SUMMARY_VIDEO) == true) {
			adapter = MedusaTransformFFmpegAdapter.TAG;
			config = MedusaTransformFFmpegAdapter.CONFIG_FLV_SUMMARY_ON_VIDEO;
			param = path;
			
			requestService(aqname, listener, adapter, config, param, uid);
		}
		else if (type.equals(TF_TYPE_SUMMARY_FACES) == true) {
			adapter = MedusaTransformImageProcAdapter.TAG;
			config = MedusaTransformImageProcAdapter.CONFIG_EXTRACT_FACES_FROM_IMAGE;
			param = path;
			
			requestService(aqname, listener, adapter, config, param, uid);
		}
		else {
			Log.e(TAG, "! Unknown summary request: " + type);
		}
	}
	
	public static void requestService(String medusaletname, MedusaletCBBase listener, String adapter, String config, String param, String uid) 
	{
		MedusaServiceE ele = new MedusaServiceE();
		StringBuilder b = new StringBuilder();

		b.append("<xml>");
			b.append("<medusaletname>"); b.append(medusaletname); b.append("</medusaletname>");
			b.append("<transform>");
				b.append("<adapter>"); b.append(adapter); b.append("</adapter>");
				b.append("<config>"); b.append(config); b.append("</config>");
				b.append("<param>"); b.append(param); b.append("</param>");
				b.append("<uid>"); b.append(uid); b.append("</uid>");
			b.append("</transform>");
		b.append("</xml>");

		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = listener;

		try {
			MedusaTransformManager.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void execute(MedusaServiceE ele) throws InterruptedException 
	{
		/* asynchronous request is not yet implemented. */
		String adapter = configMap.get("transform_adapter");
		String config = configMap.get("transform_config");
		String param = configMap.get("transform_param");
		String uid = configMap.get("transform_uid");
		String data = null, spath, msg;
		
		/* FFmpeg Adapter */
		if (adapter.equals(MedusaTransformFFmpegAdapter.TAG) == true) {
			spath = MedusaTransformFFmpegAdapter.execTransform(config, param);
			data = uid + "|" + param + "|" + spath;		/* uid|original file's path|summary file's path */
		}
		/* Image Processing Adapter */
		else if (adapter.equals(MedusaTransformImageProcAdapter.TAG) == true) {
			spath = MedusaTransformImageProcAdapter.execTransform(config, param);
			data = uid + "|" + param + "|" + spath;		/* uid|original file's path|# of faces detected */
		}
		else {
			Log.e(TAG, "! Unknown adapter: " + adapter);
		}
		
		msg = "* Transform: adapter=" + adapter + " config=" + config + " param=" + param + " result=" + data;
		Log.d(TAG, msg);
		
		if (ele.seCBListner != null) {
			ele.seCBListner.callCBFunc(data, msg);
		}
	}
}


