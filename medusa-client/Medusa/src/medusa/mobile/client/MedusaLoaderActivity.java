/**
 * 'MedusaLoader'
 *
 * - This is the debug console activity, which we can see
 * 	 important logs at runtime.
 *
 * @created: Apr. 15. 2011
 * @modified : Dec. 14th. 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu), Jeongyeup Paek (jpaek@usc.edu)
 **/

package medusa.mobile.client;

import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.gcm.GCMRegistrar;

public class MedusaLoaderActivity extends Activity implements
		TextToSpeech.OnInitListener {

	private static final String TAG = "MedusaConsole";
	private static final int TTS_DATA_CHECK_CODE = 777;

	public void onResume() {
		super.onResume();
		registerReceiver(mGCMReceiver, mOnRegisteredFilter);
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(mGCMReceiver);
	}

	public class DebugLogReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			logToBoth(intent.getExtras().getString("msg"));
		}
	}

	private class GCMReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String regId = intent.getStringExtra(G.FIELD_REGISTRATION_ID);
			Log.d("GCM ID", regId);
			// sendIdToServer(regId);
			G.updateRegistration(regId);// update gcm reg id
		}
	}

	private TextViewLogger logView;
	private DebugLogReceiver logRecv;
	private GCMReceiver mGCMReceiver;
	private IntentFilter mOnRegisteredFilter;

	/* Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// String path = "/sdcard/DCIM/Camera/VID_20120906_150559.mp4";
		// MedusaTransformFFmpegAdapter2 ad = new
		// MedusaTransformFFmpegAdapter2();
		// String log = MedusaTransformFFmpegAdapter2.FrameFeature(path, 10);
		// Log.d("==", "feature: "+log);

		final Button btn_reset = (Button) findViewById(R.id.btn_reset);
		btn_reset.setOnClickListener(cbBtnReset);
		final Button btn_clear = (Button) findViewById(R.id.btn_clear);
		btn_clear.setOnClickListener(cbBtnClear);
		final Button btn_amttask = (Button) findViewById(R.id.btn_amttask);
		btn_amttask.setOnClickListener(cbBtnAMTTask);
		final Button btn_medusa_mainpage = (Button) findViewById(R.id.btn_medusa_mainpage);
		btn_medusa_mainpage.setOnClickListener(cbBtnMedusaMainpage);

		logView = new TextViewLogger((TextView) findViewById(R.id.output));
		logView.br();
		logToBoth("* Starting Medusa Debug Console");

		logRecv = new DebugLogReceiver();
		IntentFilter filter = new IntentFilter(G.AQUA_DEBUG_LOG);
		registerReceiver(logRecv, filter);

		G.initialize(getApplicationContext());
		G.setBaseTime(); /* To measure the overhead */

		Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, TTS_DATA_CHECK_CODE);

		/* required to change once at the beginning. */
		if (G.C2DM_ID == "medusa") {
			showDialog(DIALOG_UPDATE_WID);
		}

		mGCMReceiver = new GCMReceiver();
		mOnRegisteredFilter = new IntentFilter();
		mOnRegisteredFilter.addAction(G.ACTION_ON_REGISTERED);
		registerReceiver(mGCMReceiver, mOnRegisteredFilter);
		GCMRegister();

		if (G.SENDER_ID == null) {
			Log.e(TAG, "Invalid Sender_ID");
			return;
		}
		/* end of registratoin of GCM */
	}

	@Override
	public void onDestroy() {
		if (G.tts != null) {
			G.tts.stop();
			G.tts.shutdown();
		}

		unregisterReceiver(logRecv);
		super.onDestroy();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == TTS_DATA_CHECK_CODE) {
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				// success, create the TTS instance.
				G.tts = new TextToSpeech(this, this);
				MedusaUtil.log("TTS", "* TTS instance is created.");
			} else {
				// missing data, install it.
				Intent installIntent = new Intent();
				installIntent
						.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			}
		}
	}

	// Callback Function (BTN)
	private OnClickListener cbBtnReset = new OnClickListener() {
		@Override
		public void onClick(View v) {
			MedusaUtil.stopMedusaLoaderService();
			finish();
		}
	};

	// Callback Function (BTN)
	private OnClickListener cbBtnClear = new OnClickListener() {
		@Override
		public void onClick(View v) {
			logView.clear();
		}
	};

	private OnClickListener cbBtnAMTTask = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// MedusaUtil.log(TAG, "* start method tracding named: " + G.NAME);
			// Debug.startMethodTracing(G.NAME);
			Uri uri = Uri
					.parse("https://workersandbox.mturk.com/mturk/welcome");
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
		}
	};

	private OnClickListener cbBtnMedusaMainpage = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// MedusaUtil.log(TAG, "* stopped method tracding: " + G.NAME);
			// Debug.stopMethodTracing();
			Uri uri = Uri.parse("http://tomography.usc.edu/medusa");
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
		}
	};

	/** show log on screen **/
	public void logToScreen(String string) {
		logView.printlnwt(string);
	}

	/** write log to file **/
	public void logToFile(String string) {
		// log.printlnwt(string);
	}

	/** write log to both screen and file **/
	public void logToBoth(String string) {
		// Log.d(TAG, string);
		logToScreen(string);
		logToFile(string);
	}

	/** write log to both screen and file **/
	public void logToBoth(String string, Exception e) {
		Log.e(TAG, string, e);
		logToScreen(string);
		logToFile(string);
	}

	/** create menu **/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);

		return true;
	}

	/* Menu Handling */
	private static final int DIALOG_UPDATE_WID = 1;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_1) {
			/* update WID */
			showDialog(DIALOG_UPDATE_WID);
			return true;
		} else if (item.getItemId() == R.id.menu_2) {
			/* run Medusalet */
			// MedusaUtil.log(TAG, "* trigger helloworld medusalet: " +
			// G.setBaseTime());
			// MedusaUtil.startMedusaletByName(this,
			// "medusalet_helloworld.apk");
			// MedusaUtil.startMedusaletByName(this,
			// "medusalet_getsummary.apk");
			return true;
		}
		/*
		 * switch (item.getItemId()) { case R.id.menu_1:
		 * 
		 * case R.id.menu_2: // run Medusalet // //MedusaUtil.log(TAG,
		 * "* trigger helloworld medusalet: " + G.setBaseTime());
		 * //MedusaUtil.startMedusaletByName(this, "medusalet_helloworld.apk");
		 * //MedusaUtil.startMedusaletByName(this, "medusalet_getsummary.apk");
		 * return true; }
		 */

		return false;
	}

	protected void GCMRegister() {
		GCMRegistrar.checkDevice(this);
		GCMRegistrar.checkManifest(this);
		final String regId = GCMRegistrar.getRegistrationId(this);
		if (!regId.equals("")) {
			Log.d("GCM ID", regId);
			// sendIdToServer(regId);
			G.updateRegistration(regId);// update gcm reg id
		} else {
			Log.d("GCM ID", "start to registering...");
			GCMRegistrar.register(this, G.SENDER_ID);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DIALOG_UPDATE_WID: {
				LayoutInflater inflater = (LayoutInflater) this
						.getSystemService(LAYOUT_INFLATER_SERVICE);
				View layout = inflater.inflate(R.layout.review_textdesc, null);

				TextView text = (TextView) layout
						.findViewById(R.id.textViewDesc);
				text.setText("Current C2DM ID is [" + G.C2DM_ID + "]. "
						+ "Do not use the default C2DM-ID [medusa]. "
						+ "Please change any nickname that you want to use.");

				String title = "Anonymized C2DM Identifier (ACI)";

				AlertDialog dialog = new AlertDialog.Builder(this)
						.setTitle(title)
						.setView(layout)
						.setPositiveButton("DONE",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										EditText editText = (EditText) ((AlertDialog) dialog)
												.getWindow().findViewById(
														R.id.editMultilineText);
										String val = editText.getEditableText()
												.toString();

										if (val.length() > 0) {
											G.C2DM_ID = val;
											MedusaUtil.log("T",
													"* GCM_ID has changed to ["
															+ G.C2DM_ID + "]");
											GCMRegister();
											G.saveToPrefs("C2DM_ID", G.C2DM_ID);
										} else {
											MedusaUtil
													.log("T",
															"! C2DM_ID has not changed");
										}

										dialog.dismiss();
										// finish();
									}
								})
						.setNegativeButton("DISMISS",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										dialog.dismiss();
										// finish();
									}
								})
						.setOnCancelListener(
								new DialogInterface.OnCancelListener() {
									public void onCancel(DialogInterface dialog) {
										// finish();
									}
								}).create();

				return dialog;
			}
			default:
				Log.e(TAG, "! Wrong Dialog ID=" + id);
		}
		return null;
	}

	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			/* default language is English. */
			int result = G.tts.setLanguage(Locale.US);

			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				/* Lanuage data is missing or the language is not supported. */
				MedusaUtil.log(TAG, "! Language(US) is not available, res="
						+ result);
			} else {
				G.tts.setLanguage(Locale.US);
				// MedusaUtil.talkByTTS("TTS enabled");
			}
		} else {
			/* TextToSpeech.ERROR: Initialization failed. */
			MedusaUtil.log(TAG, "! Could not initialize TextToSpeech.");
		}
	}
}
