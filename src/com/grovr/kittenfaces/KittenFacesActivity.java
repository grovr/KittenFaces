package com.grovr.kittenfaces;

import com.grovr.kittenfaces.*;

import java.io.File;
import java.io.FileOutputStream;
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
	
	private static final int MAX_NO_PICS = 3;
	
	public static final String TAG = "KITTEN_FACES";
	public static final String originalPhotoLocationID = "ORIGINAL_PHOTO_LOCATION";
	private static final String PHOTOS_TAKEN = "PHOTOS_TAKEN";

	private String iSelectedImagePath;

	private String iFilemanagerstring;
	
	private String iKittenPhotoLocation;
	
    
    private Intent iGalleryIntent;
    
    private Intent iCameraIntent;
    
    private Uri iPhotoPath;    
    
    private Intent iPhotoViewIntent;
    
    private Intent iKittenifyPhotoIntent;
    
    private File iKittenFile;
    
    private SharedPreferences iPrefs;
    
    private int iNumOfPicsDone;
    
    private AlertDialog iTooManyPicsPopup;
    

	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
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
        iTooManyPicsPopup = builder.create();
        
        Context mContext = this.getApplicationContext();
        iPrefs = mContext.getSharedPreferences("KittenFacesPrefs", 0);
        
        iNumOfPicsDone = iPrefs.getInt(PHOTOS_TAKEN, 0);
        
        if (iNumOfPicsDone >= MAX_NO_PICS)
        {
        	tooManyPhotosTaken();
        	return;
        }
        
        
        
        
        iGalleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        iGalleryIntent.setType("image/*");
        
        setupCameraIntent();      

        final Button galleryButton = (Button) findViewById(R.id.gallerybutton);
        galleryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (iNumOfPicsDone >= MAX_NO_PICS)
                {
                	tooManyPhotosTaken();
                	return;
                }
                // Launch Gallery selection here  
                System.gc();
                startActivityForResult(iGalleryIntent, SELECT_PICTURE);

            }
        });
        
        final Button cameraButton = (Button) findViewById(R.id.camerabutton);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Launch Camera here
        //    	startActivity(kittenifyPhotoIntent);
                if (iNumOfPicsDone >= MAX_NO_PICS)
                {
                	tooManyPhotosTaken();
                	return;
                }

                System.gc();
            	setupCameraIntent();
            	System.gc();
            	startActivityForResult(iCameraIntent, TAKE_PHOTO);
            }
        });
        

	    iPhotoViewIntent = new Intent(Intent.ACTION_VIEW);
	    
	    iKittenifyPhotoIntent = new Intent(this, KittenifyPicture.class);
	    
        
		final ImageView imageView = (ImageView) findViewById(R.id.facedimage);
		imageView.setVisibility(imageView.INVISIBLE);
		imageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (imageView.getVisibility() == imageView.VISIBLE)
				{
					Uri uri = Uri.fromFile(new File(iKittenPhotoLocation)); 
				    iPhotoViewIntent.setDataAndType(uri, "image/*");
				    Log.d(TAG, iKittenPhotoLocation);
				    Log.d(TAG, uri.toString());
					startActivity(iPhotoViewIntent);
				}
			}
		});

    }
    
    private void tooManyPhotosTaken()
    {
    	iTooManyPicsPopup.show();
    }
    
    
    public void setupCameraIntent(){
    	String storageState = Environment.getExternalStorageState();
        if(storageState.equals(Environment.MEDIA_MOUNTED)) {

        	Time theTime = new Time();
        	theTime.setToNow();
        	String time = theTime.toString();
            String path = Environment.getExternalStorageDirectory().getName() + File.separatorChar + "Android/data/" + this.getPackageName() + "/files/" + md5(time) + ".jpg";
            File photoFile = new File(path);
            try {
                if(photoFile.exists() == false) {
                	photoFile.getParentFile().mkdirs();
                	photoFile.createNewFile();
                }

            } catch (IOException e) {
                Log.e(TAG, "Could not create file.", e);
            }
            Log.i(TAG, path);

            iPhotoPath = Uri.fromFile(photoFile);
            iCameraIntent = null;
            System.gc();
            iCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE );
            iCameraIntent.putExtra( MediaStore.EXTRA_OUTPUT, iPhotoPath);
        }   else {
        	//TODO SD card not present
        	Log.e(TAG, "SD card not present cannot use camera");
         //   new AlertDialog.Builder(MainActivity.this)
         //   .setMessage("External Storeage (SD Card) is required.\n\nCurrent state: " + storageState)
         //   .setCancelable(true).create().show();
        }

    }
    
    public String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    
    private void goToMarket()
    {
    	Intent goToMarket = null;
    	// TODO put in my key version of app here
    	goToMarket = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.paulmaidment.games.flagsoftheworld"));
    	startActivity(goToMarket);
    	finish();
    }

    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.d(TAG, "OnActivityResult Start " + SystemClock.elapsedRealtime());
        if (resultCode == RESULT_OK) {
            System.gc();
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();

                //OI FILE Manager
                iFilemanagerstring = selectedImageUri.getPath();

                //MEDIA GALLERY
                iSelectedImagePath = getPath(selectedImageUri);

                data = null;

                //NOW WE HAVE OUR WANTED STRING
                if(iSelectedImagePath!=null)
                {
                    Bundle bundle = new Bundle();
                    bundle.putString(originalPhotoLocationID, iSelectedImagePath);
                    iKittenifyPhotoIntent.putExtras(bundle);
                	Log.d(TAG, "StartActivity Start " + SystemClock.elapsedRealtime());
                    startActivityForResult(iKittenifyPhotoIntent, KITTENIFY_PHOTO);
                }
                else
                {
                    Bundle bundle = new Bundle();
                    bundle.putString(originalPhotoLocationID, iFilemanagerstring);
                    iKittenifyPhotoIntent.putExtras(bundle);
                	Log.d(TAG, "StartActivity Start " + SystemClock.elapsedRealtime());
                    startActivityForResult(iKittenifyPhotoIntent, KITTENIFY_PHOTO);
                }
                
                
            }
            else if (requestCode == TAKE_PHOTO)
            {
                //OI FILE Manager
                iFilemanagerstring = iPhotoPath.getPath();

                //MEDIA GALLERY
                iSelectedImagePath = getPath(iPhotoPath);

                data = null;
                
                //NOW WE HAVE OUR WANTED STRING
                if(iSelectedImagePath!=null)
                {
                    Bundle bundle = new Bundle();
                    bundle.putString(originalPhotoLocationID, iSelectedImagePath);
                    iKittenifyPhotoIntent.putExtras(bundle);
                    startActivityForResult(iKittenifyPhotoIntent, KITTENIFY_PHOTO);
                }
                else
                {
                    Bundle bundle = new Bundle();
                    bundle.putString(originalPhotoLocationID, iFilemanagerstring);
                    iKittenifyPhotoIntent.putExtras(bundle);
                    startActivityForResult(iKittenifyPhotoIntent, KITTENIFY_PHOTO);
                }
            }
          	ImageView ourView = (ImageView) findViewById(R.id.facedimage);
        	ourView.invalidate();
        	ourView.setImageResource(0);
        	ourView.invalidate();
        	ourView.setVisibility(ourView.INVISIBLE);
        	TextView ourText = (TextView) findViewById(R.id.touchtext);
        	ourText.setText("");
            if (requestCode == KITTENIFY_PHOTO)
            {
                SharedPreferences.Editor edit = iPrefs.edit();
                iNumOfPicsDone++;
                edit.putInt(PHOTOS_TAKEN, iNumOfPicsDone);
                edit.commit();
            	iKittenPhotoLocation = data.getStringExtra(KittenifyPicture.kittenifiedPhotoLocationID);
            	//kittenBitmap = BitmapFactory.decodeFile(kittenPhotoLocation);
            	iKittenFile = new File(iKittenPhotoLocation);
            	ourView.setImageURI(Uri.fromFile(iKittenFile));
            	ourText.setText("Touch picture to view");
            	ourText.invalidate();
            	ourView.setVisibility(ourView.VISIBLE);
            }
        }
    }

    //UPDATED!
    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if(cursor!=null)
        {
            //HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
            //THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
            int column_index = cursor
            .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        else return null;
    }
    


}