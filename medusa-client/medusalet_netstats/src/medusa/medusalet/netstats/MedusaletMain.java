/**
 * 'NetStats Medusalet'
 *
 * - Providing Network Statistics.
 *
 * @modified : Dec. 6th 2011
 * @author   : Moo-Ryong Ra(mra@usc.edu)
 **/

package medusa.medusalet.netstats;

import java.util.ArrayList;

import medusa.mobile.client.MedusaStorageManager;
import medusa.mobile.client.MedusaStorageTextFileAdapter;
import medusa.mobile.client.MedusaTransferManager;
import medusa.mobile.client.MedusaUtil;
import medusa.mobile.client.MedusaletBase;
import medusa.mobile.client.MedusaletCBBase;
import medusa.mobile.client.G;

import android.database.sqlite.SQLiteCursor;

public class MedusaletMain extends MedusaletBase 
{
	private final String TAG = "medusalet_MedusaNetStats";
	
	private final int TYPE_FILTER_MACADDR = 1;
	private final int TYPE_FILTER_CAPABILITIES = 1 << 1;
	private final int TYPE_FILTER_FREQUENCY = 1 << 2;
	private final int TYPE_FILTER_LEVEL = 1 << 3;
	
	ArrayList<String> resultData;
	String opcode;
	String type;
	int typeFilter;
	String period;
	int totalCount;
	
	int retryInterval = 5;
	
	private String getFilePath() {
		return G.PATH_SDCARD + G.getDirectoryPathByTag("netscan") + MedusaStorageManager.generateTimestampFilename(TAG);
	}
	
	/* Receiving Scanning Results. */
	MedusaletCBBase cbScan = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) {
    		if (data != null) {
    			String result = "";
    			if (opcode.equals(MedusaTransferManager.OP_SCAN_WIFI) == true) {
	    			String[] args = ((String) data).split(",,");
	    			
	    			MedusaUtil.log(TAG, msg);
	    			
	    			for (int i = 0; i < args.length; i++) {
	    				String[] strs = args[i].split("::");
	    				String ssid = strs[0];
						String[] subargs = strs[1].split("\\|");
						
						if (result.length() > 0) result += ",";
						result += ssid;
						
						if ((typeFilter & TYPE_FILTER_MACADDR) != 0) {
							result += "|" + subargs[0];
						}
						if ((typeFilter & TYPE_FILTER_CAPABILITIES) != 0) {
							result += "|" + subargs[1];
						}
						if ((typeFilter & TYPE_FILTER_FREQUENCY) != 0) {
							result += "|" + subargs[2];
						}
						if ((typeFilter & TYPE_FILTER_LEVEL) != 0) {
							result += "|" + subargs[3];
						}
					}
					
	    			resultData.add(result);
	    		}
	    		else if (opcode.equals(MedusaTransferManager.OP_SCAN_BLUETOOTH) == true) {
	    			MedusaUtil.log(TAG, msg);
	    			
	    			String[] res = ((String)data).split("\\|");
	    			String name, addr;
	    			
	    			name = res[0];
	    			addr = res[1];
	    			
	    			result = name + "|" + addr;
	    			resultData.add(result);
	    		}
    			
    			MedusaUtil.log(TAG, "* result: " + result);
	    		
	    		try {
					Thread.sleep(Long.parseLong(period) * 1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
    		}
    		else {
    			MedusaUtil.log(TAG, "! returned null. will retry after 5 seconds.");	
    			try {
					Thread.sleep(retryInterval * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}
				
			if (--totalCount > 0) {
    			MedusaTransferManager.requestScanResult(opcode, period, cbScan);
    		}
    		else if (totalCount == 0) {
    			MedusaStorageTextFileAdapter.write(getFilePath(), resultData, true);
    		}
    		else {
    			MedusaUtil.log(TAG, "! excessive scan results totalCount=" + totalCount);
    		}
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
        				String uid = cr.getString(1 /* column index */);
        				reportUid(uid);
        			} while(cr.moveToNext());
        			
        			cr.close();
        			quitThisMedusalet();
        		}
        		else {
        			MedusaUtil.log(TAG, "! QueryRes: may not have any data.");
        		}
    		}
    		else {
    			MedusaUtil.log(TAG, "! QueryRes: the query has been executed, but data == null ?!");
    		}
    	}
    };
    
    MedusaletCBBase cbReg = new MedusaletCBBase() { 
    	public void cbPostProcess(Object data, String msg) {
    		if (((String)data).contains(TAG) == true) {
	    		MedusaStorageManager.requestServiceSQLite(TAG, cbGet, "medusadata.db"
	    				, "select path,uid from mediameta where path='" + data + "'");
	    		
	    		MedusaUtil.log(TAG, "* new file [" + data + "] has been created.");
    		}
    	}
    };
    
	@Override
	public boolean init() 
	{	
		/* Mandatory Operations */
		cbScan.setRunner(runnerInstance);
		cbGet.setRunner(runnerInstance);
		cbReg.setRunner(runnerInstance);
		
		/* Default Values */
		resultData = new ArrayList<String>();
		typeFilter = 0;
		totalCount = 3;
		
		/* Parsing arguments: <config> tag */
		opcode = this.getConfigParams("-i");
		
		String filters = this.getConfigParams("-f");
		if (filters != null) {
			String[] typestr = filters.split(",");
			for (int j = 0; j < typestr.length; j++) {
				if (typestr[j].equals("bssid") == true) {
					typeFilter |= TYPE_FILTER_MACADDR;
					MedusaUtil.log(TAG, "* BSSID(Mac Addr) filter set");
				}
				else if (typestr[j].equals("capabilities") == true) {
					typeFilter |= TYPE_FILTER_CAPABILITIES;
					MedusaUtil.log(TAG, "* Capabilities filter set");
				}
				else if (typestr[j].equals("frequency") == true) {
					typeFilter |= TYPE_FILTER_FREQUENCY;
					MedusaUtil.log(TAG, "* Frequency filter set");
				}
				else if (typestr[j].equals("level") == true) {
					typeFilter |= TYPE_FILTER_LEVEL;
					MedusaUtil.log(TAG, "* Level filter set");
				}
				else {
					MedusaUtil.log(TAG, "! unknown filter type: " + typestr[j]);
				}
			}
		}
		
		period = this.getConfigParams("-p"); 
		totalCount = this.getConfigParams("-c") == null ? 5 : Integer.parseInt( this.getConfigParams("-c") );
		
		MedusaUtil.log(TAG, "* opcode=" + opcode + ", period=" + period + ", count=" + totalCount);
		
		return (opcode == null || period == null || totalCount <= 0 ? false : true);
	}
	
    @Override
    public boolean run() {
    	MedusaUtil.log(TAG, "* Started.");
    	
    	MedusaTransferManager.requestScanResult(opcode, period, cbScan);
    	MedusaStorageManager.requestServiceSubscribe(TAG, cbReg, "text");
    	
    	MedusaUtil.log(TAG, "* Request process is done.");
    	
        return true;
    }

    @Override
    public void exit() {
    	super.exit();
    	
    	MedusaStorageManager.requestServiceUnsubscribe(TAG, cbReg, "text");
    }
}
