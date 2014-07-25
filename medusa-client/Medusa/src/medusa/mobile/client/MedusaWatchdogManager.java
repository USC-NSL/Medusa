/**
 * 'MedusaWatchdogManager'
 *
 * - Watchdog implementation.  
 *   
 * @modified : Dec. 14th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.util.HashMap;
import android.util.Log;

public class MedusaWatchdogManager extends MedusaManagerBase {

	public static int watchdogThreshold = 100000;	/* 10 seconds */
	
	private static String TAG = "MedusaWatchdogManager";
	private static MedusaWatchdogManager manager = null;
	
	HashMap<String, MedusaManagerBase> watchPool;
	HashMap<String, String> watchStatus;
	HashMap<String, Long> watchTimestamp;
	
	HashMap<String, Integer> perStageTimeoutMap;

	public static MedusaWatchdogManager getInstance() {
		if (manager == null) {
			manager = new MedusaWatchdogManager();
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
	
	@Override
	public void init(String name) {
		super.init(name);
		watchPool = new HashMap<String, MedusaManagerBase>();
		watchStatus = new HashMap<String, String>();
		watchTimestamp = new HashMap<String, Long>();
		
		/* per stage timeout map */
		perStageTimeoutMap = new HashMap<String, Integer>();
	}
	
	public static void addStageTimeout(String name, int timeout) {
		getInstance().perStageTimeoutMap.put(name, timeout);
		Log.d(TAG, "* [Adding] stage timeout: name=" + name + ", timout=" + timeout);
	}

	public static void pingStart(String thread_name, MedusaManagerBase thread_runner) {
		MedusaWatchdogManager wd = MedusaWatchdogManager.getInstance(); 
		MedusaServiceE ele = new MedusaServiceE();
		StringBuilder b = new StringBuilder();
		
		b.append("<xml>");
			b.append("<name>"); b.append(thread_name); b.append("</name>");
			b.append("<cmd>start</cmd>");
		b.append("</xml>");

		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = null;

		try {
			wd.register(thread_name, thread_runner);
			wd.requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void pingStop(String thread_name) {
		MedusaServiceE ele = new MedusaServiceE();
		StringBuilder b = new StringBuilder();
		
		b.append("<xml>");
			b.append("<name>"); b.append(thread_name); b.append("</name>");
			b.append("<cmd>stop</cmd>");
		b.append("</xml>");

		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = null;

		try {
			MedusaWatchdogManager.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void checkStatus() {
		MedusaServiceE ele = new MedusaServiceE();
		StringBuilder b = new StringBuilder();
		
		b.append("<xml>");
			b.append("<name>not-used</name>");
			b.append("<cmd>check</cmd>");
		b.append("</xml>");

		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = null;

		try {
			MedusaWatchdogManager.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void register(String thread_name, MedusaManagerBase thread_runner) {
		watchPool.put(thread_name, thread_runner);
	}
	
	private void checkWatchPool() {
		//Runtime rt = Runtime.getRuntime();
		//MedusaUtil.log(TAG, "* Mem: " + rt.freeMemory() + "/" + rt.maxMemory());
		
		for (String name: watchStatus.keySet()) {
			MedusaManagerBase runner = watchPool.get(name);
			long stime = watchTimestamp.get(name);
			long now = System.currentTimeMillis();
			int limit = watchdogThreshold; /* 10 sec */
			
			String chkname = name.replace("_run", "").replace("_cb", "");
			if (perStageTimeoutMap.get(chkname) != null) {
				limit = perStageTimeoutMap.get(chkname);
				Log.v(TAG, "* name=" + chkname + ", limit=" + limit);
			}
			/*else {
				Log.v(TAG, "! No such entry: name=" + chkname);
			}*/
			
			if (now - stime > limit * 1000) {
				MedusaUtil.log(TAG, "* Watchdog activated to kill [" + runner.getName() + "]");
				runner.forceKillThread();
				MedusaUtil.log(TAG, "* Watchdog killed an medusalet [" + runner.getName() + "]");
				removeWatchEntry(name);
				break;
			}
		}
	}
	
	private void removeWatchEntry(String name) {
		watchPool.remove(name);
		watchStatus.remove(name);
		watchTimestamp.remove(name);
	}

	@Override
	protected void execute(MedusaServiceE ele) throws InterruptedException {
		String name = configMap.get("name");
		String cmd = configMap.get("cmd");
		
		if (cmd.equals("start") == true) {
			if (watchPool.get(name) != null) {
				watchStatus.put(name, cmd);
				watchTimestamp.put(name, System.currentTimeMillis());
			}
		}
		else if (cmd.equals("stop") == true) {
			if (watchPool.get(name) != null && watchStatus.get(name) != null) {
				removeWatchEntry(name);
			}
		}
		else if (cmd.equals("check") == true) {
			checkWatchPool();
		}
	}
}



