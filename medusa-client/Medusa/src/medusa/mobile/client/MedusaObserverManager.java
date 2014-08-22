/**
 * 'MedusaObserverManager'
 *
 * - This class implements MedusaObserverManager.
 * - Its super class is MedusaServiceManager. 
 * - Primary function is to get file system event.
 * - Support multiple locations. 
 *   
 * 		
 * @created : Feb. 10th 2011
 * @modified : Dec. 8th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu), Bin Liu(binliu@usc.edu)
 **/

package medusa.mobile.client;

import java.util.HashMap;
import android.os.FileObserver;
import android.util.Log;

public class MedusaObserverManager extends MedusaManagerBase {

	private static final String TAG = "MedusaObservMgr";
	/*
	 * Static methods for enforcing singleton pattern.
	 */
	private static MedusaObserverManager manager = null;

	public static MedusaObserverManager getInstance() {
		if (manager == null) {
			manager = new MedusaObserverManager();
			manager.init("MedusaObserverManager");
		}

		return manager;
	}

	public static void terminate() {
		if (manager != null) {
			manager.markExit();
			manager = null;
		}
	}
	
	public static void requestObservByTag(String tag, String type, MedusaletCBBase listner) {
		MedusaObserverManager.requestService(tag, listner, type, "start");
	}
	
	public static void stopObservByTag(String tag, String type) {
		MedusaObserverManager.requestService(tag, null, type, "stop");
	}

	public static void requestService(String tag, MedusaletCBBase listener, String type, String action) {
		MedusaServiceE ele = new MedusaServiceE();
		StringBuilder b = new StringBuilder();

		b.append("<xml>");
			b.append("<tag>"); b.append(tag); b.append("</tag>");
			b.append("<observer>");
				b.append("<type>");	b.append(type);	b.append("</type>");
				b.append("<action>"); b.append(action);	b.append("</action>");
			b.append("</observer>");
		b.append("</xml>");

		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = listener;

		try {
			MedusaObserverManager.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	HashMap<String, FileObserver> observMap;
	
	@Override
	public void init(String name) {
		super.init(name);
		observMap = new HashMap<String, FileObserver>();

		// by default, we will see camera directory, etc.
		MedusaObserverManager.requestService("internal", null, "camera", "start");
		MedusaObserverManager.requestService("internal", null, "bin", "start");
		MedusaObserverManager.requestService("internal", null, "netscan", "start");
		MedusaObserverManager.requestService("internal", null, "videosummary", "start");
		MedusaObserverManager.requestService("internal", null, "facedetect", "start");
		MedusaObserverManager.requestService("internal", null, "genmetadata", "start"); //Xing
	}

	@Override
	protected void execute(MedusaServiceE ele) 
	{
		// TODO Auto-generated method stub
		String tag = configMap.get("tag");
		String type = configMap.get("observer_type");
		String action = configMap.get("observer_action");
		String key = type + "_" + tag;

		if (action.compareTo("start") == 0 && observMap.get(type) == null) {
			String scanpath = G.getDirectoryPathByTag(type);
			String fpath = G.PATH_SDCARD + G.getDirectoryPathByTag(type);
			MedusaObserverFileAdapter adapter = new MedusaObserverFileAdapter(fpath, 
					FileObserver.CREATE | FileObserver.CLOSE_WRITE, ele.seCBListner);

			adapter.setScanPath(scanpath);
			adapter.scanDirectory(null);
			adapter.startWatching();
			observMap.put(key, adapter);

			Log.d(TAG, "* startWatching, key=" + type	+ " path=" + fpath);
		} else if (action.compareTo("stop") == 0 && observMap.get(type) != null) {
			FileObserver observ = observMap.get(key);

			if (observ == null) {
				Log.e(TAG, "! no such object, so cannot be stopped. type=" + type);
			} else {
				observ.stopWatching();

				Log.d(TAG, "* stopWatching, type=" + type);
			}
		} else {
			Log.e(TAG, "! observMgr cannot find what to do action=" + action + ", type=" + type);
		}

		//if (ele.seCBListner != null) {
		//	ele.seCBListner.callCBFunc(null, "* REGISTERTED");
		//}
	}
}
