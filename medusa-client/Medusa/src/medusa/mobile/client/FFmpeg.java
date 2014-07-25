package medusa.mobile.client;

import android.graphics.Bitmap;

public class FFmpeg {

    static {
    	System.loadLibrary("ffmpegutils");
    }
	public  native void openFile(String path);
	public  native void drawFrame(Bitmap bitmap);
	public  native void drawFrameAt(Bitmap bitmap, int secs, int target_width, int target_height);
	
}
