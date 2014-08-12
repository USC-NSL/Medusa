package medusa.medusalet.forceexit;

import java.util.HashMap;

import medusa.mobile.client.MedusaStorageManager;
import medusa.mobile.client.MedusaUtil;
import medusa.mobile.client.MedusaletBase;
import medusa.mobile.client.MedusaTransferManager;
import android.util.Log;

public class MedusaletMain extends MedusaletBase 
{
	/* Variable for the Summary Support. */
	private static final String TAG = "medusalet_ForceExit";
	private String pid;
	
	String queryHead;
	HashMap<String, String> reportMap;
	int numReported;
            
    @Override
	public boolean init()
	{
    	return true;
	}

	@Override
    public boolean run() 
	{
		String[] input_keys = this.getConfigInputKeys();

		if (input_keys.length > 0) {
			for (int i = 0 ; i < input_keys.length; i++) {
				/* requested specific data list */
				pid = this.getConfigInputData(input_keys[i]);
				MedusaUtil.log(TAG, "* requested data tag=" + input_keys[i] + " pid= " + pid);
			}
		}

		MedusaUtil.log(TAG, "* Starting...");
		MedusaUtil.exitMedusalet(pid);
		MedusaUtil.log(TAG, "quitting...");

		quitThisMedusalet();
		return true;
	}

}


