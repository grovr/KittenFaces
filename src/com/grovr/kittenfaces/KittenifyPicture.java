package com.grovr.kittenfaces;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.os.Build;
import android.os.Bundle;

public class KittenifyPicture extends Activity {

	private Bitmap kittenFacedBitmap;

	public static final String kittenifiedPhotoLocationID = "KITTENIFIED_PHOTO_LOCATION_ID";

	private String kittenPhotoLocation;

	private String originalPhotoLocation;

	private Boolean isRunning;

	private final int numOfKittenImages = 6;

	private boolean isFreeVersion;
	
	private final int scaledSmallestDimension = 300;

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
					kittenifyPicture(originalPhotoLocation);
				}
			});

			t.start();
		}
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
	
	private void createBitmapFromFile(String location) {
		if (Build.VERSION.SDK_INT < 11) {
			Bitmap immutable = BitmapFactory.decodeFile(location);
			kittenFacedBitmap = immutable.copy(immutable.getConfig(), true);
			cleanUpBitmap(immutable);
		} else {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inMutable = true;
			kittenFacedBitmap = BitmapFactory.decodeFile(location, options);
		}
		System.gc();
	}
	
	private List<RectF> alternateFindFaces() {
		Bitmap bitmap565 = kittenFacedBitmap.copy(Bitmap.Config.RGB_565, true);
		FaceDetector fd = new FaceDetector(bitmap565.getWidth(), bitmap565.getHeight(), 100);
		Face[] faces = new Face[100];
		int facesFound = fd.findFaces(bitmap565, faces);
		cleanUpBitmap(bitmap565);
		System.gc();
		List<RectF> faceRects = new LinkedList<RectF>();
		for (int i=0; i<facesFound; i++) {
			Face currentFace = faces[i];
			if (currentFace.confidence() > 0.3) {
				PointF midPoint = new PointF();
				currentFace.getMidPoint(midPoint);
				float halfWidth = currentFace.eyesDistance() * 1.5f;
				float topLeftX = midPoint.x - halfWidth;
				float topLeftY = midPoint.y - halfWidth;
				float widthHeight = 2.0f * halfWidth;
				faceRects.add(new RectF(topLeftX, topLeftY, topLeftX + widthHeight, topLeftY + widthHeight));
			}
		}
		return faceRects;
	}

	public void kittenifyPicture(String location) {
		cleanUpBitmap(kittenFacedBitmap);
		System.gc();
		
		createBitmapFromFile(location);
		List<RectF> faceRects = alternateFindFaces();

		addKittensToKittenFacedBitmapAtLocations(faceRects);
		faceRects = null;
		System.gc();
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

	private void addKittensToKittenFacedBitmapAtLocations(
			List<RectF> rectsToAddKittensAt) {
		Canvas canvas = new Canvas(kittenFacedBitmap);
		for (RectF faceRect : rectsToAddKittensAt) {
			addKittenToCanvasAtLocation(canvas, faceRect);
		}
		if (isFreeVersion){
			drawOnWatermark(canvas);
		}
		canvas = null;
		System.gc();
	}

	private void addKittenToCanvasAtLocation(Canvas canvas,
			RectF rectToAddKittenAt) {

		Bitmap kittenFaceBitmap = getRandomKittenBitmap();
		canvas.drawBitmap(kittenFaceBitmap, null,
				rectToAddKittenAt, null);
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
	
	private void drawOnWatermark(Canvas canvas) {
		Paint paint = new Paint();
		paint.setColor(Color.CYAN);
		paint.setTextSize(canvas.getHeight()/20);
		canvas.drawText("Kitten Faces Free", 0, canvas.getHeight()/20, paint);	
	}
	
	private void scaleKittenFacedImage() {
		double heightToWidthRatio = ((double) kittenFacedBitmap.getHeight()) / ((double) kittenFacedBitmap.getWidth());
		if (heightToWidthRatio > 1) {	
			kittenFacedBitmap = Bitmap.createScaledBitmap(kittenFacedBitmap, scaledSmallestDimension, (int) (scaledSmallestDimension * heightToWidthRatio), true);
		} else {
			kittenFacedBitmap = Bitmap.createScaledBitmap(kittenFacedBitmap, (int) (scaledSmallestDimension / heightToWidthRatio), scaledSmallestDimension, true);	
		}
	}

	private void writeImageToFile(String originalLocation) {
		kittenPhotoLocation = kittenImageLocation(originalLocation);
		try {
			writeBitmapToLocation(kittenFacedBitmap, kittenPhotoLocation);
		} catch (Exception e) {
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
