/**
 * 'Generate Metadata Medusalet'
 *
 * - Providing Network Statistics.
 *
 * @modified : Apr. 1st 2012
 * @author   : Xing Xu (xingx@enl.usc.edu)
 **/

package medusa.medusalet.genmetadata2;

import java.util.ArrayList;

import org.json.JSONException;

import android.database.sqlite.SQLiteCursor;
import android.text.format.Time;
import android.util.Log;
import medusa.mobile.client.ExifMetaData;
import medusa.mobile.client.MedusaStorageManager;
import medusa.mobile.client.MedusaStorageTextFileAdapter;
import medusa.mobile.client.MedusaTransformFFmpegAdapter2;
import medusa.mobile.client.MedusaUtil;
import medusa.mobile.client.MedusaletBase;
import medusa.mobile.client.MedusaletCBBase;
import medusa.mobile.client.G;
public class MedusaletMain extends MedusaletBase 
{
	private final String TAG = "MedusaGenerateMetadata";
	ArrayList<String> resultData;
	
	private String getFilePath() {
		return G.PATH_SDCARD + G.getDirectoryPathByTag("genmetadata") + MedusaStorageManager.generateTimestampFilename(TAG);
	}
	
	MedusaletCBBase cbReg = new MedusaletCBBase() { 
    	public void cbPostProcess(Object data, String msg) {
    		if (((String)data).contains(TAG) == true) {
	    		MedusaStorageManager.requestServiceSQLite(TAG, cbGet_Output, "medusadata.db"
	    				, "select path,uid from mediameta where path='" + data + "'");
	    		
	    		MedusaUtil.log(TAG, "* new file [" + data + "] has been created.");
    		}
    	}
    };
    
    MedusaletCBBase cbGet_Output = new MedusaletCBBase() {
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
	    
    MedusaletCBBase cbGet = new MedusaletCBBase() {
    	public void cbPostProcess(Object data, String msg) 
    	{
    		MedusaUtil.log(TAG, "HERE.");
    		
    		if (data != null) {
        		SQLiteCursor cr = (SQLiteCursor)data;
        		/* send raw video files */
        		if (cr.moveToFirst()) {
        			do {
        				
        				String uid = cr.getString(0);
        				String type = cr.getString(1);
        				String file = cr.getString(2);
        				String time = cr.getString(3);
        				String lat = cr.getString(5);
        				String lng = cr.getString(4);
        				String size = cr.getString(6);
        				MedusaUtil.log(TAG, "file name: "+file);
        				if(type.equals("video")==true)
        				{
        					//String dat = "\"" + G.C2DM_ID + "\"," + uid + ",\"" + MedusaTransformFFmpegAdapter2.FrameFeature(file, 10) + "\","+lat+","+lng+"," + time+','+size;
        					String dat = G.C2DM_ID + "#" + 
        								 uid + "#" + 
        								 MedusaTransformFFmpegAdapter2.FrameFeature(file, 10) + "#" +
        								 lat + "#" + 
        								 lng +"#" + 
        								 time + "#" +
        								 size + "#";
        					MedusaUtil.log(TAG, "dat: "+dat);
        					resultData.add(dat);
        				}
        				else if (type.equals("image"))
        				{
        					        					
        					try
        					{
        						ExifMetaData imd = new ExifMetaData(file);
        						
        						String dat = G.C2DM_ID + "#" + 
        									  uid +  "#" +
        									  MedusaTransformFFmpegAdapter2.ImgFeature(file) + "#" +
        									  lat + "#" +
        									  lng + "#" + 
        								      time +"#" + 
        									  size + "#" + 
        								      imd.MetaDataJSON(imd);
        						
        						MedusaUtil.log(TAG, "dat: "+dat);
            					resultData.add(dat);
        					}
        					catch(JSONException je)
        					{
        						Log.e(TAG, je.toString());
        					}
        					
        					
        					
        				}


        			} while(cr.moveToNext());
        			
        			cr.close();
        			MedusaUtil.log(TAG, "file path: "+getFilePath());
        			MedusaStorageTextFileAdapter.write(getFilePath(), resultData, true);
        		}
        		else {
        			resultData.add("none");
        			MedusaStorageTextFileAdapter.write(getFilePath(), resultData, true);
        			MedusaUtil.log(TAG, "! QueryRes: may not have any data.");
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
		/* Mandatory Operations */
		cbGet.setRunner(runnerInstance);
		cbReg.setRunner(runnerInstance);
		cbGet_Output.setRunner(runnerInstance);
				
		resultData = new ArrayList<String>();
		
		/* Parsing arguments: <config> tag */
		String timeCode = this.getConfigParams("-t");
		if (timeCode != null)
			Integer.parseInt(timeCode);
		
		MedusaUtil.log(TAG, "* time code =" + timeCode);
		
		return true;
	}
	
    @Override
    public boolean run() {
    	MedusaUtil.log(TAG, "* Started.");
    	
    	String start_uid = "";
    	
		String[] input_keys = this.getConfigInputKeys();    	
    	if (input_keys.length > 0) {
			for (int i = 0 ; i < input_keys.length; i++) {
				String content = this.getConfigInputData(input_keys[i]);
				MedusaUtil.log(TAG, "* requested data tag=" + input_keys[i] + " uids= " + content);
				
				start_uid = content;
				break;
			}
		}
    	
    	Time now = new Time();
    	now.setToNow();
    	
		MedusaUtil.log(TAG, "'" + start_uid + "'");
    	//String statement = "select uid,type,path,mtime from mediameta where mtime >=" + Long.toString(earliest) + " and type='image'";
    	String statement = "select uid,type,path,mtime,lat,lng,fsize from mediameta where (type='video' or type='image') and uid>=" + start_uid;
    	MedusaStorageManager.requestServiceSubscribe(TAG, cbReg, "text");
		MedusaStorageManager.requestServiceSQLite(TAG, cbGet, "medusadata.db"
				, statement);
    	MedusaUtil.log(TAG, "* Request process is done. (" + statement + ")");
    	
        return true;
    }

    @Override
    public void exit() {
    	super.exit();
    	
    	MedusaStorageManager.requestServiceUnsubscribe(TAG, cbReg, "text");
    }
}