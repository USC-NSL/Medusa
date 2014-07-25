/**
 * 'MedusaletCBBase'
 *
 * - Abstract class of medusalet callback function implementation
 *
 * @modified : Dec. 14th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.util.LinkedList;
import android.util.Log;

public abstract class MedusaletCBBase 
{
	
	public class MedusaletCBInfo {
		public Object mData;
		public String mMsg;
		
	}
	
	protected MedusaletRunner runnerInstance;
	LinkedList<MedusaletCBInfo> CBInfoQueue;
	
	// constructor.
	public MedusaletCBBase() { /* empty */ }
	/*
	public MedusaletCBBase(MedusaletRunner runner) { 
		runnerInstance = runner;
		CBInfoQueue = new LinkedList<MedusaletCBInfo>();
	}
	*/
	
	public void setRunner(MedusaletRunner runner) { 
		runnerInstance = runner; 
		CBInfoQueue = new LinkedList<MedusaletCBInfo>();
	}
	
	public void addCBInfo(Object in_data, String in_msg) {
	
		MedusaletCBInfo in = new MedusaletCBInfo();
		in.mData = in_data;
		in.mMsg = in_msg;
		CBInfoQueue.offer(in);
		
	}
	
	public void callCBFunc(Object data, String msg) {
		addCBInfo(data, msg);
		if (runnerInstance != null) {
			MedusaletRunner.runCallback(runnerInstance, this);
		}
		else {
			Log.e("MedusaletCBBase", "! runnerInstance is not set. msg=" + msg);
		}
	}
	
	public void callCBFunc() {
		MedusaletRunner.runCallback(runnerInstance, this);
	}
	
	/*
	 * Methods which will be defined at runtime.
	 */
	public abstract void cbPostProcess(Object data, String msg);
}
