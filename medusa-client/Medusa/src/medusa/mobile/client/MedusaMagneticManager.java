/**
 * 'MedusaMagneticManager'
 *
 * - Manages medusalet's access to the magnetic field sensor
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

public class MedusaMagneticManager extends MedusaManagerBase {

	/*
	 * Static methods for enforcing singleton pattern.
	 */
	private static MedusaMagneticManager manager = null;

	public static MedusaMagneticManager getInstance() {
		if (manager == null) {
			manager = new MedusaMagneticManager();
			manager.init("MedusaMagneticManager");
		}

		return manager;
	}

	public static void terminate() {
		if (manager != null) {

			MedusaMagneticServiceE ele;
			ListIterator<MedusaServiceActiveE> iterator = manager.activeServiceList
					.listIterator();
			while (iterator.hasNext()) {
				ele = (MedusaMagneticServiceE) iterator.next();

				if ((ele.ret_method.compareTo("file") == 0)
						&& (ele.currCnt > 0)) {
					/*
					 * if infinite count, insert metadata into db at the
					 * beginning
					 */
					MedusaStorageManager.addFileToDb("Magnetic_M",
							ele.log.getPath());
				}
				iterator.remove();
			}

			manager.stop(); // stop services
			manager.markExit();
			manager = null;
		}
	}

	class MedusaMagneticServiceE extends MedusaServiceActiveE {
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

		MedusaMagneticServiceE() {
			super();
			currCnt = 0;
			lastUpdate = 0;
		}
	}

	private static Sensor sensor;
	private static SensorManager sensorManager;

	// CPU will not sleep while this application is running
	private static PowerManager pm;
	private static WakeLock wakeLock;
	private static boolean isRunning;

	@Override
	public void init(String name) {
		super.init(name);

		/**
		 * power manager for locking CPU so that the phone does not go into
		 * sleep
		 **/
		pm = (PowerManager) G.appCtx.getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"MagneticWakeLock");

		/** Magnetic manager **/
		sensorManager = (SensorManager) G.appCtx.getSystemService(Context.SENSOR_SERVICE);
		List<Sensor> sensors = sensorManager
				.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		if (sensors.size() > 0) {
			sensor = sensors.get(0);
		}

		/** indicates whether or not Magnetic Sensor is running */
		isRunning = false;
	}

	private void start() {
		if (!isRunning) {
			if (activeServiceList.isEmpty()) {
				Log.i("MedusaMagneticMgr",
						"Fatal error. ActiveService list empty in start()");
				return;
			}

			/** lock CPU so that the phone does not go into sleep **/
			wakeLock.acquire();

			/** request Magnetic update **/
			isRunning = sensorManager.registerListener(sensorEventListener,
					sensor, SensorManager.SENSOR_DELAY_GAME);

			if (isRunning) {
				Log.i("MedusaMagneticMgr", "OK, Magnetic sensor started");
			} else {
				Log.i("MedusaMagneticMgr", "Fail to start Magnetic sensor");
			}
		} else {
			Log.i("MedusaMagneticMgr", "Magnetic sensor already running");
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

		if (configMap.get("sensor_type").equals("magnetic")) {

			MedusaMagneticServiceE aele = new MedusaMagneticServiceE();
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

		private float x = 0;
		private float y = 0;
		private float z = 0;

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {

			x = event.values[0];
			y = event.values[1];
			z = event.values[2];

			now = event.timestamp;

			MedusaMagneticServiceE ele;
			ListIterator<MedusaServiceActiveE> iterator = activeServiceList
					.listIterator();

			while (iterator.hasNext()) {
				ele = (MedusaMagneticServiceE) iterator.next();

				if (ele.lastUpdate == 0) {
					ele.lastUpdate = now;
				} else {
					timeDiff = now - ele.lastUpdate;
					if (timeDiff >= ele.interval) {
						ele.lastUpdate = now;
					}
				}

				if (ele.lastUpdate == now) {
					String magneticString = String.format(
							"Magnetic: %.1f %.1f %.1f", x, y, z);

					// forwards magnetic to the MagneticListener
					if (ele.ret_method.compareTo("file") == 0) {
						/* if return method is 'file', create a file for GPS log */
						if (ele.currCnt == 0) {
							Log.i("MedusaMagnetic",
									"Creating new log file starting with: "
											+ magneticString);
							ele.log = new FileLogger(
									MedusaStorageManager
											.generateTimestampFilename(ele.fnamehead));
						}

						// write to the file!!
						ele.log.printlnwt(magneticString);
					} else {
						ele.MedusaServiceListner.cbPostProcess(event,
								magneticString);
					}

					if (ele.count != 0 && ++ele.currCnt >= ele.count) {
						if (ele.ret_method.compareTo("file") == 0) {
							/*
							 * we are done with the file. insert metadata into
							 * db
							 */
							MedusaStorageManager.addFileToDb("Magnetic_M",
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
		b.append("<type>magnetic</type>");
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
			MedusaMagneticManager.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
