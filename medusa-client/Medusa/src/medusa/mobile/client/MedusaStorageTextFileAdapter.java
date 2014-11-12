/**
 * 'MedusaStorageTextFileAdapter'
 *
 * @modified : Nov. 9th, 2011
 * @author   : Bin Liu (binliu@usc.edu)
 **/

package medusa.mobile.client;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MedusaStorageTextFileAdapter {

	
	public static String fmt_double(double in) {
		DecimalFormat df = new DecimalFormat("#.#####");
		return df.format(in);	
	}
	
	public static String fmt_double(double in, String fmtString) {
		DecimalFormat df = new DecimalFormat(fmtString);
		return df.format(in);	
	}
		
	public static boolean exist(String file_path) {	
		File f = new File(file_path);		
		return f.exists();
	}
	
	public static boolean delete(String file_path) {
		File f = new File(file_path);
		return f.delete();
	}
	
	public static String read(String file_path) {
		
        String read;
        String readStr = "";
        FileReader fileread = null;
        BufferedReader bufread = null;
        try {
            fileread = new FileReader(file_path);
            bufread = new BufferedReader(fileread);
            try {
                while ((read = bufread.readLine()) != null) {
                    readStr = readStr + read + "\n";
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // not return the last "\n"
        return readStr.substring(0,readStr.length()-1);
		
	}
	
	public static boolean write(String file_path, String content, boolean append) {
		
    	File f = new File(file_path);
    	try {
    		
    		BufferedWriter bw = null;
    		if (f.exists())
    			bw = new BufferedWriter(new FileWriter(f, append));
    		else 
    			bw = new BufferedWriter(new FileWriter(f, false));
    		bw.write(content);
			bw.close();
			
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;		
	}
	
	public static boolean write(String file_path, String[] content, boolean append) {
		
		File f = new File(file_path);
		try {
			
    		BufferedWriter bw = null;
    		if (f.exists())
    			bw = new BufferedWriter(new FileWriter(f, append));
    		else 
    			bw = new BufferedWriter(new FileWriter(f, false));
    		
    		for (int i = 0; i < content.length; i++)
    			bw.write(content[i] +  System.getProperty("line.separator"));
    		
			bw.close();
			
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;	
	}
	
	public static boolean write(String file_path, ArrayList<String> content, boolean append) {
		
		File f = new File(file_path);
		try {
			
    		BufferedWriter bw = null;
    		if (f.exists())
    			bw = new BufferedWriter(new FileWriter(f, append));
    		else 
    			bw = new BufferedWriter(new FileWriter(f, false));
    		
    		for (int i = 0; i < content.size(); i++)
    			bw.write(content.get(i) +  System.getProperty("line.separator"));
    		
			bw.close();
			
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;	
	}	
	
	public static boolean write(String file_path, MedusaAccelManager.MedusaAccelDataStructure[][] content, double[] gps_content,  
			String df_fmt, boolean append) {		
		
		DecimalFormat df = new DecimalFormat(df_fmt);
		
		File f = new File(file_path);
		
		try {
    		BufferedWriter bw = null;
    		if (f.exists())
    			bw = new BufferedWriter(new FileWriter(f, append));
    		else 
    			bw = new BufferedWriter(new FileWriter(f, false));
    		
    		for (int i = 0; i < content.length; i++)
    			for (int j = 0; j < content[i].length; j++) {   				
    				MedusaAccelManager.MedusaAccelDataStructure tmp = content[i][j];
    				bw.write(df.format(tmp.x) + " " + df.format(tmp.y) + " " + df.format(tmp.z) + " " + df.format(tmp.timeTick));
    				
    				for (int k = 0; k < gps_content.length; k++)
    					bw.write(" " + df.format(gps_content[k]));
    				
    				bw.write(System.getProperty("line.separator"));
    			}
    		
			bw.close();			
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		return false;
	}	
	
	public static boolean write(String file_path, MedusaAccelManager.MedusaAccelDataStructure[][] content, 
			String df_fmt, boolean append) {		
		
		DecimalFormat df = new DecimalFormat(df_fmt);
		
		File f = new File(file_path);
		
		try {
    		BufferedWriter bw = null;
    		if (f.exists())
    			bw = new BufferedWriter(new FileWriter(f, append));
    		else 
    			bw = new BufferedWriter(new FileWriter(f, false));
    		
    		for (int i = 0; i < content.length; i++)
    			for (int j = 0; j < content[i].length; j++) {   				
    				MedusaAccelManager.MedusaAccelDataStructure tmp = content[i][j];
    				bw.write(tmp.timeTick + " " + tmp.x + " " + tmp.y + " " + tmp.z);
    				bw.write(System.getProperty("line.separator"));
    			}
    		
			bw.close();			
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		return false;
	}
	
	public static boolean write(String file_path, double[] content, String df_fmt, boolean append) {
		
		DecimalFormat df = new DecimalFormat(df_fmt);
		
		File f = new File(file_path);
		try {		
    		BufferedWriter bw = null;
    		if (f.exists())
    			bw = new BufferedWriter(new FileWriter(f, append));
    		else 
    			bw = new BufferedWriter(new FileWriter(f, false));
    		
    		for (int i = 0; i < content.length; i++)
    			bw.write(df.format(content[i]) +  " ");
    		
    		bw.write(System.getProperty("line.separator"));
			bw.close();
			
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;		
	}
	
	public static boolean write(String file_path, double[][] content, String df_fmt, boolean append, int idx) {
		
		DecimalFormat df = new DecimalFormat(df_fmt);
		
		File f = new File(file_path);
		try {		
    		BufferedWriter bw = null;
    		if (f.exists())
    			bw = new BufferedWriter(new FileWriter(f, append));
    		else 
    			bw = new BufferedWriter(new FileWriter(f, false));
    		
    		for (int i = 0; i <= idx; i++) {
    			
    			for (int j = 0; j < content[i].length - 1; j++) {
    				bw.write(df.format(content[i][j]) +  " ");
    			}
    			
    			bw.write(df.format(content[i][content[i].length - 1]) + System.getProperty("line.separator"));
    			
    		}
    		
			bw.close();
			
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;		
	} 
	
	public static boolean write(String file_path, double[][] content, String df_fmt, boolean append) {
		
		DecimalFormat df = new DecimalFormat(df_fmt);
		
		File f = new File(file_path);
		try {		
    		BufferedWriter bw = null;
    		if (f.exists())
    			bw = new BufferedWriter(new FileWriter(f, append));
    		else 
    			bw = new BufferedWriter(new FileWriter(f, false));
    		
    		for (int i = 0; i < content.length; i++) {
    			
    			for (int j = 0; j < content[i].length - 1; j++) {
    				bw.write(df.format(content[i][j]) +  " ");
    			}
    			
    			bw.write(df.format(content[i][content[i].length - 1]) + System.getProperty("line.separator"));
    			
    		}
    		
			bw.close();
			
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;		
	} 
	
}
