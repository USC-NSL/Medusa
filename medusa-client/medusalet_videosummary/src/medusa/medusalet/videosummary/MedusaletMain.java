package medusa.medusalet.videosummary;

import java.util.HashMap;

import android.database.sqlite.SQLiteCursor;
import medusa.mobile.client.MedusaStorageManager;
import medusa.mobile.client.MedusaTransformManager;
import medusa.mobile.client.MedusaUtil;
import medusa.mobile.client.MedusaletBase;
import medusa.mobile.client.MedusaletCBBase;

public class MedusaletMain extends MedusaletBase {

	/* Variable for the Summary Support. */
	private static final String TAG = "medusalet_VideoSummary";
	
	/* runtime variables */
	HashMap<String, String> reportMap;
	int numReported;
	String queryHead;
	
	final MedusaletCBBase cbSummarized = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) 
    	{
    		if (data == null) return; 
        	
    		MedusaUtil.log(TAG, msg);
			SQLiteCursor cr = (SQLiteCursor)data;
    		
    		if (cr.moveToFirst()) {
    			do {
    				String path = cr.getString(0);
    				String type = cr.getString(1);
    				String uid = cr.getString(2);
    				
    				if (path.endsWith(".flv") == true) {
    					reportUid(uid);
    				}
    			} while(cr.moveToNext());
    			cr.close();
    			
    			MedusaStorageManager.requestServiceUnsubscribe(TAG, cbReg, "flv");
    			quitThisMedusalet();
        	}
    	}
    };
	
    MedusaletCBBase cbReg = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) { 
    		String sqlstmt = queryHead + "where path='" + data + "'";
    		MedusaStorageManager.requestServiceSQLite(TAG, cbSummarized, "medusadata.db", sqlstmt);
    		
    		MedusaUtil.log(TAG, "* Summarized file [" + data + "] has been created.");
    	}
    };
	
    /* To generate a summary video */
    final MedusaletCBBase cbGet = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) 
    	{
    		if (data == null) return; 
        	
    		MedusaUtil.log(TAG, msg);
			SQLiteCursor cr = (SQLiteCursor)data;
    		
    		if (cr.moveToFirst()) {
    			do {
    				String path = cr.getString(0);
    				String type = cr.getString(1);
    				String uid = cr.getString(2);
    				
    				if (type.equals("video") == true) {
    					MedusaStorageManager.requestServiceSubscribe(TAG, cbReg, "flv");
    					MedusaTransformManager.requestSummary(TAG, MedusaTransformManager.TF_TYPE_SUMMARY_VIDEO
    							, path, uid, null);
    				}
    				else {
    					MedusaUtil.log(TAG, "* skip. this file is NOT a video. type=" + type);
    				}
    			} while(cr.moveToNext());
    			
    			cr.close();
        	}
    	}
    };
    
    @Override
	public boolean init()
	{
		reportMap = new HashMap<String, String>();
		numReported = 0;
		queryHead = "select path,type,uid from mediameta ";
		
		cbReg.setRunner(runnerInstance);
		cbGet.setRunner(runnerInstance);
		cbSummarized.setRunner(runnerInstance);
		
		return true;
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
					sqlstmt += (j > 0 ? "or " : "") + "uid='" + uids[j] + "'";
				}
				sqlstmt += ") order by mtime desc";
				
				MedusaStorageManager.requestServiceSQLite(TAG, cbGet, "medusadata.db", sqlstmt);
			}
			
			MedusaUtil.log(TAG, "* Request process is done.");
		}
		else {
			MedusaUtil.log(TAG, "! <input> tag should be specified..");
		}
		
		return true;
	}
}


