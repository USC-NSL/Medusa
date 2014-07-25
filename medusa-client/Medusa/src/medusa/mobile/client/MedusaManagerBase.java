/**
 * 'MedusaManagerBase'
 *
 * - This abstract class is a super class of every 'Manager' classes.
 *   For instance, SensorMgr, StorageMgr, TransferMgr will inherit this class,
 *   and concretize their behavior in their class.
 *   
 * - Major functionalities
 * 		1. service queue management.
 * 		2. maintains main scheduler threads for incoming job dispatching.
 * 		   (default mode is FIFO)
 *      3. maintains list of currently active(running) services
 *
 * @created : Feb. 2nd 2011
 * @modified : Dec. 11th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.io.IOException;
import java.io.StringReader;
import java.lang.Thread;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

public abstract class MedusaManagerBase {

	// related data structure
	Thread dispatcherThread;
	Queue<MedusaServiceE> serviceQueue;
	Condition serviceCond;
	ReentrantLock serviceLock;
	//Vector<MedusaServiceActiveE> activeServiceList; 	// list of currently active services
	CopyOnWriteArrayList<MedusaServiceActiveE> activeServiceList;

	// contains configuration parameters. (key, value) pairs
	HashMap<String, String> configMap;

	// parameters
	boolean initialized;
	boolean willExit;

	// meta information.
	String mgrName;
	int serviceSeqNum;
	
	/*
	 * init() function - should be called at the initialization stage of the
	 * Medusalet. - this function will actually create dispatcher thread.
	 */
	public void init(String name) {
		// 1. create queue and related data structure
		// 2. initialize variables.
		// 3. run dispatcher thread.
		serviceQueue = new LinkedList<MedusaServiceE>();
		serviceLock = new ReentrantLock();
		serviceCond = serviceLock.newCondition();

		//activeServiceList = new Vector<MedusaServiceActiveE>();
		activeServiceList = new CopyOnWriteArrayList<MedusaServiceActiveE>();
		serviceSeqNum = 0;

		configMap = new HashMap<String, String>();

		willExit = false;
		mgrName = name;

		runDispatcher();

		initialized = true;
	}

	/*
	 * exit() function. - this function should not be called directly. -
	 * dispatcher thread will call when it is ready to exit.
	 */
	private void exit() {
		serviceQueue = null;
		serviceLock = null;
		serviceCond = null;
		activeServiceList.clear();
		activeServiceList = null;
		configMap = null;
		initialized = false;
		willExit = false;
	}

	/*
	 * exit() function - called by application main thread to terminate service
	 * manager. - this function will mark the exit flag for graceful exit.
	 */
	public void markExit() {
		if (serviceLock != null) {
			serviceLock.lock();
			willExit = true; // graceful shutdown.
			serviceLock.unlock();
			
			Log.d(this.getName(), "* [" + this.getName() + "] is preparing graceful shutdown.");
		}
	}

	/*
	 * shouldExit() function - dispatcher thread will use this function to check
	 * the exit condition.
	 */
	boolean shouldExit() {
		boolean ret = false;

		serviceLock.lock();
		ret = willExit;
		serviceLock.unlock();

		return ret;
	}
	
	/*
	 * forceKillThread() functon 
	 * 		- this function will be called by watchdog thread.
	 */
	public void forceKillThread() {
		if (dispatcherThread.isAlive() == true) {
			dispatcherThread.interrupt();
			Log.d(this.getName(), "* request interrupt() to the dispatcher thread");
			try {
				dispatcherThread.join();
				Log.d(this.getName(), "* termination by interrupt() has been verified");
				//exit();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * request_service() function. - will be called by Medusalet - put a service
	 * element to the queue.
	 */
	public void requestService(MedusaServiceE in) throws InterruptedException {
		if (serviceLock == null) {
			Log.e("MedusaMgrBase", "! This instance [" + mgrName + "] is already exited..");
			return;
		}
		
		serviceLock.lock();
			serviceQueue.offer(in);
			serviceCond.signal();
		serviceLock.unlock();
	}

	/*
	 * waitForServiceRequest() function. - this function will wait for any
	 * incoming service requests.
	 */
	void waitForServiceRequest() throws InterruptedException {
		serviceLock.lock();
			while (serviceQueue.isEmpty() == true) {
				serviceCond.await();
			}
		serviceLock.unlock();
	}

	/*
	 * run_dispatcher() function - create dispatcher thread. - thread will pick
	 * one element at a time, and try to serve it. - child class should override
	 * execute() function for determining custom behaviors.
	 */
	void runDispatcher() 
	{
		dispatcherThread = new Thread(mgrName) {
			@Override
			public void run() {
				try {
					Log.d(this.getName(), "* dispatcher thread initiated.");
					
					while (shouldExit() == false) {
						waitForServiceRequest();

						if (serviceQueue.isEmpty() == false) {
							MedusaServiceE ele = serviceQueue.peek();
							if (ele == null) {
								Log.w(this.getName(), "! ele == null");
								continue;
							} else {
								// run a service request.
								if (!parseRequest(ele)) {
									Log.e(this.getName(), "! parse error. check your service input format.");
								} else {
									execute(ele);
								}

								serviceQueue.remove();
							}
						}
						else {
							Log.e(this.getName(), "! service queue is empty");
						}
					}
				} 
				catch (InterruptedException ex) {
					Log.e(this.getName(), "* dispatcher thread is interrupted.");
				} 
				catch (Exception e) {
					Log.e(this.getName(), "! exception on MedusaManagerBase.");
					e.printStackTrace();
				}
				finally {
					exit();
					
					MedusaUtil.log(this.getName(), "* [overhead] [" + this.getName() + "]'s dispatcher thread exits gracefully: " + G.getElapsedTime());
					MedusaUtil.log(this.getName(), "================");
				}
			}
		};

		dispatcherThread.start();
	}

	/*
	 * parseRequest() function - Some 'Manager' implementation will override
	 * this function to support other types of service action. - default
	 * parseRequest function support xml format only.
	 */
	protected boolean parseRequest(MedusaServiceE ele)
			throws XmlPullParserException, IOException {
		boolean bRet = true;

		// Log.i("MedusaMgr", "* " + mgrName +
		// ": default parseRequest configType -> " + ele.seActionType);

		if (ele.seActionType.compareTo("xml") == 0) {
			// parse xml document.
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			Stack eleStack = new Stack();
			int parserEvent;

			parser.setInput(new StringReader(ele.seActionDesc));
			parserEvent = parser.getEventType();

			while (parserEvent != XmlPullParser.END_DOCUMENT) {
				switch (parserEvent) {
				case XmlPullParser.START_TAG:
					String newtag = parser.getName();
					if (newtag.compareTo("xml") != 0) {
						eleStack.push(newtag);
						// Log.i("MedusaMgr", "* tag -> " + newtag);
					}
					break;
				case XmlPullParser.END_TAG:
					if (parser.getName().compareTo("xml") != 0) {
						eleStack.pop();
					}
					break;
				case XmlPullParser.TEXT:
					String tagkey = "";
					for (int i = 0; i < eleStack.size(); i++) {
						tagkey += eleStack.elementAt(i);
						if (i < eleStack.size() - 1)
							tagkey += "_";
					}
					configMap.put(tagkey, parser.getText());
					break;
				default:
					break;
				}

				parserEvent = parser.next();
			}

			eleStack = null;
			parser = null;
			factory = null;
		} else {
			Log.e(this.getName(), "* default parseRequest doesn't support action type [" 
									+ ele.seActionType + "]");
		}

		return bRet;
	}

	/* 
	 * methods related to active service E 
	 */
	public void addActiveServiceE(MedusaServiceActiveE ele, MedusaletCBBase listner) 
	{
		ele.serviceId = ++this.serviceSeqNum;
		ele.serviceMgr = this;
		ele.MedusaServiceListner = listner;
		
		activeServiceList.add(ele);
	}
	
	/*
	 * getter methods. 
	 */
	public String getName() { return this.mgrName; }
	
	/*
	 * A set of abstract functions. - Every "Manager" class should implement
	 * these functions.
	 */
	protected abstract void execute(MedusaServiceE ele) throws InterruptedException, Exception;
}
