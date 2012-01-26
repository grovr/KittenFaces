package com.grovr.kittenfaces;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class KittenFacesActivity extends Activity {

	private static final int SELECT_PICTURE = 1;
	private static final int TAKE_PHOTO = 2;
	private static final int KITTENIFY_PHOTO = 3;

	public static final String TAG = "KITTEN_FACES";
	public static final String originalPhotoLocationID = "ORIGINAL_PHOTO_LOCATION";
	public static final String doWeScaleID = "DO_WE_SCALE";

	public static final int ABOUT_ID = Menu.FIRST;
	public static final int LICENSE_ID = Menu.FIRST + 1;

	private String iKittenPhotoLocation;

	private Intent galleryInent;

	private Intent cameraIntent;

	private Uri photoFileUri;

	private Intent photoViewIntent;

	private Intent iKittenifyPhotoIntent;

	private boolean doWeScale;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		setupDoWeScale();
		hideOrSetupProButton();

		setupGalleryIntentAndButton();
		setupCameraIntentAndButton();
		setupPhotoViewIntentAndButton();

		iKittenifyPhotoIntent = new Intent(this, KittenifyPicture.class);

	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(0, ABOUT_ID, 0, R.string.menu_about);
        menu.add(0, LICENSE_ID, 0, R.string.menu_license);
        return result;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case ABOUT_ID:
            showAbout();
            return true;
        case LICENSE_ID:
            showLicense();
            return true;
        }
       
        return super.onOptionsItemSelected(item);
    }
	
	private void showAbout() {
		TextView myMsg = new TextView(this);
		myMsg.setText("Copyright 2012 Matthew Grover\nEmail: kittenfaces@grovr.co.uk");
		myMsg.setGravity(Gravity.CENTER_HORIZONTAL);

		new AlertDialog.Builder(this)
			.setView(myMsg)
			.setCancelable(true).create().show();

	}
	
	private void showLicense() {
		new AlertDialog.Builder(this)
			.setMessage(R.string.opencv_license)
			.setCancelable(true).create().show();
	}

	private void setupDoWeScale() {
		doWeScale = true;
	}

	private void hideOrSetupProButton() {
		Button proButton = (Button) findViewById(R.id.getprobutton);
		if (doWeScale) {
			proButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					System.gc();
					goToMarket();
				}
			});
		} else {
			proButton.setVisibility(Button.INVISIBLE);
		}
	}

	private void goToMarket() {
		Intent goToMarket = null;
		// TODO put in my key version of app here
		goToMarket = new Intent(Intent.ACTION_VIEW,
				Uri.parse("market://details?id=com.grovr.kittenfacespro"));
		startActivity(goToMarket);
		finish();
	}

	private void setupGalleryIntentAndButton() {
		setupGalleryIntent();
		setupGalleryButton();
	}

	private void setupGalleryIntent() {
		galleryInent = new Intent(Intent.ACTION_GET_CONTENT);
		galleryInent.setType("image/*");
	}

	private void setupGalleryButton() {
		Button galleryButton = (Button) findViewById(R.id.gallerybutton);
		galleryButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				System.gc();
				startActivityForResult(galleryInent, SELECT_PICTURE);
			}
		});
	}

	private void setupCameraIntentAndButton() {
		setupCameraIntent();
		if (cameraIntent != null) {
			setupCameraButton();
		} else {
			Button cameraButton = (Button) findViewById(R.id.camerabutton);
			cameraButton.setVisibility(Button.INVISIBLE);
		}
	}

	private void setupCameraIntent() {
		if (sDCardIsPresent()) {
			cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		} else {
			new AlertDialog.Builder(this)
					.setMessage(
							"External Storage (SD Card) is required to use camera.")
					.setCancelable(true).create().show();
		}

	}

	private boolean sDCardIsPresent() {
		String storageState = Environment.getExternalStorageState();
		return storageState.equals(Environment.MEDIA_MOUNTED);
	}

	private void setNewFileForCameraIntent() {
		String photoPath = generateRandomJpgPathOnSDCard();
		File photoFile = createAndReturnFileAt(photoPath);
		photoFileUri = Uri.fromFile(photoFile);
		cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFileUri);
	}

	private File createAndReturnFileAt(String path) {
		File photoFile = new File(path);
		try {
			if (photoFile.exists() == false) {
				photoFile.getParentFile().mkdirs();
				photoFile.createNewFile();
			}

		} catch (IOException e) {
			Log.e(TAG, "Could not create file.", e);
		}
		return photoFile;
	}

	private String generateRandomJpgPathOnSDCard() {
		String randomPath = Environment.getExternalStorageDirectory().getName()
				+ File.separatorChar + "DCIM" + File.separatorChar
				+ getRandomString() + ".jpg";
		return randomPath;
	}

	private String getRandomString() {
		Time theTime = new Time();
		theTime.setToNow();
		String time = theTime.toString();
		return md5(time);
	}

	public String md5(String s) {
		try {
			MessageDigest digest = java.security.MessageDigest
					.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++)
				hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

	private void setupCameraButton() {
		Button cameraButton = (Button) findViewById(R.id.camerabutton);
		cameraButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				System.gc();
				setNewFileForCameraIntent();
				System.gc();
				startActivityForResult(cameraIntent, TAKE_PHOTO);
			}
		});
	}

	private void setupPhotoViewIntentAndButton() {
		setupPhotoViewIntent();
		setupPhotoViewButton();
	}

	private void setupPhotoViewIntent() {
		photoViewIntent = new Intent(Intent.ACTION_VIEW);
	}

	private void setupPhotoViewButton() {
		final ImageView imageView = (ImageView) findViewById(R.id.facedimage);
		imageView.setVisibility(ImageView.INVISIBLE);
		imageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (imageView.getVisibility() == ImageView.VISIBLE) {
					Uri uri = Uri.fromFile(new File(iKittenPhotoLocation));
					photoViewIntent.setDataAndType(uri, "image/*");
					startActivity(photoViewIntent);
				}
			}
		});
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "OnActivityResult Start " + SystemClock.elapsedRealtime());
		if (resultCode == RESULT_OK) {
			System.gc();
			if (requestCode == SELECT_PICTURE) {
				kittenifyPhotoFromDataIn(data);
			} else if (requestCode == TAKE_PHOTO) {
				kittenifyPhotoFromPhotoDataIn(data);
			}
			emptyImageView();
			if (requestCode == KITTENIFY_PHOTO) {
				iKittenPhotoLocation = data
						.getStringExtra(KittenifyPicture.kittenifiedPhotoLocationID);
				displayKittenPhotoInImageView();
			}
		} else if (resultCode == RESULT_FIRST_USER) {
			new AlertDialog.Builder(this)
					.setMessage(
							"Sorry, something went wrong, please try again.")
					.setCancelable(true).create().show();
		}
	}

	private void kittenifyPhotoFromDataIn(Intent getPictureIntent) {
		Uri selectedImageUri = getPictureIntent.getData();
		String selectedImagePath = getPath(selectedImageUri);
		String filemanagerstring = selectedImageUri.getPath();

		kittenifyPhotoFromFirstLocationIfNotNullElseSecond(selectedImagePath,
				filemanagerstring);
	}

	private void kittenifyPhotoFromPhotoDataIn(Intent getPhotoIntent) {
		String selectedImagePath = getPath(photoFileUri);
		String filemanagerstring = photoFileUri.getPath();

		kittenifyPhotoFromFirstLocationIfNotNullElseSecond(selectedImagePath,
				filemanagerstring);
	}

	private void kittenifyPhotoFromFirstLocationIfNotNullElseSecond(
			String firstLocation, String secondLocation) {
		if (firstLocation != null) {
			kittenifyPhotoAtLocation(firstLocation);
		} else {
			kittenifyPhotoAtLocation(secondLocation);
		}
	}

	private void kittenifyPhotoAtLocation(String location) {
		Bundle bundle = new Bundle();
		bundle.putString(originalPhotoLocationID, location);
		bundle.putBoolean(doWeScaleID, doWeScale);
		iKittenifyPhotoIntent.putExtras(bundle);
		startActivityForResult(iKittenifyPhotoIntent, KITTENIFY_PHOTO);
	}

	private void emptyImageView() {
		ImageView ourView = (ImageView) findViewById(R.id.facedimage);
		ourView.invalidate();
		ourView.setImageResource(0);
		ourView.invalidate();
		ourView.setVisibility(ImageView.INVISIBLE);
		TextView ourText = (TextView) findViewById(R.id.touchtext);
		ourText.setText("");
	}

	private void displayKittenPhotoInImageView() {
		File kittenFile = new File(iKittenPhotoLocation);

		ImageView ourView = (ImageView) findViewById(R.id.facedimage);
		ourView.setImageURI(Uri.fromFile(kittenFile));
		ourView.setVisibility(ImageView.VISIBLE);

		TextView ourText = (TextView) findViewById(R.id.touchtext);
		ourText.setText("Touch picture to view");
		ourText.invalidate();
	}

	public String getPath(Uri uri) {
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		if (cursor != null) {
			int column_index = cursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		} else {
			return null;
		}
	}

}