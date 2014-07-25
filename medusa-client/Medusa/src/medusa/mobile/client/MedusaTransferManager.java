/**
 * 'MedusaTransferManager'
 *
 * - This class implements MedusaTransferManager.
 *   Its super class is MedusaServiceManager and 
 *   has designed so that it can support multiple types 
 *   of data abstractions.
 *         
 * @created  : Apr. 1st 2011
 * @modified : Nov. 14th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Base64;
import android.util.Log;

public class MedusaTransferManager extends MedusaPriorityManagerBase 
{
	private static final String TAG = "MedusaTxMgr";
	
	private static final String OP_UPLOAD = "upload";
	private static final String OP_GET = "GET";
	private static final String OP_POST = "POST";
	private static final String OP_GETFILE = "GETFILE";
	private static final String OP_SCAN = "SCAN";
	public static final String OP_SCAN_WIFI = "wifi";
	public static final String OP_SCAN_BLUETOOTH = "bluetooth";
	
	/*
	 * Active Transfer Object.
	 */
	class ActiveTxObject 
	{
		boolean skip; 		// if file reading was failed.. set this field.

		String type; 		// upload/download
		String path; 		// full path.
		File fp;

		long offset; 		// current offset.
		long tobesent; 		// temporarily granted amount.
		int failcnt; 		// failure count;
		
		String pid;
		String qid;
		String muid;
		String review;

		// Constructor
		ActiveTxObject(String in_type, String in_path, String in_pid, String in_qid, String in_muid, String in_review) 
		{
			type = in_type;
			path = in_path;
			pid = in_pid;
			qid = in_qid;
			muid = in_muid;
			review = in_review;

			// read file info.
			fp = new File(path);

			if (fp.isFile() == true && fp.canRead() == true) {
				tobesent = 0;
				failcnt = 0;
				offset = 0;
				skip = false;
			} else {
				skip = true; 	// will not send or process..
				Log.e("MedusaTxObj", "! not a file or can't read: " + path);
			}
		}

		long fileSize() {
			return fp.length();
		}

		long lastModified() {
			return fp.lastModified();
		}

		void deleteInfo() {
			// delete
			fp.delete();
			MedusaStorageManager.requestServiceSQLite(TAG, null, "medusadata.db"
					, "delete from mediameta where path='" + path + "'");
			Log.i("MedusaTxObj", "* File [" + path + "] has been deleted.");
		}
		
		String getPid() { return pid; }
		String getQid() { return qid; }
		String getMuid() { return muid; }
		String getReview() { return review; }
	};

	/*
	 * Static methods for enforcing singleton pattern.
	 */
	private static MedusaTransferManager manager = null;

	public static MedusaTransferManager getInstance() {
		if (manager == null) {
			manager = new MedusaTransferManager();
			manager.init("MedusaTransferManager");
		}

		return manager;
	}

	public static void terminate() {
		if (manager != null) {
			manager.markExit();
			manager = null;
		}
	}
	
	/* Simple HTTP Requests */
	public static void requestHttpReq(String aqname, String method, String url, MedusaletCBBase listener)
	{
		if (method.equals(OP_GET) == true || method.equals(OP_POST) == true) {
			String encodedURL = Base64.encodeToString(url.getBytes(), Base64.DEFAULT);
			requestService(aqname, listener, method, encodedURL, null, "0", "0", "0", "0");
		}
		else {
			Log.e(TAG, "! wrong HTTP request method: [" + method + "]");
		}
	}
	
	/* File Downloading Request */
	public static void requestFileDownload(String aqname, String url, String fpath, MedusaletCBBase listener)
	{
		requestService(aqname, listener, OP_GETFILE, url, fpath, "0", "0", "0", "0");
	}
	
	/* File uploading request */
	public static void requestUpload(MedusaletBase medusalet, String uids, String paths, String reviews, MedusaletCBBase listener)
	{
		MedusaTransferManager.getInstance().waitReviewMap.put(medusalet.getName() + ":" + uids, listener);

		if (medusalet.getPreviewOpt().equals("yes") == true) {
			/* Preview the data before uploading. */
			MedusaUtil.requestPreviewForUpload(medusalet, paths, uids
					, reviews == null || reviews.length() == 0 ? "N/A" : reviews, true);
			
			Log.d(TAG, "* preview required for uploading [" + uids + "]");
		}
		else {
			/* directly upload the data wo/ preview. */
			Bundle bdl = MedusaUtil.requestPreviewForUpload(medusalet, paths, uids
					, reviews == null || reviews.length() == 0 ? "N/A" : reviews, false);
			requestUploadAfterReview(bdl);
			
			Log.d(TAG, "* don't need preview for uploading [" + uids + "]");
		}
	}
	
	/* cancel pending uploading objects */
	public static void cancelUpload(MedusaletBase medusalet, String uids)
	{
		String[] muids = uids.split("\\|");
		for (int i = 0; i < muids.length; i++) {
			Log.d(TAG, "* cancel pending upload request, muid=" + muids[i] + " now list length is " + String.valueOf(activeTxList.size()));
			
			for (int j = 0; j < ((LinkedList<ActiveTxObject>) activeTxList).size(); j++)
			{
				ActiveTxObject curActiveTxObj = (ActiveTxObject) ((LinkedList<ActiveTxObject>) activeTxList).get(j);
				if (curActiveTxObj.getMuid().equals(muids[i])) ((LinkedList<ActiveTxObject>) activeTxList).remove(j);
				Log.d(TAG, "* cancel uid=" + muids[i] + " now list length is " + String.valueOf(activeTxList.size()));
			}
		}
	}
	
	/* File uploading request after user reviews */
	public static void requestUploadAfterReview(Bundle bundle)
	{
		String aqname = bundle.getString("medusalet_name");
		String uids = bundle.getString("uids");
		String reviews = bundle.getString("tagged_reviews");
		String key = aqname + ":" + uids;
		
		MedusaletCBBase listener = MedusaTransferManager.getInstance().waitReviewMap.get(key);
		
		if (listener != null) {
			String[] paths = bundle.getString("paths").split("\\|");
			String[] muids = uids.split("\\|");
			String[] revs = reviews.split("\\|");
			String pid = bundle.getString("pid");
			String qid = bundle.getString("qid");
			
			/* to prevent weird reviews input.. */
			if (revs.length != muids.length) revs = new String[muids.length]; 
			
			for (int i = 0; i < muids.length; i++) {
				Log.d(TAG, "* resume pending upload request, aqname=" + aqname + " path=" + paths[i]
								+ " pid=" + pid + " qid=" + qid + " muid=" + muids[i]);
				
				requestService(aqname, listener, "upload", paths[i], null, pid, qid, muids[i], revs[i]);
				MedusaTransferManager.getInstance().waitReviewMap.remove(key);
			}
		}
	}
	
	public static void requestScanResult(String opcode, String period, MedusaletCBBase listener)
	{
		if (opcode.equals(OP_SCAN_WIFI) == true || opcode.equals(OP_SCAN_BLUETOOTH) == true) {
			/* WiFi scanning result */
			requestService(TAG, listener, OP_SCAN, opcode, period, "N/A", "N/A", "N/A", "N/A");
		}
		else {
			Log.e(TAG, "! opcode is not identified: [" + opcode + "]");
		}
	}

	public static void requestService(String name, MedusaletCBBase listener
									, String type, String path, String out_path
									, String pid, String qid, String muid, String review) {
		MedusaPriorityServiceE ele = new MedusaPriorityServiceE();
		StringBuilder b = new StringBuilder();
		
		//mudi split to muid and priority
		
		int credit = Integer.MAX_VALUE; 
		long fsize = 0, deadline = -1, starttime = 0;
		ele.pid = "";
		if (type.equals(OP_UPLOAD))
		{
			MedusaUtil.log(TAG, "xing* " + muid);
			String[] temp = muid.split(":");
			muid = temp[0];
			long now = System.currentTimeMillis()/1000;
			if (temp.length > 1)
				fsize = Long.valueOf(temp[1]);
			if (temp.length > 2)
				credit = Integer.valueOf(temp[2]);
			if (temp.length > 3)
				deadline = now + Integer.valueOf(temp[3]);
			if (temp.length > 4)
				starttime = now + Integer.valueOf(temp[4]);
			if (temp.length > 5)
				ele.pid = temp[5];
			
			MedusaUtil.log(TAG, "xing* credit=" + credit + "deadline=" + deadline + " starttime" + starttime);
		}

		b.append("<xml>");
			b.append("<name>"); b.append(name);	b.append("</name>");
			b.append("<pid>"); b.append(pid);	b.append("</pid>");
			b.append("<qid>"); b.append(qid);	b.append("</qid>");
			b.append("<muid>"); b.append(muid);	b.append("</muid>");
			b.append("<review>"); b.append(review);	b.append("</review>");
			b.append("<tx>");
				b.append("<type>");	b.append(type);	b.append("</type>");
				b.append("<path>");	b.append(path);	b.append("</path>");
				b.append("<outpath>");	b.append(out_path);	b.append("</outpath>");
			b.append("</tx>");
		b.append("</xml>");
		
		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = listener;
		
		ele.credit = credit;
		ele.deadline = deadline;
		ele.fsize = fsize;
		ele.starttime = starttime;
		if (ele.pid == "") ele.pid = pid;
		ele.uid = muid;

		try {
			MedusaTransferManager.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	long configTxUploadChunkSize;
	byte configFailCntThres;
	long configSleepInterval;
	boolean configCanUseWifi;
	boolean configCanUse3G;
	String configISPolicy;

	HashMap<String, MedusaletCBBase> waitReviewMap;
	static List<ActiveTxObject> activeTxList;
	MedusaTransferHttpAdapter httpAdapter;
	MedusaAPControlBase apController;

	ActiveTxObject curActiveTxObj;
	String curAPName;
	int curFailCnt;

	/*
	 * init() function : initialize MedusaTxMgr.
	 */
	@Override
	public void init(String name) 
	{
		super.init(name);

		activeTxList = new LinkedList<ActiveTxObject>();
		httpAdapter = new MedusaTransferHttpAdapter();
		waitReviewMap = new HashMap<String, MedusaletCBBase>();

		curActiveTxObj = null;
		curAPName = "UNKNOWN";
		curFailCnt = 0;

		// default parameters..
		configTxUploadChunkSize = 200000;
		configFailCntThres = 5;
		configSleepInterval = 1000; // in milisecond.
		configCanUseWifi = true;
		configCanUse3G = true;
		configISPolicy = "GREEDY";

		apController = null;
	}

	@Override
	protected void execute(MedusaPriorityServiceE ele) throws InterruptedException 
	{
		Log.d(TAG, "* caller -> [" + configMap.get("name") + "]");

		/*
		 * XML format hierarchy.
		 * 
		 * - expname - transfer - type
		 */
		String type = configMap.get("tx_type");
		String path = configMap.get("tx_path");
		String uid = configMap.get("muid");

		final MedusaServiceE inner_ele = ele;		/* for inner class use */
		
		if (type.compareTo(OP_UPLOAD) == 0) {
			/*
			 * Move path to Active Set, and
			 */
			String pid = configMap.get("pid");
			String qid = configMap.get("qid");
			//String muid = configMap.get("muid");
			String review = configMap.get("review");
			
			addToActiveList("upload", path, pid, qid, uid, review);
			Log.d(TAG, "* Starting upload request, uid=" + uid + "path=" + path + " with Credit=" + ele.credit + ", Deadline=" + ele.deadline + " fsize=" + ele.fsize + " pid=" + ele.pid);
			MedusaUtil.log(TAG, "* Starting upload request, uid=" + uid + "path=" + path + " with Credit=" + ele.credit + ", Deadline=" + ele.deadline +  " fsize=" + ele.fsize + " pid=" + ele.pid);
			uploadFiles();
		

		} 
		else if (type.compareTo(OP_GET) == 0) {
			
			String decodedURL = new String( Base64.decode(path, Base64.DEFAULT) );
			Log.d(TAG, "* HTTP GET -> " + decodedURL +  " with Credit=" + ele.credit + ", Deadline=" + ele.deadline + " pid=" + ele.pid);
			httpAdapter.httpSimpleRequest("GET", decodedURL);
			
		} 
		else if (type.compareTo(OP_POST) == 0) {
			
			String decodedURL = new String( Base64.decode(path, Base64.DEFAULT) );
			Log.d(TAG, "* HTTP POST -> " + decodedURL);
			httpAdapter.httpSimpleRequest("POST", decodedURL);
			
		} 
		else if (type.compareTo(OP_GETFILE) == 0) {
			
			Log.d(TAG, "* HTTP GET File -> " + path);
			String outpath = configMap.get("tx_outpath");
			httpAdapter.getFileViaHttpGET(path, outpath);	
		
		} 
		else if (type.compareTo(OP_SCAN) == 0) {
			if (path.compareTo(OP_SCAN_WIFI) == 0) {
				WifiManager wifiMgr = (WifiManager) G.appCtx.getSystemService(Context.WIFI_SERVICE);
				
				// scan APs and put them into table.
				IntentFilter filter = new IntentFilter();
				filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
				G.registerReceiver(new BroadcastReceiver() {
					@Override
					public void onReceive(Context c, Intent i) {
						WifiManager w = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
						List<ScanResult> res = w.getScanResults();
						Iterator<ScanResult> itr = res.iterator();
						String args = "";
						
						while (itr.hasNext() == true) {
							ScanResult sr = (ScanResult) itr.next();
							
							if (args.length() > 0) args += ",,"; 
							args += sr.SSID + "::" + sr.BSSID + "|" + sr.capabilities + "|" + sr.frequency + "|" + sr.level;
						}
						inner_ele.seCBListner.callCBFunc(args, "* Returning WiFi scan result");
					}
				}, filter, ele.seCBListner.runnerInstance.getName(), OP_SCAN_WIFI);

				// WiFi only..
				if (wifiMgr.isWifiEnabled() == false) {
					wifiMgr.setWifiEnabled(true);
				}

				wifiMgr.startScan();
				Log.d(TAG, "* startScan() called.");
			}
			else if (path.compareTo(OP_SCAN_BLUETOOTH) == 0) {
				IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
				//G.appCtx.registerReceiver(new BroadcastReceiver() {
				G.registerReceiver(new BroadcastReceiver() {
					@Override
					public void onReceive(Context c, Intent intent) {
						String action = intent.getAction();
				        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				            inner_ele.seCBListner.callCBFunc(device.getName() + "|" + device.getAddress()
				            								, "* Returning Bluetooth scan result");
				        }
					}
				}, filter, ele.seCBListner.runnerInstance.getName(), OP_SCAN_BLUETOOTH);
		
				/* 
				 * MRA: Since getDefaultAdapter function's restriction, 
				 * I relay the message to the MedusaLoaderService.. 
				 */
				MedusaUtil.passCommandToService(null, MedusaLoaderService.CMD_SCAN_BLUETOOTH, "N/A");
			}
			else {
				Log.e(TAG, "! Unknown Request on Scan Op: " + path);
			}

			return;	/* so as not to call uncessary callback. */
		}
		else {
			Log.e(TAG, "! Unknown Type: " + type);
		}
		
		if (ele.seCBListner != null) {
				ele.seCBListner.callCBFunc(type.equals(OP_UPLOAD) ? uid : path, "* [TxMsg] Type="+type+" arg="+path);
		}
		
		if (type.equals(OP_UPLOAD))
			deleteDuplicatedUid(uid);
	}
	
	/*
	 * addToActiveList() function : Add $path to the active transfer list. : The
	 * active transfer list will be considered at once : unless there is a
	 * network failure.
	 */
	private void addToActiveList(String type, String path, String pid, String qid, String muid, String review) 
	{
		ActiveTxObject aobj = new ActiveTxObject(type, path, pid, qid, muid, review);

		if (aobj.skip == false) {
			activeTxList.add(aobj);
		}
	}
	


	/*
	 * uploadFiles() function : Main Entry of File Uploading Procedure.
	 */
	private void uploadFiles() throws InterruptedException 
	{
		boolean bSend = false;

		/*
		 * Step 1: Check Availability.
		 */
		while (checkDataAvailability() == true) {
			/*
			 * Step 2: Policy Adjustment, Set NIC appropriately.
			 */
			bSend = checkISPolicy();

			/*
			 * Step 3: Send Data.
			 */
			if (bSend == true) {
				if (performHttpMultipartFormDataTx() == false) {
					curActiveTxObj = null;
					continue;
				}
			}

			/*
			 * Step 4: Transition Policy.
			 */
			transitionPolicyOnUploads();
		}

		Log.d(TAG, "* Yielding: no more data or failed N+ times.");
		System.gc();
	}

	/*
	 * Upload Step 1: Check Availability.
	 */
	private boolean checkDataAvailability() 
	{
		boolean bRet = false;

		if (curActiveTxObj == null) {
			if (activeTxList.isEmpty() == false) {
				curActiveTxObj = (ActiveTxObject) ((LinkedList<ActiveTxObject>) activeTxList).getFirst();
				((LinkedList<ActiveTxObject>) activeTxList).removeFirst();
			}
		}

		if (curActiveTxObj != null
				&& curActiveTxObj.fileSize() > curActiveTxObj.offset) {
			curActiveTxObj.tobesent = curActiveTxObj.fileSize()	- curActiveTxObj.offset;

			Log.d(TAG, "* Data Selected..");
			bRet = true;
		}

		if (bRet == false) {
			// if data in active list has already been sent,
			// check request queue.
			if (curActiveTxObj == null) {
				Log.d(TAG, "* No more data to transfer..");
			} else {
				Log.e(TAG, "! This is weird.. should not happen.");
			}
		}

		return bRet;
	}

	/*
	 * Step 2: Check NIC(Network Interface Card) Selection Policy.
	 */
	private boolean checkISPolicy() 
	{
		WifiManager wifiMgr = (WifiManager) G.appCtx.getSystemService(Context.WIFI_SERVICE);
		boolean b_ret = true;

		/*
		 * Lazy loading.
		 */
		if (apController == null) {
			if (configISPolicy.compareToIgnoreCase("greedy") == 0) {
				apController = new MedusaAPControlGreedy(configMap);
			} else if (configISPolicy.compareToIgnoreCase("wifionly") == 0) {
				apController = new MedusaAPControlWiFiOnly(configMap);
			} else if (configISPolicy.compareToIgnoreCase("salsa") == 0) {
				apController = new MedusaAPControlSALSA(configMap);
			} else {
				// if unknown policy was set, force to use greedy strategy.
				apController = new MedusaAPControlGreedy(configMap);
			}
		}

		if (apController.selectAP() != -1) {
			MedusaAPControlBase.APElement ap = apController.getCurAP();
			if (ap != null) {
				if (ap.networkmode_ == MedusaAPControlBase.APElement.NETMODE_EGPRS) {
					// connect to cellular mode.
					if (wifiMgr.isWifiEnabled() == true) {
						wifiMgr.disconnect();
						if (wifiMgr.setWifiEnabled(false) == false) {
							Log.e(TAG, "! failed to disable WiFi.");
						}
					}

					curAPName = "3G";

					Log.d(TAG,"* NET[3G] was selected. AP name: " + ap.apname_);
				} else {
					// connect to WiFi.
					if (wifiMgr.isWifiEnabled() == true) {
						curAPName = wifiMgr.getConnectionInfo().getSSID();
					} else if (wifiMgr.isWifiEnabled() == false) {
						wifiMgr.setWifiEnabled(true);
						curAPName = wifiMgr.getConnectionInfo().getSSID();
					}

					// enable the specific AP (disable others).
					wifiMgr.enableNetwork(ap.uid_, true /* disable others */);

					Log.d(TAG, "* NET[" + curAPName	+ "] was selected. AP name: " + ap.apname_);
				}
			}
		} else {
			/*
			 * Both for a) SelectAP returned -1, no AP.. b) Using
			 * Android-default AP selection..
			 */
			if (wifiMgr.isWifiEnabled() == true) {
				curAPName = wifiMgr.getConnectionInfo().getSSID();
			} else if (wifiMgr.isWifiEnabled() == false) {
				curAPName = "3G";
			}
		}

		return b_ret;
	}

	/*
	 * Step 3: Send Data.
	 */
	private boolean performHttpMultipartFormDataTx() 
	{
		boolean b_ret = true;
		long sendsize, maxbufsize = 1024 * 128; 	// 128k
		String pfname;
		String pid = curActiveTxObj.getPid();
		
		/* Keep sending until tobesent_ will become zero. */
		while (curActiveTxObj.tobesent > 0) {
			
			/* Check if data size is within our budget.. */
			Long limit = MedusaResourceMonitor.getDataLimit(pid);
			if (limit != null && limit > 0) {
				if (curActiveTxObj.tobesent > MedusaResourceMonitor.getRemainingCapacity(pid)) {
					MedusaUtil.log(TAG, "! exceed transfer limit: pid=" + pid + " size=" + curActiveTxObj.tobesent
										+ " budget=" + MedusaResourceMonitor.getRemainingCapacity(pid));
					return false;
				}
			}

			/* Set Tx size. */
			if (curActiveTxObj.tobesent > configTxUploadChunkSize) {
				sendsize = configTxUploadChunkSize;
			} else {
				sendsize = curActiveTxObj.tobesent;
			}

			/* Set intermediate chunk names.. */
			String str = (new File(curActiveTxObj.path)).getName();
			int idx = (int) (curActiveTxObj.offset / configTxUploadChunkSize);

			if (configTxUploadChunkSize < (int) (curActiveTxObj.fileSize() - curActiveTxObj.offset)) {
				pfname = String.format("%s.V%05d"
							, String.valueOf(str.toCharArray(), 0, str.lastIndexOf("."))
							, idx);
			} else {
				/* last chunk. */
				pfname = str;
			}

			/* Make URL string. */
			Time t = new Time();
			t.set(curActiveTxObj.lastModified());
			t.switchTimezone("GMT");

			String urlstr = G.URIBASE_UPLOAD + "?time="	+ t.format("%Y/%m/%d@%H:%M:%S") 
					+ "&ver=" + G.VERSION + "&PVER=" + G.tpVer + "&imei=" + G.tpIMEI 
					+ "&uid=" + G.getUID() + "&model=" + G.tpModel.replace(" ", "-") 
					+ "&apname=" + curAPName.replace(" ", "-") 
					+ "&pid=" + curActiveTxObj.getPid() + "&qid=" + curActiveTxObj.getQid() 
					+ "&muid=" + curActiveTxObj.getMuid() 
					+ "&review=" + MedusaUtil.base64Encode(curActiveTxObj.getReview());
			
			Log.d(TAG, "* Interim fname: " + pfname + " pid=" + curActiveTxObj.getPid() 
					+ " qid=" + curActiveTxObj.getQid() + " muid=" + curActiveTxObj.getMuid());
			Log.d(TAG, "* URL=" + urlstr);

			/* Sending actual data */
			try {
				G.getElapsedTime();
				
				if (httpAdapter.uploadDataViaHttpMultipartFormData(
						curActiveTxObj.path, pfname, curActiveTxObj.offset,
						sendsize, maxbufsize, urlstr) == true) {
					curActiveTxObj.tobesent -= sendsize;
					curActiveTxObj.offset += sendsize;
					
					/* Report to resource monitor. */
					Long used = MedusaResourceMonitor.spentData(pid, sendsize);
					
					Log.d(TAG, "* pid=" + pid + " sent=" + sendsize + " used=" + used + " elapsed=" + G.getElapsedTime());

					/* Reset Failure Counts.. */
					if (curFailCnt > 0) curFailCnt = 0;
				} else {
					throw new IOException();
				}

				/*
				 * MRA: Garbage collection here was important so as not to be
				 * killed by android watchdog.
				 */
				System.gc();

			} catch (IOException e) {
				Log.e(TAG, "! Tx Failed: " + pfname);
				e.printStackTrace();

				if (++curFailCnt >= configFailCntThres) {
					b_ret = false;
					break;
				}
			}
		}

		return b_ret;
	}

	/*
	 * Step 4: Wait(Transition) Policy.
	 */
	private void transitionPolicyOnUploads() throws InterruptedException 
	{
		/* Check if currentActiveTxObj is finished. */
		if (curActiveTxObj.fileSize() == curActiveTxObj.offset) {
			Log.d(TAG, "* The file [" + curActiveTxObj.path + "] has been sent completely.");
			/* remove actual video and metadata. */
			//curActiveTxObj.deleteInfo();
			//MedusaUtil.toastNotifiationViaMedusaService(G.appCtx, "sent [" + curActiveTxObj.path + "]" 
			/*MedusaUtil.log(TAG, "* sent [" + curActiveTxObj.path + "]"
					+ " pid=" + curActiveTxObj.getPid() + " qid=" + curActiveTxObj.getQid()
					+ " budget=" + MedusaResourceMonitor.getRemainingCapacity(curActiveTxObj.pid)
					+ " muid=" + curActiveTxObj.getMuid()
					);*/
			
			curActiveTxObj = null;
		}
 
		/* Gave 5 second interval. */
		long howlong = (long) (configSleepInterval * Math.pow(2, curFailCnt));
		if (howlong > 1000 * 60 * 60 * 1 /* 1 hour */) {
			howlong = 3600 * 1000;
		}

		Log.d(TAG, "* Wait " + howlong / 1000.0	+ " seconds for next trial.");
		//Thread.sleep(howlong);
	}
	
}
