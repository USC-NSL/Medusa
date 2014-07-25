/**
 * 'MedusaWatchdogChecker'
 *
 * - Polls MedusaWatchdogManager every [watchdogPollInterval] seconds.  
 *   
 * @modified : Dec. 14th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import android.util.Log;

public class MedusaWatchdogChecker extends MedusaManagerBase {
	
	public static long watchdogPollInterval = 3;	/* 3 seconds */

	private static String TAG = "MedusaWatchdogChecker";
	private static MedusaWatchdogChecker manager = null;
	
	public static MedusaWatchdogChecker getInstance() {
		if (manager == null) {
			manager = new MedusaWatchdogChecker();
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
	
	public static void checkWatchDogStatus()
	{
		requestService("watchdog", "check");
	}
	
	public static void requestService(String type, String cmd) 
	{
		MedusaServiceE ele = new MedusaServiceE();
		StringBuilder b = new StringBuilder();
		
		b.append("<xml>");
			b.append("<type>"); b.append(type); b.append("</type>");
			b.append("<cmd>"); b.append(cmd); b.append("</cmd>");
		b.append("</xml>");

		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = null;

		try {
			MedusaWatchdogChecker.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void execute(MedusaServiceE ele) throws InterruptedException {
		// TODO Auto-generated method stub
		String type = configMap.get("type");
		String cmd = configMap.get("cmd");
		
		if (type.equals("watchdog") == true && cmd.equals("check") == true) {
			MedusaWatchdogManager.checkStatus();
			Thread.sleep(watchdogPollInterval * 1000);
			checkWatchDogStatus();
		}
		else {
			Log.e(TAG, "! Unknown request: " + type);
		}
	}
}



