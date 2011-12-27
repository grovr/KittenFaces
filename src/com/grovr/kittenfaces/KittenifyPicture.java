package com.grovr.kittenfaces;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;

public class KittenifyPicture extends Activity {

	private Bitmap kittenFacedBitmap;

	private CascadeClassifier mCascade;

	private float minFaceDimension = 10.0f;

	public static final String kittenifiedPhotoLocationID = "KITTENIFIED_PHOTO_LOCATION_ID";

	private String kittenPhotoLocation;

	private String originalPhotoLocation;

	private Boolean isRunning;

	private final int numOfKittenImages = 6;

	private boolean isFreeVersion;
	
	private final int maxWidth = 300;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.processing_popup);
		isRunning = false;
	}

	protected void onStart() {
		super.onStart();

		Bundle bundle = this.getIntent().getExtras();
		originalPhotoLocation = bundle
				.getString(KittenFacesActivity.originalPhotoLocationID);
		isFreeVersion = bundle.getBoolean(KittenFacesActivity.doWeScaleID);
		if (!isRunning) {
			isRunning = true;
			Thread t = new Thread(new Runnable() {
				public void run() {
					setupCascade();
					kittenifyPicture(originalPhotoLocation);
				}
			});

			t.start();
		}
	}

	private void setupCascade() {
		File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
		File cascadeFile = createLocalCascadeFileIn(cascadeDir);
		try {
			writeResourceToFile(R.raw.haarcascade_frontalface_alt_tree,
					cascadeFile);
		} catch (FileNotFoundException e) {
			exitActivityWithFailure();
		}
		mCascade = new CascadeClassifier(cascadeFile.getAbsolutePath());
		cascadeFile.delete();
		cascadeDir.delete();
	}

	private void exitActivityWithFailure() {
		Intent intent = this.getIntent();
		if (getParent() == null) {
			setResult(Activity.RESULT_FIRST_USER, intent);
		} else {
			getParent().setResult(Activity.RESULT_FIRST_USER, intent);
		}
		finish();
	}

	private File createLocalCascadeFileIn(File directory) {
		File cascadeFile = new File(directory, "lbpcascade_frontalface.xml");
		return cascadeFile;
	}

	private void writeResourceToFile(int resourceId, File file)
			throws FileNotFoundException {
		InputStream input = getResources().openRawResource(resourceId);
		FileOutputStream output = new FileOutputStream(file);
		try {
			writeInputStreamToOutputStream(input, output);
			input.close();
			output.close();
		} catch (IOException e) {
			exitActivityWithFailure();
		}
	}

	private void writeInputStreamToOutputStream(InputStream input,
			OutputStream output) throws IOException {
		byte[] buffer = new byte[4096];
		int bytesRead;
		while ((bytesRead = input.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}
	}

	protected void onDestroy() {
		super.onDestroy();
		isRunning = false;
		cleanUpBitmap(kittenFacedBitmap);
		System.gc();
	}

	private void cleanUpBitmap(Bitmap bitmap) {
		if (bitmap != null) {
			bitmap.recycle();
			bitmap = null;
		}
	}

	public void kittenifyPicture(String location) {
		Mat image = org.opencv.highgui.Highgui.imread(location, 1);

		List<Rect> faceRects = findFaces(image);
		cleanUpBitmap(kittenFacedBitmap);
		System.gc();
		copyMatToKittenFacedBitmap(image);
		image.release();
		image = null;
		addKittensToKittenFacedBitmapAtLocations(faceRects);
		if (isFreeVersion) {
			scaleKittenFacedImage();
		}
		writeImageToFile(location);

		cleanUpBitmap(kittenFacedBitmap);
		System.gc();
		setResult();
		isRunning = false;
		finish();
	}

	private List<Rect> findFaces(Mat image) {
		Mat greyVersion = getGrayscaleVersionOf(image);
		List<Rect> faceRects = new LinkedList<Rect>();
		Size minFaceSize = new Size(minFaceDimension, minFaceDimension);
		mCascade.detectMultiScale(greyVersion, faceRects, 1.2, 2,
				Objdetect.CASCADE_DO_CANNY_PRUNING, minFaceSize);
		greyVersion.release();
		greyVersion = null;
		System.gc();
		return faceRects;

	}

	private void copyMatToKittenFacedBitmap(Mat image) {
		Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2RGBA);
		kittenFacedBitmap = Bitmap.createBitmap(image.width(), image.height(),
				Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(image, kittenFacedBitmap);
	}

	private Mat getGrayscaleVersionOf(Mat image) {
		Mat greyVersion = new Mat();
		image.convertTo(greyVersion, org.opencv.core.CvType.CV_8UC1);
		return greyVersion;
	}

	private void addKittensToKittenFacedBitmapAtLocations(
			List<Rect> rectsToAddKittensAt) {
		Canvas canvas = new Canvas(kittenFacedBitmap);
		for (Rect faceRect : rectsToAddKittensAt) {
			addKittenToCanvasAtLocation(canvas, faceRect);
		}
		if (isFreeVersion){
			drawOnWatermark(canvas);
		}
		canvas = null;
		System.gc();
	}

	private void addKittenToCanvasAtLocation(Canvas canvas,
			Rect rectToAddKittenAt) {

		Bitmap kittenFaceBitmap = getRandomKittenBitmap();
		android.graphics.Rect androidGraphicsRectToAddKittenAt = openCVRectToAndroidGraphicsRect(rectToAddKittenAt);
		canvas.drawBitmap(kittenFaceBitmap, null,
				androidGraphicsRectToAddKittenAt, null);
		cleanUpBitmap(kittenFaceBitmap);
	}

	private Bitmap getRandomKittenBitmap() {
		Random gen = new Random();
		int randomKittenIndex = gen.nextInt(numOfKittenImages) + 1;
		Bitmap kittenFaceBitmap;
		switch (randomKittenIndex) {
		case 1:
			kittenFaceBitmap = BitmapFactory.decodeResource(
					this.getResources(), R.raw.face1);
			break;
		case 2:
			kittenFaceBitmap = BitmapFactory.decodeResource(
					this.getResources(), R.raw.face2);
			break;
		case 3:
			kittenFaceBitmap = BitmapFactory.decodeResource(
					this.getResources(), R.raw.face3);
			break;
		case 4:
			kittenFaceBitmap = BitmapFactory.decodeResource(
					this.getResources(), R.raw.face4);
			break;
		case 5:
			kittenFaceBitmap = BitmapFactory.decodeResource(
					this.getResources(), R.raw.face5);
			break;
		case 6:
			kittenFaceBitmap = BitmapFactory.decodeResource(
					this.getResources(), R.raw.face6);
			break;
		default:
			kittenFaceBitmap = null;
		}
		return kittenFaceBitmap;
	}

	private android.graphics.Rect openCVRectToAndroidGraphicsRect(Rect rect) {
		return new android.graphics.Rect(rect.x, rect.y, rect.x + rect.width,
				rect.y + rect.height);
	}
	
	private void drawOnWatermark(Canvas canvas) {
		Paint paint = new Paint();
		paint.setColor(Color.CYAN);
		paint.setTextSize(canvas.getHeight()/20);
		canvas.drawText("Kitten Faces Free", 0, canvas.getHeight()/20, paint);	
	}
	
	private void scaleKittenFacedImage() {
		double heightToWidthRatio = ((double) kittenFacedBitmap.getHeight()) / ((double) kittenFacedBitmap.getWidth());
		kittenFacedBitmap = Bitmap.createScaledBitmap(kittenFacedBitmap, maxWidth, (int) (maxWidth * heightToWidthRatio), true);
	}

	private void writeImageToFile(String originalLocation) {
		kittenPhotoLocation = kittenImageLocation(originalLocation);
		try {
			writeBitmapToLocation(kittenFacedBitmap, kittenPhotoLocation);
		} catch (Exception e) {
			Log.e(KittenFacesActivity.TAG, "Image writing failed " + e.toString());
			exitActivityWithFailure();
		}
	}

	private String kittenImageLocation(String originalLocation) {
		StringBuffer kittenLocBuff = new StringBuffer(originalLocation);
		int finalDotIndex = kittenLocBuff.lastIndexOf(".");
		kittenLocBuff.insert(finalDotIndex, "kitten");
		return kittenLocBuff.toString();
	}

	private void writeBitmapToLocation(Bitmap bitmap, String location)
			throws FileNotFoundException {
		FileOutputStream out = new FileOutputStream(location);
		bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
	}

	private void setResult() {
		Intent intent = this.getIntent();
		intent.putExtra(kittenifiedPhotoLocationID, kittenPhotoLocation);
		if (getParent() == null) {
			setResult(Activity.RESULT_OK, intent);
		} else {
			getParent().setResult(Activity.RESULT_OK, intent);
		}
	}
}
