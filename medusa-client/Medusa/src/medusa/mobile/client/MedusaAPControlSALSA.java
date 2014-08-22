/**
 * 'MedusaAPControlSALSA'
 *
 * - Implements SALSA algorithm. 
 * 	 	Moo-Ryong Ra, Jeongyeup Paek, Abhishek B. Sharma, Ramesh Govindan, Martin H. Krieger, and Michael J. Neely, 
 * 		"Energy-Delay Tradeoffs in Smartphone Applications." In MobiSys 2010.
 * 		
 * - Porting effort from Symbian C++ version, **NOT TESTED** on Android.
 *
 * @modified : Dec. 14th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.util.HashMap;
import android.util.Log;

public class MedusaAPControlSALSA extends MedusaAPControlBase {

	MedusaAPControlSALSA(HashMap<String, String> configmap) {
		super(configmap);
		// TODO Auto-generated constructor stub
		Log.i("APCtrl", "* create MedusaAPControlSALSA() instance.");
	}

	@Override
	protected void atArrival() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void atDeparture(int size, boolean finalChunk) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void eachTimeSlot() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void initStatValues() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void printStats(APElement ap) {
		// TODO Auto-generated method stub

	}

	@Override
	APElement selectAlgorithm() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	String getName() {
		// TODO Auto-generated method stub
		return new String("SALSA");
	}

}
