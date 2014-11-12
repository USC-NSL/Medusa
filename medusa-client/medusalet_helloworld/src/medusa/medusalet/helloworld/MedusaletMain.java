/**
 * 'HelloWorld Medusalet'
 *
 * - This is the entry point for HelloWorld Medusalet.
 * - An Medusalet must have a class 'MedusaletMain'
 *   which extends the 'MedusaletBase'.
 *   The MedusaLoader will call the 'run' function
 *   after the initiation.
 * - Package name must be 'medusa.medusalet.<appname>'
 * - Binary filename must be 'medusalet_<appname>.apk', or 'medusalet_<appname>_<ver>.apk
 *
 * @created : Nov. 10th 2010
 * @modified : Nov. 28th 2011
 * @author   : Moo-Ryong Ra(mra@usc.edu), Jeongyeup Paek (jpaek@usc.edu)
 **/

package medusa.medusalet.helloworld;

import medusa.mobile.client.MedusaUtil;
import medusa.mobile.client.MedusaletBase;

public class MedusaletMain extends MedusaletBase 
{
	private final String TAG = "medusalet_HelloWorld";
	
    @Override
    public boolean run() {

    	MedusaUtil.log(TAG, "***************");
    	MedusaUtil.log(TAG, "* HELLO WORLD *");
    	MedusaUtil.log(TAG, "***************");
    	quitThisMedusalet();
        
        return true;
    }

}
