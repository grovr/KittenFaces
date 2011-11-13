package com.grovr.kittenfaces;

import com.grovr.kittenfaces.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.CvANN_MLP;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

public class KittenFacesActivity extends Activity {
	
	private static final int SELECT_PICTURE = 1;
	private static final int TAKE_PHOTO = 2;
	private static final int KITTENIFY_PHOTO = 3;
	
	private static final int MAX_NO_PICS_KITTENIFIED = 666;
	
	public static final String TAG = "KITTEN_FACES";
	public static final String originalPhotoLocationID = "ORIGINAL_PHOTO_LOCATION";
	public static final String doWeScaleID = "DO_WE_SCALE";
	private static final String PHOTOS_KITTENIFIED = "PHOTOS_TAKEN";

	private String iKittenPhotoLocation;
	
    private Intent galleryInent;
    
    private Intent cameraIntent;
    
    private Uri photoFileUri;    
    
    private Intent photoViewIntent;
    
    private Intent iKittenifyPhotoIntent;
    
    private File countFile;
    
    private int numOfPicsDone;
    
    private AlertDialog limitReachedPopup;
    

	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
             
        createLimitReachedDialog();
        setupInitialPhotosKittenifiedCount();
        ifShouldntAllowMoreShowDialog();
        
        setupGalleryIntentAndButton();
        setupCameraIntentAndButton();      
        setupPhotoViewIntentAndButton();

	    iKittenifyPhotoIntent = new Intent(this, KittenifyPicture.class);

    }
    
    private void createLimitReachedDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please purchase the full version to Kittenify more photos")
               .setCancelable(false)
               .setPositiveButton("Full version", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                        goToMarket();
                   }
               })
               .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                        finish();
                   }
               });
        limitReachedPopup = builder.create();
    }
    
    private void goToMarket()
    {
    	Intent goToMarket = null;
    	// TODO put in my key version of app here
    	goToMarket = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.grovr.kittenfacespro"));
    	startActivity(goToMarket);
    	finish();
    }
    
    private void setupInitialPhotosKittenifiedCount()
    {
    	String countFilePath = Environment.getExternalStorageDirectory().getName() + 
    							File.separatorChar + 
    							"Android/data/" + "system.ct";
        countFile = new File(countFilePath);
        try 
        {
	        if(countFile.exists())
	        {
	        	FileInputStream fileInputStream = new FileInputStream(countFile);
	        	DataInputStream dataInputStream = new DataInputStream(fileInputStream);
	        	numOfPicsDone = dataInputStream.readInt();
	        } 
	        else
	        {
	        	countFile.getParentFile().mkdirs();
	        	countFile.createNewFile();
	        	numOfPicsDone = 0;
	        }
        }
        catch (Exception e)
        {
        	numOfPicsDone = 0;
        }
    }
    
    private void ifShouldntAllowMoreShowDialog()
    {
    	if (!isProVersionInstalled())
    	{
	    	if(limitHasBeenReached())
	    	{
	    		limitReachedPopup.show();
	    	}
    	}
    }
    
    private boolean isProVersionInstalled()
    {
    	try
    	{
    		getPackageManager().getApplicationInfo("com.grovr.kittenfacespro",0);
    		return true;
    	}
    	catch (NameNotFoundException e)
    	{
        	return false;
    	}
    }
    
    private boolean limitHasBeenReached()
    {
    	return numOfPicsDone > MAX_NO_PICS_KITTENIFIED;
    }
    
    private void setupGalleryIntentAndButton()
    {
    	setupGalleryIntent();
    	setupGalleryButton();
    }
    
    private void setupGalleryIntent()
    {
        galleryInent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryInent.setType("image/*");
    }
    
    private void setupGalleryButton()
    {
        Button galleryButton = (Button) findViewById(R.id.gallerybutton);
        galleryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	ifShouldntAllowMoreShowDialog(); 
                System.gc();
                startActivityForResult(galleryInent, SELECT_PICTURE);
            }
        });
    }
    
    private void setupCameraIntentAndButton()
    {
    	setupCameraIntent();
    	if (cameraIntent != null)
    	{
    		setupCameraButton();		
    	}
    	else
    	{
    		Button cameraButton = (Button) findViewById(R.id.camerabutton);
    		cameraButton.setVisibility(cameraButton.INVISIBLE);
    	}
    }
    
    
    private void setupCameraIntent(){
        if(sDCardIsPresent()) {
            cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE );
        }   else {
            new AlertDialog.Builder(this)
            .setMessage("External Storage (SD Card) is required to use camera.")
            .setCancelable(true).create().show();
        }

    }
    
    private boolean sDCardIsPresent()
    {
    	String storageState = Environment.getExternalStorageState();
    	return storageState.equals(Environment.MEDIA_MOUNTED);
    }
    
    private void setNewFileForCameraIntent()
    {
        String photoPath = generateRandomJpgPathOnSDCard();
        File photoFile = createAndReturnFileAt(photoPath);
        photoFileUri = Uri.fromFile(photoFile);
        cameraIntent.putExtra( MediaStore.EXTRA_OUTPUT, photoFileUri);
    }
    
    private File createAndReturnFileAt(String path)
    {
        File photoFile = new File(path);
        try {
            if(photoFile.exists() == false) {
            	photoFile.getParentFile().mkdirs();
            	photoFile.createNewFile();
            }

        } catch (IOException e) {
            Log.e(TAG, "Could not create file.", e);
        }
        return photoFile;
    }
    
    private String generateRandomJpgPathOnSDCard()
    {
    	String randomPath = Environment.getExternalStorageDirectory().getName() + 
    						File.separatorChar + 
    						"DCIM" + 
    						File.separatorChar + 
    						getRandomString() +
    						".jpg";
    	return randomPath;
    }
    
    private String getRandomString()
    {
    	Time theTime = new Time();
    	theTime.setToNow();
    	String time = theTime.toString();
    	return md5(time);
    }
    
    public String md5(String s) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    private void setupCameraButton()
    {
        Button cameraButton = (Button) findViewById(R.id.camerabutton);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	ifShouldntAllowMoreShowDialog();
                System.gc();
                setNewFileForCameraIntent();
            	System.gc();
            	startActivityForResult(cameraIntent, TAKE_PHOTO);
            }
        });
    } 
    
    private void setupPhotoViewIntentAndButton()
    {
    	setupPhotoViewIntent();
    	setupPhotoViewButton();
    }
    
    private void setupPhotoViewIntent()
    {
	    photoViewIntent = new Intent(Intent.ACTION_VIEW);
    }
    
    private void setupPhotoViewButton()
    {
		final ImageView imageView = (ImageView) findViewById(R.id.facedimage);
		imageView.setVisibility(imageView.INVISIBLE);
		imageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (imageView.getVisibility() == imageView.VISIBLE)
				{
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
            }
            else if (requestCode == TAKE_PHOTO)
            {
            	kittenifyPhotoFromPhotoDataIn(data);
            }
            emptyImageView();
            if (requestCode == KITTENIFY_PHOTO)
            {
            	incrementNumOfPicsDone();
            	iKittenPhotoLocation = data.getStringExtra(KittenifyPicture.kittenifiedPhotoLocationID);
            	displayKittenPhotoInImageView();
            }
        }
        else if (resultCode == RESULT_FIRST_USER)
        {
            new AlertDialog.Builder(this)
            .setMessage("Sorry, something went wrong, please try again.")
            .setCancelable(true).create().show();
        }
    }
    
    private void kittenifyPhotoFromDataIn(Intent getPictureIntent)
    {
    	Uri selectedImageUri = getPictureIntent.getData();
        String selectedImagePath = getPath(selectedImageUri);
        String filemanagerstring = selectedImageUri.getPath();
        
        kittenifyPhotoFromFirstLocationIfNotNullElseSecond(selectedImagePath, filemanagerstring);
    }
    
    private void kittenifyPhotoFromPhotoDataIn(Intent getPhotoIntent)
    {
        String selectedImagePath = getPath(photoFileUri);
        String filemanagerstring = photoFileUri.getPath();
        
        kittenifyPhotoFromFirstLocationIfNotNullElseSecond(selectedImagePath, filemanagerstring);
    }
    
    private void kittenifyPhotoFromFirstLocationIfNotNullElseSecond(String firstLocation, String secondLocation)
    {
        if(firstLocation!=null)
        {
        	kittenifyPhotoAtLocation(firstLocation);
        }
        else
        {
        	kittenifyPhotoAtLocation(secondLocation);
        }
    }
     
    private void kittenifyPhotoAtLocation(String location)
    {
    	Bundle bundle = new Bundle();
    	bundle.putString(originalPhotoLocationID, location);
    	iKittenifyPhotoIntent.putExtras(bundle);
    	startActivityForResult(iKittenifyPhotoIntent, KITTENIFY_PHOTO);
    }
    
    private void emptyImageView()
    {
      	ImageView ourView = (ImageView) findViewById(R.id.facedimage);
    	ourView.invalidate();
    	ourView.setImageResource(0);
    	ourView.invalidate();
    	ourView.setVisibility(ourView.INVISIBLE);
    	TextView ourText = (TextView) findViewById(R.id.touchtext);
    	ourText.setText("");
    }
    
    private void incrementNumOfPicsDone()
    {
        numOfPicsDone++;
    	FileOutputStream fileOutputStream;
		try
		{
			fileOutputStream = new FileOutputStream(countFile);
	    	DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
	    	dataOutputStream.writeInt(numOfPicsDone);
		} 
		catch (Exception e) 
		{
		}
    }
    
    private void displayKittenPhotoInImageView()
    {
    	File kittenFile = new File(iKittenPhotoLocation);
    	
    	ImageView ourView = (ImageView) findViewById(R.id.facedimage);
    	ourView.setImageURI(Uri.fromFile(kittenFile));
    	ourView.setVisibility(ourView.VISIBLE);
    	
    	TextView ourText = (TextView) findViewById(R.id.touchtext);
    	ourText.setText("Touch picture to view");
    	ourText.invalidate();
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if(cursor!=null)
        {
            int column_index = cursor
            .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        else
        {
        	return null;
        }
    }
    


}