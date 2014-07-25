/**
 * 'TextViewLogger'
 *
 * @modified : Nov. 10. 2010
 * @author   : Jeongyeup Paek (jpaek@usc.edu)
 **/

package medusa.mobile.client;

import java.util.Calendar;
import android.widget.TextView;

public class TextViewLogger {

	private TextView outView;

	public TextViewLogger(TextView output) {
		this.outView = output;
	}

	public void clear() {
		outView.setText("");
	}

	public void set(String string) {
		outView.setText(string);
	}

	/** show log on screen **/
	public void print(String string) {
		outView.append(string);
	}

	/** print with lineend **/
	public void println(String string) {
		String stringln = string + "\n";
		print(stringln);
	}

	/** print with timestamp **/
	public void printwt(String string) {
		// set current time
		Calendar calendar = Calendar.getInstance();
		// String timeString = String.format("%1$tF %1$tT", calendar);
		String timeString = String.format("%1$tT", calendar);
		String stringwt = timeString + " " + string;
		print(stringwt);
	}

	/** print with lineend and timestamp **/
	public void printlnwt(String string) {
		String stringln = string + "\n";
		printwt(stringln);
	}

	public void br() {
		outView.append("\n");
	}
}
