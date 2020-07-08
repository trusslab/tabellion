package edu.uci.ics.charm.tabellion;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.exifinterface.media.ExifInterface;

import com.camerakit.CameraKit;
import com.camerakit.CameraKitView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/*
Created Date: 07/01/2019
Created By: Myles Liu
Last Modified: 06/11/2020
Last Modified By: Myles Liu
Notes:

Pending To Do:
 */

public class TakePhoto extends AppCompatActivity {
    //Views
    private CameraKitView cameraKitView;
    private ImageView shutterButton;
    private ImageView previewImageView;
    private ImageView confirmButton;
    private ImageView cancelButton;

    private final static String TAG = "TakePhoto";
    private MyApp myApp;
    private Context context = this;

    //Permissions Related
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
    };

    //Contract Related
    private String contractNameInTabellion = "";
    private Contract temp_contract;      // This is a fake temp_contract for temp saving

    //Data
    private Intent receivedIntent;
    private byte[] lastCapturedImage;
    private String lastCapturedInterval;
    private String pathOfLastSavedImage = "";
    private String pathOfLastSavedSignature = "";
    private String nameOfSavedImage = "";

    //Loading ProgressDialog
    ProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);
        myApp = (MyApp) getApplication();

        cameraKitView = (CameraKitView) findViewById(R.id.activity_take_photo_camera);
        shutterButton = (ImageView) findViewById(R.id.activity_take_photo_shutter);
        previewImageView = (ImageView) findViewById(R.id.activity_take_photo_preview);
        confirmButton = (ImageView) findViewById(R.id.activity_take_photo_confirm);
        cancelButton = (ImageView) findViewById(R.id.activity_take_photo_cancel);

        /*
        receivedIntent = getIntent();
        // requestcode: 1 means offeror of 3rd party's app want to take a photo using this activity
        if(Objects.requireNonNull(receivedIntent.getStringExtra("requestcode")).equals("1")){
            setUpContractRelated();
        }
        */

        setUpCameraGestureListener();
        setUpShutterButton();
        setUpConfirmAndCancelButton();

        cameraKitView.setFacing(CameraKit.FACING_FRONT);

        hideSystemUI();
        hideToolbar();
    }

    private void setUpConfirmAndCancelButton(){
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Bitmap imageGoingToBeSaved = getBitmapFromEncoded(lastCapturedImage);
                pathOfLastSavedImage = saveImage(imageGoingToBeSaved);
                pathOfLastSavedSignature = myApp.storeSignatureOfByteArrayInCustomLocation(
                        getByteArrayFromPath(pathOfLastSavedImage),
                        getExternalFilesDir(
                                Environment.DIRECTORY_DOWNLOADS) + "/photos_taken_by_user/" + myApp.getEmailAddress() +
                                "/signature" + nameOfSavedImage);
                if(!pathOfLastSavedImage.equals("")){
                    startUploadingAndVerification();
                } else {
                    Toast.makeText(context, R.string.error_happened_when_saving_file, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmButton.setVisibility(View.INVISIBLE);
                cancelButton.setVisibility(View.INVISIBLE);
                cameraKitView.setVisibility(View.VISIBLE);
                //cameraKitView.onResume();
                shutterButton.setVisibility(View.VISIBLE);
                previewImageView.setVisibility(View.INVISIBLE);
            }
        });
    }

    private byte[] getByteArrayFromPath(String path){
        File file = new File(path);
        byte[] bytes = new byte[(int)file.length()];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    private void startUploadingAndVerification(){
        showProgressDialog(getString(R.string.verifying_gesture));
        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if(!msg.getData().getBoolean("is_success")){
                    Toast.makeText(context, R.string.gesture_verify_failure, Toast.LENGTH_LONG).show();
                    finish();
                    return false;
                }
                Intent intent = new Intent();
                intent.putExtra("photopath", pathOfLastSavedImage);
                setResult(RESULT_OK, intent);
                finish();
                return true;
            }
        });
        new Thread(new Connection(context, myApp).new UploadAndAnalyzeGesture(pathOfLastSavedImage,
                pathOfLastSavedSignature, lastCapturedInterval,
                myApp.getEmailAddress(), handler, 3)).start();
    }

    private void showProgressDialog(String text){
        progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(text);
        progressDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }

    private void setUpShutterButton(){
        shutterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "setUpShutterButton: shutter is being clicked...");
                cameraKitView.captureImage(new CameraKitView.ImageCallback() {
                    @Override
                    public void onImage(CameraKitView cameraKitView, final byte[] capturedImage) {
                        lastCapturedInterval = myApp.getCurrentTimeInterval();
                        lastCapturedImage = capturedImage;
                        previewImageView.setVisibility(View.VISIBLE);
                        confirmButton.setVisibility(View.VISIBLE);
                        cancelButton.setVisibility(View.VISIBLE);
                        previewImageView.setImageBitmap(getBitmapFromEncoded(capturedImage));
                        cameraKitView.setVisibility(View.INVISIBLE);
                        //cameraKitView.onPause();
                        shutterButton.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
    }

    private String saveImage(Bitmap capturedImage){

        File path = new File(getExternalFilesDir(
                Environment.DIRECTORY_DOWNLOADS) + "/photos_taken_by_user/" + myApp.getEmailAddress());

        path.mkdirs();

        OutputStream out = null;
        //File imageFile = new File(path);

        nameOfSavedImage = String.valueOf(System.currentTimeMillis());

        File imageFile = new File(getExternalFilesDir(
                Environment.DIRECTORY_DOWNLOADS) + "/photos_taken_by_user/" + myApp.getEmailAddress()
                , nameOfSavedImage + ".jpg"); //this works

        try {
            out = new FileOutputStream(imageFile);
            // choose JPEG format
            capturedImage.compress(Bitmap.CompressFormat.JPEG, 90, out);
            // Myles: If the quality above changed, the quality for saving signature should also be
            // changed, since right now they are both hard-coded.
            out.flush();

            // Add metadata
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, nameOfSavedImage);
            exif.saveAttributes();

            return imageFile.getAbsolutePath();
        } catch (FileNotFoundException e) {
            Log.d("Saeed: ", "[1]");
            e.printStackTrace();
            // manage exception ...
        } catch (IOException e) {
            Log.d("Saeed: ", "[2]");
            e.printStackTrace();
            // manage exception ...
        } finally {

            try {
                if (out != null) {
                    out.close();
                }

            } catch (Exception exc) {
                Log.d("Saeed: ", "[3]");
                exc.printStackTrace();
            }

        }
        return "";
    }

    private void setUpContractRelated(){
        contractNameInTabellion = receivedIntent.getStringExtra("contract_name_in_tabellion");
        setUpFakeTempContract();
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        //     | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        //     | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        //     | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.STATUS_BAR_HIDDEN
        );
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

    }

    private void setUpFakeTempContract(){
        //We set up this fake temp_contract is for saving files
        temp_contract = new Contract("fake_contract_for_external_" + contractNameInTabellion);
        temp_contract.setContractId("temp_external_" + contractNameInTabellion);
        temp_contract.setCurrentRole(1);
        temp_contract.setContractStatus(12);
        myApp.setCurrentViewingContract(temp_contract);
    }

    @Override
    protected void onStart() {
        super.onStart();
        cameraKitView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraKitView.onResume();
    }

    @Override
    protected void onPause() {
        cameraKitView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        cameraKitView.onStop();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        cameraKitView.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void grantStoragePermission(){
        int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private void hideToolbar(){
        Objects.requireNonNull(getSupportActionBar()).hide();
    }

    private void showToolbar(){
        Objects.requireNonNull(getSupportActionBar()).show();
    }

    private Bitmap getBitmapFromEncoded(byte[] image){
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    private void setUpCameraGestureListener(){
        cameraKitView.setGestureListener(new CameraKitView.GestureListener() {
            @Override
            public void onTap(CameraKitView cameraKitView, float v, float v1) {

            }

            @Override
            public void onLongTap(CameraKitView cameraKitView, float v, float v1) {

            }

            @Override
            public void onDoubleTap(CameraKitView cameraKitView, float v, float v1) {

            }

            @Override
            public void onPinch(CameraKitView cameraKitView, float v, float v1, float v2) {

            }
        });
    }

}
