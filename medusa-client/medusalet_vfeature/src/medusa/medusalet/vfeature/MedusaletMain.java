package medusa.medusalet.vfeature;

import java.util.ArrayList;
import java.util.HashMap;

import android.database.sqlite.SQLiteCursor;
import medusa.mobile.client.MedusaSoundFrameManager;
import medusa.mobile.client.MedusaStorageManager;
import medusa.mobile.client.MedusaStorageTextFileAdapter;
import medusa.mobile.client.MedusaTransformFeatureAdapter;
import medusa.mobile.client.MedusaTransformMFCCAdapter;
import medusa.mobile.client.MedusaUtil;
import medusa.mobile.client.MedusaletBase;
import medusa.mobile.client.MedusaletCBBase;
import medusa.mobile.client.math.Matrix;

public class MedusaletMain extends MedusaletBase {
	
	public int APP_TYPE_SOUND = 0;
	public int APP_TYPE_ACC = 1;
	public int app_type;
	
	public String base_path = MedusaUtil.getRootPath() + "/medusa_text_file/bin/";
		
	private String MYNAME = "medusalet_vFeature";
	
	String df_fmt = "#.#####";
	
	HashMap<String, String> reportMap;
	HashMap<String, String> reviewMap;
	String op_type;
	String sub_config;  
    
	MedusaletCBBase cbRetrive = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) 
    	{   		
    		if (data != null) {
    			
        		SQLiteCursor cr = (SQLiteCursor)data;
        		String path = null;
        		String uid = null;
        		
        		if (cr.moveToFirst()) {
        			do {
        				path = cr.getString(0);
        				uid = cr.getString(3);
        				reportMap.remove(path);    					
        			} while(cr.moveToNext());
        			
        			cr.close();  			
        		}
        		else {
        			MedusaUtil.log(MYNAME, "! may have no data => cr.moveToFirst() was null");
        		}
        		
        		/*
        		for (Map.Entry<String, String> a:tmpMap.entrySet()) {      			
        			MedusaStorageManager.updateReviewColumnOnDB(a.getKey(), a.getValue(), reviewMap.get(a.getKey()));
        			reportUid(a.getValue());
        		}
        		*/
        		
        		if (uid != null) {      		
	    			MedusaStorageManager.updateReviewColumnOnDB(path, uid, reviewMap.get(path));
	    			reportUid(uid);        		
	        		if (reportMap.isEmpty()) 
	        			quitThisMedusalet();
        		}
        		
    		}
    		else {
    			MedusaUtil.log(MYNAME, "* Step 2: db queries has been processed, but data == null");
    		} 		
    		
    		MedusaUtil.log(MYNAME, "* step6: feature file retrive"); 
    	}
    };    
    	
    MedusaletCBBase cbGet = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) 
    	{ 
    		
    		if (data != null) {
    			 			
        		SQLiteCursor cr = (SQLiteCursor)data;    		
        		/* send raw video files */
        		if (cr.moveToFirst()) {
        			do {
        				String path = cr.getString(0);
        				String review = cr.getString(4);
        				
        				ArrayList<double[]> tmp = new ArrayList<double[]>();
        				
        		        String[] read = MedusaStorageTextFileAdapter.read(path).split("\n");
        		        
        				for (int i = 0; i < read.length; i++) {
        					
		                	String[] read_set = read[i].split(" ");
		                	
		                	double[] read_num = new double[read_set.length];		                	
		                	for (int j = 0; j < read_set.length; j++)
		                		read_num[j] = Double.parseDouble(read_set[j]);
		                   
		                	tmp.add(read_num);       					
        				}     				
        				
        				double[][] input_r = new double[tmp.size()][];      				
        				for (int i = 0; i < input_r.length; i++)
        				{
        					double[] tmp_d = tmp.get(i);
        					input_r[i] = tmp_d;
        				}
        				
        				
        				double[][] r = null;
        				
        				if (app_type == APP_TYPE_SOUND) {
        					
        					if (sub_config.equals("default") || sub_config.equals("mfcc")) {       					
        						MedusaTransformMFCCAdapter mfcc = new MedusaTransformMFCCAdapter(MedusaSoundFrameManager.sampleRate, MedusaSoundFrameManager.winLen, 13, 
        							true, 20, 4000, 40, MedusaSoundFrameManager.overlapWinLen);
        						
								r = mfcc.process(input_r[0]);	
								
								if (r == null) {
									
									MedusaUtil.log(MYNAME, "* wrong parameter in mfcc transformation..");
									return;
								}
        					}
        					
        					MedusaUtil.log(MYNAME, "* step 5: Sound feature extraction");
        				} 
        				else if (app_type == APP_TYPE_ACC) {
        					MedusaTransformFeatureAdapter atf = new MedusaTransformFeatureAdapter(sub_config);       					
        					r = atf.extract(input_r);
        					MedusaUtil.log(MYNAME, "* step 5: Acc feature extraction");
        				}
        				
        				Matrix rm = new Matrix(r);
        				String[] r_s = rm.writeToFeatureVector(df_fmt);
        				  				
        				//MedusaUtil.log(TAG, "* step 999: " + r_s[0] + ", " + r_s[1]);       				
        	    	
        				// write to file
        				String[] tmp_set = path.split("/");
        	    		String file_name = "feature_" + tmp_set[tmp_set.length - 1];
        	    		String feature_path = base_path + file_name;
        	    		
        	    		String sqlstmt = "select path,type,fsize,uid,review from mediameta where path='" + feature_path + "'";  		
        	    		reportMap.put(feature_path, sqlstmt);
        	    		reviewMap.put(feature_path, review);
        	    		
        	    		//MedusaUtil.log(MYNAME, "*********************** step zzzz:" + sqlstmt);
        	    		
        	    		MedusaStorageTextFileAdapter.write(feature_path, r_s, false); 
        				
        			} while(cr.moveToNext());
        			
        			
        			cr.close();
        			cr = null;
        			
        			
        		}
        		else {
        			MedusaUtil.log(MYNAME, "* [cbGet] step 5: may not have any data.");
        		}
    		}
    		else {
    			MedusaUtil.log(MYNAME, "* [cbGet] step 5: db queries has been processed, but data == null.");
    		}
    	}
    };
    
    MedusaletCBBase cbReg = new MedusaletCBBase() { 
    	public void cbPostProcess(Object data, String msg) { 		
    		if (reportMap.containsKey(data)) {  			
    			MedusaStorageManager.requestServiceSQLite(MYNAME, cbRetrive, "medusadata.db", reportMap.get(data)); 			
    		}
    	}
    };
    
	@Override
	public void exit()
	{
		super.exit();		
		MedusaStorageManager.requestServiceUnsubscribe(MYNAME, cbReg, "text");
	}    
    
    @Override
	public boolean init()
	{
		boolean ret = true;
		
		reportMap = new HashMap<String, String>();
		reviewMap = new HashMap<String, String>();
		
		cbRetrive.setRunner(runnerInstance);
		cbGet.setRunner(runnerInstance);
		cbReg.setRunner(runnerInstance);
		
		op_type = this.getConfigParams("-t");
		
		if (op_type.equals("acc")) {
			
			app_type = APP_TYPE_ACC;
			
		} else if (op_type.equals("sound")) {
			
			app_type = APP_TYPE_SOUND;
			
		} else {
			
			MedusaUtil.log(MYNAME, "! error request type");
			return false;
		}
		
		sub_config = this.getConfigParams("-v");		
		return ret;
	}    
	
	@Override
    public boolean run() 
	{
		
		String[] input_keys = this.getConfigInputKeys();
		String sqlstmt;
		
		MedusaStorageManager.requestServiceSubscribe(MYNAME, cbReg, "text");
        
		if (input_keys.length > 0) {			
			for (int i = 0 ; i < input_keys.length; i++) {
				/* requested specific data list */
				String content = this.getConfigInputData(input_keys[i]);
				MedusaUtil.log(MYNAME, "* requested data tag=" + input_keys[i] + " uids= " + content);
				
				sqlstmt = "select path,type,fsize,uid,review from mediameta where (";
				String[] uids = content.split("\\|");
				for (int j = 0; j < uids.length; j++) {
					sqlstmt += (j > 0 ? "or " : "") + "uid='" + uids[j] + "'";
				}
				sqlstmt += ") order by mtime desc";
				
				MedusaStorageManager.requestServiceSQLite(MYNAME, cbGet, "medusadata.db", sqlstmt);
			}
		}
		else {
			/* request all data. */
			MedusaUtil.log(MYNAME, "* request all data");
			
			sqlstmt = "select path,type,fsize,uid,review from mediameta order by mtime desc";
	        MedusaStorageManager.requestServiceSQLite(MYNAME, cbGet, "medusadata.db", sqlstmt);
		}
        
		MedusaUtil.log(MYNAME, "* step 4: Request process is done.");

 		return true;
	}	
}
