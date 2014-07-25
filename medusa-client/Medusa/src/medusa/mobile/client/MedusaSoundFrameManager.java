/**
 * 'MedusaSoundFrameManager'
 *
 * - Manages medusalet's accesses to sound sensor.
 *   
 * @modified : Nov. 3rd 2011
 * @author   : Bin Liu (binliu@usc.edu)		    	
 **/

package medusa.mobile.client;

import java.util.ListIterator;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;

public class MedusaSoundFrameManager extends MedusaManagerBase {

	/*
	 * Static periodics for enforcing singleton pattern.
	 */
	private static MedusaSoundFrameManager manager = null;

	public static MedusaSoundFrameManager getInstance() {
		if (manager == null) {
			manager = new MedusaSoundFrameManager();
			manager.init("MedusaSoundFrameManager");
		}

		return manager;
	}

	public static void terminate() {
		if (manager != null) {

			MedusaSoundFrameServiceE ele;
			ListIterator<MedusaServiceActiveE> iterator = manager.activeServiceList.listIterator();
			
			while (iterator.hasNext()) {
				ele = (MedusaSoundFrameServiceE) iterator.next();
				
				if ((ele.ret_method.compareTo("file") == 0)
						&& (ele.currFrameIndex > 0)) {
					/*
					 * if infinite count, insert metadata into db at the
					 * beginning
					 */
					MedusaStorageManager
							.addFileToDb("Sound_M", ele.log.getPath());
				}				
				
				iterator.remove();
				ele = null;
			}

			manager.stop(); // stop services
			manager.markExit();
			manager = null;
		}
	}
	
	public static void deRequestService(String medusalet_name) {
		
		if (manager != null) {

			MedusaSoundFrameServiceE ele;
			ListIterator<MedusaServiceActiveE> iterator = manager.activeServiceList.listIterator();
			
			while (iterator.hasNext()) {
				ele = (MedusaSoundFrameServiceE) iterator.next();
				
				if (ele.medusaletName.equals(medusalet_name)) {
					if ((ele.ret_method.compareTo("file") == 0)
							&& (ele.currFrameIndex > 0)) {
						/*
						 * if infinite count, insert metadata into db at the
						 * beginning
						 */
						MedusaStorageManager
								.addFileToDb("Sound_M", ele.log.getPath());
					}				
					
					iterator.remove();
					ele = null;
					break;
				}
			}

			if (manager.activeServiceList.isEmpty()) {
				manager.stop(); // stop services
				manager.markExit();
				manager = null;
			}
		}		
		
		
	}

	class MedusaSoundFrameServiceE extends MedusaServiceActiveE {
		/* configuration parameters */
		public int frameNum; // how many frames we want, NOTE: this is not the
								// buffer length! Since frames may be
								// overlapped!
		public Boolean periodic; // yes:1, no:0
		
		public String ret_method; // file, location
		public String fnamehead; // filename header		

		/* runtime variables */
		public int currFrameIndex;
		public double[] frameBuffer;
		public int frameBufferLen;
		public FileLogger log;		

		MedusaSoundFrameServiceE() {
			super();
			currFrameIndex = 0;
			frameBuffer = null;
			frameBufferLen = 0;
			log = null;
		}
	}

	// try to maintain only one pair of recorder and track
	private static AudioRecord recorder = null;
	private static AudioTrack track = null;

	// the below paramters need to be set before, or else will use the default
	// value
	public static int sampleRate = 8000;
	private static double winSecond = 0.064;
	private static double overlapSecond = 0.032;
	public static boolean mPlay = true; // play the sound or not
	// the below parameters are calculated according to the above
	public static double sampleSecond = 1.0 / sampleRate;
	private static int singleBufferLen = (int) ((overlapSecond == 0 ? winSecond
			: overlapSecond) / sampleSecond);
	private static int bufferNum = 256;// (int)(5.12/overlapSecond);
	public static int winLen = (int) (winSecond / sampleSecond);
	public static int overlapWinLen = (overlapSecond == 0 ? 0 : singleBufferLen);
	private static short[][] buffers = new short[bufferNum][singleBufferLen]; // use
																				// multiple
																				// buffers
	private static double scale;
	private static int ix;
	private static int N;

	// CPU will not sleep while this application is running
	private static PowerManager pm;
	private static WakeLock wakeLock;
	private static boolean isRunning;	
	private static Thread running_instance = null;
	
	@Override
	public void init(String name) {
		super.init(name);

		/**
		 * power manager for locking CPU so that the phone does not go into
		 * sleep
		 **/
		pm = (PowerManager) G.appCtx.getSystemService(Context.POWER_SERVICE);
		wakeLock = pm
				.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GpsWakeLock");
		/** recorder/track manager **/
		isRunning = false;
	}

	public void start() {

		wakeLock.acquire();

		ix = 0;
		N = AudioRecord.getMinBufferSize(sampleRate,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		recorder = new AudioRecord(AudioSource.MIC, sampleRate,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
				N * 10);
		if (mPlay) {
			track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
					AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT, N * 10,
					AudioTrack.MODE_STREAM);
		}
		scale = 1.0 / ((double) recorder.getChannelCount() * (1 << ((recorder
				.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT ? 16 : 8) - 1)));
		recorder.startRecording();
		if (mPlay) {
			track.play();
		}

		isRunning = true;

		running_instance = new Thread(startSound);
		running_instance.run();
	}

	private Runnable startSound = new Runnable() {
		@Override
		public void run() {
			startSoundHandler();
		}
	};

	private void startSoundHandler() {

		while (isRunning) {

			short[] buffer = buffers[ix++ % buffers.length];
			N = recorder.read(buffer, 0, buffer.length);

			double[] tmp = new double[buffer.length];
			for (int i = 0; i < buffer.length; i++) {
				tmp[i] = (buffer[i]) * scale;
			}

			if (mPlay) {
				track.write(buffer, 0, buffer.length);
			}

			MedusaSoundFrameServiceE ele;
			ListIterator<MedusaServiceActiveE> iterator = activeServiceList
					.listIterator();

			while (iterator.hasNext()) {
				ele = (MedusaSoundFrameServiceE) iterator.next();

				for (int i = 0; i < buffer.length; i++) {
					ele.frameBuffer[ele.currFrameIndex + i] = tmp[i];
				}
				ele.currFrameIndex += buffer.length;

				if (ele.currFrameIndex == ele.frameBufferLen) {
										
					if (ele.ret_method.compareTo("file") == 0) {
						/* if return method is 'file', create a file for log */
						Log.i("MedusaSoundFrame", "Creating new log file for MedusaSoundFrameManager.");
						ele.log = new FileLogger(MedusaStorageManager.generateTimestampFilename(ele.fnamehead));

						String soundString = "SoundFrame";
						for (int i = 0; i < ele.frameBufferLen; i++)
							soundString += String.format(" %.4f", ele.frameBuffer[i]);

						// write to the file!!
						ele.log.printlnwt(soundString);
					}
					
					double[] r = new double[ele.frameBufferLen];
					for (int m = 0; m < ele.frameBufferLen; m++)
						r[m] = ele.frameBuffer[m];
					
					ele.MedusaServiceListner.callCBFunc(
							r,
							String.valueOf(ele.frameBufferLen));

					if (!ele.periodic) {
						// we are done with this active service. remove from
						// list.
						iterator.remove();
						break;
					}

					for (int i = 0; i < buffer.length; i++) {
						ele.frameBuffer[i] = tmp[i];
					}

					ele.currFrameIndex = buffer.length;

				}
			}

			if (activeServiceList.isEmpty()) {
				// if there are no more active services, stop
				stop();
			}
			
			/*
			try{ 
				Thread.sleep(2);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			*/
		}

	}

	private void restart() {
		if (isRunning) {
			Log.i("MedusaGpsMgr", "* Sound sensor restarting...");
			stop();
			start();
		} else {
			Log.i("MedusaGpsMgr",
					"* Sound sensor restart requested while not running");
		}
	}

	private void stop() {
		if (isRunning) {
			// no pending requests
			if (recorder != null) {
				recorder.stop();
				recorder.release();
				recorder = null;
			}

			if (mPlay && track != null) {
				track.stop();
				track.release();
				track = null;
			}

			wakeLock.release(); // release CPU
			isRunning = false;
			//running_instance.interrupt();
			running_instance = null;
			
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
		
		

		Log.i("MedusaSoundFrameMgr", "* " + mgrName + "'s execute() is running.");
		Log.i("MedusaSoundFrameMgr",
				"* Config. parameter reading test: expname -> ["
						+ configMap.get("expname") + "]");
		Log.i("MedusaSoundFrameMgr",
				"* Config. parameter reading test: sensor_type -> ["
						+ configMap.get("sensor_type") + "]");

		/*
		 * XML format hierarchy.
		 * 
		 * - expname - sensor - type - periodic - period - thres - return
		 */

		if (configMap.get("sensor_type").equals("soundFrame")) {

			MedusaSoundFrameServiceE aele = new MedusaSoundFrameServiceE();
		    aele.medusaletName = configMap.get("expname");
			aele.frameNum = Integer.parseInt(configMap
					.get("sensor_frameNumber"));
			aele.periodic = (configMap.get("sensor_periodic").compareTo(
					"periodic") == 0 ? true : false);
			aele.frameBufferLen = (aele.frameNum
					* (winLen - overlapWinLen) + overlapWinLen);
			aele.frameBuffer = new double[aele.frameBufferLen];
			aele.ret_method = configMap.get("sensor_ret_method");
			aele.fnamehead = configMap.get("sensor_fnamehead");

			/* add this service request to currently active service list */
			addActiveServiceE(aele, ele.seCBListner);

			/* start GPS */
			if (!isRunning)
				start();
			else
				restart();
		} else {
			ele.seCBListner.callCBFunc(null, "sensor_type "
					+ configMap.get("sensor_type")
					+ " is wrong or not yet implemented.");
		}
	}

	public static void requestService(String expname, MedusaletCBBase listener, 
			int frameNum, String periodic, String ret_method, String fnamehead) {

		StringBuilder b = new StringBuilder();
		b.append("<xml>");
		b.append("<expname>");
		b.append(expname);
		b.append("</expname>");
		b.append("<sensor>");
		b.append("<type>soundFrame</type>");
		b.append("<frameNumber>");
		b.append(frameNum);
		b.append("</frameNumber>");
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

		MedusaServiceE ele = new MedusaServiceE();
		ele.seActionType = "xml";
		ele.seActionDesc = b.toString();
		ele.seCBListner = listener;

		try {
			MedusaSoundFrameManager.getInstance().requestService(ele);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
