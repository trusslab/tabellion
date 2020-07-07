package edu.uci.ics.charm.tabellion;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/*
Created Date: 11/27/2019
Created By: Myles Liu
Last Modified: 03/22/2020
Last Modified By: Myles Liu
Notes:
 */

public class CommentActivity extends AppCompatActivity implements Callback {

    private static final String TAG = "CommentActivity";

    private Context context = this;

    private MyApp myApp;

    // Views
    private TextView originalTextView;
    private ImageView originalImageView;
    private TextView commentTextView;

    // Internal Data
    private Bitmap originalBitmap;
    private String originalString;
    private String commentString;

    // For Biometric Authentication
    private static final String KEY_NAME = "yourKey";
    private Cipher cipher;
    private KeyStore keyStore;
    private KeyGenerator keyGenerator;
    private FingerprintManager.CryptoObject cryptoObject;
    private FingerprintManager fingerprintManager;
    private KeyguardManager keyguardManager;
    private boolean waitingForAuthentication = false;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        myApp = (MyApp) getApplication();

        originalTextView = (TextView) findViewById(R.id.activity_comment_text_be_commented_textView);
        originalImageView = (ImageView) findViewById(R.id.activity_comment_image_be_commented_imageView);
        commentTextView = (TextView) findViewById(R.id.activity_comment_comment_textView);

        Intent intent = getIntent();
        originalString = intent.getStringExtra("original_string");
        Gson gson = new Gson();
        originalBitmap = (Bitmap)gson.fromJson(intent.getStringExtra("original_bitmap"),
                new TypeToken<Bitmap>() {}.getType());
        commentString = intent.getStringExtra("comment_string");

        // Set Original Text if possible
        if(originalString != null && !originalString.isEmpty()){
            originalTextView.setText(originalString);
        } else {
            originalTextView.setText(myApp.getCurrentViewingContract().getContractName());
        }

        // Set Original Bitmap if possible
        if(originalBitmap != null){
            originalImageView.setImageBitmap(originalBitmap);
        } else {
            originalImageView.setVisibility(View.GONE);
        }

        // Set Comment
        if(commentString != null && !commentString.isEmpty()){
            commentTextView.setText(commentString);
        } else {
            commentTextView.setText("N/A");
        }

        fingerprint_init();
        tryStartFingerprintAuthentication();
        hideSystemUI();

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
                commentTextView.setText("Your device doesn't support fingerprint authentication");
            }
            //Check whether the user has granted your app the USE_FINGERPRINT permission//
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                // If your app doesn't have this permission, then display the following text//
                commentTextView.setText("Please enable the fingerprint permission");
            }

            //Check that the user has registered at least one fingerprint//
            if (!fingerprintManager.hasEnrolledFingerprints()) {
                // If the user hasn’t configured any fingerprints, then display the following message//
                commentTextView.setText("No fingerprint configured. Please register at least one fingerprint in your device's Settings");
            }

            //Check that the lockscreen is secured//
            if (!keyguardManager.isKeyguardSecure()) {
                // If the user hasn’t secured their lockscreen with a PIN password or pattern, then display the following text//
                commentTextView.setText("Please enable lockscreen security in your device's Settings");
            } else {
                try {
                    generateKey();
                } catch (CommentActivity.FingerprintException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void generateKey() throws CommentActivity.FingerprintException {
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
            throw new CommentActivity.FingerprintException(exc);
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

    private class FingerprintException extends Exception {
        public FingerprintException(Exception e) {
            super(e);
        }
    }

    public String storeScreenshot(Bitmap bitmap) {

        //page_counter++;
        String screenshotPath = Screenshot.storeScreenshot(bitmap, "revision_" + myApp.getCurrentViewingContract().getRevistedNumCount(), myApp.getCurrentViewingContract());
        return screenshotPath;
    }

    public String signScreenshot(Bitmap bitmap){
        return myApp.storeScreenshotSign(bitmap, "signature_for_revision_" + myApp.getCurrentViewingContract().getRevistedNumCount());
    }

    private void showProgressDialog(){
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(getText(R.string.syncing_with_server));
        progressDialog.show();
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

    @Override
    public void onAuthenticated() {
        waitingForAuthentication = false;

        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Intent intent = new Intent();
                if(msg.getData().getBoolean("is_success")){
                    intent.putExtra("is_success", true);
                } else {
                    Toast.makeText(context, R.string.cannot_send_comment, Toast.LENGTH_LONG).show();
                    intent.putExtra("is_success", false);
                }
                progressDialog.dismiss();
                Log.d(TAG, "Going to set result to: " + RESULT_OK);
                setResult(RESULT_OK, intent);
                finish();
                return false;
            }
        });


        Bitmap bitmap = Screenshot.takescreenshotOfRootView(commentTextView);

        File screenshot = new File(storeScreenshot(bitmap));

        File signature = new File(signScreenshot(bitmap));

        new Thread(new Connection(context, myApp).new SubmitComment(
                handler, myApp.getCurrentViewingContract().getContractId(),
                commentTextView.getText().toString(), screenshot, signature)).start();
        showProgressDialog();
    }
}
