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

public class MedusaPriorityServiceE extends MedusaServiceE
{
	public int credit;
	public long deadline;
	public long starttime;
	public long fsize;
	
	public String pid;
	public String uid;
}
