/**
 * 'MedusaResourceMonitor'
 *
 * - To harness the amount of data transfer.
 *
 * @modified : Dec. 14th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.util.HashMap;

import android.util.Log;

public class MedusaResourceMonitor {
	
	private static String TAG = "MedusaResMon";
	private static HashMap<String, Long> dataLimitMap;	/* PID, LIMIT pair */
	private static HashMap<String, Long> dataUsageMap;	/* PID, Usage pair */

	public static void initialize() {
		dataLimitMap = new HashMap<String, Long>();
		dataUsageMap = new HashMap<String, Long>();
	}
	
	public static void reset() {
		dataLimitMap.clear();
		dataUsageMap.clear();
	}
	
	/* Harnessing data transfer by setting limit per task(pid) */
	public static void setDataLimit(String pid, String dlimit) {
		String[] strs = dlimit.split("[ ]");
		if (strs.length != 2) return;
		int unit = 1;
		if (strs[1].startsWith("K") == true) {
			unit = 1000;
		}
		else if (strs[1].startsWith("M") == true) {
			unit = 1000000;
		}
		else if (strs[1].startsWith("G") == true) {
			unit = 1000000000;
		}
		long dl = Long.parseLong(strs[0]) * unit;
		dataLimitMap.put(pid, dl);
		
		MedusaUtil.log(TAG, "* setting data transfer limit: pid=" + pid + ", limit=" + dl);
	}
	
	public static Long getDataLimit(String pid) {
		return dataLimitMap.get(pid);
	}
	
	public static Long getRemainingCapacity(String pid) {
		Long limit = dataLimitMap.get(pid);
		Long spent = dataUsageMap.get(pid);
		
		if (limit == null) {
			return (long)0;
		}
		else {
			return limit - (spent == null ? 0 : spent);
		}
	}
	
	public static Long spentData(String pid, Long amount) {
		if (amount == null || amount == 0) {
			Log.e(TAG, "! wrong request, amount=" + amount);
			return (long)0;
		}
		
		Long used = (dataUsageMap.get(pid) != null ? dataUsageMap.get(pid) : 0) + amount;
		dataUsageMap.put(pid, used);
		
		return used;
	}
}

