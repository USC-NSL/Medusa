package medusa.mobile.client;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import net.semanticmetadata.lire.imageanalysis.CEDD;
import net.semanticmetadata.lire.imageanalysis.LireFeature;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.util.Log;

public class MedusaTransformFFmpegAdapter2 {
	public static final String TAG = "MedusaTransformFFmpegAdapter2";
	
	public static final String CONFIG_FEATURE_ON_VIDEO = "feature";
	public static final String CONFIG_FUTURE_USE = "whatever..";

	private static boolean bInit = false;
	private static Bitmap bmp;
	private static int target_width = 200;
	private static int target_height = 200;
	private static int FPS = 1;
	private static float Threshold = 20;
	private static String ImgName = "/sdcard/ffmpeg_temp_save.jpg";
	//private static FFmpeg adapter;
	public static void initialize() {
		if (bInit == false) {
			Log.d("==", "loading the lib");
			System.loadLibrary("ffmpegutils");
			//adapter = new FFmpeg();
			Log.d("==", "loaded the lib");
			bInit = true;
		} else {
			// already initialized, so do nothing..
		}
	}

	public  native void openFile(String path);
	public  native void drawFrame(Bitmap bitmap);
	public  native void drawFrameAt(Bitmap bitmap, int secs, int target_width, int target_height);
	/* native function interface to ffmepg command line utility */
	//public native int runFFmpeg(String argument, String logpath);
	
	/* Entry function for all transformation */
	/*
	public static String execTransform(String cfg, String path,  int video_length)
	{
		
		if (cfg.equals(CONFIG_FEATURE_ON_VIDEO) == true) {
			return FrameFeature (  path,video_length);
		}
		else {
			Log.e(TAG, "! Unknown configuration: " + cfg);
			return "";
		}
	}
	
	*/
	
	public static void Save(Bitmap bmp)
	{
		File f = new File(ImgName);
		//if (f.exists())Log.d("==", "file exist!");
		try{
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
		bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos);
		bos.flush();
		bos.close();
		}
		catch(Exception e)
		{
			Log.e("==",e.getMessage());
			e.printStackTrace();
		}
		
	}
	public static String ImgFeature(String path)
	{
		CEDD cedd = new CEDD();
		Bitmap bmp = BitmapFactory.decodeFile(path);
		cedd.extract(bmp);
		String feature = cedd.getStringRepresentation();
		return feature;
	
	}
	public static String FrameFeature ( String path, int video_length)
	{
		

		MedusaTransformFFmpegAdapter2.initialize();
		MedusaTransformFFmpegAdapter2 adapter = new MedusaTransformFFmpegAdapter2();
		//bmp = Bitmap.createBitmap(target_width, target_height, Bitmap.Config.ARGB_8888);
		Bitmap [] mBMP = new Bitmap [video_length];
		adapter.openFile(path);
		//byte[][] BT = new byte[video_length][];
		String feature = "";
		CEDD cedd1 = new CEDD();
		CEDD cedd2 = new CEDD();
		for(int i = 1; i < video_length; i += FPS)
		{
			mBMP[i] = Bitmap.createBitmap(target_width, target_height, Bitmap.Config.ARGB_8888);
			adapter.drawFrameAt(mBMP[i], i, target_width, target_height);
			if (i == 1)
			{
				Save(mBMP[i]);
				mBMP[i] = BitmapFactory.decodeFile(ImgName);
				cedd1.extract(mBMP[i]);
				feature =Integer.toString(i) +" " +cedd1.getStringRepresentation() ;
			}
			else
			{
				//cedd2 = new CEDD();
				Save(mBMP[i]);
				mBMP[i] = BitmapFactory.decodeFile(ImgName);
				if(mBMP[i] == null){Log.d("==", Integer.toString(i)+" bitmap null");}
				cedd2.extract(mBMP[i]);
				if(cedd1.getDistance((LireFeature)cedd2) > Threshold)
				{
				//	cedd1 = new CEDD();
					cedd1.extract(mBMP[i]);
					feature += "|" +Integer.toString(i) +" " + cedd1.getStringRepresentation();
				}
			}
			
	//		CEDD cedd = new CEDD();
	//		cedd.extract(bmp);
	//		BT[i] = cedd.getByteArrayRepresentation();
			
		}
		
		return feature;
		
	}

}
