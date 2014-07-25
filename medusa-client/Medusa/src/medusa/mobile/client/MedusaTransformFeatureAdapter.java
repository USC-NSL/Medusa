/**
 * 'MedusaTransformFeatureAdapter'
 *
 * - ...
 *
 * @modified : Nov. 27th 2011
 * @author   : Bin Liu (binliu@usc.edu)
 **/

package medusa.mobile.client;

import java.util.HashMap;

import android.hardware.SensorManager;

public class MedusaTransformFeatureAdapter {	
	
	//public static double[] feature_max = {31,11,19,15};
	//public static double[] feature_min = {9.5, 1.2, 9.5, 0.01};
	
		
	private int feature_dim;
	private String[] feature_str_set;
	
	public MedusaTransformFeatureAdapter(String feature_str) {
		
		if (feature_str.equals("default")) {
			feature_str_set = new String[4];
			feature_str_set[0] = "pa";
			feature_str_set[1] = "va";
			feature_str_set[2] = "avg";
			feature_str_set[3] = "vari";
			
			feature_dim = 4;
			
		} else {		
			feature_str_set = feature_str.split("\\|");
			feature_dim = feature_str_set.length;
		}
		
	}
	
	
	public double[][] extract(double[][] input_set) {
				
		double[][] r = new double[input_set.length][feature_dim];				
		
		HashMap<String, Double> feature_param = new HashMap<String, Double>();
		feature_param.put("pa", new Double(0.0));
		feature_param.put("va", new Double(0.0));
		feature_param.put("avg", new Double(0.0));
		feature_param.put("vari", new Double(0.0));		
		
		for (int j = 0; j < r.length; j++) {
		
			double[] input = input_set[j];
			
			int n_peak = 0, n_valley = 0;		
			double avg = 0, vari = 0, s_peak = 0, s_valley = 0, p1 = 0, p2 = 0, p3 = 0;
			
	    	//long beforeTime=System.currentTimeMillis();
			
			for (int i = 0; i < input.length - 2; i++) {
				p1 = input[i];
				p2 = input[i+1];
				p3 = input[i+2];
				
				avg += input[i];
				
				if (p1 < p2 && p3 < p2)
				{
					n_peak++;
					s_peak += p2;
				}
				else if (p1 > p2 && p3 > p2) {
					n_valley++;
					s_valley += p2;
				}			
			}
			
			avg += input[input.length - 2] + input[input.length - 1];
			avg /= input.length;
			
			for (int i = 0; i < input.length; i++) {
				vari += (input[i] - avg)*(input[i] - avg);
			}
			
			vari = Math.sqrt(vari/input.length);
			
			double pa = SensorManager.GRAVITY_EARTH, va = SensorManager.GRAVITY_EARTH;
			
			if (n_peak > 0)
				pa = s_peak / n_peak;
			if (n_valley > 0)
				va = s_valley / n_valley;
			
			feature_param.put("pa", new Double(pa));
			feature_param.put("va", new Double(va));
			feature_param.put("avg", new Double(avg));
			feature_param.put("vari", new Double(vari));			
			
			for (int i = 0; i < feature_dim; i++) {
				
				Double tmp = feature_param.get(feature_str_set[i]);
									
				if (tmp != null) {
					r[j][i] = tmp.doubleValue();
				} else {
					r[j][i] = 0.0;
				}
			}			
		}
		
        //long afterTime=System.currentTimeMillis();
        //long timeDistance=afterTime-beforeTime;       
		return r;	
		
	}	

}
