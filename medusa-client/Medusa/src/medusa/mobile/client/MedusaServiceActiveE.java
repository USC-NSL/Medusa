/**
 * 'MedusaServiceActiveE'
 *
 * - MedusaServiceActiveElement: This class provides a shell of object(s), 
 * 	 which is an active instance of a MedusaServiceElement.
 *   
 * @modified : Feb. 2nd 2011
 * @author   : Jeongyeup Paek (jpaek@usc.edu)
 **/

package medusa.mobile.client;

public class MedusaServiceActiveE {
	/*
	 * Medusalet ID - unique identifier for an instance of an medusalet
	 */
	public int medusaletId;

	/*
	 * Medusalet name - name of the medusalet.
	 */
	public String medusaletName;

	/*
	 * Service ID - unique identifier "per service manager" for an instance of a
	 * service request
	 */
	public int serviceId;

	/*
	 * Service Manager - service manager that is currently holding this active
	 * service.
	 */
	public MedusaManagerBase serviceMgr;

	/*
	 * Basic Callback Interface to the caller application. - Caller(Medusalet) can
	 * define the callback function at runtime depending on the application's
	 * context.
	 */
	public MedusaletCBBase MedusaServiceListner;
}
