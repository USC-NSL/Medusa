/**
 * 'MedusaObserverFileAdapter'
 *
 * - Observe the directory to catch file creation event.
 * - May add more events later.
 *
 * @modified : Dec. 14th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.io.File;

import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;

public class MedusaObserverFileAdapter extends FileObserver {

	private static final String TAG = "MedusaObservFileAdapter";
	
	String mPath;
	int mMask;

	// callback interface for a caller medusalet..
	MedusaletCBBase listner;

	public MedusaObserverFileAdapter(String path, int mask, MedusaletCBBase l) {
		// note that path and mPath should be different..-_-;;
		super(path, mask);
		mMask = mask;
		listner = l;
	}

	public void setScanPath(String path) {
		mPath = path;
	}

	@Override
	public void onEvent(int event, String path) {
		switch (event) {
		case FileObserver.CLOSE_WRITE:
			// Log.i("MedusaFileObservAdapter", "* CLOSE_WRITE event detected: " +
			// path);
			scanDirectory(null, path);
			if (listner != null) {
				String full_path = G.PATH_SDCARD + mPath + path;
				listner.callCBFunc(full_path, "* CLOSE_WRITE: " + path);
			}
			break;
		default:
			Log.d(TAG, "* unhandled code=" + event + " path=" + path);
			break;
		}
	}

	public void scanDirectory(String path) {
		scanDirectory(path, null);
	}

	public void scanDirectory(String path, String fname) {
		String lpath = path;
		if (path == null)
			lpath = mPath;

		Log.d(TAG, "* Scanning path: " + lpath);
		
		File ipath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + lpath);
		File fs[] = ipath.listFiles();

		if (fs != null) {
			for (int i = 0; i < fs.length; i++) {
				Log.v(TAG, "* entry " + i + " " + fs[i].getName());

				if (fname != null) {
					if (fname.equals(fs[i].getName()) == true) {
						MedusaStorageManager.addFileToDb(TAG, fs[i].getPath(), fs[i].length(), fs[i].lastModified());
					}
				} else {
					MedusaStorageManager.addFileToDb(TAG, fs[i].getPath(), fs[i].length(), fs[i].lastModified());
				}
			}
			Log.d(TAG, "* " + fs.length + " files found, and inserted into the metadata table");
		} else {
			Log.e(TAG, "! no such files.. path=" + path);
		}
	}

}
