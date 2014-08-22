/**
 * 'MedusaExampleManager'
 *
 * - Demonstrating MedusaManagerBase instantiation.
 *   
 * @created: Nov. 11th 2011
 * @modified : Nov. 11th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import android.util.Log;

public class MedusaExampleManager extends MedusaManagerBase {

	/*
	 * Static methods for enforcing singleton pattern.
	 */
	private static MedusaExampleManager manager = null;

	public static MedusaExampleManager getInstance() {
		if (manager == null) {
			manager = new MedusaExampleManager();
			manager.init("MedusaTestManager");
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
	 * Define behavior per service request.
	 * 
	 * @see
	 * medusa.mobile.client.MedusaServiceManager#execute(medusa.mobile.client.MedusaServiceE
	 * ) - don't use while loop here, it will prohibit to serve other service
	 * requests.
	 */
	@Override
	protected void execute(MedusaServiceE ele) {
		/*
		 * In practical manager implementation, cb- function may be called after
		 * gathering some data or a certain firing condition has been met.
		 */
		Log.i("MedusaMgr", "* TestManager " + mgrName + "'s execute() function.");
		Log.i("MedusaMgr", "* Config. parameter reading test: expname -> ["
				+ configMap.get("expname") + "]");
		Log.i("MedusaMgr", "* Config. parameter reading test: sensor_type -> ["
				+ configMap.get("sensor_type") + "]");
		Log.i("MedusaMgr", "* Config. parameter reading test: sensor_method -> ["
				+ configMap.get("sensor_method") + "]");
		Log.i("MedusaMgr", "* Config. parameter reading test: sensor_period -> ["
				+ configMap.get("sensor_period") + "]");
		Log.i("MedusaMgr", "* Config. parameter reading test: sensor_thres -> ["
				+ configMap.get("sensor_thres") + "]");

		ele.seCBListner.callCBFunc(null, "Test OK. See Log messages in DDMS.");
	}
}
