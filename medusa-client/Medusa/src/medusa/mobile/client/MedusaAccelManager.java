/**
 * 'MedusaAccelManager'
 *
 * - Manages medusalet's accesses to accelerometer.
 *   
 * @modified : Nov. 8th 2011
 * @author   : Bin Liu (binliu@usc.edu), Jeongyeup Paek (jpaek@usc.edu)		    	
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

public class MedusaAccelManager extends MedusaManagerBase {

	
public class MedusaAccelDataStructure implements Cloneable {
		
		public float x;
		public float y;
		public float z;
		public long timeTick;
		
		@Override
		public Object clone() {
			try { 
				// call clone in Object. 
				return super.clone(); 
			} catch(CloneNotSupportedException e) { 
				e.printStackTrace(); 
				return null; 
			} 
		} 
		
		public MedusaAccelDataStructure(float _x, float _y, float _z, long _timeTick) {
			x = _x;
			y = _y;
			z = _z;
			timeTick = _timeTick;			
		}
}
	
	/*
	 * Static methods for enforcing singleton pattern.
	 */
	private static MedusaAccelManager manager = null;

	public static MedusaAccelManager getInstance() {
		if (manager == null) {
			manager = new MedusaAccelManager();
			manager.init("MedusaAccelManager");
		}

		return manager;
	}
	
	public static void deRequestService(String medusalet_name) {
		
		if (manager != null) {

			MedusaAccelServiceE ele;
			ListIterator<MedusaServiceActiveE> iterator = manager.activeServiceList
					.listIterator();
			while (iterator.hasNext()) {
				ele = (MedusaAccelServiceE) iterator.next();

				if (ele.medusaletName.equals(medusalet_name)) {
					
					if ((ele.ret_method.compareTo("file") == 0)
							&& (ele.currCnt > 0)) {
						/*
						 * if infinite count, insert metadata into db at the
						 * beginning
						 */
						MedusaStorageManager
								.addFileToDb("Accel_M", ele.log.getPath());
					}
					
					iterator.remove();
					ele = null;
					
					break;
				}
			}

			if (manager.activeServiceList.isEmpty()) {
				// if there are no more active services, stop
				manager.stop(); // stop services
				manager.markExit();
				manager = null;
			}
			
		}
		
		
		
	}

	public static void terminate() {
		if (manager != null) {

			MedusaAccelServiceE ele;
			ListIterator<MedusaServiceActiveE> iterator = manager.activeServiceList
					.listIterator();
			while (iterator.hasNext()) {
				ele = (MedusaAccelServiceE) iterator.next();

				if ((ele.ret_method.compareTo("file") == 0)
						&& (ele.currCnt > 0)) {
					/*
					 * if infinite count, insert metadata into db at the
					 * beginning
					 */
					MedusaStorageManager
							.addFileToDb("Accel_M", ele.log.getPath());
				}
				
				iterator.remove();
				ele = null;
			}

			manager.stop(); // stop services
			manager.markExit();
			manager = null;
		}
	}

	class MedusaAccelServiceE extends MedusaServiceActiveE {
		/* configuration parameters */
		public int interval; // interval per sample
		public int frameLength; // sample number per frame
		public int count; // how many total frames we want to get before storing
							// to file and return
		public boolean periodic; // yes 1, no 0

		public String ret_method; // file, location
		public String fnamehead; // filename header
		public Boolean original;

		/* runtime variables */
		public int currCnt;
		public int currFrameIndex;
		public long lastUpdate;
		public double[][] frame;
		public MedusaAccelDataStructure[][] struct_frame;
		public FileLogger log;

		MedusaAccelServiceE() {
			super();
			currCnt = 0;
			currFrameIndex = 0;
			lastUpdate = 0;
			frame = null;
			struct_frame = null;
			log = null;
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
				"AccelWakeLock");

		/** Accel manager **/
		sensorManager = (SensorManager) G.appCtx.getSystemService(Context.SENSOR_SERVICE);
		List<Sensor> sensors = sensorManager
				.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (sensors.size() > 0) {
			sensor = sensors.get(0);
		}

		/** indicates whether or not Accelerometer Sensor is running */
		isRunning = false;
	}

	private void start() {
		if (!isRunning) {
			if (activeServiceList.isEmpty()) {
				Log.i("MedusaAccel",
						"Fatal error. ActiveService list empty in start()");
				return;
			}

			/** lock CPU so that the phone does not go into sleep **/
			wakeLock.acquire();

			/** request Accel update **/
			isRunning = sensorManager.registerListener(sensorEventListener,
					sensor, SensorManager.SENSOR_DELAY_FASTEST);

			if (isRunning) {
				Log.i("MedusaAccel", "OK, Accel started");
			} else {
				Log.i("MedusaAccel", "Fail to start Accel");
			}
		} else {
			Log.i("MedusaAccel", "Accel already running");
		}
	}

	private void stop() {
		// no pending requests
		if (isRunning) {
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

		Log.i("MedusaAccel", "* " + mgrName + "'s execute() is running.");
		Log.i("MedusaAccel", "* Config. parameter reading test: expname -> ["
				+ configMap.get("expname") + "]");
		Log.i("MedusaAccel", "* Config. parameter reading test: sensor_type -> ["
				+ configMap.get("sensor_type") + "]");

		/*
		 * XML format hierarchy.
		 * 
		 * - expname - sensor - type - method - period - thres
		 */

		if (configMap.get("sensor_type").equals("accelerometer")) {

			MedusaAccelServiceE aele = new MedusaAccelServiceE();
			aele.medusaletName = configMap.get("expname");
			aele.interval = Integer.parseInt(configMap.get("sensor_interval"));
			aele.frameLength = Integer.parseInt(configMap
					.get("sensor_frameLength"));
			aele.count = Integer.parseInt(configMap.get("sensor_count"));
			aele.periodic = (configMap.get("sensor_periodic").compareTo(
					"periodic") == 0 ? true : false);
			aele.ret_method = configMap.get("sensor_ret_method");
			aele.fnamehead = configMap.get("sensor_fnamehead");
			aele.original = Boolean.parseBoolean(configMap.get("sensor_original"));
			
			if (aele.original)
			{
				aele.struct_frame = new MedusaAccelDataStructure[aele.count][aele.frameLength];
			} else {
				aele.frame = new double[aele.count][aele.frameLength];
			}

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

			//now = event.timestamp;
			now = System.currentTimeMillis();

			x = event.values[0];
			y = event.values[1];
			z = event.values[2];

			MedusaAccelServiceE ele;
			ListIterator<MedusaServiceActiveE> iterator = activeServiceList
					.listIterator();

			while (iterator.hasNext()) {
				ele = (MedusaAccelServiceE) iterator.next();

				if (ele.lastUpdate == 0) {
					ele.lastUpdate = now;
				} else {
					timeDiff = now - ele.lastUpdate;
					if (timeDiff >= ele.interval) {
						if (ele.currFrameIndex < ele.frameLength) {
							
							if (ele.original) {
								ele.struct_frame[ele.currCnt][ele.currFrameIndex] = new MedusaAccelDataStructure(x, y, z, now);
							} else {			
								ele.frame[ele.currCnt][ele.currFrameIndex] = Math.sqrt(x * x + y * y + z * z);								
							}
							ele.currFrameIndex++;
						}

						ele.lastUpdate = now;
					}
				}

				if (ele.currFrameIndex == ele.frameLength) {

					if (ele.ret_method.compareTo("file") == 0) {
						/* if return method is 'file', create a file for log */
						if (ele.currCnt == 0) {
							Log.i("MedusaAccel", "Creating new log file for MedusaAccelManager.");
							ele.log = new FileLogger(
									MedusaStorageManager.generateTimestampFilename(ele.fnamehead));
						}

						String accelString = "Accel";
						int j = ele.currCnt;
						if (ele.original) {
							for (int i = 0; i < ele.frameLength; i++) {
								MedusaAccelDataStructure aad = ele.struct_frame[j][i];
								accelString = String.format(" %.4f %.4f %.4f %d", aad.x, aad.y, aad.z, aad.timeTick);								
							}						
						} else {
							for (int i = 0; i < ele.frameLength; i++) {							
								accelString = String.format(" %.4f", ele.frame[j][i]);
							}
						}

						// write to the file!!
						ele.log.printlnwt(accelString);
					}

					ele.currFrameIndex = 0;

					if (ele.count != 0 && ++ele.currCnt >= ele.count) {
						if (ele.ret_method.compareTo("file") == 0) {
							/*
							 * we are done with the file. insert metadata into db
							 */
							MedusaStorageManager.addFileToDb("Accel_M", ele.log.getPath());
						}						
						
						if (ele.original) {
							
							MedusaAccelDataStructure[][] r = new MedusaAccelDataStructure[ele.count][ele.frameLength];
							for (int m = 0; m < ele.count; m++)
								for (int n = 0; n < ele.frameLength; n++)
									r[m][n] = (MedusaAccelDataStructure)ele.struct_frame[m][n].clone();
							
							ele.MedusaServiceListner.callCBFunc(r, "");
						} else {
							double[][] r = new double[ele.count][ele.frameLength];
							for (int m = 0; m < ele.count; m++)
								for (int n = 0; n < ele.frameLength; n++)
									r[m][n] = ele.frame[m][n];
							
							ele.MedusaServiceListner.callCBFunc(r, ""); // may return file information in the future						
						}
						
						ele.currCnt = 0;

						if (!ele.periodic) {
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
			MedusaletCBBase listener, int interval, int frameLength,
			int count, String periodic, String ret_method, String fnamehead, Boolean original) {

		StringBuilder b = new StringBuilder();
		b.append("<xml>");
		b.append("<expname>");
		b.append(expname);
		b.append("</expname>");
		b.append("<sensor>");
		b.append("<type>accelerometer</type>");
		b.append("<interval>");
		b.append(interval);
		b.append("</interval>");
		b.append("<frameLength>");
		b.append(frameLength);
		b.append("</frameLength>");
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
		b.append("<original>");
		b.append(original);
		b.append("</original>");
		b.append("</sensor>");
		b.append("</xml>");

		MedusaServiceE ele = new MedusaServiceE();
		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = listener;

		try {
			MedusaAccelManager.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
