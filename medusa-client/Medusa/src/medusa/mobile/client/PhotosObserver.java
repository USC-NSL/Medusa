package medusa.mobile.client;

import java.io.File;
import java.io.IOException;

import medusa.mobile.client.Media;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

public class PhotosObserver extends ContentObserver {
	
		Context applicationContext;
		
		public PhotosObserver(Context context)
		{
			super(null);
			this.applicationContext = context;
		}
	
		@Override
		public void onChange(boolean selfChange)
		{
			super.onChange(selfChange);
			Media media = readFromMediaStore(applicationContext,
			MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			
			Log.d("New Image", media.getFile().getName());
			
			try 
			{
				float[] floatLatLong = new float[2];
				ExifInterface exif = new ExifInterface(media.getFile().getAbsolutePath());
				boolean isLocation = exif.getLatLong(floatLatLong);
				
				if(isLocation)
				{
					exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, Float.toString(floatLatLong[1])); //longitude at index 1
					exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, Float.toString(floatLatLong[0])); //latitude at index 0
				}
				else
				{
					GPSLocation gpsLocation = new GPSLocation(applicationContext);
					
					if(gpsLocation.getLocation() != null)
					{
						exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, Double.toString(gpsLocation.getLocation().getLongitude()));
						exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, Double.toString(gpsLocation.getLocation().getLatitude()));
						exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, Double.toString(gpsLocation.getLocation().getAltitude()));
					}
					else
					{
						exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null);
						exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null);
						exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null);
					}
				}
				
			} 
			catch (IOException e) 
			{
				Log.e("PhotosObserver", e.toString());
			}
			
		}
		
		
		public Media readFromMediaStore(Context context, Uri uri) 
		{
			Cursor cursor = context.getContentResolver().query(uri, null, null,	null, "date_added DESC");
			Media media = null;
			if (cursor.moveToNext()) 
			{
				int dataColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
				String filePath = cursor.getString(dataColumn);
				int mimeTypeColumn = cursor
						.getColumnIndexOrThrow(MediaColumns.MIME_TYPE);
				String mimeType = cursor.getString(mimeTypeColumn);
				media = new Media(new File(filePath), mimeType);
			}
			cursor.close();
			return media;
	}

}
