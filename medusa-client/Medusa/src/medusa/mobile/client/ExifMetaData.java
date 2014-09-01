package medusa.mobile.client;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.util.Log;

public class ExifMetaData 
{
	private final static int DEFAULT_INT = -1;
	private final static double DEFAULT_DOUBLE = -1.0;
	private final static String METADATA_NULL = "NOT_AVAILABLE";
	private final static int MAX_FACES = 10; //should be enough
	private final static String NUM_OF_FACES = "NumberOfFaces";
	private final static String HAS_FACES = "HasFaces";
	private final static String IMAGE_NAME = "ImageName";
	
	private String imageName;
	private String aperture;
	private String creationTime;
	private String exposureTime;
	private String flash;
	private String altitude;
	private String altitudeRef; //0 - Above Sea Level, 1- Below sea level
	private String latitude;
	private String longitude;
	private String focalLength;
	private String imageLength;
	private String imageWidth;
	private String isoSpeedRating;
	private String make;
	private String model;
	private String orientation; 
	private String whiteBalance;
	private String numOfFaces;
	private String hasFaces; // 1 - Yes, 0 - No. eventually it has to be converted to string for JSON
	private Face[] faces; 
	
	public String getAltitude() {
		return altitude;
	}
	public void setAltitude(String altitude) {
		this.altitude = altitude;
	}
	public String getAltitudeRef() {
		return altitudeRef;
	}
	public void setAltitudeRef(String altitudeRef) {
		this.altitudeRef = altitudeRef;
	}
	public String getLatitude() {
		return latitude;
	}
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}
	public String getLongitude() {
		return longitude;
	}
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	
	public String getImageName() {
		return imageName;
	}
	public void setImageName(String imageName) {
		this.imageName = imageName;
	}
	public Face[] getFaces() {
		return faces;
	}
	public void setFaces(Face[] faces) {
		this.faces = faces;
	}
	public String getNumOfFaces() {
		return numOfFaces;
	}
	public void setNumOfFaces(String numOfFaces) {
		this.numOfFaces = numOfFaces;
	}
	public String getHasFaces() {
		return hasFaces;
	}
	public void setHasFaces(String hasFaces) {
		this.hasFaces = hasFaces;
	}
	public String getAperture() {
		return aperture;
	}
	public void setAperture(String aperture) {
		this.aperture = aperture;
	}
	public String getCreationTime() {
		return creationTime;
	}
	public void setCreationTime(String creationTime) {
		this.creationTime = creationTime;
	}
	public String getExposureTime() {
		return exposureTime;
	}
	public void setExposureTime(String exposureTime) {
		this.exposureTime = exposureTime;
	}
	public String getFlash() {
		return flash;
	}
	public void setFlash(String flash) {
		this.flash = flash;
	}
	public String getFocalLength() {
		return focalLength;
	}
	public void setFocalLength(String focallength) {
		this.focalLength = focallength;
	}
	public String getImageLength() {
		return imageLength;
	}
	public void setImageLength(String imageLength) {
		this.imageLength = imageLength;
	}
	public String getImageWidth() {
		return imageWidth;
	}
	public void setImageWidth(String imageWidth) {
		this.imageWidth = imageWidth;
	}
	public String getIsoSpeedRating() {
		return isoSpeedRating;
	}
	public void setIsoSpeedRating(String isoSpeedRating) {
		this.isoSpeedRating = isoSpeedRating;
	}
	public String getMake() {
		return make;
	}
	public void setMake(String make) {
		this.make = make;
	}
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}
	public String getOrientation() {
		return orientation;
	}
	public void setOrientation(String orientation) {
		this.orientation = orientation;
	}
	public String getWhiteBalance() {
		return whiteBalance;
	}
	public void setWhiteBalance(String whiteBalance) {
		this.whiteBalance = whiteBalance;
	}
	
	public ExifMetaData(String filename) {
		try
		{
			ExifInterface exif = new ExifInterface(filename);
				
			this.imageName = new File(filename).getName();
			this.aperture = exif.getAttribute(ExifInterface.TAG_APERTURE);
			this.creationTime = exif.getAttribute(ExifInterface.TAG_DATETIME);
			this.exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
			this.flash = Integer.toString(exif.getAttributeInt(ExifInterface.TAG_FLASH, DEFAULT_INT));
			this.focalLength = Double.toString(exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, DEFAULT_DOUBLE));
			this.imageLength = Integer.toString(exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, DEFAULT_INT));
			this.imageWidth = Integer.toString(exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, DEFAULT_INT));
			this.isoSpeedRating = exif.getAttribute(ExifInterface.TAG_ISO);
			this.make = exif.getAttribute(ExifInterface.TAG_MAKE);
			this.model = exif.getAttribute(ExifInterface.TAG_MODEL);
			this.orientation = Integer.toString(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, DEFAULT_INT));
			this.whiteBalance = Integer.toString(exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, DEFAULT_INT));
			this.altitude = Double.toString(exif.getAltitude(DEFAULT_DOUBLE));
			this.altitudeRef = Integer.toString(exif.getAttributeInt(ExifInterface.TAG_GPS_ALTITUDE_REF, DEFAULT_INT));
			this.latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
			this.longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
			MedusaUtil.log("", "latitude exif:"+exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
			 FaceDetector fd = new FaceDetector(Integer.parseInt(this.imageWidth), 
					 				Integer.parseInt(this.imageLength), MAX_FACES);
			 
			 this.faces = new Face[MAX_FACES];
			 
			 int nFaces = fd.findFaces(BitmapFactory.decodeFile(filename), faces);
			 
			 this.numOfFaces = Integer.toString(nFaces); 
			 
			 this.hasFaces = (nFaces > 0) ? Integer.toString(1) : Integer.toString(0) ;
			 
			
		}
		catch(IOException e)
		{
			Log.e("medusa.mobile.client.ExifMetaData", e.toString(), e);
		}
		
		
	}
	
	public String MetaDataJSON(ExifMetaData imd) throws JSONException{
		
		
		JSONObject metadatadetails = new JSONObject();
		JSONObject metadata = new JSONObject();
		
		if(imd.imageName == null) {
			metadatadetails.put(IMAGE_NAME, METADATA_NULL);
		}
		else {
			metadatadetails.put(IMAGE_NAME, imd.imageName);
		}
		
		if(imd.aperture == null) {
			metadatadetails.put(ExifInterface.TAG_APERTURE, METADATA_NULL);
		}
		else {
			metadatadetails.put(ExifInterface.TAG_APERTURE, imd.aperture);
		}
		
		if(imd.creationTime == null) {
			metadatadetails.put(ExifInterface.TAG_DATETIME, METADATA_NULL);
		}
		else {
			metadatadetails.put(ExifInterface.TAG_DATETIME, imd.creationTime);	
		}
		
		if(imd.exposureTime == null) {
			metadatadetails.put(ExifInterface.TAG_EXPOSURE_TIME, METADATA_NULL);
		}
		else {
			metadatadetails.put(ExifInterface.TAG_EXPOSURE_TIME, imd.exposureTime);
		}
		
		if(imd.flash == null) {
			metadatadetails.put(ExifInterface.TAG_FLASH, METADATA_NULL);
		}
		else {
			metadatadetails.put(ExifInterface.TAG_FLASH, imd.flash);
		}
		
		if(imd.focalLength == null) {
			metadatadetails.put(ExifInterface.TAG_FOCAL_LENGTH, METADATA_NULL);
		}
		else {
			metadatadetails.put(ExifInterface.TAG_FOCAL_LENGTH, imd.focalLength);
		}
		
		if(imd.imageLength == null) {
			metadatadetails.put(ExifInterface.TAG_IMAGE_LENGTH, METADATA_NULL);
		}
		else {
			metadatadetails.put(ExifInterface.TAG_IMAGE_LENGTH, imd.imageLength);
		}
		
		if(imd.imageWidth == null) {
			metadatadetails.put(ExifInterface.TAG_IMAGE_WIDTH, METADATA_NULL);
		}
		else {
			metadatadetails.put(ExifInterface.TAG_IMAGE_WIDTH, imd.imageWidth);
		}
		
		if(imd.isoSpeedRating == null) {
			metadatadetails.put(ExifInterface.TAG_ISO, METADATA_NULL);
		}
		else {
			metadatadetails.put(ExifInterface.TAG_ISO, imd.isoSpeedRating);
		}
		
		if(imd.make == null) {
			metadatadetails.put(ExifInterface.TAG_MAKE, METADATA_NULL);
		}
		else {
			metadatadetails.put(ExifInterface.TAG_MAKE, imd.make);
		}
		
		if(imd.model == null) {
			metadatadetails.put(ExifInterface.TAG_MODEL, METADATA_NULL);
		}
		else {
			metadatadetails.put(ExifInterface.TAG_MODEL, imd.model);
		}
		
		if(imd.orientation == null) {
			metadatadetails.put(ExifInterface.TAG_ORIENTATION, METADATA_NULL);
		}
		else {
			metadatadetails.put(ExifInterface.TAG_ORIENTATION, imd.orientation);
		}
		
		if(imd.whiteBalance == null) {
			metadatadetails.put(ExifInterface.TAG_WHITE_BALANCE, METADATA_NULL);
		}
		else {
			metadatadetails.put(ExifInterface.TAG_WHITE_BALANCE, imd.whiteBalance);
		}
		
		if(imd.numOfFaces == null) {
			metadatadetails.put(NUM_OF_FACES, METADATA_NULL);
		}
		else {
			metadatadetails.put(NUM_OF_FACES, imd.numOfFaces);
		}
		
		if(imd.hasFaces == null) {
			metadatadetails.put(HAS_FACES, METADATA_NULL);
		}
		else {
			metadatadetails.put(HAS_FACES, imd.hasFaces);
		}
		
		if(imd.altitude == null) {
			metadatadetails.put(ExifInterface.TAG_GPS_ALTITUDE, METADATA_NULL);
		}
		else {
			metadatadetails.put(ExifInterface.TAG_GPS_ALTITUDE, imd.altitude);
		}
		
		if(imd.altitudeRef == null) {
			metadatadetails.put(ExifInterface.TAG_GPS_ALTITUDE_REF, METADATA_NULL);
		}
		else {
			metadatadetails.put(ExifInterface.TAG_GPS_ALTITUDE_REF, imd.altitudeRef);
		}
		
		if(imd.latitude == null) {
			metadatadetails.put(ExifInterface.TAG_GPS_LATITUDE, METADATA_NULL);
		}
		else {
			metadatadetails.put(ExifInterface.TAG_GPS_LATITUDE, imd.latitude);
		}
		
		if(imd.longitude == null) {
			metadatadetails.put(ExifInterface.TAG_GPS_LONGITUDE, METADATA_NULL);
		}
		else {
			metadatadetails.put(ExifInterface.TAG_GPS_LONGITUDE, imd.longitude);
		}
		
		metadata.put("metadata", metadatadetails);
		return metadata.toString();
	}


}
