/**
 * 'MedusaTransformFFmpegAdapter'
 *
 * - This class is an interface to use ffmpeg application.  
 *   
 * @created : Sept. 12nd 2011
 * @modified : Dec. 2nd 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.io.File;

import android.util.Log;

public class MedusaTransformFFmpegAdapter 
{
	public static final String TAG = "MedusaTransformFFmpegAdapter";
	
	public static final String CONFIG_FLV_SUMMARY_ON_VIDEO = "flv_summary_video";
	public static final String CONFIG_FUTURE_USE = "whatever..";
	
	private static boolean bInit = false;

	public static void initialize() {
		if (bInit == false) {
			System.loadLibrary("ffmpeg");
			bInit = true;
		} else {
			// already initialized, so do nothing..
		}
	}

	/* native function interface to ffmepg command line utility */
	public native int runFFmpeg(String argument, String logpath);
	
	/* Entry function for all transformation */
	public static String execTransform(String cfg, String param)
	{
		if (cfg.equals(CONFIG_FLV_SUMMARY_ON_VIDEO) == true) {
			return makeFLVSummaryOnVideo(param);
		}
		else {
			Log.e(TAG, "! Unknown configuration: " + cfg);
			return "";
		}
	}
	
	public static String makeFLVSummaryOnVideo(String path)
	{
		/* Summary parameters. */
		int summaryDuration = 40;
		int summaryResolX = 320, summaryResolY = 240;
		int ret = 0;
		double summaryInFPS = 0.2;
		double summaryOutFPS = 2.0;
		String args = "";
		String srcFilePath = path; /* full path for the source video file. */
		String inPath = G.PATH_SDCARD + G.getDirectoryPathByTag("medusatmp");
		String smPath = G.PATH_SDCARD + G.getDirectoryPathByTag("videosummary");
		String imgName = ""; /* interim image file name. */
		String smName = ""; /* the name of the summary video file. */
		String summaryPath = "";

		File srcFile = new File(path);
		imgName = srcFile.getName().replace(".3gp", ".jpg");
		imgName = imgName.replace(".mp4", ".jpg");
		smName = imgName.replace(".jpg", ".flv");

		Log.d(TAG, "* making FLV video from [" + path + "]");

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
}



