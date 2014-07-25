package medusa.medusalet.probedata;

import java.util.HashMap;

import android.database.sqlite.SQLiteCursor;
import medusa.mobile.client.MedusaStorageManager;
import medusa.mobile.client.MedusaUtil;
import medusa.mobile.client.MedusaletBase;
import medusa.mobile.client.MedusaletCBBase;

public class MedusaletMain extends MedusaletBase {

	/* Variable for the Summary Support. */
	private static final String TAG = "medusalet_ProbeData";
	
	/* configuration variables */
	String sqlLimit;
	String sqlOffset;
	String sqlTimeFrom;
	String sqlTimeTo;
	String sqlType;
	
	/* runtime variables */
	HashMap<String, String> reportMap;
	int numReported;
	
    final MedusaletCBBase cbGet = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) 
    	{
    		MedusaUtil.log(TAG, msg);
    		
    		if (data != null) {
        		SQLiteCursor cr = (SQLiteCursor)data;
        		
        		if (cr.moveToFirst()) {
        			do {
        				String path = cr.getString(0);
        				String mtime = cr.getString(1);
        				String uid = cr.getString(2);
        				
        				MedusaUtil.log(TAG, "* uid=" + uid + ", mtime=" + mtime + ", path=" + path);
        				reportUid(uid);
        			} while(cr.moveToNext());
        			cr.close();
        			
        			/* termination condition */
        			if (isStopCondSetAs("notification") == false) {
    					quitThisMedusalet();
    				}
        		}
        		else {
        			MedusaUtil.log(TAG, "! cr.moveToFirst() == null.");
        		}
    		}
    		else {
    			MedusaUtil.log(TAG, "! data == null.");
    		}
    	}
    };
    
    @Override
	public boolean init()
    {	
		reportMap = new HashMap<String, String>();
		
		cbGet.setRunner(runnerInstance);
	
		/* default values */
		sqlTimeFrom = this.getConfigParams("-from");
		sqlTimeTo = this.getConfigParams("-to");
		sqlLimit = this.getConfigParams("-limit");
		sqlOffset = this.getConfigParams("-offset");
		sqlType = this.getConfigParams("-type");
    	
		return true;
	}
		
	@Override
    public boolean run() 
	{
		MedusaUtil.log(TAG, "* Started..");
		
		/* make a query statement */
		String sqlstmt = "select path,mtime,uid from mediameta";
		boolean need_end = false;
		if (sqlTimeFrom != null || sqlTimeTo != null || sqlType != null) {
			sqlstmt += " where";
			if (sqlTimeFrom != null) {
				if (need_end) sqlstmt += " and";
				sqlstmt += " mtime >= '" + MedusaUtil.convertStrTimeToLong(sqlTimeFrom) + "'";
				need_end = true;
			}
			if (sqlTimeTo != null) {
				if (need_end) sqlstmt += " and";
				sqlstmt += " mtime <= '" + MedusaUtil.convertStrTimeToLong(sqlTimeTo) + "'";
				need_end = true;
			}
			if (sqlType != null) {
				if (need_end) sqlstmt += " and";
				sqlstmt += " type = '" + sqlType + "'";
				need_end = true;
			}
		}
		if (sqlLimit != null) {
			sqlstmt += " limit " + sqlLimit;
			if (sqlOffset != null) sqlstmt += " offset " + sqlOffset;
		}
		
		MedusaUtil.log(TAG, "* sqlstmt: " + sqlstmt);
		MedusaStorageManager.requestServiceSQLite(TAG, cbGet, "medusadata.db", sqlstmt);
		
 		return true;
	}
}


