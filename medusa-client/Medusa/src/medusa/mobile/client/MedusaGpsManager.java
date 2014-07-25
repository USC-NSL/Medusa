/**
 * 'MedusaGpsManager'
 *
 * - Manages medusalet's accesses to GPS.
 *   
 * @modified : May 14th 2012
 * @author   : Bin Liu (binliu@usc.edu), Jeongyeup Paek (jpaek@usc.edu)
 **/

package medusa.mobile.client;

import java.util.ListIterator;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;


public class MedusaGpsManager extends MedusaManagerBase 
{
	private static String TAG = "MedusaGpsMgr";
	
	private class locationTrigger 
	{	
	    public final static double PI   = 3.14159265354;  
	    private final static double D2R = 0.017453 ;  
	    private final static double a2  = 6378137.0;  
	    private final static double e2  = 0.006739496742337;  		
		
		private double lat;
		private double lng;
		private double dis;
		private String msg;
		
		public locationTrigger(double in_lat, double in_lng, double in_dis, String in_msg) 
		{			
			lat = in_lat; lng = in_lng; dis = in_dis;
			msg = in_msg;		
		}
		
		public String getMsg() { return msg; }
		
		public boolean within(double in_lat, double in_lng) 
		{	
	        if (lng == in_lng && lat == in_lat ) {  
	            return true;  
	        } 
	        else {  
	            double fdLambda =(lng - in_lng) * D2R;  
	            double fdPhi = (lat - in_lat) * D2R;  
	            double fPhimean = ((lat + in_lat) / 2.0) * D2R;  
	            double fTemp = 1 - e2 * (Math.pow (Math.sin(fPhimean),2));  
	            double fRho = (a2 * (1 - e2)) / Math.pow(fTemp, 1.5);  
	            double fNu = a2 / (Math.sqrt(1 - e2 * (Math.sin(fPhimean) * Math.sin(fPhimean))));  
	            double fz = Math.sqrt (Math.pow(Math.sin(fdPhi / 2.0), 2) +  
	                    Math.cos(in_lat*D2R)*Math.cos(lat*D2R )*Math.pow( Math.sin(fdLambda / 2.0),2));  
	            fz = 2 * Math.asin(fz);
	            double fAlpha = Math.cos(in_lat * D2R) * Math.sin(fdLambda) * 1 / Math.sin(fz);  
	            fAlpha = Math.asin (fAlpha);  
	            double fR = (fRho * fNu) / ((fRho * Math.pow( Math.sin(fAlpha),2)) + (fNu * Math.pow( Math.cos(fAlpha),2)));  
	            return fz*fR <= dis;  
	        }			
		}	
	}
	
	/*
	 * Static periodics for enforcing singleton pattern.
	 */
	private static MedusaGpsManager manager = null;
	private static int dim_num = 5;

	public static MedusaGpsManager getInstance() 
	{
		if (manager == null) {
			manager = new MedusaGpsManager();
			manager.init("MedusaGpsManager");
		}

		return manager;
	}

	public static void terminate() 
	{
		if (manager != null) {
			MedusaGpsServiceE ele;
			ListIterator<MedusaServiceActiveE> iterator = manager.activeServiceList.listIterator();
			while (iterator.hasNext()) {
				ele = (MedusaGpsServiceE) iterator.next();

				if (ele != null) { 
					if (ele.ret_method != null && (ele.ret_method.compareTo("file") == 0)
							&& (ele.currCnt > 0)) {
						/*
						 * if infinite count, insert metadata into db at the
						 * beginning
						 */
						MedusaStorageManager.addFileToDb("GPS_M", ele.log.getPath());
					}
					manager.activeServiceList.remove(ele);
				}
			}

			manager.stop(); // stop services
			manager.markExit();
			manager = null;
		}
	}

	class MedusaGpsServiceE extends MedusaServiceActiveE 
	{
		/* configuration parameters */
		float thresh;
		int interval;
		int count;
		int periodic; 		// yes:1, no:0
		String type;

		String ret_method; 	// file, location
		String fnamehead; 	// filename header
		FileLogger log;
		locationTrigger mTrigger;

		/* runtime variables */
		int currCnt;
		long lastUpdate;
		public double[][] frame;
		
		/* for lazy update */
		String pkey; /* path */
		int timeout;

		MedusaGpsServiceE() {
			super();
			currCnt = 0;
			lastUpdate = 0;
			log = null;
			frame = null;
			mTrigger = null;
			pkey = "";
			timeout = 10000;
		}
	}

	private static LocationManager gpsMgr;

	private static long common_interval;
	private static float common_thresh;

	// CPU will not sleep while this application is running
	private static PowerManager pm;
	private static WakeLock wakeLock;
	private static boolean isRunning;
	
	private Thread gpsThread;

	public static boolean isRunning() 
	{		
		return isRunning;
	}
	
	@Override
	public void init(String name) 
	{
		super.init(name);

		/**
		 * power manager for locking CPU so that the phone does not go into
		 * sleep
		 **/
		pm = (PowerManager) G.appCtx.getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GpsWakeLock");

		/** GPS manager **/
		gpsMgr = (LocationManager) G.appCtx.getSystemService(Context.LOCATION_SERVICE);

		isRunning = false;
		gpsThread = null;
	}
	
	public static void deRequestService(String medusalet_name) 
	{	
		if (manager != null) {
			MedusaGpsServiceE ele;
			ListIterator<MedusaServiceActiveE> iterator = manager.activeServiceList.listIterator();
			
			while (iterator.hasNext()) {
				ele = (MedusaGpsServiceE) iterator.next();

				if (ele.medusaletName.equals(medusalet_name)) {
					if ((ele.ret_method.compareTo("file") == 0)	&& (ele.currCnt > 0)) {
						/*
						 * if infinite count, insert metadata into db at the
						 * beginning
						 */
						MedusaStorageManager.addFileToDb("GPS_M", ele.log.getPath());
					}
					
					//iterator.remove();
					manager.activeServiceList.remove(ele);
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

	public boolean start() 
	{
		if (isRunning == false) {
			gpsThread = new Thread() {
				@Override
				public void run() {
					if (activeServiceList.isEmpty()) {
						Log.e(TAG, "! No GPS instances to serve.");
						return;
					}
					
					/* lock CPU so that the phone does not go into sleep */
					wakeLock.acquire();
			
					/* MRA: To use a handler in worker thread */
					Looper.prepare();
			
					/* request GPS update */
					gpsMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,
							common_interval, common_thresh, gpsLocationListener);
					
					gpsMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
							common_interval, common_thresh, gpsLocationListener);
					
					Log.d(TAG, "* starting GPS thread with interval=" 
												+ common_interval + " thresh=" + common_thresh);
					isRunning = true;
					Looper.loop();
						
					/* 
					 * after getting the location data, the thread will exit. 
					 */
					Log.d(TAG, "* quiting GPS thread: " + this.getId());
					
					gpsMgr.removeUpdates(gpsLocationListener);
					wakeLock.release();
					isRunning = false;
					gpsThread = null;
				}
			};
			
			gpsThread.start();
			
			return true;
		}
		else {
			return false;
		}
	}
	
	private void stop() 
	{
		if (isRunning == true) {
			gpsMgr.removeUpdates(gpsLocationListener); 	// release GPS
			gpsThread.interrupt();
			
			wakeLock.release(); 	// release CPU
			isRunning = false;
		}
	}

	private void restart() 
	{
		if (isRunning) {
			int cnt = 0;
			
			Log.i(TAG, "* GPS restarting...");
			
			//stop();
			while (start() == false && cnt++ < 60) {
				try {
					Thread.sleep(2000);	/* 2 sec. */
				} 
				catch (InterruptedException e) {
					e.printStackTrace();
				}	
			}
		} else {
			Log.e(TAG, "! GPS restart requested while not running");
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
	protected void execute(MedusaServiceE ele) 
	{
		Log.v(TAG, "* " + mgrName + "'s execute() is running.");

		/*
		 * XML format hierarchy.
		 * 
		 * - name - sensor - type - periodic - period - thres - return
		 */
		if (configMap.get("sensor_type").equals("gps")) {
			if (configMap.get("name").equals("LazyUpdateOnMetaTable") == true) {
				MedusaGpsServiceE aele = new MedusaGpsServiceE();
				
				aele.medusaletName = configMap.get("name");
				aele.interval = Integer.parseInt(configMap.get("sensor_interval"));
				aele.thresh = Integer.parseInt(configMap.get("sensor_thres"));
				aele.type = configMap.get("sensor_subtype");
				aele.pkey = configMap.get("sensor_path");
				aele.timeout = Integer.parseInt(configMap.get("sensor_timeout"));
				
				addActiveServiceE(aele, ele.seCBListner);
			}
			else if (configMap.get("name").equals("SystemLocationTrigger") == true) {
				MedusaGpsServiceE aele = new MedusaGpsServiceE();
				aele.medusaletName = configMap.get("name");
				aele.interval = Integer.parseInt(configMap.get("sensor_interval"));
				aele.type = configMap.get("sensor_subtype");
				aele.thresh = Integer.parseInt(configMap.get("sensor_thres"));
				
				double lat = Double.parseDouble(configMap.get("sensor_lat"));
				double lng = Double.parseDouble(configMap.get("sensor_lng"));
				double dis = Double.parseDouble(configMap.get("sensor_dis"));
				String msg = ele.seMsg;			
				
				aele.mTrigger = new locationTrigger(lat, lng, dis, msg);
				
				/* add this service request to currently active service list */
				addActiveServiceE(aele, null);
			}
			else {
				MedusaGpsServiceE aele = new MedusaGpsServiceE();
				aele.medusaletName = configMap.get("name");
				aele.interval = Integer.parseInt(configMap.get("sensor_interval"));
				aele.thresh = Integer.parseInt(configMap.get("sensor_thres"));
				aele.count = Integer.parseInt(configMap.get("sensor_count"));
				aele.periodic = (configMap.get("sensor_periodic").compareTo("periodic") == 0 ? 1 : 0);
				aele.ret_method = configMap.get("sensor_ret_method");
				aele.fnamehead = configMap.get("sensor_fnamehead");
				aele.type = configMap.get("sensor_subtype");
				
				//5: time, latitude, longitude, speed, accuracy : bin -- in the future, may directly return "Location"
				aele.frame = new double[aele.count][dim_num]; 
	
				/* add this service request to currently active service list */
				addActiveServiceE(aele, ele.seCBListner);
			} 
	
			/* find a common parameter set for running GPS */
			findCommonParam();			
			
			/* start GPS */
			if (isRunning() == false) start();
			//else restart();
		} 
		else {
			ele.seCBListner.callCBFunc(null, "sensor_type "
					+ configMap.get("sensor_type")
					+ " is wrong or not yet implemented.");
		}
	}

	private void updateWithNewGpsLocation(Location location) 
	{
		// get system time
		long now = System.currentTimeMillis();
		long timeDiff = 0;

		// see location
		if (location != null) {
			String provider = location.getProvider();
			double lat = location.getLatitude();
			double lng = location.getLongitude();
			double spd = location.getSpeed();
			double acc = location.getAccuracy();

			// String latLngString =
			// String.format("[GPS]\n Lat: %.6f \n Lng: %.6f \n Time: %d \n Acc: %.1f",
			// lat, lng, now/1000, acc);
			String latLngString = String.format(
					"[GPS] Lat %.6f Lng %.6f Time %d Spd %.1f Acc %.1f", lat, lng,
					now / 1000, spd, acc);

			MedusaGpsServiceE ele;
			ListIterator<MedusaServiceActiveE> iterator = activeServiceList.listIterator();
			Log.d(TAG, "* SIZE = " + activeServiceList.size());

			while (iterator.hasNext()) {
				ele = (MedusaGpsServiceE) iterator.next();

				if ((provider.equals(LocationManager.NETWORK_PROVIDER) && ele.type.equals("gps")) 
						|| (provider.equals(LocationManager.GPS_PROVIDER) && ele.type.equals("wps"))) {					
					continue;
				}	
				
				/* Type-specific operations */
				if (ele.medusaletName.equals("SystemLocationTrigger") == true) {
					if (ele.lastUpdate == 0) {
						ele.lastUpdate = now;
					} else {
						timeDiff = now - ele.lastUpdate;
						if (timeDiff >= ele.interval) {
							ele.lastUpdate = now;
						}
					}
					
					if (ele.lastUpdate == now && ele.mTrigger.within(lat, lng)) {
						//MedusaUtil.startMedusaletByXML(G.appCtx, ele.mTrigger.getMsg());
						MedusaUtil.invokeMedusalet(ele.mTrigger.getMsg());
						//iterator.remove();
						activeServiceList.remove(ele);
					}			
					continue;
				}
				else if (ele.medusaletName.equals("LazyUpdateOnMetaTable") == true) {
					/* lazy update */
					StringBuilder sb = new StringBuilder();
					sb.append("UPDATE mediameta set lat='" + lat + "', lng='" + lng + "' where path='" + ele.pkey + "'");
					MedusaStorageManager.requestServiceSQLite("temp", null, "medusadata.db", sb.toString());
					
					Log.d(TAG, "* GPS [" + lat + "," + lng + "] has updated on [" + ele.pkey + "]");
					
					activeServiceList.remove(ele);
					continue;
				}
				else {
					MedusaUtil.log(TAG, "! unknown gps-req type=" + ele.medusaletName);
				}
										
				if (ele.lastUpdate == 0) {
					ele.frame[ele.currCnt] = new double[] {now, lat, lng, spd, acc};
					ele.lastUpdate = now;					
				} 
				else {
					timeDiff = now - ele.lastUpdate;
					if (timeDiff >= ele.interval) {
						ele.frame[ele.currCnt] = new double[] {now, lat, lng, spd, acc};
						ele.lastUpdate = now;
					}
				}
				
				if (ele.lastUpdate == now) {
					if (ele.ret_method.compareTo("file") == 0) {
						/* if return method is 'file', create a file for GPS log */
						if (ele.currCnt == 0) {
							ele.log = new FileLogger(MedusaStorageManager.generateTimestampFilename(ele.fnamehead), "GMT");
						}
						ele.log.printlnwt(latLngString);
					}
					
					if (ele.count != 0 && ++ele.currCnt >= ele.count) {
						if (ele.ret_method.compareTo("file") == 0) {
							/*
							 * we are done with the file. insert metadata into db.
							 */
							MedusaStorageManager.addFileToDb("GPS_M", ele.log.getPath());
						}
						
						double[][] r = new double[ele.count][dim_num];
						for (int m = 0; m < ele.count; m++) {
							for (int n = 0; n < dim_num; n++) {
								r[m][n] = ele.frame[m][n];		
							} 
						}
						
						ele.MedusaServiceListner.callCBFunc(r, latLngString);
						ele.currCnt = 0;

						if (configMap.get("sensor_periodic").compareTo("periodic") != 0) {
							// we are done with this active service. remove from list.
							//iterator.remove();
							activeServiceList.remove(ele);
						}
					}
				}
			}			
			
			if (activeServiceList.isEmpty()) {
				Looper.myLooper().quit();
			}
		}
	}

	private final LocationListener gpsLocationListener = new LocationListener() 
	{
		@Override
		public void onLocationChanged(Location location) {
			Log.i(TAG, "* onLocationChanged " + location.getProvider());
			updateWithNewGpsLocation(location);
		}

		@Override
		public void onProviderDisabled(String provider) {
			Log.e(TAG, "* onProviderDisabled: " + provider);
		}

		@Override
		public void onProviderEnabled(String provider) {
			Log.e(TAG, "* onProviderEnabled: " + provider);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			Log.i(TAG, "* onStatusChanged: " + provider + " status: " + status);
		}
	};

	private void findCommonParam() 
	{
		MedusaGpsServiceE ele;
		ListIterator<MedusaServiceActiveE> iterator = activeServiceList.listIterator();

		float min_thresh = -1;
		long min_interval = -1;
		long min_diff_interval = -1;
		long diff_interval;

		while (iterator.hasNext()) {
			ele = (MedusaGpsServiceE) iterator.next();

			if (min_thresh < 0) {
				min_thresh = ele.thresh;
			} else if (ele.thresh < min_thresh) {
				min_thresh = ele.thresh;
			}

			if (min_interval < 0) {
				min_interval = ele.interval;
			} else if (ele.interval < min_interval) {
				min_interval = ele.interval;
			}

			if (min_diff_interval < 0) {
				min_diff_interval = ele.interval;
			} else if (ele.interval > min_interval) {
				diff_interval = ele.interval - min_interval;
				if (diff_interval < min_diff_interval) {
					min_diff_interval = diff_interval;
				}
			}
		}

		if (min_interval < min_diff_interval) {
			common_interval = min_interval;
		} else {
			common_interval = min_diff_interval;
		}
		common_thresh = min_thresh;
	}

	/* 
	 * Request functions. 
	 */
	public static void requestLocationTrigger(double lat, double lng, double dis, String xml_msg)
	{		
		StringBuilder b = new StringBuilder();
		b.append("<xml>");
			b.append("<name>SystemLocationTrigger</name>");
			b.append("<sensor>");
				b.append("<type>gps</type>");
				b.append("<interval>" + 10 + "</interval>");
				b.append("<thres>0</thres>");
				b.append("<subtype>hps</subtype>");		
				b.append("<lat>" + lat + "</lat>");
				b.append("<lng>" + lng + "</lng>");		
				b.append("<dis>" + dis + "</dis>");
			b.append("</sensor>");
		b.append("</xml>");

		MedusaServiceE ele = new MedusaServiceE();
		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = null;
		ele.seMsg = xml_msg;
		
		try {
			MedusaGpsManager.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void requestUpdateGpsOnMetaTable(String tag, String path, int timeout) 
	{
		/* YX project: lazy GPS update onto metadata table entry */
		StringBuilder b = new StringBuilder();
		b.append("<xml>");
			b.append("<name>LazyUpdateOnMetaTable</name>");
			b.append("<sensor>");
				b.append("<type>gps</type>");
				b.append("<interval>1</interval>");
				b.append("<thres>0</thres>");		
				b.append("<path>" + path + "</path>");
				b.append("<timeout>" + timeout + "</timeout>");
				b.append("<subtype>hps</subtype>");
			b.append("</sensor>");
		b.append("</xml>");

		MedusaServiceE ele = new MedusaServiceE();
		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = null;
		
		try {
			MedusaGpsManager.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Log.v(TAG, "* requested lazy GPS info update for [" + path + "]");
	}
	
	public static void requestService(String name,
			MedusaletCBBase listener, int interval, int thres,
			int count, String periodic, String ret_method, String fnamehead, String type) 
	{		
		StringBuilder b = new StringBuilder();
		b.append("<xml>");
			b.append("<name>" + name + "</name>");
			b.append("<sensor>");
				b.append("<type>gps</type>");
				b.append("<interval>" + interval + "</interval>");
				b.append("<thres>" + thres + "</thres>");
				b.append("<count>" + count + "</count>");
				b.append("<periodic>" + periodic + "</periodic>");
				b.append("<ret_method>" + ret_method + "</ret_method>");
				b.append("<fnamehead>" + fnamehead + "</fnamehead>");
				b.append("<subtype>" + type + "</subtype>");
			b.append("</sensor>");
		b.append("</xml>");

		MedusaServiceE ele = new MedusaServiceE();
		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = listener;

		try {
			MedusaGpsManager.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
