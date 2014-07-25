/**
 * 'MedusaC2DMReceiver'
 *
 * - BroadcastReceiver for Android Cloud to Device Messaging Frarmework. (http://code.google.com/android/c2dm/) 
 *   
 * - AndroidManifest.xml should have the following entry.
 * 		<receiver android:name=".MedusaC2DMReceiver" android:permission="com.google.android.c2dm.permission.SEND">
 *        <!-- Receive the actual message -->
 *        <intent-filter>
 *            <action android:name="com.google.android.c2dm.intent.RECEIVE" />
 *            <category android:name="medusa.mobile.client" />
 *        </intent-filter>
 *        <!-- Receive the registration id -->
 *        <intent-filter>
 *            <action android:name="com.google.android.c2dm.intent.REGISTRATION" />
 *            <category android:name="medusa.mobile.client" />
 *        </intent-filter>
 *     	</receiver>
 *      
 *		<permission android:name="medusa.mobile.client.permission.C2D_MESSAGE" android:protectionLevel="signature" />
 *   	<uses-permission android:name="medusa.mobile.client.permission.C2D_MESSAGE" />
 *   	<uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />     
 *	
 * @modified : Mar. 26th 2012
 * @author   : Xing Xu (xingx@usc.edu), Yurong Jiang (yurongji@usc.edu)
 **/

package medusa.mobile.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MedusaC2DMReceiver extends BroadcastReceiver {
	
	private String tag = "MedusaC2DM";
	private Context context;
	
	@Override
	public void onReceive(Context context, Intent intent) 
	{
	    this.context = context;
	    
		if (intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
	        handleRegistration(context, intent);
	    } 
		else if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) {
	        handleMessage(context, intent);
	    }
	}

	private void handleRegistration(Context context, Intent intent) 
	{
	    String regid = intent.getStringExtra("registration_id");
	    
	    if (intent.getStringExtra("error") != null) {
	        // Registration failed, should try again later.
	    	MedusaUtil.log(tag, "! C2DM registration failed: [" + intent.getStringExtra("error") + "]");
	    } 
	    else if (intent.getStringExtra("unregistered") != null) {
	        // unregistration done, new messages from the authorized sender will be rejected
	    	MedusaUtil.log(tag, "! C2DM unregistered.");
	    } 
	    else if (regid != null) {
	    	// Send the registration ID to the 3rd party site that is sending the messages.
	    	updateRegistration(regid);
	    }
	    else {
	    	MedusaUtil.log(tag, "! unknown situation: " + regid);
	    }
	}
	
	private void updateRegistration(String rid)
	{
		MedusaTransferHttpAdapter httpAdapter = new MedusaTransferHttpAdapter();
		
		MedusaUtil.log("MedusaC2DM", "* C2DM RegID:" + rid);
		MedusaUtil.syncC2DMVars(G.C2DM_ID, rid);
	}

	private void handleMessage(Context context, Intent intent)
	{
	    String payload = intent.getStringExtra("payload");
	    Log.d("MedusaC2DM", "* C2DM-Msg:" + payload);
	    
	    MedusaUtil.invokeMedusalet(payload);
	}
}
