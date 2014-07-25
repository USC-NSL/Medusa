/**
 * 'MedusaOrientationManager'
 *
 * - Manages medusalet's access to orientation sensor.
 *   
 * @modified : Feb. 7th 2011
 * @author   : Jeongyeup Paek (jpaek@usc.edu)
 **/

package medusa.mobile.client;

import java.util.List;
import java.util.ListIterator;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class MedusaOrientationManager extends MedusaManagerBase {

	/*
	 * Static methods for enforcing singleton pattern.
	 */
	private static MedusaOrientationManager manager = null;

	public static MedusaOrientationManager getInstance() {
		if (manager == null) {
			manager = new MedusaOrientationManager();
			manager.init("MedusaOrientationManager");
		}

		return manager;
	}

	public static void terminate() {
		if (manager != null) {

			MedusaOrientationServiceE ele;
			ListIterator<MedusaServiceActiveE> iterator = manager.activeServiceList
					.listIterator();
			while (iterator.hasNext()) {
				ele = (MedusaOrientationServiceE) iterator.next();

				if ((ele.ret_method.compareTo("file") == 0)
						&& (ele.currCnt > 0)) {
					/*
					 * if infinite count, insert metadata into db at the
					 * beginning
					 */
					MedusaStorageManager.addFileToDb("Orient_M",
							ele.log.getPath());
				}
				iterator.remove();
			}

			manager.stop(); // stop services
			manager.markExit();
			manager = null;
		}
	}

	class MedusaOrientationServiceE extends MedusaServiceActiveE {
		/* configuration parameters */
		public int interval;
		public int count;
		int periodic; // yes:1, no:0

		String ret_method; // file, location
		String fnamehead; // filename header
		FileLogger log;

		/* runtime variables */
		private int currCnt;
		private long lastUpdate;
		private Side reportSide;

		MedusaOrientationServiceE() {
			super();
			currCnt = 0;
			lastUpdate = 0;
			reportSide = null;
		}
	}

	private static Sensor sensor;
	private static SensorManager sensorManager;

	// CPU will not sleep while this application is running
	private static PowerManager pm;
	private static WakeLock wakeLock;
	private static boolean isRunning;

	/** Sides of the phone */
	enum Side {
		TOP, BOTTOM, LEFT, RIGHT;
	}

	@Override
	public void init(String name) {
		super.init(name);

		/**
		 * power manager for locking CPU so that the phone does not go into
		 * sleep
		 **/
		pm = (PowerManager) G.appCtx.getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"OrientationWakeLock");

		/** Orientation manager **/
		sensorManager = (SensorManager) G.appCtx.getSystemService(Context.SENSOR_SERVICE);
		List<Sensor> sensors = sensorManager
				.getSensorList(Sensor.TYPE_ORIENTATION);
		if (sensors.size() > 0) {
			sensor = sensors.get(0);
		}

		/** indicates whether or not Orientation Sensor is running */
		isRunning = false;
	}

	private void start() {
		if (!isRunning) {
			if (activeServiceList.isEmpty()) {
				Log.i("MedusaOrientationMgr",
						"Fatal error. ActiveService list empty in start()");
				return;
			}

			/** lock CPU so that the phone does not go into sleep **/
			wakeLock.acquire();

			/** request Orientation update **/
			isRunning = sensorManager.registerListener(sensorEventListener,
					sensor, SensorManager.SENSOR_DELAY_NORMAL);

			if (isRunning) {
				Log.i("MedusaOrientationMgr", "OK, Orientation sensor started");
			} else {
				Log.i("MedusaOrientationMgr", "Fail to start Orientation sensor");
			}
		} else {
			Log.i("MedusaOrientationMgr", "Orientation sensor already running");
		}
	}

	private void stop() {
		if (isRunning) {
			// no pending requests
			try {
				if (sensorManager != null && sensorEventListener != null) {
					sensorManager.unregisterListener(sensorEventListener);
				}
			} catch (Exception e) {
			}
			wakeLock.release(); // release CPU
			isRunning = false;
		}
	}

	/*
	 * Define behavior per service request.
	 * 
	 * @see
	 * medusa.mobile.client.MedusaServiceManager#execute(medusa.mobile.client.MedusaServiceE
	 * ) - don't use while loop here, it will prohibit to serve other service
	 * requests.
	 */
	@Override
	protected void execute(MedusaServiceE ele) {

		Log.i("MedusaMgr", "* " + mgrName + "'s execute() is running.");
		Log.i("MedusaMgr", "* Config. parameter reading test: expname -> ["
				+ configMap.get("expname") + "]");
		Log.i("MedusaMgr", "* Config. parameter reading test: sensor_type -> ["
				+ configMap.get("sensor_type") + "]");

		/*
		 * XML format hierarchy.
		 * 
		 * - expname - sensor - type - method - period - thres
		 */

		if (configMap.get("sensor_type").equals("orientation")) {

			MedusaOrientationServiceE aele = new MedusaOrientationServiceE();
			aele.interval = Integer.parseInt(configMap.get("sensor_interval"));
			aele.count = Integer.parseInt(configMap.get("sensor_count"));
			aele.periodic = (configMap.get("sensor_periodic").compareTo(
					"periodic") == 0 ? 1 : 0);
			aele.ret_method = configMap.get("sensor_ret_method");
			aele.fnamehead = configMap.get("sensor_fnamehead");

			/* add this service request to currently active service list */
			addActiveServiceE(aele, ele.seCBListner);

			/* start accelerometer */
			start();
		} else {
			ele.seCBListner.callCBFunc(null, "sensor_type "
					+ configMap.get("sensor_type")
					+ " is wrong or not yet implemented.");
		}
	}

	private SensorEventListener sensorEventListener = new SensorEventListener() {

		private long now = 0;
		private long timeDiff = 0;

		/** The side that is currently up */
		private Side currentSide = null;

		private float azimuth;
		private float pitch;
		private float roll;

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {

			azimuth = event.values[0]; // azimuth
			pitch = event.values[1]; // pitch
			roll = event.values[2]; // roll

			if (pitch < -45 && pitch > -135) {
				// top side up
				currentSide = Side.TOP;
			} else if (pitch > 45 && pitch < 135) {
				// bottom side up
				currentSide = Side.BOTTOM;
			} else if (roll > 45) {
				// right side up
				currentSide = Side.RIGHT;
			} else if (roll < -45) {
				// left side up
				currentSide = Side.LEFT;
			}

			now = event.timestamp;

			MedusaOrientationServiceE ele;
			ListIterator<MedusaServiceActiveE> iterator = activeServiceList
					.listIterator();

			while (iterator.hasNext()) {
				ele = (MedusaOrientationServiceE) iterator.next();

				if (ele.lastUpdate == 0) {
					ele.lastUpdate = now;
				} else {
					timeDiff = now - ele.lastUpdate;
					if (timeDiff >= ele.interval) {
						ele.lastUpdate = now;
					}
				}

				if (ele.lastUpdate == now) {
					String orientationString = String.format(
							"Orientation: azimuth %.1f pitch %.1f roll %.1f",
							azimuth, pitch, roll);
					String sideString = null;

					if (currentSide != null
							&& !currentSide.equals(ele.reportSide)) {
						switch (currentSide) {
						case TOP:
							sideString = "Top Up";
							break;
						case BOTTOM:
							sideString = "Bottom Up";
							break;
						case LEFT:
							sideString = "Left Up";
							break;
						case RIGHT:
							sideString = "Right Up";
							break;
						}
						ele.reportSide = currentSide;
					}

					// forwards orientation to the OrientationListener
					if (ele.ret_method.compareTo("file") == 0) {
						/* if return method is 'file', create a file for GPS log */
						if (ele.currCnt == 0) {
							Log.i("MedusaOrientation",
									"Creating new log file starting with: "
											+ orientationString);
							ele.log = new FileLogger(
									MedusaStorageManager
											.generateTimestampFilename(ele.fnamehead));
						}

						// write to the file!!
						ele.log.printlnwt(orientationString);
						if (sideString != null)
							ele.log.printlnwt(sideString);
					} else {
						ele.MedusaServiceListner.cbPostProcess(event,
								orientationString);
						if (sideString != null)
							ele.MedusaServiceListner.cbPostProcess(event,
									sideString);
					}

					if (ele.count != 0 && ++ele.currCnt >= ele.count) {
						if (ele.ret_method.compareTo("file") == 0) {
							/*
							 * we are done with the file. insert metadata into
							 * db
							 */
							MedusaStorageManager.addFileToDb("Orientation_M",
									ele.log.getPath());
						}
						ele.currCnt = 0;

						if (configMap.get("sensor_periodic").compareTo(
								"periodic") != 0) {
							// we are done with this active service. remove from
							// list.
							iterator.remove();
						}
					}

				}
			}

			if (activeServiceList.isEmpty()) {
				// if there are no more active services, stop
				stop();
			}

		}
	};

	public static void requestService(String expname,
			MedusaletCBBase listener, int interval, int count,
			String periodic, String ret_method, String fnamehead) {

		MedusaServiceE ele = new MedusaServiceE();
		StringBuilder b = new StringBuilder();
		b.append("<xml>");
		b.append("<expname>");
		b.append(expname);
		b.append("</expname>");
		b.append("<sensor>");
		b.append("<type>orientation</type>");
		b.append("<interval>");
		b.append(interval);
		b.append("</interval>");
		b.append("<count>");
		b.append(count);
		b.append("</count>");
		b.append("<periodic>");
		b.append(periodic);
		b.append("</periodic>");
		b.append("<ret_method>");
		b.append(ret_method);
		b.append("</ret_method>");
		b.append("<fnamehead>");
		b.append(fnamehead);
		b.append("</fnamehead>");
		b.append("</sensor>");
		b.append("</xml>");

		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = listener;

		try {
			MedusaOrientationManager.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
