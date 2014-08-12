package medusa.medusalet.uploadprioritydata;

import java.util.HashMap;
import java.util.LinkedList;

import medusa.mobile.client.MedusaStorageManager;
import medusa.mobile.client.MedusaTransferManager;
import medusa.mobile.client.MedusaUtil;
import medusa.mobile.client.MedusaletBase;
import medusa.mobile.client.MedusaletCBBase;
import android.database.sqlite.SQLiteCursor;

public class MedusaletMain extends MedusaletBase 
{
	/* Variable for the Summary Support. */
	private static final String TAG = "medusalet_UploadPriorityData";
	
	String queryHead;
	LinkedList<String> uidList, pathList, reviewList;
	HashMap<String, String> reportMap;
	HashMap<String, String> uidCreditMap, uidDeadlineMap, uidStarttimeMap, uidPidMap;
	int numReported;
	
	MedusaletCBBase cbSent = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) 
    	{
    		String uid = (String)data;
    		
    		if (msg == "duplicated")
    			MedusaUtil.log(TAG, "* deleted [" + uid + "], duplicated");
    		else if (msg == "expired")
    			MedusaUtil.log(TAG, "* deleted [" + uid + "], expired");
    		else
    		MedusaUtil.log(TAG, "* uploaded [" + uid + "] seq=" + (++numReported) + " with Credit=" + (uidCreditMap.get(uid) == null ? "highest" : uidCreditMap.get(uid)) + " with Deadline=" + (uidDeadlineMap.get(uid) == null ? "-1" : uidDeadlineMap.get(uid)));
    		
    		if (uid != null) {
    			reportUid(uid);
    			reportMap.remove(uid);
    			if (reportMap.size() == 0) quitThisMedusalet();
    		}
    	}
    };
    
    MedusaletCBBase cbGet = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) 
    	{
    		String uids = "";
    		String paths = "";
    		String reviews = "";
    		
    		if (data != null) {
        		SQLiteCursor cr = (SQLiteCursor)data;
        		
        		/* send raw video files */
        		if (cr.moveToFirst()) {
        			do {
        				String path = cr.getString(0 /* column index */);
        				String type = cr.getString(1 /* column index */);
        				String fsize = cr.getString(2 /* column index */);
        				String uid = cr.getString(3 /* column index */);
        				String review = cr.getString(4 /* column index */);
        				
        				/* review may be null */
        				review = (review == null ? "N/A" : review);
        				        	    		
        				/* Here we should create tuple uploading features. */
        				uids = (uids.length() == 0 ? uid : uids + "|" + uid) + ":" + fsize + (uidCreditMap.get(uid) != null ? ":" + uidCreditMap.get(uid) : "") + (uidDeadlineMap.get(uid) != null ? ":" + uidDeadlineMap.get(uid) : "") + (uidStarttimeMap.get(uid) != null ? ":" + uidStarttimeMap.get(uid) : "") + (uidPidMap.get(uid) != null ? ":" + uidPidMap.get(uid) : "");
        				paths = (paths.length() == 0 ? path : paths + "|" + path);
        				reviews = (reviews.length() == 0 ? review : reviews + "|" + review);
        				reportMap.put(uid, path);
        			} while(cr.moveToNext());
        			
        			cr.close();
        			
        			/* aggregated upload request */
        			MedusaTransferManager.requestUpload(runnerInstance.getMedusaletInstance(), uids, paths, reviews, cbSent);
        			//String uid = uidList.getFirst();
        			//MedusaTransferManager.requestUpload(runnerInstance.getMedusaletInstance(), uid + ":" + uidPriorityMap.get(uid), pathList.getFirst(), reviewList.getFirst(), cbSent);
        			MedusaUtil.log(TAG, "* Tx req. on [" + uids + "] has made.");
        		}
        		else {
        			MedusaUtil.log(TAG, "! QueryRes: may not have any data. quiting the uploader medusalet.");
        			quitThisMedusalet();
        		}
    		}
    		else {
    			MedusaUtil.log(TAG, "! QueryRes: the query has been executed, but data == null ?!");
    		}
    	}
    };
            
    @Override
    public void exit()
    {
  /*  	String pendingUids = "";
    	for (String uid : pendingList)
    		pendingUids += uid + "|";
    	pendingUids = pendingUids.substring(0, pendingUids.length() - 1);
    	MedusaTransferManager.cancelUpload(runnerInstance.getMedusaletInstance(), pendingUids);*/
    	super.exit();
    }
    
    @Override
	public boolean init()
	{
		boolean ret = true;
		
		queryHead = "select path,type,fsize,uid,review from mediameta ";
		reportMap = new HashMap<String, String>();
		uidCreditMap = new HashMap<String, String>();
		uidDeadlineMap = new HashMap<String, String>();
		uidStarttimeMap = new HashMap<String, String>();
		uidPidMap = new HashMap<String, String>();
		numReported = 0;
		
		cbSent.setRunner(runnerInstance);
		cbGet.setRunner(runnerInstance);
		
		return ret;
	}

	@Override
    public boolean run() 
	{
		MedusaUtil.log(TAG, "* Started [" + TAG + "]");
        
		String[] input_keys = this.getConfigInputKeys();
		String sqlstmt;

		if (input_keys.length > 0) {
			for (int i = 0 ; i < input_keys.length; i++) {
				/* requested specific data list */
				String content = this.getConfigInputData(input_keys[i]);
				MedusaUtil.log(TAG, "* requested data tag=" + input_keys[i] + " uids= " + content);
				
				sqlstmt = queryHead + "where (";
				String[] uids = content.split("\\|");
				
				if ((uids.length == 2) && (Integer.valueOf(uids[0]) < 0))
				{
					MedusaTransferManager.uploadScheme = Integer.valueOf(uids[0]);
					MedusaTransferManager.windowLength = Integer.valueOf(uids[1]);
					MedusaUtil.log(TAG, "* Change uploadScheme to [" + uids[0] + "].");
					quitThisMedusalet();
				}
					
				for (int j = 0; j < uids.length; j++) {
					String[] uid = uids[j].split(":"); // ignore priority after ":"
					if (uid.length > 1) 
					{
						uidCreditMap.put(uid[0], uid[1]);
						uidDeadlineMap.put(uid[0], uid[2]);
						if (uid.length > 3)
						{
							uidStarttimeMap.put(uid[0], uid[3]);
							uidPidMap.put(uid[0], uid[4]);
						}
					}
					sqlstmt += (j > 0 ? " or " : "") + "uid='" + uid[0] + "'";
				}
				sqlstmt += ") order by mtime desc";
				
				MedusaStorageManager.requestServiceSQLite(TAG, cbGet, "medusadata.db", sqlstmt);
			}
		}
		else {
			/* request all data. */
			MedusaUtil.log(TAG, "* request all data available on the phone");
			
			sqlstmt = queryHead + "order by mtime desc";
	        MedusaStorageManager.requestServiceSQLite(TAG, cbGet, "medusadata.db", sqlstmt);
		}
		
		MedusaUtil.log(TAG, "* Request process is done.");

 		return true;
	}

}


