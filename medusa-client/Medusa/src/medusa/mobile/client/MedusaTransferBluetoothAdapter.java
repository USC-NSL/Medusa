/**
 * 'MedusaTransferBluetoothAdapter'
 *
 * - Implement Bluetooth related functions here..
 *
 * @modified : Dec. 14th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

public class MedusaTransferBluetoothAdapter {
	
	private static final String TAG = "MedusaBluetoothAdapter";
	
	/* This function should be called in UI thread.. */
	public static void startScan() 
	{
		BluetoothAdapter bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
		if (!bluetooth_adapter.isEnabled()) {
		    //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    //startActivityForResult(enableBtIntent, 0);
			MedusaUtil.log(TAG, "! Bluetooth needs to be turned on.");
		}
		bluetooth_adapter.startDiscovery();
		
		Log.d(TAG, "* startDiscovery() called.");
	}
}
