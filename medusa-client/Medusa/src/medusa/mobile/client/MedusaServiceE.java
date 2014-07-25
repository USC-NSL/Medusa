/**
 * 'MedusaServiceE'
 *
 * - MedusaServiceElement: This class provides a shell of object(s), 
 * 	 which will go into the medusa manager's service queue.
 *   
 * @modified : Jan. 7th 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu), Bin Liu (binliu@usc.edu)
 * 	
 **/

package medusa.mobile.client;

public class MedusaServiceE {
	/*
	 * Action Type - determine the type of Action Descriptor. (seActionDesc) -
	 * default type is xml, but possibly binary protocol, or something else can
	 * be used.
	 */
	public String seActionType;

	/*
	 * Action Desciprtor - will contain detail information of service actions. -
	 * default format is xml document.
	 */
	public String seActionDesc;

	/*
	 * Basic Callback Interface to the caller application. - Caller(Medusalet) can
	 * define the callback function at runtime depending on the application's
	 * context.
	 */
	public MedusaletCBBase seCBListner;
	
	/**
	 * some msg...
	 */
	public String seMsg = "";
}
