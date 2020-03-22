package edu.uci.ics.charm.tabellion;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/*
Created Date: 06/07/2019
Created By: Myles Liu
Last Modified: 09/15/2019
Last Modified By: Myles Liu
Notes:

 */

public class SigningActivityExternal extends AppCompatActivity implements Callback,
        GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    private final static String TAG = "SigningActivityExternal";
    private Context context = this;
    private MyApp myApp;

    // For views
    private ConstraintLayout rootLayout;
    private ImageView screenshotImageView;
    private LinearLayout overlapNoteLayout;
    private ImageView overlapImageView;
    private TextView overlapTextView;

    // For authenticating
    private static final String KEY_NAME = "yourKey";
    private Cipher cipher;
    private KeyStore keyStore;
    private KeyGenerator keyGenerator;
    private FingerprintManager.CryptoObject cryptoObject;
    private FingerprintManager fingerprintManager;
    private KeyguardManager keyguardManager;
    private boolean waitingForAuthentication = false;

    // For handling touch event
    private float x1,x2;
    static final int MIN_DISTANCE = 150;

    // For signing
    // 0 means not finished signing;
    // 1 means successfully finished signing and uploaded;
    // 2 means successfully finished signing but failed to upload
    // 3 means the phone is not capable of signing temp_contract through the system
    // 4 means some other errors appear
    private Integer resultCode = 0;

    String contractNameInTabellion;
    Contract temp_contract;      // This is a fake temp_contract for temp saving

    String currentRole = "";

    private ProgressDialog progressDialog;

    private int initStartPageCounter = 0;
    private int currentPageCounter = 1;
    private int firstWaitingToSignPageNum = 1;
    private int totalPageCounter = -1;

    private List<Bitmap> screenshotsList = new ArrayList<>();

    private int countOfPreparingContracts = 0; // For syncing with servers when opening the app

    private boolean isFreezing = false;

    // For gesture detection
    GestureDetector gestureDetector;

    // For entering edit mode
    String appPackageName = "";
    String appEditClassName = "";

    // For determining is it in normal signing mode or comment signing mode
    // 0 means normal, 1 means signing comment
    int currentMode = 0;

    // The contract file
    ContractBlockChain contractBlockChain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singing_external);

        myApp = (MyApp) getApplication();

        gestureDetector = new GestureDetector(context, SigningActivityExternal.this);

        // Init views...
        rootLayout = (ConstraintLayout) findViewById(R.id.activity_signing_external_constraintlayout_root);
        screenshotImageView = (ImageView) findViewById(R.id.activity_signing_external_screenshot_imageView);
        overlapNoteLayout = (LinearLayout) findViewById(R.id.activity_signing_external_linearLayout_overlap_review_note);
        overlapImageView = (ImageView) findViewById(R.id.activity_signing_external_imageView_check_mark);
        overlapTextView = (TextView) findViewById(R.id.activity_signing_external_textView_note);

        // Getting screenshots and other data from 3rd party App...
        Intent intent = getIntent();
        contractNameInTabellion = intent.getStringExtra("contract_name_in_tabellion");
        currentRole = intent.getStringExtra("current_role");
        Log.d(TAG, "onCreate: Going to sign the contract as: " + currentRole);
        String screenshotsPath = intent.getStringExtra("screenshots");
        String contractFilePath = intent.getStringExtra("contract_file_path");
        String zipFilePath = intent.getStringExtra("zip_file_path");
        appPackageName = intent.getStringExtra("app_package_name");
        appEditClassName = intent.getStringExtra("app_edit_class_name");
        initStartPageCounter = intent.getIntExtra("init_start_page", 0);
        currentMode = intent.getIntExtra("signing_mode", 0);
        Log.d(TAG, "onCreate: current signing mode is: " + currentMode);
        /*
        String screenshotsData = "";
        try {
            screenshotsData = myApp.getStringFromFile(screenshotsPath);
        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_LONG).show();
            finish();
        }

        Gson gson = new Gson();
        List<byte[]> screenshotList = gson.fromJson(screenshotsData, new TypeToken<List<byte[]>>(){}.getType());
        //Log.d(TAG, "onCreate: We got " + screenshotList.size() + " screenshots from the third party App.");
        if (screenshotsData == null | screenshotList == null){
            // Kill the activity if we did not get screenshot successfully...
            finish();
        }

         */

        showProgressDialog("Loading contract...");
        try {
            ZipManager.unzip(zipFilePath, context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) +
                            "/tabellion/contracts");
        } catch (IOException e) {
            e.printStackTrace();
            finish();   // Exit when a problem found in unzipping files
        }
        Log.d(TAG, "onCreate: Going to try to open contract file: " +
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) +
                "/tabellion/contracts/" + contractNameInTabellion + "/" +
                contractNameInTabellion + ".tc");
        contractBlockChain = getContractBlockChainFromFilePath(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) +
                        "/tabellion/contracts/" + contractNameInTabellion + "/" +
                contractNameInTabellion + ".tc");
        progressDialog.dismiss();
        int totalNumOfScreenshots = contractBlockChain.getLatestContract().getTotalNumOfOfferorOriginalScreenshots();

        Log.d(TAG, "onCreate: total number of original screenshots we get is: " +
                totalNumOfScreenshots);

        /*
        for(byte[] screenshotData: screenshotList){
            screenshotsList.add(BitmapFactory.decodeByteArray(screenshotData, 0, screenshotData.length));
        }
         */

        for(int i = 0; i < totalNumOfScreenshots; ++i){
            screenshotsList.add(contractBlockChain.getLatestContract().getScreenshotFromOfferorOriginalScreenshots(i, context));
        }

        totalPageCounter = screenshotsList.size();
        Log.d(TAG, "We got a total of " + totalPageCounter + " screenshots to sign...");
        /*
        byte[] screenshotByteArray = intent.getByteArrayExtra("screenshot");
        if (screenshotByteArray == null){
            // Kill the activity if we did not get screenshot successfully...
            finish();
        }
        Bitmap screenshot = BitmapFactory.decodeByteArray(screenshotByteArray, 0, screenshotByteArray.length);

        // Setting screenshot to current page and save the original screenshot
        screenshotImageView.setImageBitmap(screenshot);
        Screenshot.storeScreenshot(screenshot, "screenshot_default_externally.png", new Contract("Test_Contract"), context);

        */

        //syncContractWithTabellionServerAndProcess(contractNameInTabellion);
        //setUpFakeTempContract(contractNameInTabellion);

        try {
            syncContractWithTabellionServerAndProcess(contractNameInTabellion);
        }
        catch (Exception e){
            e.printStackTrace();
            progressDialog.hide();
            finish();
        }

    }

    private ContractBlockChain getContractBlockChainFromFilePath(String pathOfFile){
        try {
            Gson gson = new Gson();
            return gson.fromJson(myApp.getStringFromFile(pathOfFile),
                    new TypeToken<ContractBlockChain>(){}.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ContractBlockChain();
    }

    private void setUpFakeTempContract(String contractNameInTabellion){
        //We set up this fake temp_contract is for saving files
        temp_contract = new Contract("fake_contract_for_external_" + contractNameInTabellion);
        temp_contract.setContractId("temp_external_" + contractNameInTabellion);
        temp_contract.setCurrentRole(1);
        temp_contract.setContractStatus(12);
        myApp.setCurrentViewingContract(temp_contract);
    }

    @Override
    protected void onPause() {
        if(progressDialog != null){
            progressDialog.dismiss();
        }
        OperationsWithNDK.turnOffLedLightForSigning();
        super.onPause();
    }

    private void showProgressDialog(String text){
        progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(text);
        progressDialog.show();
    }

    private void syncContractWithTabellionServerAndProcess(final String contractNameInTabellion){
        showProgressDialog(getString(R.string.syncing_with_server));
        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if(msg.getData().getBoolean("is_success")){
                    Log.d(TAG, "syncContractWithTabellionServerAndProcess: " +
                            "there is such temp_contract, going to start signing...");
                    syncContractWithServer(contractNameInTabellion);
                } else {
                    Log.d(TAG, "syncContractWithTabellionServerAndProcess: " +
                            "there is no such temp_contract, going to exit...");
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_LONG).show();
                    finish();
                }
                return false;
            }
        });
        new Thread(new Connection(context, myApp).new
                CheckIfContractNameExist(contractNameInTabellion, totalPageCounter,
                myApp.getEmailAddress(), handler, currentRole)).start();
    }

    private void tryStartSigning(String contractNameInTabellion){
        // Start Signing Session...
        //setUpCurrentViewingContract(contractNameInTabellion);
        //progressDialog.hide();
        change_overlap_note(0);
        fingerprint_init();
        //screenshotImageView.setImageBitmap(screenshotsList.get(0));
        goToTheFirstNotSignedPage();
        goToInitStartPageIfPossible();
        tryStartFingerprintAuthentication();
    }

    private void tryStartSigningComment(){
        change_overlap_note(0);
        fingerprint_init();
        tryStartFingerprintAuthentication();
        screenshotImageView.setImageBitmap(screenshotsList.get(0));
    }

    private void goToInitStartPageIfPossible(){
        // only 0 will not be accepted
        if(initStartPageCounter != 0){
            currentPageCounter = initStartPageCounter;
            initStartPageCounter = 0;
        }

        refreshPageAsCurrentPageCounter();
    }

    private void setUpCurrentViewingContract(String contractNameInTabellion){
        List<Contract> pendingToSignContract = myApp.getSavedPendingToSignContractList();
        for (Contract contract: pendingToSignContract){
            Log.d(TAG, "setUpCurrentViewingContract: Checking " + contract.getContractName() + " with " + contractNameInTabellion);
            if(contract.getContractName().equals(contractNameInTabellion)){
                if(currentRole.equals("offeror")){
                    contract.setCurrentRole(0);
                } else {
                    contract.setCurrentRole(1);
                }
                myApp.addOrUpdateToCorrespondingContractList(contract);
                myApp.setCurrentViewingContract(contract);
                Log.d(TAG, "setUpCurrentViewingContract: Target signing Contract located!!!");
                break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        refresh_result_msg();
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //hideSystemUI();
        hideToolbar();
        if(myApp.getCurrentViewingContract() != null &&
                !myApp.getCurrentViewingContract().isPageSigned(currentPageCounter)){
            OperationsWithNDK.turnOnLedLightForSigning();
        }
    }

    private void refresh_result_msg(){
        // This function should be called every time the resultCode get updated
        // Sending msg back to previous activity
        Intent intent = new Intent();
        intent.putExtra("signing_result", resultCode);
        setResult(Activity.RESULT_OK, intent);
    }

    private void change_overlap_note(int state){
        // state: 0 is not signed; 1 is signed
        if(state == 0){
            overlapNoteLayout.setBackgroundColor(getColor(R.color.colorPendingSignContractBackground));
            overlapTextView.setText(R.string.user_is_in_signing_session_during_signing);
            overlapImageView.setVisibility(View.GONE);
        } else if (state == 1){
            overlapNoteLayout.setBackgroundColor(getColor(R.color.design_default_color_secondary));
            overlapTextView.setText(R.string.user_is_in_review_session_during_signing);
            overlapImageView.setVisibility(View.VISIBLE);
        } else if (state == 2){
            overlapNoteLayout.setBackgroundColor(getColor(R.color.colorPendingSignContractLastPageBackground));
            overlapTextView.setText(R.string.user_is_in_signing_session_last_page_during_signing);
            overlapImageView.setVisibility(View.GONE);
        }
    }

    private void hideToolbar(){
        Objects.requireNonNull(getSupportActionBar()).hide();
    }

    private void showToolbar(){
        Objects.requireNonNull(getSupportActionBar()).show();
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        Objects.requireNonNull(getSupportActionBar()).hide();
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(screenshotImageView.getVisibility() == View.INVISIBLE || isFreezing){
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
                        Log.d(TAG, "onTouchEvent: current page counter: " + currentPageCounter);
                        if(currentPageCounter != 1){
                            --currentPageCounter;
                            refreshPageAsCurrentPageCounter();
                        } else {
                            Toast.makeText(context, R.string.already_first_page_during_signing, Toast.LENGTH_SHORT).show();
                        }
                    }
                    // Right to left swipe action
                    else
                    {
                        Log.d(TAG, "onTouchEvent: Right to Left swipe [Next]");Log.d(TAG, "onTouchEvent: isSwipeBackEnabled: " + myApp.getCurrentViewingContract().isSwipeBackEnabled());
                        Log.d(TAG, "onTouchEvent: current page counter: " + currentPageCounter);
                        if(currentPageCounter == totalPageCounter){
                            Toast.makeText(context, R.string.already_last_page_during_signing, Toast.LENGTH_SHORT).show();
                        } else {
                            ++currentPageCounter;
                            refreshPageAsCurrentPageCounter();
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



    @Override
    public void onAuthenticated() {
        //Toast.makeText(context, "Authenticated!!!", Toast.LENGTH_LONG).show();

        // Following if is just a temp test for signing comment
        if(currentMode == 1){
            resultCode = 1;
            refresh_result_msg();
            Log.d(TAG, "onAuthenticated: comment mode is enacted, and signing session is successfully completed.");
            finish();
            return;
        }

        waitingForAuthentication = false;

        // review mode
        if(myApp.getCurrentViewingContract().isPageSigned(currentPageCounter)){
            return;
        }

        /*
        // In the case the user is reviewing previous pages (Legacy, should be deleted)
        if(firstWaitingToSignPageNum != currentPageCounter){
            return;
        }
        */

        Bitmap screenshot = take_screenshotAndSign();
        myApp.getCurrentViewingContract().setSignedImage(currentPageCounter,
                screenshot, myApp.getScreenshotSign(screenshot));    // Add current page to signed pages
        myApp.addOrUpdateToCorrespondingContractList(myApp.getCurrentViewingContract());
        Log.d(TAG, "onAuthenticated: Now we have " + myApp.getCurrentViewingContract().getSignedPagesNums());
        ++currentPageCounter;
        firstWaitingToSignPageNum = currentPageCounter;

        if(myApp.getCurrentViewingContract().getSignedPagesNums().size() == totalPageCounter){

            // Finished signing, but still not uploaded
            resultCode = 2;
            refresh_result_msg();

            myApp.getCurrentViewingContract().setLastActionTimeInterval(myApp.getCurrentTimeInterval());
            myApp.addOrUpdateToCorrespondingContractList(myApp.getCurrentViewingContract());

            // TO-DO: Upload final screenshot and signature and start verification
            showProgressDialog(getString(R.string.syncing_with_server));
            upload_contract_and_process_verification();

        } else {

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
                    myApp.getCurrentViewingContract(), currentPageCounter - 1,
                    handlerForUploadingScreenshot)).start();

            new Thread(new Connection(context, myApp).new UploadSignatureExternal(
                    myApp.getCurrentViewingContract(), currentPageCounter - 1,
                    handlerForUploadingSignature)).start();


            // The following if phrase is for the case where some page(s)
            // left though the user signs the last page
            if(myApp.getCurrentViewingContract().getSignedPagesNums().size() + 1 == totalPageCounter |
                    currentPageCounter - 1 == totalPageCounter){
                goToTheFirstNotSignedPage();
            } else {
                refreshPageAsCurrentPageCounter();
            }
        }
    }

    private void goToTheFirstNotSignedPage(){
        for(int i = 1; i <= totalPageCounter; ++i){
            Log.d(TAG, "goToTheFirstNotSignedPage: Checking is " + i + " signed...");
            if (!myApp.getCurrentViewingContract().getSignedPagesNums().contains(i)){
                Log.d(TAG, "goToTheFirstNotSignedPage: going to sign " + i + " as last page...");
                currentPageCounter = i;
                break;
            }
        }
        if(currentPageCounter - 1 != totalPageCounter){
            refreshPageAsCurrentPageCounter();
        }
    }

    private void refreshPageAsCurrentPageCounter(){
        if(myApp.getCurrentViewingContract().getSignedPagesNums().contains(currentPageCounter)){
            OperationsWithNDK.turnOffLedLightForSigning();
        } else {
            OperationsWithNDK.turnOnLedLightForSigning();
        }
        if(myApp.getCurrentViewingContract().getSignedPagesNums().contains(currentPageCounter)){
            change_overlap_note(1);
        } else if (myApp.getCurrentViewingContract().getSignedPagesNums().size() + 1 == totalPageCounter){
            change_overlap_note(2);
        } else {
            change_overlap_note(0);
        }
        resultCode = 0;
        refresh_result_msg();
        screenshotImageView.setImageBitmap(screenshotsList.get(currentPageCounter - 1));
        if(!myApp.getCurrentViewingContract().getSignedPagesNums().contains(currentPageCounter)){
            tryStartFingerprintAuthentication();
        }
    }

    private void try_process_contract(){
        //Log.d(TAG, "try_process_contract: Step 1");
        //sync_temp_contract_with_real_contract();
        Log.d(TAG, "try_process_contract: Step 2");
        setUpCurrentViewingContract(contractNameInTabellion);
        progressDialog.dismiss();

        //The following line is only for debugging
        myApp.getCurrentViewingContract().clearAllSignedPages();

        if(currentMode == 0) {
            tryStartSigning(contractNameInTabellion);
        } else if(currentMode == 1) {
            tryStartSigningComment();
        }

        //Log.d(TAG, "try_process_contract: Step 3");
        //upload_contract_and_process_verification();
    }

    private void sync_temp_contract_with_real_contract(){
        List<Contract> pendingToSignContract = myApp.getSavedPendingToSignContractList();
        for (Contract contract: pendingToSignContract){
            Log.d(TAG, "setUpCurrentViewingContract: Checking " + contract.getContractName() + " with " + contractNameInTabellion);
            if(contract.getContractName().equals(contractNameInTabellion)){
                Log.d(TAG, "setUpCurrentViewingContract: Target signing Contract located!!!");
                copy_temp_contract_to_real_contract(contract);
                break;
            }
        }
    }

    private void copy_temp_contract_to_real_contract(Contract real_contract){
        FileOperation.copyAllFilesInDirToAnotherDir(
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" + temp_contract.getContractId(),
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" + real_contract.getContractId());

    }

    private void upload_contract_and_process_verification(){

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
                if(msg.getData().getBoolean("is_success")){
                    new Thread(new Connection(context, myApp).new
                            SendVerifyAndProcessRequestExternal(
                            myApp.getCurrentViewingContract(), currentRole)).start();
                    Toast.makeText(context,
                            R.string.signature_verification_request_submitted, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_LONG).show();
                }
                resultCode = 1;
                refresh_result_msg();
                progressDialog.hide();
                finish();
                return false;
            }
        });

        new Thread(new Connection(context, myApp).new UploadScreenshotExternal(
                myApp.getCurrentViewingContract(), currentPageCounter - 1,
                handlerForUploadingScreenshot)).start();

        new Thread(new Connection(context, myApp).new UploadSignatureExternal(
                myApp.getCurrentViewingContract(), currentPageCounter - 1,
                handlerForUploadingSignature)).start();
    }

    public Bitmap take_screenshotAndSign() {

        Bitmap b = Screenshot.takescreenshotOfRootView(rootLayout);
        //page_counter++;
        Screenshot.storeScreenshot(b, "screenshot" + currentPageCounter,
                myApp.getCurrentViewingContract(), context);

        myApp.storeScreenshotSign(b, "signature" + currentPageCounter);
        return b;
    }

    public void syncContractWithServer(final String contractNameInTabellion){
        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                ArrayList<String> contractIDs = msg.getData().getStringArrayList("contractIDs");
                Log.d(TAG, "syncContractWithServer: what we get from server: " + contractIDs);
                Log.d(TAG, "syncContractWithServer: what we get from local: " + myApp.getAllContractIDs());
                List<String> localContractIDs = myApp.getAllContractIDs();

                if(contractIDs != null && !contractIDs.isEmpty()){
                    for(String localContractID: localContractIDs){
                        if(!contractIDs.contains(localContractID)){
                            myApp.removeContractFromCorrespondingContractList(localContractID);
                        }
                    }
                    prepareContracts(contractIDs, contractNameInTabellion);
                } else {
                    for(String localContractID: localContractIDs){
                        myApp.removeContractFromCorrespondingContractList(localContractID);
                    }
                    //tryStartSigning(contractNameInTabellion);
                    try_process_contract();
                }
                return false;
            }
        });
        new Thread(new Connection(context, myApp).new SyncAllContracts(handler, myApp.getEmailAddress())).start();
    }

    private void prepareContracts(List<String> contractIDs, final String contractNameInTabellion){
        final int numOfContracts = contractIDs.size();
        if(numOfContracts == 0){
            //tryStartSigning(contractNameInTabellion);
            try_process_contract();
        } else {
            for(final String contractID: contractIDs){
                Handler handler = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        ArrayList<String> contractInfo = msg.getData().getStringArrayList("contractInfo");
                        Log.d(TAG, "prepareContracts: " + "going to add or update temp_contract " + contractID + " with info: " + contractInfo);
                        myApp.addOrUpdateToCorrespondingContractList(myApp.newContractByInfo(contractInfo, contractID));
                        ++countOfPreparingContracts;
                        if(countOfPreparingContracts == numOfContracts){
                            countOfPreparingContracts = 0;
                            //tryStartSigning(contractNameInTabellion);
                            try_process_contract();
                        }
                        return false;
                    }
                });
                new Thread(new Connection(context, myApp).new GetOneContract(handler, contractID)).start();
            }
        }
    }

    private void generateKey() throws SigningActivityExternal.FingerprintException {
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
            throw new SigningActivityExternal.FingerprintException(exc);
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
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        Log.d(TAG, "onDoubleTap: double tap detected, entering edit mode...");
        if(currentMode != 1){
            if(currentRole.equals("offeree")){
                startEditModeInPreviousApp();
            }
        }
        return false;
    }

    private void startEditModeInPreviousApp(){
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(appPackageName, appEditClassName);
        if(myApp.getCurrentViewingContract().getCurrentRole() == 0){
            intent.putExtra("current_role", "offeror");
        } else if (myApp.getCurrentViewingContract().getCurrentRole() == 1){
            intent.putExtra("current_role", "offeree");
        }
        intent.putExtra("current_mode", "edit");
        Log.d(TAG, "startEditModeInPreviousApp: Going to start edit mode in page: " +
                (currentPageCounter - 1));
        intent.putExtra("current_page", currentPageCounter - 1);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
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
                Toast.makeText(context, "Your device doesn't support fingerprint authentication", Toast.LENGTH_LONG).show();
            }
            //Check whether the user has granted your app the USE_FINGERPRINT permission//
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                // If your app doesn't have this permission, then display the following text//
                Toast.makeText(context, "Please enable the fingerprint permission", Toast.LENGTH_LONG).show();
            }

            //Check that the user has registered at least one fingerprint//
            if (!fingerprintManager.hasEnrolledFingerprints()) {
                // If the user hasn’t configured any fingerprints, then display the following message//
                Toast.makeText(context, "No fingerprint configured. Please register at least one" +
                        " fingerprint in your device's Settings", Toast.LENGTH_LONG).show();
            }

            //Check that the lockscreen is secured//
            if (!keyguardManager.isKeyguardSecure()) {
                // If the user hasn’t secured their lockscreen with a PIN password or pattern, then display the following text//
                Toast.makeText(context, "Please enable lockscreen security in your device's Settings", Toast.LENGTH_LONG).show();
            } else {
                try {
                    generateKey();
                } catch (SigningActivityExternal.FingerprintException e) {
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

}
