package medusa.mobile.client;
import com.google.android.gcm.GCMBaseIntentService;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
//import com.sqisland.android.gcm_client.Constants;

public class GCMIntentService extends GCMBaseIntentService{
	  private String TAG = "MedusaGCM";
	  @Override
	  protected void onRegistered(Context context, String regId) {
	    Intent intent = new Intent(G.ACTION_ON_REGISTERED);
	    intent.putExtra(G.FIELD_REGISTRATION_ID, regId);
	    Log.i(TAG, "registered: "+ regId);
	    context.sendBroadcast(intent);
	  }

	  @Override
	  protected void onUnregistered(Context context, String regId) {
	    Log.i(TAG, "onUnregistered: "+ regId);
	  }

	  @Override
	  protected void onMessage(Context context, Intent intent) {
	    String msg = intent.getStringExtra(G.FIELD_MESSAGE);
	    
	    //Log.d("==", msg);
	    Log.d("MedusaGCM Message", "* GCM-Msg:" + msg);
	    
	    MedusaUtil.invokeMedusalet(msg);
	  }

	  @Override
	  protected void onError(Context context, String errorId) {
	    Toast.makeText(context, errorId, Toast.LENGTH_LONG).show();
	  }
	  
	
}
