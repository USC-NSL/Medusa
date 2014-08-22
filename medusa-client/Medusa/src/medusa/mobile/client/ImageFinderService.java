package medusa.mobile.client;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.provider.MediaStore;

public class ImageFinderService extends Service{
	
		PhotosObserver instUploadObserver;
	
		public ImageFinderService()
		{
			super();
		}
	
		
		@Override
		public void onCreate()
		{
			instUploadObserver = new PhotosObserver(getApplicationContext());
		}
	
		@Override
		public int onStartCommand(Intent intent, int flags, int startId)
		{
			//Toast.makeText(getApplicationContext(), "REgistering", Toast.LENGTH_LONG).show();
			this.getApplication().
				getContentResolver().
				registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
						false, instUploadObserver);
			return START_NOT_STICKY;
		}
	
		@Override
		public void onDestroy() {
			this.getApplicationContext().
			getContentResolver().unregisterContentObserver(instUploadObserver);
			super.onDestroy();
		}
		
		@Override
		public IBinder onBind(Intent arg0) {
			// TODO Auto-generated method stub
			return null;
		}

}
