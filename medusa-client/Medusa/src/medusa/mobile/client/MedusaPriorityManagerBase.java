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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

public abstract class MedusaPriorityManagerBase {

	// related data structure
	Thread dispatcherThread;
	List<MedusaPriorityServiceE> serviceQueue;
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
	
	public static int uploadScheme = -6; 
	// -1 for Max Credit First
	// -2 for Earliest Deadline First
	// -3 for round_robin
	/*
	 * init() function - should be called at the initialization stage of the
	 * Medusalet. - this function will actually create dispatcher thread.
	 */
	public void init(String name) {
		// 1. create queue and related data structure
		// 2. initialize variables.
		// 3. run dispatcher thread.
		
		serviceQueue = new LinkedList<MedusaPriorityServiceE>();
		//serviceQueue = new PriorityQueue<MedusaPriorityServiceE>();
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
	public void requestService(MedusaPriorityServiceE in) throws InterruptedException {
		if (serviceLock == null) {
			Log.e("MedusaMgrBase", "! This instance [" + mgrName + "] is already exited..");
			return;
		}
		
		serviceLock.lock();
			int i;
			for (i = 0; i < serviceQueue.size(); i++)
				if (serviceQueue.get(i).credit < in.credit) break;
			serviceQueue.add(i, in);
			serviceCond.signal();
		serviceLock.unlock();
	}

	/*
	 * waitForServiceRequest() function. - this function will wait for any
	 * incoming service requests.
	 */
	void waitForServiceRequest() throws InterruptedException {
		serviceLock.lock();
			//timerOK = true;
			while (serviceQueue.isEmpty() == true) {
				timerOK = false;
				serviceCond.await();
			}
		serviceLock.unlock();
	}
	
	int previous = -1;
	MedusaPriorityServiceE pickRoundRobin()
	{
		long now = System.currentTimeMillis()/1000;
		int index = -1, min = Integer.MAX_VALUE;
		for (int i = 0; i < serviceQueue.size(); i++)
		{
			if (serviceQueue.get(i).starttime > now) continue;
			int i_pid = Integer.valueOf(serviceQueue.get(i).pid);
			if (i_pid > previous) 
			{
				if (i_pid <= min) //pick the last, which has latest deadline
				{
					index = i;
					min = i_pid;
				}
			}
		}
		if (index == -1)
		{
			min = Integer.MAX_VALUE;
			for (int i = 0; i < serviceQueue.size(); i++)
			{
				if (serviceQueue.get(i).starttime > now) continue;
				int i_pid = Integer.valueOf(serviceQueue.get(i).pid);
				if (i_pid <= min) 
				{
					index = i;
					min = i_pid;
				}
			}			
		}
	
		if (index == -1) return null;
		MedusaUtil.log("TAG", "Picking = " + serviceQueue.get(index).pid + "(" + String.valueOf(previous) + ")");
		previous = Integer.valueOf(serviceQueue.get(index).pid);
		return returnIndex(index);
	}
	
	MedusaPriorityServiceE pickMaxCredit()
	{
		int index = -1, max = 0;
		long now = System.currentTimeMillis()/1000;
		for (int i = 0; i < serviceQueue.size(); i++)
		{
			if (serviceQueue.get(i).starttime > now) continue;
			if (serviceQueue.get(i).credit > max) 
			{
				index = i;
				max = serviceQueue.get(i).credit;
			}
		}
		return returnIndex(index);	
	}

	MedusaPriorityServiceE pickEarliestDeadline()
	{	
		int index = -1;
		long max = Long.MAX_VALUE;
		long now = System.currentTimeMillis()/1000;
		for (int i = 0; i < serviceQueue.size(); i++)
		{
			if (serviceQueue.get(i).starttime > now) continue;
			if (serviceQueue.get(i).deadline < max) 
			{
				index = i;
				max = serviceQueue.get(i).deadline;
			}
		}
		return returnIndex(index);	
	}
	
	public static int windowLength = 5;
	MedusaPriorityServiceE pickWindowedMaxCredit(int window)
	{
		int index = -1, max = 0;
		long now = System.currentTimeMillis()/1000;
		for (int i = 0; i < serviceQueue.size(); i++)
		{
			if (serviceQueue.get(i).starttime > now || serviceQueue.get(i).deadline > now + window) continue;
			if (serviceQueue.get(i).credit > max) 
			{
				index = i;
				max = serviceQueue.get(i).credit;
			}
		}
		return returnIndex(index);
	}

	MedusaPriorityServiceE pickWeightedDeadline()
	{
		return pickEarliestDeadline();
	}
	
	
	public static int oneObject = 3;
	class line
	{
		int start;
		int end;
		line(int a, int b)
		{
			start = a;
			end = b;
		}
	};
	boolean lineAdd(double time, int deadline, List<line> space)
	{
		if (space.size() == 0)
		{
			space.add(0, new line((int)Math.round((deadline-time)), deadline)); // change to round()
			return true;
		}

		int now = space.size(), now_end = deadline;
		boolean connect = false;
		for (int i = 0; i < space.size(); i++)
			if (space.get(i).end > deadline)
			{
				now = i;
				now_end = Math.min(deadline, space.get(now).start);
				connect = deadline >= space.get(now).start;
				break;
			}
		
		while (true)
		{
			if (now == 0 || now_end - time >= space.get(now-1).end)
			{
				if (connect && now != space.size())
					space.get(now).start = (int)Math.round(space.get(now).start - time);
				else
					space.add(now, new line((int)Math.round(now_end - time), now_end));
				return now == 0;
			}
			else 
			{
				connect = true;
				now_end = space.get(now-1).start;
				now --;
			}
		}		 
	}
	MedusaPriorityServiceE pickGreedyArrange()
	{
		List<line> space = new LinkedList<line>();
		long now = System.currentTimeMillis()/1000;
		int index = -1;
		for (int i = 0; i < serviceQueue.size(); i++)
		{
			if (serviceQueue.get(i).starttime > now) continue;
			if (lineAdd(oneObject, (int)(serviceQueue.get(i).deadline-now), space) && space.get(0).start >= 0) index = i;
			if (space.get(0).start < 0) break;
			for (int j =0; j <space.size(); j++)
			{
				MedusaUtil.log(this.getName(), String.valueOf(space.get(j).start) + "," + String.valueOf(space.get(j).end) + "   ");
			}	
			MedusaUtil.log(this.getName(), "round end");
		}
		
		if (index == -1) return pickMaxCredit();
		int index_ = -1, max = 0;
		for (int i = 0; i < serviceQueue.size(); i++)
		{
			MedusaPriorityServiceE e = serviceQueue.get(i);
			if (e.deadline == serviceQueue.get(index).deadline)
			{
				if (e.credit > max)
				{
					max = e.credit;
					index_ = i;
				}
			}
		}
		return returnIndex(index_);
	}	
	
	MedusaPriorityServiceE returnIndex(int index)
	{
		if (index == -1) return null;
		
		serviceLock.lock();
		MedusaPriorityServiceE ret_val = serviceQueue.remove(index);
		serviceLock.unlock();
		return ret_val;		
	}
	
	MedusaPriorityServiceE pickOneForUpload()
	{
		/*
		String temp = "";
		for (int i = 0; i < serviceQueue.size(); i++)
		{
			temp += "c="+ Integer.toString(serviceQueue.get(i).credit) + ",d=" + Long.toString(serviceQueue.get(i).deadline) + ",uid=" +serviceQueue.get(i).uid + ",pid=" +serviceQueue.get(i).pid+"   ";
			
		}
		
		MedusaUtil.log("TAG", temp);*/
		if (serviceQueue.get(0).deadline < 0)
			{MedusaUtil.log("TAG", "Sending notification..."); return serviceQueue.remove(0);}

		
		List<MedusaPriorityServiceE> removeList = new LinkedList<MedusaPriorityServiceE>();
		for (int i = 0; i< serviceQueue.size(); i++)
			if (serviceQueue.get(i).deadline <= System.currentTimeMillis()/1000)
				removeList.add(serviceQueue.get(i));

		for (int i = 0; i<removeList.size(); i++)
		{
			MedusaPriorityServiceE e = removeList.get(i); 
			e.seCBListner.callCBFunc(e.uid, "expired");
			serviceLock.lock();
			serviceQueue.remove(e);
			serviceLock.unlock();
		}
		
		if (serviceQueue.isEmpty()) return null;

		if (uploadScheme == -1)
			return pickMaxCredit();
		else if (uploadScheme == -2) 
			return pickEarliestDeadline();
		else if (uploadScheme == -3)
			return pickRoundRobin();
		else if (uploadScheme == -4)
			return pickWindowedMaxCredit(windowLength);
		else if (uploadScheme == -5)
			return pickWeightedDeadline();
		else if (uploadScheme == -6)
			return pickGreedyArrange();
		else return pickEarliestDeadline();
	}

	/*
	 * run_dispatcher() function - create dispatcher thread. - thread will pick
	 * one element at a time, and try to serve it. - child class should override
	 * execute() function for determining custom behaviors.
	 */
	double startTimer;
	boolean timerOK = true;
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
//xing							MedusaServiceE ele = serviceQueue.peek();
							double timeObject = System.currentTimeMillis()/1000.0 - startTimer;
							if (timerOK && timeObject < 20)
							{
								oneObject = (int)Math.round(timeObject) + 1;
								MedusaUtil.log(this.getName(), "Uploading time updated to "+String.valueOf(oneObject));			
							}
							
							MedusaPriorityServiceE ele = pickOneForUpload();
							if (ele == null) {
								timerOK = false;
								oneObject = 4;
								continue;
							} else {
								if (ele.deadline > 0)
								{
									startTimer = System.currentTimeMillis()/1000.0;
									timerOK = true;
								}
								// run a service request.
								if (!parseRequest(ele)) {
									Log.e(this.getName(), "! parse error. check your service input format.");
								} else {
									execute(ele);
								}

//xing								serviceQueue.remove();
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
	/*public void addActiveServiceE(MedusaServiceActiveE ele, MedusaletCBBase listner) 
	{
		ele.serviceId = ++this.serviceSeqNum;
		ele.serviceMgr = (MedusaServiceE)this;
		ele.MedusaServiceListner = listner;
		
		activeServiceList.add(ele);
	}*/
	
	/*
	 * getter methods. 
	 */
	public String getName() { return this.mgrName; }
	
	/*
	 * A set of abstract functions. - Every "Manager" class should implement
	 * these functions.
	 */
	protected abstract void execute(MedusaPriorityServiceE ele) throws InterruptedException, Exception;
	
	protected void deleteDuplicatedUid(String uid)
	{
		serviceLock.lock();
		for (int i=0; i<serviceQueue.size(); i++)
		{
			MedusaPriorityServiceE e = serviceQueue.get(i);
			if (e.uid.equals(uid))
			{
				serviceQueue.remove(i);
				--i;
				if (e.seCBListner != null) {
					e.seCBListner.callCBFunc(uid, "* [TxMsg] arg="+uid);
				}
				MedusaUtil.log("MedusaPriorityManager", "* Deleting duplicated uid=" + uid + " with pid=" + e.pid);
			}
		}
		serviceLock.unlock();
	}
	
	protected void deleteServicesOfPid(String pid)
	{
		serviceLock.lock();
		LinkedList<MedusaPriorityServiceE> toBeDeleted = new LinkedList<MedusaPriorityServiceE>();
		for (MedusaPriorityServiceE e : serviceQueue)
			if (e.pid.equals(pid))
				toBeDeleted.add(e);
		
		for (MedusaPriorityServiceE e : toBeDeleted)
		{
				e.seCBListner.callCBFunc(e.uid, "duplicated");
				serviceQueue.remove(e);
				MedusaUtil.log("MedusaPriorityManager", "* Deleting quitted task (pid=" + e.pid + ") uid=" + e.uid);
		}
		
		serviceLock.unlock();		
	}
}
