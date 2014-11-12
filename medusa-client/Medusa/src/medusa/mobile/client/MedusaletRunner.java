/**
 * 'MedusaletRunner'
 *
 * - Container for the stage thread. 
 * - Individual stage will run in the MedusaletRunner instance.
 * - Share the same structure with other manager classes.
 *
 * @modified : Dec. 14th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.util.HashMap;
import java.util.Iterator;

import android.util.Log;

public class MedusaletRunner extends MedusaManagerBase {

	/* Constants */
	public static String TAG = "MedusaletRunner";
	
	private static String CMD_RUN_AQUALET = "run_medusalet";
	private static String CMD_RUN_CALLBACK_FUNCTION = "run_callback";
	private static String CMD_EXIT_AQUALET = "exit_medusalet";
	
	/* Medusalet management framework */
	static HashMap<String, MedusaletRunner> medusaletRunnerMap;
	
	public static void initializeMedusaletRunner()
	{
		medusaletRunnerMap = new HashMap<String, MedusaletRunner>();
	}
	
	public static MedusaletRunner getRunner(String name)
	{
		return medusaletRunnerMap.get(name);
	}
	
	/* terminate all running medusalets. */
	public static void terminateAll() {
		Iterator it = medusaletRunnerMap.entrySet().iterator();
	    while (it.hasNext()) {
	        HashMap.Entry pairs = (HashMap.Entry)it.next();
	        MedusaletRunner runner = (MedusaletRunner)pairs.getValue();
	        
	        G.unregisterReceiver(runner.getMedusaletInstance().getName());
			runner.getMedusaletInstance().exit();
	        runner.terminate();
	    }
	}
	
	/* Instance variables */
	private MedusaletBase medusaletInstance;
	
	/* 
	 * Constructor: when created, immediately runs the dispatcher thread.
	 */
	MedusaletRunner(String name, MedusaletBase medusalet)
	{
		super.init(name);
		setMedusaletInstance(medusalet);
		
		medusaletRunnerMap.put(name, this);
	}
	
	public void terminate()
	{
		markExit();
	}
	
	/*
	 * Callback function call for medusa managers.
	 * 		- cid: caller(medusa manager) id 
	 */
	public static void runCallback(MedusaletRunner runner, MedusaletCBBase cb_func)
	{
		MedusaServiceE ele = new MedusaServiceE();
		StringBuilder b = new StringBuilder();
		
		b.append("<xml>");
			b.append("<cmd>"); b.append(CMD_RUN_CALLBACK_FUNCTION); b.append("</cmd>");
		b.append("</xml>");

		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = cb_func;

		try {
			runner.requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void exitMedusalet(String proc_name	) 
	{
		MedusaServiceE ele = new MedusaServiceE();
		StringBuilder b = new StringBuilder();
		
		MedusaUtil.log(TAG, "* [overhead] request termination of medusalet [" + proc_name + "]: " + G.getElapsedTime());
		
		b.append("<xml>");
			b.append("<cmd>"); b.append(CMD_EXIT_AQUALET); b.append("</cmd>");
		b.append("</xml>");

		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = null;

		try {
			if (MedusaletRunner.getRunner(proc_name) != null)
				MedusaletRunner.getRunner(proc_name).requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void runMedusaletMain(MedusaletBase medusalet)
	{
		MedusaServiceE ele = new MedusaServiceE();
		StringBuilder b = new StringBuilder();
		
		b.append("<xml>");
			b.append("<cmd>"); b.append(CMD_RUN_AQUALET); b.append("</cmd>");
		b.append("</xml>");

		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = null;

		try {
			requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void execute(MedusaServiceE ele) throws InterruptedException, Exception
	{
		// TODO Auto-generated method stub
		String cmd = configMap.get("cmd");
		
		Log.i(this.getName(), "* execute [" + cmd + "]");
		
		if (cmd.equals(CMD_RUN_AQUALET) == true) {
			String wdname = this.getName() + "_run";
			
			MedusaUtil.log(TAG, "[overhead] medusaBox latency to medusalet.run() execution: " + G.getElapsedTime());
			
			MedusaWatchdogManager.pingStart(wdname, this);
			try {
				if (getMedusaletInstance().init() == true) {
					getMedusaletInstance().run();
				}
				else {
					Log.e(this.getName(), "! medusalet [" + getMedusaletInstance().getName() 
										+ "] initialization failed");
					getMedusaletInstance().quitThisMedusalet();
				}
			}
			finally {
				MedusaWatchdogManager.pingStop(wdname);
			}
		}
		else if (cmd.equals(CMD_RUN_CALLBACK_FUNCTION) == true) {
			/* 
			 * here, we should call cbPostProcess function directly.
			 * since callCBFunc() is a bridge to deliver callback instance to this thread! 
			 */
			MedusaletCBBase.MedusaletCBInfo m = ele.seCBListner.CBInfoQueue.poll();
			String wdname = this.getName() + "_cb";
			
			MedusaWatchdogManager.pingStart(wdname, this);
			try {
				ele.seCBListner.cbPostProcess(m.mData, m.mMsg);
			}
			finally {
				MedusaWatchdogManager.pingStop(wdname);
			}
		}
		else if (cmd.equals(CMD_EXIT_AQUALET) == true) {
			G.setBaseTime();
			G.unregisterReceiver(this.getName());
			//getMedusaletInstance().exit();
			terminate();
		}
		else {
			Log.e(this.getName(), "! unknown cmd=" + cmd);
		}
	}

	public void setMedusaletInstance(MedusaletBase medusaletInstance) {
		this.medusaletInstance = medusaletInstance;
	}

	public MedusaletBase getMedusaletInstance() {
		return medusaletInstance;
	}
}


