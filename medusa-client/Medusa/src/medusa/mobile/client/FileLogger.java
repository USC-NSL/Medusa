/**
 * 'FileLogger'
 * - write message to a log file in sdcard.
 *
 * @modified : Apr. 23. 2011
 * @author   : Jeongyeup Paek (jpaek@usc.edu)
 **/

package medusa.mobile.client;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.TimeZone;

import android.os.Environment;
import android.util.Log;

public class FileLogger {

	private String filename;
	private FileOutputStream fout = null;
	private OutputStreamWriter osw = null;
	private String tz = null;

	public FileLogger(String filename, String tzin) {
		this.tz = tzin;
		this.filename = filename;
	}

	public FileLogger(String filename) {
		this.filename = filename;
	}

	public String filename() {
		return filename;
	}

	public String getPath() {
		if (filename != null)
			return Environment.getExternalStorageDirectory().getAbsolutePath()
					+ "/" + filename;
		return null;
	}

	/** write log to file **/
	public void print(String string) {
		try {
			fout = new FileOutputStream(getPath(), true);
			osw = new OutputStreamWriter(fout);
			// osw.write(string + "\n");
			osw.write(string);
			osw.flush();
			osw.close();
			fout.close();
		} catch (IOException e) {
			Log.e("Medusa", "Err. Cannot append to log file. " + e.toString());
		}
	}

	/** print with lineend **/
	public void println(String string) {
		String stringln = string + "\n";
		print(stringln);
	}

	/** print with timestamp **/
	public void printwt(String string) {
		// set current time
		Calendar calendar = null;
		if (tz == null)
			calendar = Calendar.getInstance();
		else
			calendar = Calendar.getInstance(TimeZone.getTimeZone(tz));

		String timeString = String.format("%1$tF %1$tT", calendar);
		String stringwt = timeString + " " + string;
		print(stringwt);
	}

	/** print with lineend and timestamp **/
	public void printlnwt(String string) {
		String stringln = string + "\n";
		printwt(stringln);
	}
}
