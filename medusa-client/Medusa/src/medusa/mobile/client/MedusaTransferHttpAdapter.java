/**
 * 'MedusaTransferHttpAdapter'
 *
 * - Implement HTTP related functions.
 *
 * @modified : Dec. 14th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

public class MedusaTransferHttpAdapter 
{	
	private static final String TAG = "MedusaTxHttpAdapter";
	
	public String respMsg;

	public boolean uploadDataViaHttpMultipartFormData(
			String fpath, String pfname, long ofs, 
			long send_size, long max_buf_size, String url_str) throws IOException 
	{
		String boundary = "AaB03x";
		String lineEnd = "\r\n";
		String twoHyphens = "--";

		// Connection Establishment.
		URL connectURL = new URL(url_str);
		HttpURLConnection conn = (HttpURLConnection) connectURL.openConnection();

		conn.setDoInput(true); 		// Allow Inputs
		conn.setDoOutput(true); 	// Allow Outputs
		conn.setUseCaches(false); 	// Don't use caches.
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Connection", "Keep-Alive");
		conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

		DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
		dos.writeBytes(twoHyphens + boundary + lineEnd);
		dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + pfname + "\"" + lineEnd);
		dos.writeBytes(lineEnd);

		// int bufferSize = (int)Math.min(sendsize, maxbufsize);

		// Read file and write it into form...
		FileInputStream fis = new FileInputStream(fpath);
		fis.skip(ofs);

		byte[] buf = new byte[(int) (max_buf_size + 512)];
		long tot_size = send_size;
		int bytesRead; // = fis.read(buf, 0, sendsize);

		while (tot_size > 0) {
			bytesRead = fis.read(buf, 0, (int) Math.min(tot_size, max_buf_size));
			dos.write(buf, 0, bytesRead);
			tot_size -= bytesRead;
		}

		// Send multipart form data necesssary after file data...
		dos.writeBytes(lineEnd + twoHyphens + boundary + twoHyphens + lineEnd);

		// Close streams
		fis.close();
		dos.flush();

		// Retrieve the response from server
		// !! Blocking operations !!
		InputStream is = conn.getInputStream();
		StringBuffer b = new StringBuffer();
		int ch;
		while ((ch = is.read()) != -1) {
			b.append((char) ch);
		}
		dos.close();

		// Log.i(TAG, "* HTTP Resp: " + b.toString());

		return true;
	}
	
	public int httpSimpleRequest(String method, String in_url)  
	{
		int resp_code = 0;
		
		try {
			URL u;
			
			Log.d(TAG, "* " + method + " " + in_url);

			u = new URL(in_url);
			HttpURLConnection c = (HttpURLConnection) u.openConnection();
			c.setRequestMethod(method);
			c.setDoOutput(true);
			c.connect();

			InputStream in = c.getInputStream();
			byte[] buffer = new byte[1024];
			int len = 0;

			/*respMsg = "";
			while ((len = in.read(buffer)) > 0) {
				respMsg += new String(buffer, 0, len);
			}*/
			resp_code = c.getResponseCode();
			in.close();
			c.disconnect();
			
			Log.d(TAG, "* " + method + " Response Code=" + resp_code);
			//Log.d(TAG, "* " + method + " Response Msg=" + respMsg);
			Log.d(TAG, "* " + method + " completed.");
			 	 
		} catch (Exception e) {
			// e.printStackTrace();
			Log.e(TAG, "! HttpAdapter::httpSimpleRequest() error." + e.getMessage());
			resp_code = -1;
		}
		
		return resp_code;
	}
	
	public boolean getFileViaHttpGET(String in_url, String in_fpath) 
	{
		try {
			URL u;

			u = new URL(in_url);
			HttpURLConnection c = (HttpURLConnection) u.openConnection();
			c.setRequestMethod("GET");
			c.setDoOutput(true);
			c.connect();
			FileOutputStream f = new FileOutputStream(in_fpath);

			InputStream in = c.getInputStream();
			byte[] buffer = new byte[1024];
			int len = 0;

			while ((len = in.read(buffer)) > 0) {
				f.write(buffer, 0, len);
				Log.d(TAG, "* " + len + " bytes read.");
			}
			f.close();
			in.close();
			c.disconnect();

			Log.d(TAG, "* File [" + in_fpath + "] downloaded and saved.");
			
			return true;
			
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG, "! HttpAdapter::getFileViaHttpGET() error.");
		}
		
		return false;
	}
	
	public boolean httpPostRequest(
			String url_str, String paras 
			) throws IOException 
	{
		// Connection Establishment.
		try {
		URL connectURL = new URL(url_str);
		HttpURLConnection connection = null;
		connection = (HttpURLConnection)connectURL.openConnection();
	    connection.setRequestMethod("POST");
	    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");				
	    connection.setRequestProperty("Content-Length", "" + Integer.toString(paras.getBytes().length));
	    connection.setRequestProperty("Content-Language", "en-US");  
				
	    connection.setUseCaches (false);
	    connection.setDoInput(true);
	    connection.setDoOutput(true);
	    
	    DataOutputStream wr = new DataOutputStream (connection.getOutputStream ());
        wr.writeBytes (paras);
        wr.flush ();
        wr.close ();

      //Get Response	
        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuffer response = new StringBuffer(); 
        while((line = rd.readLine()) != null) {
          response.append(line);
          response.append('\r');
       }
       rd.close();   
		} catch (Exception e) {
			// e.printStackTrace();
			Log.e(TAG, "! HttpAdapter::httpPostRequest() error." + e.getMessage());
		}
	   
       return true;
	}
}
