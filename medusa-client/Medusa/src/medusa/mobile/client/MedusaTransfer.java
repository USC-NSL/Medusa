/**
 * ***************************************
 * 		DEPRECATED (w/ MedusaPollManager)
 * ***************************************
 * 'MedusaTransfer'
 * - This class will be extended to implement negotiation protocol
 *   between the medusa phone and the server
 *
 * @modified : Nov. 10. 2010
 * @author   : Moo-Ryong Ra, Jeongyeup Paek (jpaek@usc.edu)
 **/

package medusa.mobile.client;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class MedusaTransfer 
{
	public class HttpFileUploader {

		URL connectURL;
		String params;
		String responseString;
		String fileName;
		byte[] dataToServer;
		FileInputStream fileInputStream = null;

		// Constructor
		HttpFileUploader(String urlString, String fileName) {
			try {
				connectURL = new URL(urlString);
			} catch (Exception e) {
				Log.e("! URL Formation", "MALFORMATED URL");
			}

			this.params = params + "=";
			this.fileName = fileName;
		}

		void doStart(FileInputStream stream) {
			fileInputStream = stream;
			String exsistingFileName = fileName;

			String lineEnd = "\r\n";
			String twoHyphens = "--";
			String boundary = "***42bx03e***";
			String Tag = "HTTP";

			try {
				// Log.e(Tag, "Starting to send");

				// Open a HTTP connection to the URL
				HttpURLConnection conn = (HttpURLConnection) connectURL
						.openConnection();

				conn.setDoInput(true); // Allow Inputs
				conn.setDoOutput(true); // Allow Outputs
				conn.setUseCaches(false); // Don't use caches.

				// Use a post method.
				conn.setRequestMethod("POST");

				conn.setRequestProperty("Connection", "Keep-Alive");
				conn.setRequestProperty("Content-Type",
						"multipart/form-data;boundary=" + boundary);

				DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

				dos.writeBytes(twoHyphens + boundary + lineEnd);
				dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\""
						+ exsistingFileName + "\"" + lineEnd);
				dos.writeBytes(lineEnd);

				// Log.e(Tag, "Headers are written");

				// Create a buffer of maximum size
				int bytesAvailable = fileInputStream.available();
				int maxBufferSize = 1024 * 128; // 128KB
				int bufferSize = Math.min(bytesAvailable, maxBufferSize);
				byte[] buffer = new byte[bufferSize];

				// Read file and write it into form...
				int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
				while (bytesRead > 0) {
					dos.write(buffer, 0, bufferSize);
					bytesAvailable = fileInputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);
				}

				// Send multipart form data necesssary after file data...
				dos.writeBytes(lineEnd + twoHyphens + boundary + twoHyphens
						+ lineEnd);

				// Close streams
				// Log.e(Tag, "File is written");
				fileInputStream.close();
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

				Log.i("MR", "* Reponse has arrived: " + b.toString());
			} catch (MalformedURLException ex) {
				Log.e(Tag, "! Err(M. URL. E): " + ex.getMessage(), ex);
			} catch (IOException ioe) {
				Log.e(Tag, "! Err(IO): " + ioe.getMessage(), ioe);
			}
		}
	}

	private final String serverurl = "http://tomography.usc.edu/medusa";
	private final String APP_PATH = "demo_riverside";
	private final String DN_DIRPATH = "to_be_downloaded";
	private final String UP_DIRPATH = "to_be_uploaded";

	public String[] fnames;
	public int nCheckedFile;

	public boolean checkFileExist() {
		// check if there is a file on the server.
		String url = serverurl + "/" + APP_PATH + "/proc.php?CMD=CHECK&PATH="
				+ DN_DIRPATH + "&IMEI=" + G.tpIMEI;
		boolean bFileExist = false;
		int resp_code = 0;
		
		nCheckedFile = 0;
		fnames = null;

		//HttpBasicRequest httpAgent = new HttpBasicRequest();
		MedusaTransferHttpAdapter httpAgent = new MedusaTransferHttpAdapter();

		// Check if server has a file.
		try {
			resp_code = httpAgent.httpSimpleRequest("GET", url);
		} catch (Exception e) {
			Log.e("Medusa", "Check File Err. " + e.toString());
		}

		if (resp_code == 200) {
			fnames = TextUtils.split(httpAgent.respMsg, " ");
			try {
				nCheckedFile = Integer.parseInt(fnames[0]);
				if (nCheckedFile > 0) {
					bFileExist = true;
				}
			} catch (Exception e) {
				nCheckedFile = 1;
				fnames = new String[2];
				fnames[0] = "1";
				fnames[1] = "medusalet_helloworld.apk";
				bFileExist = true;
			}
			Log.i("Medusa", "Got " + fnames[0] + " files starting with "
					+ fnames[1]);
		} else if (resp_code == 202) {
			Log.i("Medusa", "Resp 202, No files...");
		} else {
			Log.i("Medusa", "Server unavailable");
		}
		return bFileExist;
	}

	public boolean downloadFile(String filename) 
	{
		boolean dnResult = false;
		String url = serverurl + "/" + APP_PATH + "/" + DN_DIRPATH + "/"
				+ filename;
		String fpath = String.format("/sdcard/%s", filename);

		// Download a file
		try {
			//HttpBasicRequest downloader = new HttpBasicRequest();
			MedusaTransferHttpAdapter downloader = new MedusaTransferHttpAdapter();

			if (downloader.getFileViaHttpGET(url, fpath) == true) {
				dnResult = true;
			}
		} catch (Exception e) {
			Log.e("Medusa", "Download Err. " + e.toString());
		}
		
		return dnResult;
	}

	public boolean uploadFile(String filename) {
		boolean upResult = false;
		String url = serverurl + "/" + APP_PATH + "/proc.php?CMD=PUTFILE"
				+ "&IMEI=" + G.tpIMEI;
		File file = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath(), "/" + filename);
		String fullpath = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/" + filename;

		// Upload a file.
		try {

			if (file.exists() == true) 
			{
				Log.i("Medusa", "* Sending a file");

				FileInputStream fis = new FileInputStream(fullpath);

				HttpFileUploader uploader = new HttpFileUploader(url, filename);
				uploader.doStart(fis);
				fis.close();

				upResult = true;
			} else {
				Log.i("Medusa", "! File [" + filename + "] doesn't exist.");
			}

		} catch (Exception e) {
			Log.e("Medusa", "Upload Err. " + e.toString());
		}
		return upResult;
	}

}
