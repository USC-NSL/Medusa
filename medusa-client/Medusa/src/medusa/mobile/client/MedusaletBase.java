/**
 * 'MedusaletBase'
 *
 * MedusaletBase is template class for medusalet program. 
 * 		- individual medusalet has its own thread.
 *
 * @created: Nov. 11th 2011
 * @modified : Nov. 13rd 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.util.ArrayList;
import java.util.HashMap;

import android.util.Base64;
import android.util.Log;

public abstract class MedusaletBase 
{	
	public static final String REVIEW_NONE = "none";
	public static final String REVIEW_YESNO = "yesno";
	public static final String REVIEW_LABELING = "labeling";
	public static final String REVIEW_TEXTDESC = "textdesc";
	
	/* runtime variables */
	protected MedusaletRunner runnerInstance;
	protected int pid;
	protected int qid;
	protected String reviewMethod;
	protected String previewOpt;
	
	HashMap<String, String> configParams;		/* <config><params> </params></config> */
	HashMap<String, String> configInput;		/* <config><input> </input></config> */
	String[] configInputKeys;
	
	/* runtime variables */
	ArrayList<String> reportUidList;
	private String reviewOptions;
	
	protected MedusaletBase() {
		reportUidList = new ArrayList<String>();
		configParams = new HashMap<String,String>();
		configInput = new HashMap<String, String>();
		
		reviewMethod = REVIEW_NONE;
		reviewOptions = null;
		
		setPreviewOpt("yes");
	}
	
	/* Getter/Setter methods on parameters */
	//public void setName(String in_name) { medusaletName = in_name; }
	public String getName() { return runnerInstance.getName(); }

	public void setPid(int in_pid) { pid = in_pid; }
	public int getPid() { return pid; }
	
	public void setQid(int in_qid) { qid = in_qid; }
	public int getQid() { return qid; }
	
	/* <config><params> </params></config> */
	public void setConifgParams(String in_config_params) {
		String raw = new String(Base64.decode(in_config_params, Base64.DEFAULT));
		String[] tokens = raw.split("\\s+");
		String key = null, val = null;
		int i = 0;
		
		while (tokens.length > 0 && i < tokens.length && tokens[i].length() > 0) {
			if (tokens[i].startsWith("-") == true) {
				key = tokens[i];
			}
			else {
				val = tokens[i];
				if (key != null) {
					configParams.put(key, val);
					MedusaUtil.log("Params", "<params> KEY=" + key + " VAL=" + val);
					key = null;
				}
				else {
					MedusaUtil.log("WARN", "! invalid config-params: " + in_config_params);
				}
			}
			i++;
		} 
	}
	public String getConfigParams(String opt) { return configParams.get(opt); }
	
	/* <config><input> </input></config> */
	public void setConfigInput(String in_config_input) {
		/* format: key=value,key=value */
		String[] entries = in_config_input.split(",");
		configInputKeys = new String[entries.length];
		for (int i = 0; i < entries.length; i++) {
			String[] strs = entries[i].split("=");
			String decoded = new String(Base64.decode(strs[1], Base64.DEFAULT));
			configInput.put(strs[0], decoded);
			configInputKeys[i] = strs[0];
			
			// debug
			MedusaUtil.log("Input", "<input> KEY=" + strs[0] + " VAL=" + strs[1]);
		}
	}
	public String getConfigInputData(String key) { return configInput.get(key); }
	public String[] getConfigInputKeys() { return configInputKeys; }
	public String getConfigInputKeys(int idx) { return idx < configInputKeys.length ? configInputKeys[idx] : null; }
	
	public void setReviewMethod(String in_review_method) {
		if (in_review_method.equals(REVIEW_NONE) == false &&
				in_review_method.equals(REVIEW_YESNO) == false &&
				in_review_method.equals(REVIEW_LABELING) == false &&
				in_review_method.equals(REVIEW_TEXTDESC) == false) {
			Log.e(this.getName(), "! invalid review method=[" + in_review_method + "], 'default' will be used instead.");
			reviewMethod = REVIEW_YESNO;
		}
		else {
			reviewMethod = in_review_method;
		}
	}
	public String getReviewMethod() { return reviewMethod; }
	
	public void setPreviewOpt(String opt) { previewOpt = opt; }
	public String getPreviewOpt() { return previewOpt; }
	
	public void setReviewOptions(String aq_review_options) { reviewOptions = aq_review_options; }
	public String getReviewOptions() { return reviewOptions; }
	
	public void setRunner(MedusaletRunner runner) { runnerInstance = runner; }
	public MedusaletRunner getRunner() { return runnerInstance; }
	
	/* report function. */
	String getBaseParams(String action, String type) { 
		return "action=" + action
			+ "&type=" + type 
			+ "&amtid=" + G.getUID()
			+ "&pid=" + pid 
			+ "&qid=" + qid 
			+ "&qtype=" + this.getName();
	}
	
	String getDataParams() {
		int itr = reportUidList.size();
		String pr = "";
		
		for (int i = 0; i < itr; i++) {
			pr += reportUidList.get(i);
			if ( i != (itr-1) ) pr += "|";
		}
		
		return pr;
	}
	
	/* some helper function */
	public boolean isStopCondSetAs(String cond) {
		String stop_cond = getConfigParams("-s");
		return (stop_cond != null && stop_cond.equals(cond));
	}
	
	/* 
	 * Whenever a certain data has sent to the server,
	 * this function should be called. 
	 */
	protected void reportUid(String uid)
	{
		reportUidList.add(uid);
		
		/*String uri;
		
		if (reviewMethod.equals("none") == true) {
			// report immediately..
			uri = G.URIBASE_REPORT + "?" + getBaseParams("report", "uid") + "&uidlist=" + uid;
			MedusaTransferManager.requestHttpReq(this.getName(), "GET", uri, null);
			reportUidList.clear();
		}
		else if (reviewMethod.equals(REVIEW_YESNO) == true || 
					reviewMethod.equals(REVIEW_LABELING) == true ||
					reviewMethod.equals(REVIEW_TEXTDESC) == true) {
			reportUidList.add(uid);
		}
		else {
			Log.e(this.getName(), "! unhandled: uid=" + uid);
		}
		*/
	}
	
	/* 
	 * Whenever a certain data has sent to the server,
	 * this function should be called. 
	 */
	protected void reportDataSnapshot(String type, String str) 
	{
		String uri;
		
		uri = G.URIBASE_REPORT + "?" + getBaseParams("report", type) + "&custom=" + MedusaUtil.base64Encode(str);
		MedusaTransferManager.requestHttpReq(this.getName(), "GET", uri, null);
		
		//MedusaUtil.log(this.getName(), " * reporting data: " + str);
		//MedusaUtil.log(this.getName(), " * encoded=" + uri + " encoded-length=" + uri.length());
	}
	
	/* 
	 * if reviewMethod is NOT "none", this function should be called 
	 * by the medusalet to let the TFS know the status of the application.
	 */
	protected void reportComplete() 
	{
		String uri = G.URIBASE_REPORT + "?" + getBaseParams("completeTask", "uid")
					+ "&uidlist=" + getDataParams();
		MedusaTransferManager.requestHttpReq(runnerInstance.getName(), "GET", uri, null);
		reportUidList.clear();
	}
		
	/* run() funtion must be implemented */
	public boolean init() { return true; }
	public void exit() { 
		reportComplete();
	}
	public void quitThisMedusalet() {
		exit();
		MedusaletRunner.exitMedusalet(runnerInstance.getName());
	}
	public abstract boolean run() throws InterruptedException;
}


