/**
 * 'MedusaAPControlBase'
 *
 * - Base class for wireless network interface selection algorithms.
 *
 * @modified : Dec. 14th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.text.format.Time;
import android.util.Log;

public abstract class MedusaAPControlBase {
	
	private static String TAG = "APCtrl";
	
	/*
	 * AP element class
	 */
	protected class APElement {
		final static int RATE_WINDOW_SIZE = 32;
		final static int NETMODE_EGPRS = 1;
		final static int NETMODE_WLAN_INFRA = 2;
		final static int NETMODE_WLAN_ADHOC = 3;
		final static int WLAN_CONNSECURITY_OPEN = 0;
		final static int WLAN_CONNSECURITY_WEP = 1;
		final static int WLAN_CONNSECURITY_WPA = 3;
		final static int WLAN_CONNSECURITY_WPA_PSK = 4;
		final static int AP_FAIL_LIMIT = 6;
		final static int AP_NO_SCAN_LIMIT = 2;

		String apname_; // SSID (50 bytes)
		String bssid_; // BSSID(MAC) (6 bytes)

		int uid_; // id in commDB
		int networkmode_; // infra, adhoc, gprs
		int freq_; // operating frequency

		int rssi_; // Receiver SignalStrength Indicator
		boolean defined_; // defined AP is in the commDB
		byte security_; // Open, WEP, WPA, WPA-PSK

		// Dynamic variables.
		byte refresh_; // For aging. If age is greater than threshold, this AP
						// will not be used.
		byte failcnt_;
		byte successcnt_;
		boolean redirected_;
		boolean verified_; // At least, could indicate whether this is usable AP
							// or not.

		byte ratehistcnt_;
		double ratehist_[];

		Time createdtime_;

		APElement(ScanResult sr) {
			apname_ = sr.SSID;
			uid_ = -1;
			networkmode_ = NETMODE_WLAN_INFRA;

			rssi_ = sr.level; // big enough number..
			defined_ = true;

			security_ = WLAN_CONNSECURITY_OPEN;
			bssid_ = sr.BSSID;
			freq_ = sr.frequency;

			refresh_ = 0;
			failcnt_ = 0;
			successcnt_ = 0;
			redirected_ = false;

			ratehistcnt_ = 0;
			ratehist_ = new double[RATE_WINDOW_SIZE];
			for (int i = 0; i < RATE_WINDOW_SIZE; i++)
				ratehist_[i] = 0;

			createdtime_ = new Time();
			createdtime_.setToNow();
		}

		APElement(String apname) {
			apname_ = apname;
			uid_ = -1;
			networkmode_ = NETMODE_WLAN_INFRA;

			rssi_ = 150; // big enough number..
			defined_ = false;

			security_ = WLAN_CONNSECURITY_OPEN;
			bssid_ = "N/A";
			freq_ = 0;

			refresh_ = 0;
			failcnt_ = 0;
			successcnt_ = 0;
			redirected_ = false;

			ratehistcnt_ = 0;
			ratehist_ = new double[RATE_WINDOW_SIZE];
			for (int i = 0; i < RATE_WINDOW_SIZE; i++)
				ratehist_[i] = 0;

			createdtime_ = new Time();
			createdtime_.setToNow();
		}

		boolean isBetterThan(APElement ap) {
			return (avgRate() > ap.avgRate() || (avgRate() == ap.avgRate() && rssi_ < ap.rssi_)) ? true
					: false;
		}

		void updateRateHistory(float rate) {
			int idx = ratehistcnt_++ % RATE_WINDOW_SIZE;
			ratehist_[idx] = rate;
		}

		double avgRate() {
			int itr = (ratehistcnt_ > RATE_WINDOW_SIZE ? RATE_WINDOW_SIZE
					: ratehistcnt_);
			double avgRate = 0.0;

			if (itr > 0) {
				for (int i = 0; i < itr; i++) {
					avgRate += ratehist_[i];
				}

				avgRate /= itr;
			} else {
				if (networkmode_ == NETMODE_EGPRS)
					avgRate = 10.0;
				else if (networkmode_ == NETMODE_WLAN_INFRA)
					avgRate = 100.0;
				else
					avgRate = 1.0;

				// Use Initial values as a first entry.
				itr = 1;
				ratehist_[0] = avgRate;
			}

			return avgRate;
		}

		boolean isUsable() {
			// Should be called after scanning IAP database.
			if (apname_.length() <= 0)
				return false;
			if (networkmode_ == NETMODE_WLAN_ADHOC)
				return false;
			if (redirected_ != false)
				return false;

			if ((security_ == WLAN_CONNSECURITY_WEP && defined_ == false)
					|| (security_ == WLAN_CONNSECURITY_WPA_PSK && defined_ == false)
					|| (security_ == WLAN_CONNSECURITY_WPA && defined_ == false)) {
				return false;
			}

			return true;
		}

	} // end APElement implementation.

	/*
	 * Constants.
	 */
	final int MIN_ENERGY_THRESHOLD = 60;
	final int SS_THRESHOLD = 80; // Signal Strength Threshold (-80dBm), acutal
									// value will be set at xml conf. file.
	final int AP_FAIL_LIMIT = 6; // Up to this no., vcaps will try to use.
	final int AP_NO_SCAN_LIMIT = 2; // If not scanned cnt exceed this number, it
									// will be disabled.

	/*
	 * Variables.
	 */
	// MNode *iTargetNode;
	ConcurrentHashMap<String, APElement> apTable;
	HashMap<String, Integer> failedTimePool;
	HashMap<String, String> configMap;

	/* selected AP */
	int curAPId;
	String curAPName;

	/* don't scan too often */
	Time scanTime; // Last Scanning timestamp.
	boolean forceScan;
	Time phoneStatusTime; // Last Scanning timestamp.

	/* consider phone status while selecting an AP */
	// CPhoneStatusCheck* iPhoneStatus;

	int slotCnt;
	Time beginTimeSlot;

	boolean flightMode;
	boolean canUseGPRS;
	boolean canUseWiFi;
	boolean netRegistered;
	int networkMode;

	int ssThreshold;

	// Constructor
	MedusaAPControlBase(HashMap<String, String> configmap) {
		configMap = configmap;

		apTable = new ConcurrentHashMap<String, APElement>();
		failedTimePool = new HashMap<String, Integer>();

		beginTimeSlot = new Time();
		scanTime = new Time();
		phoneStatusTime = new Time();

		reset();

		scanTime.setToNow();
		phoneStatusTime.setToNow();
		netRegistered = true;
		flightMode = false;
	}

	/*
	 * Public Methods.
	 */
	public int selectAP() {
		// [ENTRY POINT] Select and set current AP with WLAN or GPRS
		APElement p = null;

		/* scan APs before selecting an AP */
		if (scanAPs() != false) {
			/* reduce failcnt by half, input: 3 minutes */
			releaseFailedCntByTimeout(3);

			/* perform selection */
			p = selectAlgorithm();

			/* check defined/registration */
			if (p != null) {
				if (p.networkmode_ == APElement.NETMODE_EGPRS) {
					checkPhoneStatus(); // this gets both flight & net-register
										// info
					if ((netRegistered == false) || (flightMode != false)) {
						p = null;
					}
				} else if (p.uid_ == -1) { // non defined WLAN AP
					p.uid_ = createDefinedAP(p);
				}
			} else {
				// There is no usable AP
				checkPhoneStatus(); // this gets both flight & net-register info
			}
		}

		/* set current AP to the selected AP */
		return setCurrentAP(p);
	}

	// Set configurable info
	public void setSSTheshold(int ssval) {
		ssThreshold = ssval;
	}

	public void disableGPRS() {
		canUseGPRS = false;
	}

	public void disableWiFi() {
		canUseWiFi = false;
	}

	public void enableGPRS() {
		canUseGPRS = true;
	}

	public void enableWiFi() {
		canUseWiFi = true;
	}

	// Get info about currently selected AP
	public APElement getCurAP() {
		// Get current APElement
		if (curAPId != -1) {
			return apTable.get(curAPName);
		} else {
			return null;
		}
	}

	public int getCurAPUid() {
		// Get current AP UID
		if (apTable.get(curAPName) != null) {
			return curAPId;
		} else {
			return -1;
		}
	}

	public String getCurAPName(String name) {
		// Get current AP name
		if (curAPId != -1) {
			return curAPName;
		} else {
			return null;
		}
	}

	public double getCurTimeSlot() {
		Time now = new Time();
		double curts = slotCnt;
		double slotofs = 0.0;

		now.setToNow();
		slotofs = (now.toMillis(true) - beginTimeSlot.toMillis(true)) / 1000.0;
		curts += (slotofs / 20.0);

		return curts;
	}

	public void reset() {
		// reset the AP list
		forceScan = true;
		curAPId = -1;
		curAPName = "";
	}

	public void increaseFailCntOfCurrentAP(int count) {
		// Increase Failure Count..
		APElement ap = apTable.get(curAPName);
		if (ap != null) {
			ap.failcnt_ += count;
			if (ap.failcnt_ > APElement.AP_FAIL_LIMIT) {
				ap.failcnt_ = APElement.AP_FAIL_LIMIT;
			}
		}
	}

	public void increaseSuccessCntOfCurrentAP(int count) {
		// Increase Failure Count..
		APElement ap = apTable.get(curAPName);
		if (ap != null) {
			ap.successcnt_ += count;
			ap.verified_ = true;
			if (ap.successcnt_ >= 3) {
				ap.failcnt_--;
				ap.successcnt_ = 0;
			}
		}
	}

	// Check if tmobile or attwifi AP
	public int doesAPReqWebAuthen() {
		return -1; /* not yet implemented */
	}

	/*
	 * Abstract Methods for the Control Algorithms.
	 */
	abstract protected void eachTimeSlot();

	abstract protected void atArrival();

	abstract protected void atDeparture(int size, boolean bFinalChunk);

	abstract protected void initStatValues();

	abstract protected void printStats(APElement ap);

	abstract APElement selectAlgorithm(); // apply AP selection algorithm

	abstract String getName();

	/*
	 * Protected Methods.
	 */
	protected boolean scanAPs() {
		// Re-Scan APs before selecting
		APElement ap = null;
		long tdiff = 0;
		boolean bScanned = true;

		if (curAPId != -1) {
			ap = apTable.get(curAPName);
		}

		tdiff = getElapsedTime(scanTime);

		if (forceScan != false /* min scan time (15 sec) */
				|| apTable.size() == 0
				// || (ap && ap->iNetworkMode == EGPRS)
				|| tdiff > 60 /* 1 min */) {
			Log.d(TAG, String.format(
					"[ScanAPs] Scanning, (%d) sec from the previous", tdiff));

			WifiManager wifiMgr = (WifiManager) G.appCtx
					.getSystemService(Context.WIFI_SERVICE);

			// scan APs and put them into apTable.
			IntentFilter iF = new IntentFilter();
			iF.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			G.appCtx.registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context c, Intent i) {
					// Code to execute when SCAN_RESULTS_AVAILABLE_ACTION event
					// occurs
					WifiManager w = (WifiManager) c
							.getSystemService(Context.WIFI_SERVICE);
					List<ScanResult> res = w.getScanResults(); // Returns a
																// <list> of
																// scanResults
					Iterator<ScanResult> resitr;
					int cnt = 0;

					resitr = res.iterator();

					while (resitr.hasNext() == true) {
						ScanResult sres = resitr.next();
						int netid = -1;

						APElement ape = new APElement(sres);
						apTable.put(sres.SSID, ape);
						cnt++;
					}

					Log.d(TAG, "* scan results: " + cnt
							+ " entries has been added to the AP pool.");
				}
			}, iF);

			// WiFi only..
			if (wifiMgr.isWifiEnabled() == false) {
				wifiMgr.setWifiEnabled(true);
			}

			wifiMgr.startScan();

			Log.d(TAG, "* startScan() called.");

			// Update last scanning timestamp.
			scanTime.setToNow();
			forceScan = false;
		}

		return bScanned;
	}

	/*
	 * !!!! Should re-work on this function later.. with reviving failedTimePool
	 * impl. !!!!
	 */
	protected void releaseFailedCntByTimeout(int timeout) {
		// Release iFailCnt by timeout.
		// TExpirableTimeItr iter(*(iFailedTimePool->GetPool()));
		boolean bAct = false;
		int to = timeout;
		String cur = "";

		// while (cur = (TTmpBuf8 *)iter.NextKey(), cur) {
		APElement ap = apTable.get(cur);
		if (ap != null) {
			if (ap.verified_ == false)
				to = timeout * 2;
			// if (failedTimePool.CheckIfExpiredByMin(ap.apname_, to) != false)
			// {
			// decrease iFailCnt value by 1 so that we might try once more in
			// near future..
			if (ap.failcnt_ > 0) {
				if (ap.failcnt_ == APElement.AP_FAIL_LIMIT)
					ap.failcnt_ -= 3;
				else
					ap.failcnt_--;

				if (ap.failcnt_ == 0) {
					// iter.RemoveCurrent();
				} else {
					// reset timestamp.
					// failedTimePool->RegisterTime(ap->iAPName);
				}

				bAct = true; // for logging
				Log.d(TAG, String.format(
						"* [%s] failCnt decreased by one: %d/%d", ap.apname_,
						ap.failcnt_, AP_FAIL_LIMIT));
			}
			// }
		} else {
			// No such ap in the current APTable.
			// iter.RemoveCurrent();
		}
		// }

		if (bAct == true)
			Log.d(TAG, String.format(
					"* failedTimePool adjusted: # of entry(%d)",
					failedTimePool.size()));
	}

	protected int setCurrentAP(APElement p) {
		// Set current AP to the selected AP
		String bssid;

		/* re-initialize selection */
		curAPId = -1;
		curAPName = "";

		if (p != null) {
			/* set currenet AP */
			curAPId = p.uid_;
			curAPName = p.apname_;

			Log.d(TAG, String.format(
					"* Current AP: %S(%d)\tFail(%d)\t%S\tRSSI(%d)\tRate(%.3f)",
					p.apname_, p.uid_, p.failcnt_,
					(p.networkmode_ == APElement.NETMODE_EGPRS ? "EGPRS"
							: "WLAN"), p.rssi_, p.avgRate()));
		}

		return curAPId;
	}

	protected int createDefinedAP(APElement p) {
		/*
		 * This function is platform-dependent.
		 */
		return -1;
	}

	// helper function
	protected void checkPhoneStatus() {
		if (getElapsedTime(phoneStatusTime) > 900 /* 15 min */) {
			// iPhoneStatus->GetFlightInfo(); // this gets both flight &
			// net-register info
			phoneStatusTime.setToNow();
		}
	}

	protected long getElapsedTime(Time from) {
		Time now = new Time();
		now.setToNow();
		return (now.toMillis(true) - from.toMillis(true)) / 1000;
	}

	/* examine APs that are "IMPOSSIBLE" to use. */
	protected boolean checkIfUsable(APElement p) 
	{
		/* 1. Basic Check. */
		if (p.isUsable() == false)
			return false;

		/* 2. Check with device status. */
		if (p.networkmode_ == APElement.NETMODE_EGPRS) {
			if (netRegistered == false || flightMode != false)
				return false;
			p.rssi_ = ssThreshold - 1;
		}

		/* 3. Check with configurable params. */
		if (p.networkmode_ == APElement.NETMODE_EGPRS && canUseGPRS == false)
			return false;
		if ((p.networkmode_ == APElement.NETMODE_WLAN_INFRA || p.networkmode_ == APElement.NETMODE_WLAN_ADHOC)
				&& canUseWiFi == false)
			return false;

		/*
		   4. Special Restrictions..
		*/
		if (p.networkmode_ == APElement.NETMODE_WLAN_INFRA
				|| p.networkmode_ == APElement.NETMODE_WLAN_ADHOC) {
			String policy = configMap.get("tx_wifi_policy");

			if (policy != null) {
				if (policy.compareToIgnoreCase("white") == 0) {
					// Check ALLOW list only.
					String allowAPList = configMap.get("tx_wifi_allow");
					if (allowAPList.contains(p.apname_) == false
							&& allowAPList.compareToIgnoreCase("all") != 0)
						return false;
				} else {
					// Check DENY list only.
					String denyAPList = configMap.get("tx_wifi_deny");
					if (denyAPList.contains(p.apname_) == true
							|| denyAPList.compareToIgnoreCase("all") == 0)
						return false;
				}
			}
		}

		// For Allowed WiFi and GPRS
		return true;
	}

}
