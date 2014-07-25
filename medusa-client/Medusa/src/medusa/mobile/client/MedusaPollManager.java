/**
 * ********************
 * 		DEPRECATED
 * ********************
 * 
 * 'MedusaPollManager'
 *
 * - This class implements MedusaPollManager.
 *   Its super class is MedusaServiceManager and 
 *   has designed so that it can support multiple types 
 *   of data abstractions.
 *   
 * @created : Feb. 19th 2011
 * @modified : Feb. 19th 2011
 * @deprecated : Nov. 11th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import android.util.Log;

public class MedusaPollManager extends MedusaManagerBase {

	/*
	 * Static methods for enforcing singleton pattern.
	 */
	private static MedusaPollManager manager = null;

	public static MedusaPollManager getInstance() {
		if (manager == null) {
			manager = new MedusaPollManager();
			manager.init("MedusaPollManager");
		}

		return manager;
	}

	public static void terminate() {
		if (manager != null) {
			manager.markExit();
			manager = null;
		}
	}

	MedusaTransfer trans;

	@Override
	protected void execute(MedusaServiceE ele) {
		// TODO Auto-generated method stub
		try {
			// Polling if there is a file on the server.
			Log.i("MedusaPollMgr", "Check server for file. ");

			if (trans == null)
				trans = new MedusaTransfer();
			boolean bFileExist = trans.checkFileExist();

			String filename = "";

			if (bFileExist == true) {
				for (int i = 0; i < trans.nCheckedFile; i++) {
					boolean dnResult = false, upResult = false;

					filename = trans.fnames[i + 1];

					if (filename.matches("\\w+.apk") == false) {
						Log.i("MedusaPollMgr", "Error, " + filename
								+ " is not an .apk file.");
						continue;
					}

					Log.i("MedusaPollMgr", "* For file: " + filename
							+ ", conducting a test..");

					dnResult = trans.downloadFile(filename);

					if (dnResult == true) {
						Log.i("MedusaPollMgr", "* download complete");
					} else {
						Log.i("MedusaPollMgr", "* download failed");
					}

					upResult = trans.uploadFile(filename);

					if (upResult == true) {
						Log.i("MedusaPollMgr", "* upload complete");
					} else {
						Log.i("MedusaPollMgr", "* upload failed");
					}

					final String fname = filename;

					// ((MedusaLoader) MedusaLoader.loader).loadMedusalet(filename);
					/*((MedusaLoader) MedusaLoader.loader)
							.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									((MedusaLoader) MedusaLoader.loader)
											.loadMedusalet(fname);
								}
							});*/
					MedusaUtil.startMedusaletByName(null, fname);

					System.gc();
				}
			} else {
				Log.i("MedusaPollMgr", "Could not get any file from the server.");
			}

			// 10 seconds polling interval.
			Thread.sleep(10 * 1000);
			MedusaPollManager.requestService();

		} catch (Exception e) {
			Log.i("MedusaPollMgr", "Err: " + e.toString());
		}
	}

	static void requestService() {
		MedusaServiceE ele = new MedusaServiceE();
		StringBuilder b = new StringBuilder();

		b.append("<xml>");
		b.append("<name>");
		b.append("poll");
		b.append("</name>");
		b.append("</xml>");

		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = null;

		try {
			MedusaPollManager.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
