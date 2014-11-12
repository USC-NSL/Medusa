/**
 * 'MedusaTransformImageProcAdapter'
 *
 * - This class willl contain a set of image processing algorithm.
 * - Currently implements
 * 		- Face detection algorithm.  
 *   
 * @created : Sept. 12nd 2011
 * @modified : Dec. 2nd 2011
 * @author   : Moo-Ryong Ra (mra@usc.edu)
 **/

package medusa.mobile.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.util.Log;
import java.util.Date;

public class MedusaTransformImageProcAdapter 
{
	public static final String TAG = "MedusaTransformImageProcAdapter";
	
	public static final String CONFIG_EXTRACT_FACES_FROM_IMAGE = "detect_faces_from_image";
	public static final String CONFIG_FUTURE_USE = "whatever..";
	
	public class MedusaFaceDetector 
	{
		private final int MAX_NUM_FACES = 5; 	// max allowed is 64
		private FaceDetector arrayFaces;
		private FaceDetector.Face getAllFaces[] = new FaceDetector.Face[MAX_NUM_FACES];
		private FaceDetector.Face getFace = null;

		public PointF eyesMidPts[] = new PointF[MAX_NUM_FACES];
		public float eyesDistance[] = new float[MAX_NUM_FACES];

		public Bitmap sourceImage; 	// bitmap of the source image

		public int numFaces; 		// number of faces detected
		public Bitmap faceImage; 	// bitmap of a face
		public int faceIdx;
		public static final int DEFAULT_QUALITY = 80;

		public MedusaFaceDetector() {
			reset();
		}

		public void reset() {
			sourceImage = null;
			arrayFaces = null;
			getFace = null;
			
			numFaces = 0;
			for (int i = 0; i < MAX_NUM_FACES; i++) {
				eyesMidPts[i] = null;
			}
		}

		public void setImage(String filename) {
			reset();
			BitmapFactory.Options bfo = new BitmapFactory.Options();
			bfo.inPreferredConfig = Bitmap.Config.RGB_565;
			sourceImage = BitmapFactory.decodeFile(filename, bfo);
		}

		public void setImage(Bitmap image) {
			reset();
			sourceImage = image;
		}

		public Bitmap scaleImage(Bitmap src_img, int width, int height) {
			if (src_img == null) return null;
			Bitmap tmpImg = Bitmap.createScaledBitmap(src_img, width, height, false);
			return tmpImg;
		}

		public void scaleImage(int width, int height) {
			if (sourceImage == null) return;
			sourceImage = scaleImage(sourceImage, width, height);
		}
		
		public void scaleImageByWidth(int width) {
			if (sourceImage == null) return;
			float scale = ((float)width / (float)sourceImage.getWidth());
			int nw = (int) (sourceImage.getWidth() * scale);
			int nh = (int) (sourceImage.getHeight() * scale);
			sourceImage = scaleImage(sourceImage, nw, nh);
		}
		
		public void scaleImage(float scale) {
			if (sourceImage == null) return;
			int width = (int) (sourceImage.getWidth() * scale);
			int height = (int) (sourceImage.getHeight() * scale);
			sourceImage = scaleImage(sourceImage, width, height);
		}

		public boolean detect(String imagename) {
			setImage(imagename);
			return detect();
		}

		public boolean detect(Bitmap image) {
			setImage(image);
			return detect();
		}

		public boolean detect() {
			if (sourceImage == null) return false;

			arrayFaces = new FaceDetector(sourceImage.getWidth(), sourceImage.getHeight(), MAX_NUM_FACES);
			numFaces = arrayFaces.findFaces(sourceImage, getAllFaces);

			for (int i = 0; i < numFaces; i++) {
				getFace = getAllFaces[i];
				try {
					PointF eyesMP = new PointF();
					getFace.getMidPoint(eyesMP);
					eyesDistance[i] = getFace.eyesDistance();
					eyesMidPts[i] = eyesMP;

					Log.d("Face", i + " " + getFace.confidence() + " "
									+ getFace.eyesDistance() + " " + "Pose: ("
									+ getFace.pose(FaceDetector.Face.EULER_X) + ","
									+ getFace.pose(FaceDetector.Face.EULER_Y) + ","
									+ getFace.pose(FaceDetector.Face.EULER_Z) + ")"
									+ "Eyes Midpoint: (" + eyesMidPts[i].x + ","
									+ eyesMidPts[i].y + ")");
				} catch (Exception e) {
					Log.e("FaceDetect", "Face" + i + " is null");
				}
			}
			arrayFaces = null;

			return numFaces > 0 ? true : false;
		}

		public int numFaces() {
			return numFaces;
		}

		public Bitmap getOneFace(int i) {
			if (i >= numFaces) {
				Log.e("FaceDetect", "setOneFaceToBitmap: " + i + " >= "
						+ numFaces);
				return null;
			}
			if (eyesMidPts[i] == null) {
				Log.e("FaceDetect", "setOneFaceToBitmap: " + i + " is null");
				return null;
			}

			faceImage = Bitmap.createBitmap(sourceImage, left(i), top(i), width(i), height(i));
			faceIdx = i;

			return faceImage;
		}

		/* write face bitmaps to file */
		public void saveBitmapToFile(Bitmap image, String filename, int quality) {
			FileOutputStream fout = null;

			if (image == null) {
				Log.e("FaceDetect", "Err. image is null in 'saveBitmapToFile'");
				return;
			}
			try {
				/* 
				 * if exists, delete it first because of 
				 * the need to trigger observerMgr event. 
				 */
				File file = new File(filename);
				if (file.exists() == true) {
					file.delete();
				}
				
				fout = new FileOutputStream(filename);
				image.compress(Bitmap.CompressFormat.JPEG, quality, fout);
				fout.close();
			} catch (IOException e) {
				Log.e("FaceDetect",
						"Err. Cannot append to face file. " + e.toString());
			}
		}

		public void saveBitmapToFile(Bitmap image, String filename) {
			saveBitmapToFile(image, filename, DEFAULT_QUALITY);
		}

		/* write all faces to files */
		public void writeFacesToFile(String filename, int quality) {
			Bitmap allFaces = null;
			int i, max_width = 0, max_height = 0, face_cnt = 0, cnt;
			
			for (i = 0; i < eyesMidPts.length; i++) {
				if (eyesMidPts[i] != null) {
					if (width(i) > max_width) max_width = width(i);
					if (height(i) > max_height) max_height = height(i);
					face_cnt++;
				}
			}
			
			allFaces = Bitmap.createBitmap(max_width * face_cnt, max_height, Bitmap.Config.RGB_565);
			Canvas faces_canv = new Canvas(allFaces);
			
			cnt = 0;
			for (i = 0; i < eyesMidPts.length; i++) {
				if (eyesMidPts[i] != null) {
					/* extract faces */
					Bitmap one_face = Bitmap.createBitmap(sourceImage, left(i), top(i), width(i), height(i));
					/* concatenate them */
					faces_canv.drawBitmap(Bitmap.createScaledBitmap(one_face, max_width, max_height, false)
										, max_width * cnt++, 0f, null);
				}
			}
			
			if (allFaces != null && filename != null) {
				saveBitmapToFile(allFaces, filename);
			}
		}

		public void writeFacesToFile(String filename) {
			writeFacesToFile(filename, DEFAULT_QUALITY);
		}

		public String faceFileName(String imagename, int i) {
			StringBuilder b = new StringBuilder();
			b.append(imagename);
			return b.toString();
		}

		/* check boundaries */
		public int left(int i) {
			if (eyesMidPts[i].x >= eyesDistance[i])
				return (int) (eyesMidPts[i].x - eyesDistance[i]);
			return 0;
		}

		public int top(int i) {
			if (eyesMidPts[i].y >= eyesDistance[i])
				return (int) (eyesMidPts[i].y - eyesDistance[i]);
			return 0;
		}

		public int height(int i) {
			//int h = (int) (2.5 * eyesDistance[i]);
			int h = (int) (3 * eyesDistance[i]);
			if (top(i) + h > sourceImage.getHeight())
				return (sourceImage.getHeight() - top(i));
			return h;
		}

		public int width(int i) {
			int w = (int) (2.5 * eyesDistance[i]);
			if (left(i) + w > sourceImage.getWidth())
				return (sourceImage.getWidth() - left(i));
			return w;
		}

		public int right(int i) {
			if (left(i) + width(i) <= sourceImage.getWidth())
				return (left(i) + width(i));
			return sourceImage.getWidth();
		}

		public int bottom(int i) {
			if (top(i) + height(i) <= sourceImage.getHeight())
				return (top(i) + height(i));
			return sourceImage.getHeight();
		}
	}
	
	/* Entry function for all transformation */
	public static String execTransform(String cfg, String param)
	{
		if (cfg.equals(CONFIG_EXTRACT_FACES_FROM_IMAGE) == true) {
			return extractFaceFromImage(param);
		}
		else {
			Log.e(TAG, "! Unknown configuration: " + cfg);
			return "";
		}
	}
	
	public static String extractFaceFromImage(String path)
	{
		MedusaFaceDetector fd = (new MedusaTransformImageProcAdapter()).new MedusaFaceDetector();
		String sm_path = G.PATH_SDCARD + G.getDirectoryPathByTag("facedetect");
		String img_name, face_path = "null";
		
		MedusaUtil.ensureDirectoryPath(sm_path);
		
		File src_file = new File(path);
		Date date = new Date();
		img_name = "faces_" + src_file.getName().replace(".jpg", "####")
												.replace(".", "")
												.replace(" ", "")
												.replace("####", "_" + date.getTime() + ".jpg");
		
		fd.setImage(path);
		fd.scaleImageByWidth(800);
		fd.detect();
		
		if (fd.numFaces() > 0) {
			face_path = sm_path + img_name;
			fd.writeFacesToFile(face_path);
		}
		
		System.gc();
		
		return fd.numFaces() + "";
	}
}



