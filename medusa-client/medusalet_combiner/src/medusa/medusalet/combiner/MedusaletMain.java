package medusa.medusalet.combiner;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import android.database.sqlite.SQLiteCursor;
import medusa.mobile.client.MedusaObserverManager;
import medusa.mobile.client.MedusaStorageManager;
import medusa.mobile.client.MedusaStorageTextFileAdapter;
import medusa.mobile.client.MedusaUtil;
import medusa.mobile.client.MedusaletBase;
import medusa.mobile.client.MedusaletCBBase;

public class MedusaletMain extends MedusaletBase {
	
	private String MYNAME = "medusalet_Combiner";
	
	public String combine_type_1 = "";
	public String combine_type_2 = "";
	
	public String df_fmt = "#.#####";	
	public String base_path = MedusaUtil.getRootPath() + "/medusa_text_file/bin/";
	
	HashMap<String, String> reportMap;
	
	String op_type;
	String op_label;
	
	public class tse {
		public long s;
		public long e;		
	}
	
	
	MedusaletCBBase cbRetrive = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) 
    	{
    		if (data != null) {
    			
        		SQLiteCursor cr = (SQLiteCursor)data;    		
        		if (cr.moveToFirst()) {
        			do {
        				String path = cr.getString(0);
        				String uid = cr.getString(3);
        				
        	    		if (uid != null) {
        	    			reportUid(uid);
        	    			reportMap.remove(path);
        	    		}
    					
        			} while(cr.moveToNext());
        			
        			cr.close();
        		}
        		else {
        			MedusaUtil.log(MYNAME, "! may have no data => cr.moveToFirst() was null");
        		}
        		
    			if (reportMap.size() == 0) quitThisMedusalet();
        		
    		}
    		else {
    			MedusaUtil.log(MYNAME, "* Step 2: db queries has been processed, but data == null");
    		} 		
    		
    		MedusaUtil.log(MYNAME, "* step6: feature file retrive"); 
    	}
    };	
	
	
    MedusaletCBBase cbReg = new MedusaletCBBase() { 
    	public void cbPostProcess(Object data, String msg) { 
    		/*
    		 * Whenever new metadata is inserted to the metadata db.
    		 * This function will be called.
    		 */		
    		if (reportMap.containsKey(data)) {
    			String sqlstmt = "select path,type,fsize,uid,review from mediameta where path='" + data + "'"; 			
    			MedusaStorageManager.requestServiceSQLite(MYNAME, cbRetrive, "medusadata.db", sqlstmt); 		
    			MedusaUtil.log(MYNAME, "* step 2: request file");
    		}
    	}
    }; 
	
    MedusaletCBBase cbGet = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) 
    	{ 
    		
    		if (data != null) {
    			
    			HashMap<String, tse> acc_file_set = new HashMap<String, tse>();
    			HashMap<String, tse> gps_file_set = new HashMap<String, tse>();
    			
        		SQLiteCursor cr = (SQLiteCursor)data;
        		
        		if (cr.moveToFirst()) {
        			do {
        				String path = cr.getString(0);
        				
        				if (path.contains(combine_type_1)) {    					
        					int tmp_idx = path.lastIndexOf(combine_type_1);
        					String[] tmp = path.substring(tmp_idx+combine_type_1.length()+1).split("\\.")[0].split("_");
        					tse t = new tse();
        					t.s = Long.parseLong(tmp[0]);
        					t.e = Long.parseLong(tmp[1]); 					
        					acc_file_set.put(path, t);      					
        				} else if (path.contains(combine_type_2)) {
        					int tmp_idx = path.lastIndexOf(combine_type_2);
        					String[] tmp = path.substring(tmp_idx+combine_type_2.length()+1).split("\\.")[0].split("_");
        					tse t = new tse();
        					t.s = Long.parseLong(tmp[0]);
        					t.e = Long.parseLong(tmp[1]); 	     				
        					gps_file_set.put(path, t);
        				}
        				
        				
        			} while(cr.moveToNext());      			
        			cr.close();
        			  			
        			for (Map.Entry<String, tse> a:acc_file_set.entrySet()){ 
        				      				
        				TreeMap<Long, String> hm = new TreeMap<Long, String> (); 				
        				for (Map.Entry<String, tse> b:gps_file_set.entrySet()) {      					
        					if ( b.getValue().e >= a.getValue().s || b.getValue().s <= a.getValue().e) {
        						
        				        String[] read = MedusaStorageTextFileAdapter.read(b.getKey()).split("\n");
        				        
        				        
        				        for (int i = 0; i < read.length; i++) {
        				        	
				                	String[] e = read[i].split(" ", 2);
				                	Long tick = Long.parseLong(e[0]);
				                	
				                	if (tick >= a.getValue().s && tick <= a.getValue().e) {
				                		hm.put(tick, e[1]);
				                	}        				        	
        				        }   
        				        
        					}     					
        				}
        				
        				
        				ArrayList<String> hn = new ArrayList<String>(); 
        				
				        String[] read = MedusaStorageTextFileAdapter.read(a.getKey()).split("\n");
		                for (int i = 0; i < read.length; i++) {        				                	
		                	String[] e = read[i].split(" ", 2);
		                	Long tick = Long.parseLong(e[0]);
		                	
		                	Long previous = new Long(0);
		                	Long next = new Long(0);
		                	for (Map.Entry<Long, String> c:hm.entrySet()) {
		                						                		
		                		if (c.getKey() < tick) {				                			
		                			previous = c.getKey();				                		
		                		} else if (c.getKey()>= tick) {
		                			next = c.getKey();
		                			break;
		                		}				                		
		                	}
		                	
		                	Long final_tick = new Long(0);
		                	if (previous == 0 && next != 0) {
		                		final_tick = next;
		                	} else if (previous !=0 && next == 0) {
		                		final_tick = previous;
		                	} else if (previous !=0 && next != 0) {
		                		Long diff1 = tick-previous;
		                		Long diff2 = next - tick;				                		
		                		final_tick = diff1 < diff2 ? previous:next;
		                	}
		                	
		                	if (final_tick !=0 && Math.abs(final_tick-tick) < 1500)
		                		hn.add(final_tick + " " + e[1] + " " + hm.get(final_tick));		                			                	
		                }
				        
				        
        				// write to file
				        String mPath = a.getKey();
        				String[] tmp_set = mPath.split("/");
        	    		String file_name = "combine_gps_" + tmp_set[tmp_set.length - 1];
        	    		String cPath = base_path + file_name;		
			    		reportMap.put(cPath, "");
						MedusaStorageTextFileAdapter.write(cPath, hn, false);						
        			}       			
        			
        		}
        		else {
        			MedusaUtil.log(MYNAME, "* [cbGet] Step 5: may not have any data.");
        		}
    		}
    		else {
    			MedusaUtil.log(MYNAME, "* [cbGet] Step 5: db queries has been processed, but data == null.");
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
		
		cbGet.setRunner(runnerInstance);
		cbReg.setRunner(runnerInstance);
		cbRetrive.setRunner(runnerInstance);
		
		op_type = this.getConfigParams("-t");
		op_label = this.getConfigParams("-l");
		
		return ret;
	} 
	
	@Override
    public boolean run() 
	{		
		MedusaUtil.log(MYNAME, "* Started [" + MYNAME + "]");		
		
		String[] op_type_set = op_type.split(",");
		
		
		if (op_type_set.length != 2) {			
			MedusaUtil.log(MYNAME, "! error request type, please combine two each time");
			return false;
		}
		
		combine_type_1 = op_type_set[0];
		combine_type_2 = op_type_set[1];
        
		String[] input_keys = this.getConfigInputKeys();
		String sqlstmt;
		
		if (input_keys.length > 0) {
			
			sqlstmt = "select path,type,fsize,uid,review from mediameta where (";			
			String new_content = "";			
			for (int i = 0 ; i < input_keys.length; i++) {
				/* requested specific data list */
				new_content += this.getConfigInputData(input_keys[i]) + "|";				
			}
			new_content = new_content.substring(0,new_content.length()-1);			
			String[] uids = new_content.split("\\|");
			for (int j = 0; j < uids.length; j++) {
				sqlstmt += (j > 0 ? "or " : "") + "uid='" + uids[j] + "'";
			}			
			sqlstmt += ") order by mtime desc";			
			MedusaStorageManager.requestServiceSQLite(MYNAME, cbGet, "medusadata.db", sqlstmt);			
		}
		else {
			/* request all data. */
			MedusaUtil.log(MYNAME, "* request all data available on the phone");
			
			sqlstmt = "select path,type,fsize,uid,review from mediameta order by mtime desc";
	        MedusaStorageManager.requestServiceSQLite(MYNAME, cbGet, "medusadata.db", sqlstmt);
		}
		
		MedusaObserverManager.getInstance();		
		MedusaStorageManager.requestServiceSubscribe(MYNAME, cbReg, "text");
        
		MedusaUtil.log(MYNAME, "* step 4: Request process is done.");

 		return true;
	}
	
}
