package medusa.medusalet.gpsrawcollect;

import java.util.HashMap;

import android.database.sqlite.SQLiteCursor;
import medusa.mobile.client.MedusaGpsManager;
import medusa.mobile.client.MedusaObserverManager;
import medusa.mobile.client.MedusaStorageManager;
import medusa.mobile.client.MedusaStorageTextFileAdapter;
import medusa.mobile.client.MedusaUtil;
import medusa.mobile.client.MedusaletBase;
import medusa.mobile.client.MedusaletCBBase;

public class MedusaletMain extends MedusaletBase {
	
	private String MYNAME = "medusalet_GpsRawCollect";
	
	public String df_fmt = "#.#####";	
	public String base_path =  MedusaUtil.getRootPath() + "/medusa_text_file/bin/";
	
	int numReported;
	HashMap<String, String> reportMap;
	
	String op_type;
	String op_label;
	int interval;
	int current_num;
	int batch_num;
	double[][] buffer;	
	
    final MedusaletCBBase cbGet = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) 
    	{  		
    		if (data != null) {
        		SQLiteCursor cr = (SQLiteCursor)data;
        		
        		if (cr.moveToFirst()) {
        			do {
        				String uid = cr.getString(3);
        				
        	    		if (uid != null) {
        	    			reportUid(uid);
        	    			MedusaUtil.log(MYNAME, "* step 3: GPS raw data Report " + uid);
        	    			numReported++;
        	    		}
        				
        				
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
	
	
	MedusaletCBBase cbCaptureGps = new MedusaletCBBase() { 
    	public void cbPostProcess(Object data, String msg) {  		
    		double[] s = ((double[][])data)[0];
    		
    		if (current_num < batch_num) {
    			
    			buffer[current_num] = s;    			
    			current_num++;   			
    		} else {
    			
    			buffer[batch_num-1][0] = System.currentTimeMillis();
    			
        		String filename = op_label + "_gps_" + (long)buffer[0][0] + "_" + (long)buffer[batch_num-1][0] + ".txt";
        		String path = base_path + filename;	
        		reportMap.put(path, "");
    			MedusaStorageTextFileAdapter.write(path, buffer, df_fmt, false);		
        		
    			buffer = new double[batch_num][];
    			current_num = 0;
    		}
    		
    		
    		
    		s = null;
    	}
    };
    
    MedusaletCBBase cbReg = new MedusaletCBBase() { 
    	public void cbPostProcess(Object data, String msg) { 
    		/*
    		 * Whenever new metadata is inserted to the metadata db.
    		 * This function will be called.
    		 */		
    		if (reportMap.containsKey((String)data)) {
    			String sqlstmt = "select path,type,fsize,uid,review from mediameta where path='" + data + "'"; 			
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
		MedusaGpsManager.deRequestService(MYNAME);	
	}
	    
	
    @Override
	public boolean init()
	{
		boolean ret = true;
		
		numReported = 0;
		reportMap = new HashMap<String, String>();
		
		cbCaptureGps.setRunner(runnerInstance);
		cbGet.setRunner(runnerInstance);
		cbReg.setRunner(runnerInstance);
		
		op_type = this.getConfigParams("-t");
		op_label = this.getConfigParams("-l");
		interval = Integer.parseInt(this.getConfigParams("-i"));
		batch_num = Integer.parseInt(this.getConfigParams("-c"));
		buffer = new double[batch_num][];
		current_num = 0;
		
		return ret;
	}    
	
	@Override
    public boolean run() 
	{		
		
		if (op_type.equals("gps") || op_type.equals("wps") || op_type.equals("hps")) {
			
			MedusaObserverManager.getInstance();		
			MedusaStorageManager.requestServiceSubscribe(MYNAME, cbReg, "text");	
			
			
			MedusaGpsManager.requestService(MYNAME, cbCaptureGps, interval, 0,  1, "periodic", "locale", "", op_type);
				
		} else {			
			MedusaUtil.log(MYNAME, "! error request type");
			return false;
		}
		
		return true;
		
	}	

}
