package medusa.medusalet.uploaddata;

import java.util.HashMap;

import medusa.mobile.client.MedusaStorageManager;
import medusa.mobile.client.MedusaTransferManager;
import medusa.mobile.client.MedusaUtil;
import medusa.mobile.client.MedusaletBase;
import medusa.mobile.client.MedusaletCBBase;

import android.database.sqlite.SQLiteCursor;

public class MedusaletMain extends MedusaletBase 
{
	/* Variable for the Summary Support. */
	private static final String TAG = "medusalet_UploadData";
	
	String queryHead;
	HashMap<String, String> reportMap;
	int numReported;
	
	MedusaletCBBase cbSent = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) 
    	{
    		String fpath = (String)data;
    		String uid = reportMap.get(fpath); 
    		
    		MedusaUtil.log(TAG, "* sent [" + fpath + "] seq=" + (++numReported) );
    		
    		if (uid != null) {
    			reportUid(uid);
    			reportMap.remove(fpath);
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
        				uids = (uids.length() == 0 ? uid : uids + "|" + uid);
        				paths = (paths.length() == 0 ? path : paths + "|" + path);
        				reviews = (reviews.length() == 0 ? review : reviews + "|" + review);
        				reportMap.put(path, uid);
        			} while(cr.moveToNext());
        			
        			cr.close();
        			
        			/* aggregated upload request */
        			MedusaTransferManager.requestUpload(runnerInstance.getMedusaletInstance(), uids, paths, reviews, cbSent);
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
	public boolean init()
	{
		boolean ret = true;
		
		queryHead = "select path,type,fsize,uid,review from mediameta ";
		reportMap = new HashMap<String, String>();
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
				for (int j = 0; j < uids.length; j++) {
					sqlstmt += (j > 0 ? " or " : "") + "uid='" + uids[j] + "'";
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


