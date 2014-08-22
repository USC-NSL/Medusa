package medusa.medusalet.vcollect;

import java.util.HashMap;

import android.database.sqlite.SQLiteCursor;
import medusa.mobile.client.MedusaAccelManager;
import medusa.mobile.client.MedusaObserverManager;
import medusa.mobile.client.MedusaSoundFrameManager;
import medusa.mobile.client.MedusaStorageManager;
import medusa.mobile.client.MedusaStorageTextFileAdapter;
import medusa.mobile.client.MedusaletBase;
import medusa.mobile.client.MedusaletCBBase;
import medusa.mobile.client.MedusaUtil;

public class MedusaletMain extends MedusaletBase {

	public int APP_TYPE_SOUND = 0;
	public int APP_TYPE_ACC = 1;
	public int app_type;
	
	private String MYNAME = "medusalet_vCollect";
	HashMap<String, String> reportMap;
	String df_fmt = "#.#####";
	
	String op_type;
	String op_label;
	Boolean op_raw;
	int op_interval;
	int op_frameLength;
	int op_count;
	
	public String base_path =  MedusaUtil.getRootPath() + "/medusa_text_file/bin/"; 
    
    final MedusaletCBBase cbGet = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) 
    	{  		
    		if (data != null) {
        		SQLiteCursor cr = (SQLiteCursor)data;
        		
        		if (cr.moveToFirst()) {
        			do {
        				String path = cr.getString(0);
        				String uid = cr.getString(1);
        				
        				MedusaUtil.requestReviewForRawData(runnerInstance.getMedusaletInstance(), path, uid);      	    			
        	    		reportUid(uid);
        	    		MedusaUtil.log(MYNAME, "* step 3: Acc Raw data Report " + uid);	
        	    		
        			} while(cr.moveToNext());
        			
        			cr.close();
        			   			
        		}
        		else {
        			MedusaUtil.log(MYNAME, "! may have no data => cr.moveToFirst() was null");
        		}
    		}
    		else {
    			MedusaUtil.log(MYNAME, "* Step 2: db queries has been processed, but data == null");
    		}
    	}
    };    
	
	MedusaletCBBase cbCapture = new MedusaletCBBase() { 
    	public void cbPostProcess(Object data, String msg) { 

    		String path = "";
    		if (app_type == APP_TYPE_SOUND) {
    		
	    		double[] s = (double[])data;
	    		
	    		// write to file
	    		String file_name = MedusaStorageManager.generateTimestampFilename(op_label + "_sound");
	    		path = base_path + file_name;
	    		reportMap.put(path, "");
	    		MedusaStorageTextFileAdapter.write(path, s, df_fmt, false);
	    		
	    		s = null;
	    		MedusaUtil.log(MYNAME, "* step 1: Sound raw data");	    		
	    		
    		} else if (app_type == APP_TYPE_ACC) {
    			
    			if (op_raw) {
    				
    	    		MedusaAccelManager.MedusaAccelDataStructure[][] s = (MedusaAccelManager.MedusaAccelDataStructure[][])data;    		  			
    	    		int s_row_dim = s.length;
    	    		int s_col_dim = s[0].length;   		
    	    		String filename = op_label + "_acc_" + s[0][0].timeTick + "_" + s[s_row_dim-1][s_col_dim-1].timeTick + ".txt";
    	    		path = base_path + filename;			
    	    		reportMap.put(path, "");
    				MedusaStorageTextFileAdapter.write(path, s, df_fmt, false);
    	    		
    	    		s = null;
    	    		MedusaUtil.log(MYNAME, "* step1: Acc raw data - true");
    			} else {
    			
	        		double[][] ss = (double[][])data;       		
	        		// write to file
	        		String file_name = MedusaStorageManager.generateTimestampFilename(op_label + "_acc");
	        		path = base_path + file_name;
	        		reportMap.put(path, "");
	        		MedusaStorageTextFileAdapter.write(path, ss, df_fmt, false);   	
	        		
	        		ss = null;
	        		
	        		MedusaUtil.log(MYNAME, "* step1: Acc raw data - false");
    			}
    		}
    		   		
    	}
    };
    
    MedusaletCBBase cbReg = new MedusaletCBBase() { 
    	public void cbPostProcess(Object data, String msg) {
    		
    		if (reportMap.containsKey(data)) {
    			String sqlstmt = "select path,uid from mediameta where path='" + data + "'"; 			
    			MedusaStorageManager.requestServiceSQLite(MYNAME, cbGet, "medusadata.db", sqlstmt); 		
    			MedusaUtil.log(MYNAME, "* step 2: request file");
    		}
    	}
    };	    
    
	@Override
	public void exit()
	{
		super.exit();
		
		MedusaStorageManager.requestServiceUnsubscribe(MYNAME, cbReg, "text");
		
		if (app_type == APP_TYPE_SOUND) {
			
			MedusaSoundFrameManager.deRequestService(MYNAME);
		}
		else if (app_type == APP_TYPE_ACC) {
			MedusaAccelManager.deRequestService(MYNAME);			
		}
	}
	
    @Override
	public boolean init()
	{	
		reportMap = new HashMap<String, String>();
		
		cbCapture.setRunner(runnerInstance);
		cbGet.setRunner(runnerInstance);
		cbReg.setRunner(runnerInstance);
		
		op_type = this.getConfigParams("-t");
		
		if (op_type.equals("acc")) {
			
			app_type = APP_TYPE_ACC;
			op_raw = Boolean.parseBoolean(this.getConfigParams("-r"));
			op_interval = Integer.parseInt(this.getConfigParams("-i"));
			op_frameLength = Integer.parseInt(this.getConfigParams("-f"));
			op_count = Integer.parseInt(this.getConfigParams("-c"));
			
		} else if (op_type.equals("sound")) {
		
			app_type = APP_TYPE_SOUND;
			op_frameLength = Integer.parseInt(this.getConfigParams("-f"));
		} else {
			MedusaUtil.log(MYNAME, "! error request type");
			return false;
		}
		
		op_label = this.getConfigParams("-l");
		
		return true;
	}
		
	@Override
    public boolean run() 
	{		
		if (app_type == APP_TYPE_SOUND) {
			MedusaObserverManager.getInstance();
			MedusaStorageManager.requestServiceSubscribe(MYNAME, cbReg, "text");			
			MedusaSoundFrameManager.requestService(MYNAME, cbCapture, op_frameLength, "periodic", "locale", "");
		} else if (app_type == APP_TYPE_ACC) {
			MedusaObserverManager.getInstance();		
			MedusaStorageManager.requestServiceSubscribe(MYNAME, cbReg, "text");
			MedusaAccelManager.requestService(MYNAME, cbCapture, op_interval, op_frameLength, op_count, "periodic", "locale", "", op_raw);
		}
		
        return true;		
	}
}
