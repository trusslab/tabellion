package edu.uci.ics.charm.tabellion;

import android.Manifest;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collections;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/*
Created Date: 2018
Created By: Saeed Mirzamohammadi
Last Modified: 12/07/2019
Last Modified By: Myles Liu
Notes:

 */

public class SigningActivity extends AppCompatActivity implements Callback,
        GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener{

    public static final int REQUEST_CODE = 1002;

    private static final String KEY_NAME = "yourKey";
    private Cipher cipher;
    private KeyStore keyStore;
    private KeyGenerator keyGenerator;
    private TextView textView;
    private FingerprintManager.CryptoObject cryptoObject;
    private FingerprintManager fingerprintManager;
    private KeyguardManager keyguardManager;
    private ImageView imageView;
    private static Context sContext;
    private Context context = this;
    private String TAG = "SigningActivity";
    private MyApp myApp;
    private SpannableString text;
    private TextView tv;
    private ImageView iv;
    private NotificationManager notificationManager;
    private LinearLayout reviewNoteLayout;

    public int page_counter = 1;
    private int current_signing_counter = 1;
    private ProgressDialog progressDialog;

    private boolean isFreezing = false;

    private StringBuilder logBuilder;

    private Time timeStampForStartViewingPage;
    private Time timeStampForEndViewingPage;

    private float counterForTakingScreenShotAndSigning = (float) 0.0;

    // The following two are for detecting swipe
    private float x1,x2;
    static final int MIN_DISTANCE = 150;
    private boolean waitingForAuthentication = false;

    private int counterForSuccessfullyUploadingBothScreenshotAndSignature = 0;

    // For gesture detection
    GestureDetector gestureDetector;

    @Override
    public void onAuthenticated(){
        waitingForAuthentication = false;

        // If in reviewing session
        if(current_signing_counter != page_counter){
            return;
        }
        //Callback function
        if(imageView.getVisibility() == View.INVISIBLE){
            Toast.makeText(context, R.string.remove_finger, Toast.LENGTH_SHORT).show();
            tryStartFingerprintAuthentication();
            return;
        }
        if(isFreezing){
            tryStartFingerprintAuthentication();
            return;
        }

        // Lines for adding log
        logBuilder.append("Current Page: ");
        logBuilder.append(page_counter);
        logBuilder.append("\n");
        logBuilder.append("Time spent by the user: ");
        String tempTimeForProcessing = new Time(System.currentTimeMillis() - timeStampForStartViewingPage.getTime()).toString();
        logBuilder.append(tempTimeForProcessing.split(":")[1]);
        logBuilder.append(":");
        logBuilder.append(tempTimeForProcessing.split(":")[2]);
        logBuilder.append(" (");
        logBuilder.append(System.currentTimeMillis() - timeStampForStartViewingPage.getTime());
        logBuilder.append(")");
        logBuilder.append("\n");
        // Lines for adding log (End)

        take_screenshotAndSign();
        myApp.getCurrentViewingContract().addToSignedPages(page_counter);
        myApp.addOrUpdateToCorrespondingContractList(myApp.getCurrentViewingContract());

        Log.d("Saeed: ", "Authenticated");
        page_counter++;
        current_signing_counter = page_counter;

        if(page_counter <= myApp.getCurrentViewingContract().getTotalImageNums() + 1) { // Plus one since we have Nlast-1

            if(myApp.getCurrentViewingContract().getCurrentRole() == 1){
                // The following lines are for uploading a single page's screenshot and signature
                Handler handlerForUploadingScreenshot = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        if(!msg.getData().getBoolean("is_success")){
                            Toast.makeText(context, "handlerForUploadingScreenshot: A screenshot" +
                                    " is failed to be uploaded...", Toast.LENGTH_SHORT).show();
                        }
                        return false;
                    }
                });

                Handler handlerForUploadingSignature = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        if(!msg.getData().getBoolean("is_success")){
                            Toast.makeText(context, "handlerForUploadingSignature: A signature" +
                                    " is failed to be uploaded...", Toast.LENGTH_SHORT).show();
                        }
                        return false;
                    }
                });

                new Thread(new Connection(context, myApp).new UploadScreenshotExternal(
                        myApp.getCurrentViewingContract(), page_counter - 1,
                        handlerForUploadingScreenshot)).start();

                new Thread(new Connection(context, myApp).new UploadSignatureExternal(
                        myApp.getCurrentViewingContract(), page_counter - 1,
                        handlerForUploadingSignature)).start();
                // lines for uploading a single page's screenshot and signature over here
            }

            iv = (ImageView) findViewById(R.id.signing_imageView);
            String fname;
            Log.d(TAG, "onAuthenticated: current page: " + page_counter + ", totalPage: " + myApp.getCurrentViewingContract().getTotalImageNums());
            if(page_counter == myApp.getCurrentViewingContract().getTotalImageNums() + 1) {
                fname = "Nlast-1.png";
            }
            else
                fname = "Ndoc-" + page_counter + ".png";
            File imagemain = new File(sContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    + "/" + myApp.getCurrentViewingContract().getContractId() + "/", fname);

            String path = imagemain.getPath();
            Log.d("Saeed: ", path);
            Bitmap image = BitmapFactory.decodeFile(path);
            iv.setScaleType(ImageView.ScaleType.FIT_XY);
            iv.setImageBitmap(image);

            tryStartFingerprintAuthentication();
            imageView.setVisibility(View.INVISIBLE);
            textView.setVisibility(View.VISIBLE);
            textView.setText(R.string.signing_going_to_next_page);

            final int timeLimit;
            final int freezingTime;
            if(myApp.isTransitionAndFreezingOn()){
                timeLimit = myApp.getCurrentViewingContract().getTransitionTime();
                freezingTime = 2000;
            } else {
                timeLimit = 0;
                freezingTime = 0;
            }

            new CountDownTimer(timeLimit, 1000){

                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    imageView.setVisibility(View.VISIBLE);
                    textView.setVisibility(View.INVISIBLE);
                    isFreezing = true;
                    new CountDownTimer(freezingTime, 1000){
                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            isFreezing = false;
                            //onAuthenticated();  // For evaluation purpose
                        }
                    }.start();
                }
            }.start();
        }
        else {

            myApp.writeLogToFile(logBuilder.toString(),
                    myApp.getCurrentViewingContract().getContractId() + "_signing_log.txt", "signing");

            Log.d(TAG, "onAuthenticated: The average for taking screenshot and signing is: "
                    + counterForTakingScreenShotAndSigning / myApp.getCurrentViewingContract().getTotalImageNums());

            myApp.getCurrentViewingContract().setLastActionTimeInterval(myApp.getCurrentTimeInterval());
            myApp.addOrUpdateToCorrespondingContractList(myApp.getCurrentViewingContract());
            Toast.makeText(this, "Document signatures submitted!",Toast.LENGTH_LONG).show();
            try {
                final Handler handler = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        if(msg.getData().getBoolean("is_success") && myApp.getCurrentViewingContract().getCurrentRole() == 0){
                            new Thread(new Connection(context, myApp).new SendVerifyAndProcessRequest(myApp.getCurrentViewingContract())).start();
                            Toast.makeText(sContext, R.string.signature_verification_request_submitted, Toast.LENGTH_LONG).show();
                        } else {
                            if(++counterForSuccessfullyUploadingBothScreenshotAndSignature == 2 &&
                                    msg.getData().getBoolean("is_success")){
                                new Thread(new Connection(context, myApp).new SendVerifyAndProcessRequest(myApp.getCurrentViewingContract())).start();
                                Toast.makeText(sContext, R.string.signature_verification_request_submitted, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(sContext, R.string.unknown_error, Toast.LENGTH_LONG).show();
                                Log.d(TAG, "handler: failure in uploading screenshot or signature, " +
                                        "counter is: " + counterForSuccessfullyUploadingBothScreenshotAndSignature);
                            }
                        }
                        progressDialog.dismiss();
                        Intent intent = new Intent(sContext, MainActivity.class);
                        startActivity(intent);
                        finish();
                        return false;
                    }
                });

                if(myApp.getCurrentViewingContract().getCurrentRole() == 1){
                    // The following lines are for uploading a single page's screenshot and signature

                    new Thread(new Connection(context, myApp).new UploadScreenshotExternal(
                            myApp.getCurrentViewingContract(), page_counter - 1,
                            handler)).start();

                    new Thread(new Connection(context, myApp).new UploadSignatureExternal(
                            myApp.getCurrentViewingContract(), page_counter - 1,
                            handler)).start();
                    // lines for uploading a single page's screenshot and signature over here
                } else {
                    new Thread(new Connection(sContext, myApp).new UploadScreenshotsAndSigns(myApp.getCurrentViewingContract(), handler))
                            .start(); // Upload signatures and real signatures
                }


                progressDialog = new ProgressDialog(context);
                progressDialog.setMessage(getString(R.string.uploading_signature));
                progressDialog.setCancelable(false);
                progressDialog.show();

            }
            catch (Exception e){
                Log.d("Saeed:", "execute error");
            }

        }

    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if(!myApp.getCurrentViewingContract().isContractCreatedByTabellion()){
            return false;
        }
        Log.d(TAG, "onDoubleTap: double tap detected, entering edit mode...");
        if(current_signing_counter == page_counter && myApp.getCurrentViewingContract().getCurrentRole() == 1){
            if(myApp.getCurrentViewingContract().getCurrentRole() == 1){
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                LinearLayout linearLayout = new LinearLayout(context);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams linearLayoutLayoutParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                linearLayout.setLayoutParams(linearLayoutLayoutParams);
                final EditText editText = new EditText(context);
                LinearLayout.LayoutParams editTextLayoutParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                editTextLayoutParams.setMargins(15, 10, 15, 15);
                editText.setLayoutParams(editTextLayoutParams);
                editText.setHint(R.string.leave_comment_on_contract);
                linearLayout.addView(editText);
                alertDialogBuilder.setView(linearLayout);
                alertDialogBuilder.setTitle(R.string.comment_dialog_title);
                alertDialogBuilder.setPositiveButton(R.string.confirm_comment_and_exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TO-DO: Start CommentActivity
                        Intent intent = new Intent(context, CommentActivity.class);
                        intent.putExtra("comment_string", editText.getText().toString());
                        startActivityForResult(intent, REQUEST_CODE);
                    }
                });
                alertDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                alertDialogBuilder.create().show();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(imageView.getVisibility() == View.INVISIBLE || isFreezing){
            return super.onTouchEvent(event);
        }
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = x2 - x1;

                if (Math.abs(deltaX) > MIN_DISTANCE)
                {
                    // Left to Right swipe action
                    if (x2 > x1)
                    {
                        Log.d(TAG, "onTouchEvent: Left to Right swipe [Previous]");
                        Log.d(TAG, "onTouchEvent: isSwipeBackEnabled: " + myApp.getCurrentViewingContract().isSwipeBackEnabled());
                        Log.d(TAG, "onTouchEvent: current page counter: " + page_counter);
                        if(myApp.getCurrentViewingContract().isSwipeBackEnabled() & page_counter != 1){
                            OperationsWithNDK.turnOffLedLightForSigning();
                            --page_counter;
                            goToPageCounterPage("Ndoc-", false);
                            reviewNoteLayout.setVisibility(View.VISIBLE);
                        } else if (myApp.getCurrentViewingContract().isSwipeBackEnabled()){
                            Toast.makeText(context, R.string.already_first_page_during_signing, Toast.LENGTH_SHORT).show();
                        }
                    }
                    // Right to left swipe action
                    else
                    {
                        Log.d(TAG, "onTouchEvent: Right to Left swipe [Next]");Log.d(TAG, "onTouchEvent: isSwipeBackEnabled: " + myApp.getCurrentViewingContract().isSwipeBackEnabled());
                        Log.d(TAG, "onTouchEvent: current page counter: " + page_counter);
                        if(myApp.getCurrentViewingContract().isSwipeBackEnabled() & page_counter + 1 < current_signing_counter){
                            OperationsWithNDK.turnOffLedLightForSigning();
                            ++page_counter;
                            goToPageCounterPage("Ndoc-", false);
                            reviewNoteLayout.setVisibility(View.VISIBLE);
                        } else if (myApp.getCurrentViewingContract().isSwipeBackEnabled() & page_counter + 1 == current_signing_counter) {
                            if(myApp.isTransitionAndFreezingOn()){
                                OperationsWithNDK.turnOnLedLightForSigning();
                            }
                            ++page_counter;
                            goToPageCounterPage("Ndoc-", true);
                            reviewNoteLayout.setVisibility(View.GONE);
                        } else if (myApp.getCurrentViewingContract().isSwipeBackEnabled()){
                            Toast.makeText(context, R.string.cannot_go_forward_during_signing, Toast.LENGTH_SHORT).show();
                        }
                    }

                }
                else
                {
                    // consider as something else - a screen tap for example
                }
                break;
        }
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private void goToPageCounterPage(final String fileName, final boolean withFreezingAndTransition){
        String fname = fileName + page_counter + ".png";
        if(page_counter == myApp.getCurrentViewingContract().getTotalImageNums() + 1) {
            fname = "Nlast-1.png";
        }
        File imagemain = new File(sContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                + "/" + myApp.getCurrentViewingContract().getContractId() + "/", fname);

        String path = imagemain.getPath();
        Log.d("Saeed: ", path);
        Bitmap image = BitmapFactory.decodeFile(path);
        iv.setScaleType(ImageView.ScaleType.FIT_XY);
        iv.setImageBitmap(image);

        imageView.setVisibility(View.INVISIBLE);
        textView.setVisibility(View.VISIBLE);
        textView.setText(R.string.signing_going_to_previous_page);
        textView.setPaintFlags(textView.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);

        final int timeLimit;
        final int timeLimitForFreezing;
        if(myApp.isTransitionAndFreezingOn() & withFreezingAndTransition){
            timeLimit = myApp.getCurrentViewingContract().getTransitionTime();
        } else {
            timeLimit = 0;
        }
        if(!withFreezingAndTransition){
            timeLimitForFreezing = 0;
        } else {
            timeLimitForFreezing = 2000;
        }

        new CountDownTimer(timeLimit, 1000){

            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                imageView.setVisibility(View.VISIBLE);
                textView.setVisibility(View.INVISIBLE);
                textView.setPaintFlags(0);
                isFreezing = true;
                if(withFreezingAndTransition){
                    tryStartFingerprintAuthentication();
                }
                new CountDownTimer(timeLimitForFreezing, 1000){
                    @Override
                    public void onTick(long millisUntilFinished) {

                    }

                    @Override
                    public void onFinish() {
                        isFreezing = false;
                    }
                }.start();
            }
        }.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        OperationsWithNDK.turnOffLedLightForSigning();  // Only works when the App has root access
        notificationManager.cancel(1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(myApp.isTransitionAndFreezingOn()){
            OperationsWithNDK.turnOnLedLightForSigning();  // Only works when the App has root access
        }
        // The following lines are for setting up signing notification
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelID = "Ongoing";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelID, "Ongoing channel", NotificationManager.IMPORTANCE_DEFAULT);
            channel.shouldShowLights();
            notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle(getString(R.string.currently_signing))
                .setSound(null)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);
        Notification ledNotification = builder.build();
        notificationManager.notify(1, ledNotification);
        // The above lines are for setting up signing notification

        tryStartFingerprintAuthentication();
        Log.d("Saeed: ", "run()");
    }

    public void take_screenshotAndSign() {

        Time timestamp = new Time(System.currentTimeMillis());

        imageView = (ImageView) findViewById(R.id.signing_imageView);
        Bitmap b = Screenshot.takescreenshotOfRootView(imageView);
        //page_counter++;
        Screenshot.storeScreenshot(b, "screenshot" + String.valueOf(page_counter), myApp.getCurrentViewingContract());

        myApp.storeScreenshotSign(b, "signature" + String.valueOf(page_counter));

        // Lines for adding log
        logBuilder.append("Time spent on generating screenshot and corresponding signature: ");
        logBuilder.append(System.currentTimeMillis() - timestamp.getTime());
        logBuilder.append("\n");
        counterForTakingScreenShotAndSigning += System.currentTimeMillis() - timestamp.getTime();
        // Lines for adding log (End)

//        try {
//            // Create a URL for the desired page
//            URL url = new URL("https://www.ics.uci.edu/~saeed/sample_doc.txt");
//            Log.d("Saeed: ", "Connected to " + url);
//            new UploadImageTask().execute(url);
//        }
//        catch (MalformedURLException e) {
//            Log.d("Saeed: ", "Error1");
//            e.printStackTrace();
//        }
    }

    public void test_colors(SpannableString text, TextView tv) {
        text.setSpan(new ForegroundColorSpan(Color.GREEN), 0, text.length(), 0);
        tv.setText(text, TextView.BufferType.SPANNABLE);
        take_screenshotAndSign();

        text.setSpan(new ForegroundColorSpan(Color.BLUE), 0, text.length(), 0);
        tv.setText(text, TextView.BufferType.SPANNABLE);
        take_screenshotAndSign();

        text.setSpan(new ForegroundColorSpan(Color.CYAN), 0, text.length(), 0);
        tv.setText(text, TextView.BufferType.SPANNABLE);
        take_screenshotAndSign();

        text.setSpan(new ForegroundColorSpan(Color.RED), 0, text.length(), 0);
        tv.setText(text, TextView.BufferType.SPANNABLE);
        take_screenshotAndSign();
    }
    public void test_bgcolors(SpannableString text, TextView tv) {
        text.setSpan(new BackgroundColorSpan(0xFF000000),0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(text, TextView.BufferType.SPANNABLE);
        take_screenshotAndSign();

        text.setSpan(new BackgroundColorSpan(0xFF0F0000),0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(text, TextView.BufferType.SPANNABLE);
        take_screenshotAndSign();

        text.setSpan(new BackgroundColorSpan(0xFF00FF00),0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(text, TextView.BufferType.SPANNABLE);
        take_screenshotAndSign();

        text.setSpan(new BackgroundColorSpan(0xFF0000FF),0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(text, TextView.BufferType.SPANNABLE);
        take_screenshotAndSign();

        text.setSpan(new BackgroundColorSpan(0xFFFFFFFF),0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(text, TextView.BufferType.SPANNABLE);
    }
    public void test_fonts(SpannableString text, TextView tv) {
        //Resources res = getResources();
        text.setSpan(new TypefaceSpan("monospace"), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(text, TextView.BufferType.SPANNABLE);
        take_screenshotAndSign();

        text.setSpan(new TypefaceSpan("serif"), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(text, TextView.BufferType.SPANNABLE);
        take_screenshotAndSign();

        text.setSpan(new TypefaceSpan("sans-serif"), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(text, TextView.BufferType.SPANNABLE);
        take_screenshotAndSign();
    }
    public void test_sizes(SpannableString text, TextView tv) {
        for( float i=0.25f; i<=4; i*=2) {
            text.setSpan(new RelativeSizeSpan(i), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tv.setText(text, TextView.BufferType.SPANNABLE);
            take_screenshotAndSign();
            text.setSpan(new RelativeSizeSpan(1/i), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
    public void test_transparency(SpannableString text, TextView tv) {
        int offset, val=0x10000000;

        offset = 0x00000000 + val;
        for( int i=0; i<8; i++) {
            text.setSpan(new ForegroundColorSpan(offset), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tv.setText(text, TextView.BufferType.SPANNABLE);
            take_screenshotAndSign();
            val *= 2;
            offset += val;
        }
    }

    public void fingerprint_init() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Get an instance of KeyguardManager and FingerprintManager//
            keyguardManager =
                    (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            fingerprintManager =
                    (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);

            //textView = (TextView) findViewById(R.id.textView2);

            //Check whether the device has a fingerprint sensor//
            if (!fingerprintManager.isHardwareDetected()) {
                // If a fingerprint sensor isn’t available, then inform the user that they’ll be unable to use your app’s fingerprint functionality//
                textView.setText("Your device doesn't support fingerprint authentication");
            }
            //Check whether the user has granted your app the USE_FINGERPRINT permission//
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                // If your app doesn't have this permission, then display the following text//
                textView.setText("Please enable the fingerprint permission");
            }

            //Check that the user has registered at least one fingerprint//
            if (!fingerprintManager.hasEnrolledFingerprints()) {
                // If the user hasn’t configured any fingerprints, then display the following message//
                textView.setText("No fingerprint configured. Please register at least one fingerprint in your device's Settings");
            }

            //Check that the lockscreen is secured//
            if (!keyguardManager.isKeyguardSecure()) {
                // If the user hasn’t secured their lockscreen with a PIN password or pattern, then display the following text//
                textView.setText("Please enable lockscreen security in your device's Settings");
            } else {
                try {
                    generateKey();
                } catch (FingerprintException e) {
                    e.printStackTrace();
                }

//                if (initCipher()) {
//                    //If the cipher is initialized successfully, then create a CryptoObject instance//
//                    cryptoObject = new FingerprintManager.CryptoObject(cipher);
//
//                    // Here, I’m referencing the FingerprintHandler class that we’ll create in the next section. This class will be responsible
//                    // for starting the authentication process (via the startAuth method) and processing the authentication process events//
//                    FingerprintHandler helper = new FingerprintHandler(this);
//                    helper.startAuth(fingerprintManager, cryptoObject);
//                }
            }
        }
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

//        int b = decorView.getSystemUiVisibility();
//        if ( (b | View.SYSTEM_UI_FLAG_LAYOUT_STABLE) == b) {
//            Log.d("Saeed: ", "enabled1");
//        }
//        if ( (b | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION) == b) {
//            Log.d("Saeed: ", "enabled2");
//        }
//        if ( (b | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) == b) {
//            Log.d("Saeed: ", "enabled3");
//        }

    }

    private void generateKey() throws FingerprintException {
        try {
            // Obtain a reference to the Keystore using the standard Android keystore container identifier (“AndroidKeystore”)//
            keyStore = KeyStore.getInstance("AndroidKeyStore");

            //Generate the key//
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            //Initialize an empty KeyStore//
            keyStore.load(null);

            //Initialize the KeyGenerator//
            keyGenerator.init(new

                    //Specify the operation(s) this key can be used for//
                    KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)

                    //Configure this key so that the user has to confirm their identity with a fingerprint each time they want to use it//
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());

            //Generate the key//
            keyGenerator.generateKey();

        } catch (KeyStoreException
                | NoSuchAlgorithmException
                | NoSuchProviderException
                | InvalidAlgorithmParameterException
                | CertificateException
                | IOException exc) {
            exc.printStackTrace();
            throw new FingerprintException(exc);
        }
    }
    //Create a new method that we’ll use to initialize our cipher//
    public boolean initCipher() {
        try {
            //Obtain a cipher instance and configure it with the properties required for fingerprint authentication//
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException |
                NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }

        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            //Return true if the cipher has been initialized successfully//
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {

            //Return false if cipher initialization failed//
            return false;
        } catch (KeyStoreException | CertificateException
                | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    private void tryStartFingerprintAuthentication(){
        if(!waitingForAuthentication){
            if (initCipher()) {
                //If the cipher is initialized successfully, then create a CryptoObject instance//
                cryptoObject = new FingerprintManager.CryptoObject(cipher);

                // Here, I’m referencing the FingerprintHandler class that we’ll create in the next section. This class will be responsible
                // for starting the authentication process (via the startAuth method) and processing the authentication process events//
                FingerprintHandler helper = new FingerprintHandler(this, this);
                helper.startAuth(fingerprintManager, cryptoObject);
                waitingForAuthentication = true;
            }
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    private class FingerprintException extends Exception {
        public FingerprintException(Exception e) {
            super(e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signing);

        myApp = (MyApp) getApplication();

        gestureDetector = new GestureDetector(context, SigningActivity.this);

        imageView = findViewById(R.id.signing_imageView);
        iv = imageView;
        textView = findViewById(R.id.signing_textView);
        tv = textView;

        reviewNoteLayout = findViewById(R.id.signing_linearLayout_overlap_review_note);

        logBuilder = new StringBuilder();

        tv.setVisibility(View.GONE);

        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
        }

        hideSystemUI();   // Should use style for theme instead (However, for some unknown reasons, style cannot hide navigation bar)

        sContext = getApplicationContext();
        fingerprint_init();

        String fname ="Ndoc-" + page_counter + ".png";
        File imagemain = new File(sContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                + "/" + myApp.getCurrentViewingContract().getContractId() + "/", fname);

        String path = imagemain.getPath();
        Log.d("Saeed: ", path);
        Bitmap image = BitmapFactory.decodeFile(path);
        iv.setScaleType(ImageView.ScaleType.FIT_XY);
        iv.setImageBitmap(image);

        // Lines for adding log
        timeStampForStartViewingPage = new Time(System.currentTimeMillis());
        logBuilder.append("Contract Name: ");
        logBuilder.append(myApp.getCurrentViewingContract().getContractName());
        logBuilder.append("\n");
        logBuilder.append("Contract ID: ");
        logBuilder.append(myApp.getCurrentViewingContract().getContractId());
        logBuilder.append("\n");
        // Lines for adding log (End)

        Log.d("Saeed1: ", String.valueOf(iv.getMaxHeight()));
        Log.d("Saeed2: ", String.valueOf(iv.getMaxWidth()));

        if(!myApp.getCurrentViewingContract().getSignedPages().isEmpty() &&
                myApp.getCurrentViewingContract().getCurrentRole() == 1){
            ArrayList<Integer> signedPages = myApp.getCurrentViewingContract().getSignedPages();
            Collections.sort(signedPages);
            for(int i = 1;;++i){
                if(!signedPages.contains(i)){
                    page_counter = i;
                    current_signing_counter = page_counter;
                    goToPageCounterPage("Ndoc-", false);
                    break;
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "The requestCode: " + requestCode + "; and the resultCode: " + resultCode);
        if(requestCode == REQUEST_CODE && resultCode == RESULT_OK){
            if(data != null){
                Log.d(TAG, "Testing should we exit signing session: " + data.getBooleanExtra("is_success", false));
            } else {
                Log.d(TAG, "For some reason, the data is null in activity result.");
            }
            if(data != null && data.getBooleanExtra("is_success", false)){
                finish();
            } else {
                Toast.makeText(context, R.string.cannot_send_comment, Toast.LENGTH_LONG).show();
            }
        }
    }
}
