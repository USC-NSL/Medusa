/**
 * 'MedusaSMSReceiver'
 *
 * - This class extends Android's built-in BroadcastReceiver, 
 * 	 hook the event of sms/mms message arrivals,
 *   interpret incoming xml commands inside the message,
 *   invoke appropriate medusalet(s).
 *   
 * - AndroidManifest.xml should have the following entry.
 * 		<receiver android:name=".MedusaSMSReceiver">
 *          <intent-filter android:priority="100">
 *              <action android:name="android.provider.Telephony.SMS_RECEIVED" />
 *          </intent-filter>
 *          <intent-filter android:priority="0">
 *              <action android:name="android.provider.Telephony.WAP_PUSH_RECEIVED" />
 *              <data android:mimeType="application/vnd.wap.mms-message" />
 *          </intent-filter>
 *      </receiver>
 *      
 * - [DEBUG] Messaging via email provided by US Carriers.
 * 		- AT&T: [phonenumber]@txt.att.net => long SMS will become multipart SMS msg. (parseWAPPush())
 * 		- T-Mobile: [phonenumber]@tmomail.net => long SMS will be sent via MMS msg. (parseSMS())
 * 		- Verizon: [phonenumber]@vtext.com
 * 		- Sprint: [phonenumber]@messaging.sprintpcs.com
 * 
 * - The code on receiving multipart SMS message is from
 * 		http://code.google.com/p/gtalksms/source/browse/src/com/googlecode/gtalksms/
 *	
 * @modified : Dec. 9th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class MedusaSMSReceiver extends BroadcastReceiver {

	private static String TAG = "MedusaSMS";
	private static int nConv = 0;
	
	public void updateConvCount(Context ctx)
	{
		MedusaSMSReceiver.nConv = this.getConversationCount(ctx);
	}
	
	/*
	 * - parseSMS() implementation is from 
	 * 		http://code.google.com/p/gtalksms/source/browse/src/com/googlecode/gtalksms/
	 *   and modified along with our purpose.
	 */
	private void parseSMS(Context context, Bundle bundle)
	{
        Map<String, String> msg = null; 
        SmsMessage[] msgs = null;
        
        if (bundle != null && bundle.containsKey("pdus")) {
            Object[] pdus = (Object[]) bundle.get("pdus");

            if (pdus != null) {
                int nbrOfpdus = pdus.length;
                msg = new HashMap<String, String>(nbrOfpdus);
                msgs = new SmsMessage[nbrOfpdus];

                //
                for (int i = 0; i < nbrOfpdus; i++) {
                    msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                    
                    String originatinAddress = msgs[i].getOriginatingAddress();
                    
                    // Check if index with number exists                    
                    if (!msg.containsKey(originatinAddress)) { 
                        msg.put(msgs[i].getOriginatingAddress(), msgs[i].getMessageBody()); 
                        
                    } else {    
                        String previousparts = msg.get(originatinAddress);
                        String msgString = previousparts + msgs[i].getMessageBody();
                        
                        msg.put(originatinAddress, msgString.trim());
                    }
                }
                
                for (String sender : msg.keySet()) {
                    String msg_content = msg.get(sender);
                    
                    /* For AT&T multipart SMS message parsing */
                    msg_content = msg_content.replaceAll("\\(Con't\\).*[0-9]\\s+of\\s+[0-9]", "");
                    msg_content = msg_content.replaceAll("\\(Con't.*[0-9]\\s+of\\s+[0-9]", "");		// MRA: This line due to the weird error from AT&T messaing system.. :(
                    msg_content = msg_content.replaceAll("\\(Con'.*[0-9]\\s+of\\s+[0-9]", "");		// MRA: This line due to the weird error from AT&T messaing system.. :(
                    msg_content = msg_content.replaceAll("\\(Con.*[0-9]\\s+of\\s+[0-9]", "");		// BIN: \cite{MRA}
                    msg_content = msg_content.replaceAll("\\(Co.*[0-9]\\s+of\\s+[0-9]", "");		// BIN: \cite{MRA}
                    msg_content = msg_content.replaceAll("\\(C.*[0-9]\\s+of\\s+[0-9]", "");			// BIN: What's the hell! Can you believe they miss 5 characters?? :(
                    msg_content = msg_content.replaceAll("[\r\n]", "");
                    
                    int start_idx = msg_content.indexOf("<xml>");
            		int end_idx = msg_content.lastIndexOf("</xml>");
            		
            		if (start_idx != -1 && end_idx != -1) {
            			Log.d(TAG, "* invokeMedusalet msg: " + msg_content);
            			
            			MedusaUtil.invokeMedusalet(msg_content);
            		}
            		else {
            			Log.d(TAG, "* passing msg: " + msg_content);
            		}
                }
            }
        }
	}
	
	private String getMmsText(Context context, String id) 
	{
	    Uri partURI = Uri.parse("content://mms/part/" + id);
	    InputStream is = null;
	    StringBuilder sb = new StringBuilder();
	    try {
	        is = context.getContentResolver().openInputStream(partURI);
	        if (is != null) {
	            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
	            BufferedReader reader = new BufferedReader(isr);
	            String temp = reader.readLine();
	            while (temp != null) {
	                sb.append(temp);
	                temp = reader.readLine();
	            }
	        }
	    } catch (IOException e) {}
	    finally {
	        if (is != null) {
	            try {
	                is.close();
	            } catch (IOException e) {}
	        }
	    }
	    return sb.toString();
	}
	
	private String getAddressNumber(Context context, int id) 
	{
	    String selectionAdd = new String("msg_id=" + id);
	    String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
	    Uri uriAddress = Uri.parse(uriStr);
	    Cursor cAdd = context.getContentResolver().query(uriAddress, null,
	        selectionAdd, null, null);
	    String name = null;
	    if (cAdd.moveToFirst()) {
	        do {
	            String number = cAdd.getString(cAdd.getColumnIndex("address"));
	            if (number != null) {
	                try {
	                    Long.parseLong(number.replace("-", ""));
	                    name = number;
	                } 
	                catch (NumberFormatException nfe) {
	                    if (name == null) {
	                        name = number;
	                    }
	                }
	            }
	        } while (cAdd.moveToNext());
	    }
	    if (cAdd != null) {
	        cAdd.close();
	    }
	    return name;
	}
	
	private int getConversationCount(Context context) 
	{
		ContentResolver contentResolver = context.getContentResolver();
	    final String[] projection = new String[] {"_id", "ct_t"};
	    Uri uri = Uri.parse("content://mms-sms/conversations/");
	    Cursor query = contentResolver.query(uri, projection, null, null, "date DESC");
	    int cnt = query.getCount();
	    query.close();
	    
	    return cnt;
	}
	
	public void parseWAPPush(Context ctx, Bundle bundle) throws InterruptedException
	{
		String content = "";
        String addr = "";
        int cnt = 0;
        
        if (MedusaSMSReceiver.nConv == 0) this.updateConvCount(ctx);
        while (this.getConversationCount(ctx) <= MedusaSMSReceiver.nConv && cnt++ < 3 /* 6 sec */) {
        	Thread.sleep(2000);
        }
        
        ContentResolver contentResolver = ctx.getContentResolver();
        final String[] projection = new String[] {"_id", "ct_t"};
        Uri uri = Uri.parse("content://mms-sms/conversations/");
        Cursor query = contentResolver.query(uri, projection, null, null, "date DESC");
        if (query.moveToFirst()) {
            do {
            	String idstr = query.getString(query.getColumnIndex("_id"));
                String string = query.getString(query.getColumnIndex("ct_t"));
                if ("application/vnd.wap.multipart.related".equals(string)) {
                    // it's MMS
                	String mmsId = idstr;
                	addr = getAddressNumber(ctx, Integer.parseInt(idstr));
                	
                	String selectionPart = "mid=" + mmsId;
                	uri = Uri.parse("content://mms/part");
                	Cursor cur = ctx.getContentResolver().query(uri, null, selectionPart, null, null);
                	if (cur.moveToFirst()) {
                	    do {
                	        String partId = cur.getString(cur.getColumnIndex("_id"));
                	        String type = cur.getString(cur.getColumnIndex("ct"));
                	        if ("text/plain".equals(type)) {
                	            String data = cur.getString(cur.getColumnIndex("_data"));
                	            String body;
                	            if (data != null) {
                	                // implementation of this method below
                	                body = getMmsText(ctx, partId);
                	            } 
                	            else {
                	                body = cur.getString(cur.getColumnIndex("text"));
                	            }
                	            
                	            Log.i(TAG, "id=" + idstr + " data=" + data + ", body=" + body);
                	            content = body;
                	        }
                	    } while (cur.moveToNext());
                	    
                	    cur.close();
                	}
                } 
                else {
                    // it's SMS
                }
                break;
            } while (query.moveToNext());
            
            query.close();
        }
        this.updateConvCount(ctx);
        
        MedusaUtil.invokeMedusalet(content);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) 
	{
		Bundle extras = intent.getExtras();
		if (extras == null) {
			return;
		}
		
		G.setBaseTime();
		
		if (intent.getAction().equals(G.SMS_RECEIVED) == true) {
			Log.d(TAG, "SMS_RECEIVED has been received: " + intent.getAction());
			
			parseSMS(context, extras);
		}
		if (intent.getAction().equals(G.WAP_PUSH) == true) {
			Log.d(TAG, "WAP_PUSH_RECEIVED has been received: " + intent.getAction());
			
			try {
				parseWAPPush(context, extras);
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
