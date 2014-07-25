/**
 * 'MedusaAPControlWiFiOnly'
 *
 * - Implements Greedy algorithm. 
 * 	 	Moo-Ryong Ra, Jeongyeup Paek, Abhishek B. Sharma, Ramesh Govindan, Martin H. Krieger, and Michael J. Neely, 
 * 		"Energy-Delay Tradeoffs in Smartphone Applications." In MobiSys 2010.
 * 		
 * - Porting effort from Symbian C++ version, Not tested on Android.
 *
 * @modified : Dec. 14th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.util.HashMap;
import java.util.Iterator;

import android.text.format.Time;
import android.util.Log;

public class MedusaAPControlGreedy extends MedusaAPControlBase {
	double avgU;
	double miscU;

	Time startTime;

	MedusaAPControlGreedy(HashMap<String, String> configmap) {
		super(configmap);
		// TODO Auto-generated constructor stub
		Log.i("APCtrl", "* create MedusaAPControlGreedy() instance.");
	}

	@Override
	protected void atArrival() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void atDeparture(int size, boolean finalChunk) {
		// TODO Auto-generated method stub
		if (finalChunk == false) {
			miscU += size / 1000000.0;
		} else {
			miscU = 0.0;
		}
	}

	@Override
	protected void eachTimeSlot() {
		// TODO Auto-generated method stub
		slotCnt++;
		printStats(null);

		beginTimeSlot.setToNow();
	}

	@Override
	protected void initStatValues() {
		// TODO Auto-generated method stub
		slotCnt = 1;
		avgU = 0.0;
		miscU = 0.0;
		startTime = new Time();

		startTime.setToNow();
		beginTimeSlot.setToNow();
	}

	@Override
	protected void printStats(APElement ap) {
		// TODO Auto-generated method stub
		double uval = 0.0;
		double ureal = 0.0;

		// U
		// uval = GET_VIDEO_DIR_SIZE();
		ureal = (uval) / 1000000.0; // unit: MB
		ureal -= miscU;

		// Calculate Average Values..
		if (ureal < 0)
			ureal = 0;
		avgU = (avgU * (slotCnt - 1) + ureal) / slotCnt;
		if (avgU < 0)
			avgU = 0.0;

		Log.i("APCtrlGreedy", String.format("[STAT]\tGreedy\t%d\t%.6f\t%.6f",
				slotCnt, ureal, avgU));
	}

	@Override
	APElement selectAlgorithm() {
		// TODO Auto-generated method stub
		APElement p = null, cur = null;
		Iterator<APElement> itr = apTable.values().iterator();

		while (itr.hasNext() == true) {
			cur = itr.next();

			if (checkIfUsable(cur) == false)
				continue;

			if (cur.failcnt_ >= APElement.AP_FAIL_LIMIT)
				continue;
			if (cur.refresh_ >= AP_NO_SCAN_LIMIT
					&& (cur.networkmode_ == APElement.NETMODE_WLAN_INFRA || cur.networkmode_ == APElement.NETMODE_WLAN_ADHOC))
				continue;

			if (cur.rssi_ < ssThreshold) {
				if (p == null || cur.isBetterThan(p) != false) {
					p = cur;
				}
			}
		}

		return p;
	}

	@Override
	String getName() {
		// TODO Auto-generated method stub
		return new String("GREEDY");
	}

}
