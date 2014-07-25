package medusa.medusalet.facedetect;

import java.util.HashMap;

import android.database.sqlite.SQLiteCursor;
import medusa.mobile.client.MedusaStorageManager;
import medusa.mobile.client.MedusaTransformManager;
import medusa.mobile.client.MedusaUtil;
import medusa.mobile.client.MedusaletBase;
import medusa.mobile.client.MedusaletCBBase;

public class MedusaletMain extends MedusaletBase {

	/* Variable for the Summary Support. */
	private static final String TAG = "medusalet_FaceDetector";
	
	/* configuration variables */
	String queryHead;
	
	/* runtime variables */
	HashMap<String, String> reportMap;
	int numReport;
	
	final MedusaletCBBase cbTransformed = new MedusaletCBBase() {
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
    				
    				reportUid(uid);
    				
    				MedusaUtil.log(TAG, "* reported one face [" + path + "], remained=" + numReport);
    			} while(cr.moveToNext());
    			cr.close();
    			
    			if (--numReport <= 0) {
    				MedusaStorageManager.requestServiceUnsubscribe(runnerInstance.getName(), cbCreated, "face");
	    			quitThisMedusalet();
    			}
        	}
    	}
    };
	
    MedusaletCBBase cbCreated = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) {
    		if (data != null) {
	    		String path = (String)data;
	    		String sqlstmt = queryHead + "where path='" + path + "'";
	    		
	    		if (path.contains("faces") == true) {
		    		MedusaStorageManager.requestServiceSQLite(TAG, cbTransformed, "medusadata.db", sqlstmt);    		
		    		MedusaUtil.log(TAG, "* Faces file [" + data + "] has been created.");
	    		}
	    		else {
	    			MedusaUtil.log(TAG, "! looks another image is created: " + path);
	    		}
    		}
    		else {
    			MedusaUtil.log(TAG, "! data is null, msg=" + msg);
    		}
    	}
    };
    
    MedusaletCBBase cbDetected = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) {
    		if (data != null) {
	    		String[] args = ((String)data).split("\\|");
	    		String uid = args[0], orig = args[1], num_faces = args[2];
	    		
	    		MedusaUtil.log(TAG, "* uid=" + uid + " num_faces=" + num_faces + " remained=" + numReport);
	    		
	    		if (num_faces != null && Integer.parseInt(num_faces) == 0) {
	    			reportUid("0");
	    			if (--numReport == 0) {
	    				MedusaStorageManager.requestServiceUnsubscribe(runnerInstance.getName(), cbCreated, "face");
		    			quitThisMedusalet();
	    			}
	    		}
    		}
    		else {
    			MedusaUtil.log(TAG, "! no data field, msg=" + msg);
    		}
    	}
    };

    final MedusaletCBBase cbGet = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) 
    	{
    		MedusaUtil.log(TAG, msg);
    		
    		if (data != null) {
        		SQLiteCursor cr = (SQLiteCursor)data;
        		
        		numReport = cr.getCount();
        		MedusaStorageManager.requestServiceSubscribe(runnerInstance.getName(), cbCreated, "face");
        		
        		if (cr.moveToFirst()) {
        			do {
        				String path = cr.getString(0);
        				String type = cr.getString(1);
        				String uid = cr.getString(2);
        				
        				MedusaTransformManager.requestSummary(TAG, MedusaTransformManager.TF_TYPE_SUMMARY_FACES
        													, path, uid, cbDetected);
      					MedusaUtil.log(TAG, "* request for face detection has been made.");
        				
        			} while(cr.moveToNext());
        			
        			cr.close();
        		}
        		else {
        			MedusaUtil.log(TAG, "! may have no data => cr.moveToFirst() was null.");
        		}
    		}
    		else {
    			MedusaUtil.log(TAG, "! db queries has been processed, but data == null.");
    		}
    	}
    };
    
    @Override
	public boolean init()
	{
		reportMap = new HashMap<String, String>();
		numReport = 0;
		
		queryHead = "select path,type,uid from mediameta ";
		
		cbTransformed.setRunner(runnerInstance);
		cbDetected.setRunner(runnerInstance);
		cbCreated.setRunner(runnerInstance);
		cbGet.setRunner(runnerInstance);
		
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
				
				sqlstmt = queryHead + "where path not like \"%faces%\" and (";
				String[] uids = content.split("\\|");
				for (int j = 0; j < uids.length; j++) {
					sqlstmt += (j > 0 ? " or " : "") + "uid='" + uids[j] + "'";
				}
				sqlstmt += ") order by mtime desc";
				
				MedusaStorageManager.requestServiceSQLite(TAG, cbGet, "medusadata.db", sqlstmt);
			}
		}
		else {
			MedusaUtil.log(TAG, "* No given UIDs");
		}
		
		MedusaUtil.log(TAG, "* Request process is done.");
		
 		return true;
	}
}


