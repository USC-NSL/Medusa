package medusa.medusalet.mediagen;

import java.util.HashMap;

import android.database.sqlite.SQLiteCursor;
import medusa.mobile.client.MedusaStorageManager;
import medusa.mobile.client.MedusaUtil;
import medusa.mobile.client.MedusaletBase;
import medusa.mobile.client.MedusaletCBBase;

public class MedusaletMain extends MedusaletBase {

	/* Variable for the Summary Support. */
	private static final String TAG = "medusalet_MediaDataGenerator";
	
	/* configuration variables */
	String opType;
	
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
        				String uid = cr.getString(1);
        				
        				MedusaUtil.requestReviewForRawData(runnerInstance.getMedusaletInstance(), path, uid);
        				reportUid(uid);
        			} while(cr.moveToNext());
        			cr.close();
        			
        			/* Unless -s option is set as "notification", exit the medusalet */
        			String stop_cond = getConfigParams("-s");
        			if (stop_cond != null && stop_cond.equals("notification") == true) {
        				;
        			}
        			else if (--numReported <= 0) {
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
    
    /* Stage 1a: subscribe "media" contents to the StorageManager */
    MedusaletCBBase cbReg = new MedusaletCBBase() { 
    	public void cbPostProcess(Object data, String msg) { 
    		queryMetaData("where path='" + data + "'");
    		
    		MedusaUtil.log(TAG, "* new file [" + data + "] has been created.");
    	}
    };
    
    private void queryMetaData(String sqllast) {
    	String sqlstmt = "select path,uid from mediameta " + sqllast; 
		MedusaStorageManager.requestServiceSQLite(TAG, cbGet, "medusadata.db", sqlstmt);
    }
	
    @Override
	public boolean init()
	{
		reportMap = new HashMap<String, String>();
		
		cbReg.setRunner(runnerInstance);
		cbGet.setRunner(runnerInstance);
		
		String num = getConfigParams("-n");
		numReported = (num != null ? Integer.parseInt(num) : 1);
		
		opType = this.getConfigParams("-t") == null ? "media" : this.getConfigParams("-t");
		MedusaUtil.log(TAG, "* optype=" + opType);
		
		return true;
	}
	
	@Override
	public void exit()
	{
		super.exit();
		MedusaStorageManager.requestServiceUnsubscribe(TAG, cbReg, opType);
	}
	
	@Override
    public boolean run() 
	{
		MedusaUtil.log(TAG, "* Started..");
		MedusaStorageManager.requestServiceSubscribe(TAG, cbReg, opType);
		MedusaUtil.invokeSensingApp(opType);
		MedusaUtil.log(TAG, "* Start to monitor [" + opType + "] type.");
		
 		return true;
	}
}


