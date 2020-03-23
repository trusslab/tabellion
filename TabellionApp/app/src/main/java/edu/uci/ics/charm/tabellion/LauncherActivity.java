package edu.uci.ics.charm.tabellion;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashSet;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/*
Created Date: 03/28/2019
Created By: Myles Liu
Last Modified: 03/22/2020
Last Modified By: Myles Liu
Notes:
 */

public class LauncherActivity extends AppCompatActivity {

    private static final String TAG = "LauncherActivity ";

    private MyApp myApp;
    private Context context = this;

    private TextView textView;
    private ProgressBar progressBar;

    private static final String KEY_NAME = "yourKey";
    private Cipher cipher;
    private KeyStore keyStore;
    private KeyGenerator keyGenerator;

    private int reRegisterCounter = 1;

    static final int RE_REGISTER_REQUEST = 1;

    private Handler handlerForDeletingAccount;

    private int sizeOfReRegisteredRequiredAccounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        myApp = (MyApp) getApplication();

        initActivity();

        if(myApp.isWaitingForNetWork()){
            textView.setText(R.string.waiting_for_internet);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (myApp.isWaitingForNetWork()){}
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    });
                }
            }).start();
        }else {
            startNetworkCheck();
        }
    }

    @Override
    public void onBackPressed() {
        if(!myApp.isWaitingForNetWork()){
            super.onBackPressed();
        }
    }

    private void startNetworkCheck(){
        if(myApp.isInternetConnected()){
            startEnvironmentCheck();
        } else {
            textView.setText(R.string.waiting_for_internet);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!myApp.isInternetConnected()){}
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startEnvironmentCheck();
                        }
                    });
                }
            }).start();
        }
    }

    private void startEnvironmentCheck(){
        textView.setText(R.string.checking_environment);
        try {
            generateOrGetKey();
            if(initCipher()){
                goToLoginScreen();
            } else {
                if(!myApp.getLocalRegisteredRecord().isEmpty()){
                    showReRegisterRequiredAlertAndStartReRegister();
                } else {
                    goToLoginScreen();
                }
            }
        } catch (FingerprintException e){
            textView.setText(R.string.unknown_error);
            progressBar.clearAnimation();
        }
    }

    private void showReRegisterRequiredAlertAndStartReRegister(){
        new AlertDialog.Builder(context)
                .setTitle(R.string.notification)
                .setMessage(R.string.biometric_changed)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startReRegisterProcess(true);
                    }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
        if(myApp.isReRegistering()){
            startReRegisterProcess();
        }
        */
    }

    private void startReRegisterProcess(boolean isFirstTimeCalled){
        HashSet<String> reRegisteredRequiredAccounts = myApp.getReRegisterRequiredRecord();
        if(isFirstTimeCalled){
            if(reRegisteredRequiredAccounts == null || reRegisteredRequiredAccounts.isEmpty()){
                HashSet<String> emailRegisteredOnThisDevice = myApp.getLocalRegisteredRecord();
                if(emailRegisteredOnThisDevice != null){
                    for(String email: emailRegisteredOnThisDevice){
                        myApp.addEmailToReRegisterRequiredRecord(email);
                    }
                    reRegisteredRequiredAccounts = myApp.getReRegisterRequiredRecord();
                }
            }
        }
        sizeOfReRegisteredRequiredAccounts = reRegisteredRequiredAccounts.size();
        if(reRegisteredRequiredAccounts != null && !reRegisteredRequiredAccounts.isEmpty()){
            textView.setText(String.format(getString(R.string.re_registering_info), reRegisterCounter, sizeOfReRegisteredRequiredAccounts));
            myApp.setIsReRegistering(true);
            setUpHandlerForDeletingAccount();
            myApp.setEmailAddress((String) (reRegisteredRequiredAccounts.toArray())[0]);
            new Connection(context, myApp).new DeleteAUser(handlerForDeletingAccount, myApp.getEmailAddress()).execute();
        } else {
            myApp.setIsReRegistering(false);
            goToLoginScreen();
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

            Log.d(TAG, "Going to init the cipher.");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            //Return true if the cipher has been initialized successfully//
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            Log.d(TAG, "The key has been permanently disabled due to a change in fingerprints.");
            //Return false if cipher initialization failed//
            return false;
        } catch (KeyStoreException | CertificateException
                | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    private void generateOrGetKey() throws FingerprintException {
        try {
            // Obtain a reference to the Keystore using the standard Android keystore container identifier (“AndroidKeystore”)//
            keyStore = KeyStore.getInstance("AndroidKeyStore");

            //Generate the key//
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            //Initialize an empty KeyStore//
            keyStore.load(null);

            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME, null);

            if(key == null){
                Log.d(TAG, "Key does not exist, going to generate a new one.");
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
            }

        } catch (KeyStoreException
                | NoSuchAlgorithmException
                | NoSuchProviderException
                | InvalidAlgorithmParameterException
                | CertificateException
                | IOException exc) {
            exc.printStackTrace();
            throw new FingerprintException(exc);
        } catch (UnrecoverableKeyException e){
            e.printStackTrace();
        }
    }

    private class FingerprintException extends Exception {
        public FingerprintException(Exception e) {
            super(e);
        }
    }

    private void initActivity(){
        textView = findViewById(R.id.activity_launcher_textView);
        progressBar = findViewById(R.id.activity_launcher_progressBar);
    }

    private void goToLoginScreen(){
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        myApp.startInternetConnectionWatchDog();
        finish();
    }

    private void setUpHandlerForDeletingAccount(){
        handlerForDeletingAccount = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if(msg.getData().getBoolean("is_success")){
                    startActivityForResult(new Intent(context, RegisterNewUser.class), RE_REGISTER_REQUEST);
                } else {
                    textView.setText(R.string.unknown_error);
                }
                return false;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);  // Change not tested (03-22-2020)
        Log.d(TAG, "onActivityResult: requestCode: " + requestCode + ", resultCode: " + resultCode);
        if (requestCode == RE_REGISTER_REQUEST) {
            if (resultCode == RESULT_OK) {
                myApp.removeEmailFromReRegisterRequiredRecord(myApp.getEmailAddress());
                startReRegisterProcess(false);
            } else {
                startActivityForResult(new Intent(context, RegisterNewUser.class), RE_REGISTER_REQUEST);
            }
        }
    }
}
