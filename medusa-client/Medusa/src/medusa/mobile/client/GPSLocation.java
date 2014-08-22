package medusa.mobile.client;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class GPSLocation implements LocationListener
{
		private Context context;
		private Location location;
		
		// The minimum distance to change Updates in meters
		private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
		
		// The minimum time between updates in milliseconds
		private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute
		
		public GPSLocation(Context context)
		{
			this.setContext(context);
			this.setLocation(getCurrentLocation());
		}
		
		public Location getLocation() 
		{
			return location;
		}
		
		public void setLocation(Location location) 
		{
			this.location = location;
		}
		
		public Context getContext() 
		{
			return context;
		}
		
		public void setContext(Context context) 
		{
			this.context = context;
		}
		
		public Location getCurrentLocation()
		{
			LocationManager locationManager;
			boolean isGPSEnabled = false;
		
			try
			{
				locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
				isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
				
				if(isGPSEnabled)
				{
					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,MIN_TIME_BW_UPDATES,MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
					
					if (locationManager != null) 
					{
						location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					
						if (location != null) 
						{
							return location;
						}
					}
				}
				
				return null;
			}
			catch(Exception e)
			{
				return null;
			}
		}
		
		@Override
		public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		}
		@Override
		public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		}
		@Override
		public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		}
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		}

}
