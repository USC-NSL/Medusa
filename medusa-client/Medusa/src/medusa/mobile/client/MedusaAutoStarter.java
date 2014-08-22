/**
 * 'MedusaAutoStarter'
 *
 * - The class for the any operations for the boot time.
 *
 * @created : Feb. 19th 2011
 * @modified : Nov. 11th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/
package medusa.mobile.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MedusaAutoStarter extends BroadcastReceiver 
{
	@Override
	public void onReceive(Context context, Intent intent) {
		// 
		Intent serviceIntent = new Intent();
		serviceIntent.setAction(G.AQUA_SERVICE_NAME);
		context.startService(serviceIntent);
	}
}
