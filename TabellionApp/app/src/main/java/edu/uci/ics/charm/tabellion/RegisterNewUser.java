package edu.uci.ics.charm.tabellion;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricPrompt;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import br.com.simplepass.loadingbutton.customViews.CircularProgressButton;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

/*
Created Date: 02/12/2019
Created By: Myles Liu
Last Modified: 10/14/2019
Last Modified By: Myles Liu
Notes:

 */

public class RegisterNewUser extends AppCompatActivity implements Callback{

    private static final String TAG = "RegisterNewUser";
    private MyApp myApp;
    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText passwordForConfirmation;
    private CircularProgressButton circularProgressButton;
    private Context context = this;
    private ConstraintLayout constraintLayoutInfo;
    private RelativeLayout relativeLayoutFingerprint;
    private TextView textViewFingerprint;
    private ImageView imageViewFingerprint;

    //Permissions Related
    private static final int REQUEST_CAMERA = 1;
    private static String[] PERMISSIONS_CAMERA = {
            Manifest.permission.CAMERA
    };

    private static final String KEY_NAME = "yourKey";
    private Cipher cipher;
    private KeyStore keyStore;
    private KeyGenerator keyGenerator;
    private FingerprintManager.CryptoObject cryptoObject;
    private FingerprintManager fingerprintManager;
    private KeyguardManager keyguardManager;
    private Callback ic;

    static final int REQUEST_TAKE_PHOTO = 1;

    private FragmentActivity fragmentActivity;
    private Executor executor;

    private Handler handlerForRegistering;
    private boolean isRegistered = false;

    private void lockEmailEditText(){
        emailEditText.setFocusable(false);
        emailEditText.setClickable(false);
        emailEditText.setFocusableInTouchMode(false);
        emailEditText.setCursorVisible(false);
    }

    private void lockFirstNameEditText(){
        firstNameEditText.setFocusable(false);
        firstNameEditText.setClickable(false);
        firstNameEditText.setFocusableInTouchMode(false);
        firstNameEditText.setCursorVisible(false);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }

    private void lockLastNameEditText(){
        lastNameEditText.setFocusable(false);
        lastNameEditText.setClickable(false);
        lastNameEditText.setFocusableInTouchMode(false);
        lastNameEditText.setCursorVisible(false);
    }

    private void lockPasswordEditText(){
        passwordEditText.setFocusable(false);
        passwordEditText.setClickable(false);
        passwordEditText.setFocusableInTouchMode(false);
        passwordEditText.setCursorVisible(false);
        passwordForConfirmation.setFocusable(false);
        passwordForConfirmation.setClickable(false);
        passwordForConfirmation.setFocusableInTouchMode(false);
        passwordForConfirmation.setCursorVisible(false);
    }

    private void lockEverything(){
        lockEmailEditText();
        lockFirstNameEditText();
        lockLastNameEditText();
        lockPasswordEditText();
    }

    private void unlockEmailEditText(){
        emailEditText.setFocusable(true);
        emailEditText.setClickable(true);
        emailEditText.setFocusableInTouchMode(true);
        emailEditText.setCursorVisible(true);
    }

    private void unlockFirstNameEditText(){
        firstNameEditText.setFocusable(true);
        firstNameEditText.setClickable(true);
        firstNameEditText.setFocusableInTouchMode(true);
        firstNameEditText.setCursorVisible(true);
    }

    private void unlockLastNameEditText(){
        lastNameEditText.setFocusable(true);
        lastNameEditText.setClickable(true);
        lastNameEditText.setFocusableInTouchMode(true);
        lastNameEditText.setCursorVisible(true);
    }

    private void unlockPasswordEditText(){
        passwordEditText.setFocusable(true);
        passwordEditText.setClickable(true);
        passwordEditText.setFocusableInTouchMode(true);
        passwordEditText.setCursorVisible(true);
        passwordForConfirmation.setFocusable(true);
        passwordForConfirmation.setClickable(true);
        passwordForConfirmation.setFocusableInTouchMode(true);
        passwordForConfirmation.setCursorVisible(true);
    }

    private void unlockEverything(){
        unlockEmailEditText();
        unlockFirstNameEditText();
        unlockLastNameEditText();
        unlockPasswordEditText();
    }

    private void setUpHandlerForRegistering(){
        handlerForRegistering = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                if(bundle.getBoolean("is_success")){
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.finish);
                    circularProgressButton.doneLoadingAnimation(getColor(R.color.colorFinish), bitmap);
                    myApp.setEmailAddress(emailEditText.getText().toString());
                    isRegistered = true;
                    myApp.addEmailToLocalRegisteredRecord(emailEditText.getText().toString());
                    if(myApp.isReRegistering()){
                        if(isRegistered){
                            Log.d(TAG, "onDestroy: Setting result to: " + RESULT_OK);
                            setResult(RESULT_OK);
                        } else {
                            Log.d(TAG, "onDestroy: Setting result to: " + RESULT_CANCELED);
                            setResult(RESULT_CANCELED);
                        }
                    }
                    finish();
                } else {
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_LONG).show();
                    circularProgressButton.revertAnimation(new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            return null;
                        }
                    });
                    relativeLayoutFingerprint.setVisibility(View.GONE);
                    constraintLayoutInfo.setVisibility(View.VISIBLE);
                    unlockEverything();
                }
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAuthenticated() {
        textViewFingerprint.setText(R.string.registering);
        //imageViewFingerprint.setImageResource(R.drawable.fingerprint_ok);
        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                if(bundle.getBoolean("is_success")){
                    myApp.setCurrentUserFirstName(firstNameEditText.getText().toString());

                    setUpHandlerForRegistering();

                    myApp.setEmailAddress(emailEditText.getText().toString());

                    Connection connection = new Connection(context, myApp);
                    new Thread(connection.new RegisterNewUser(handlerForRegistering, firstNameEditText.getText().toString(),
                            lastNameEditText.getText().toString(), emailEditText.getText().toString(),
                            passwordEditText.getText().toString())).start();

                } else {
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_LONG).show();
                    circularProgressButton.revertAnimation(new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            return null;
                        }
                    });
                    relativeLayoutFingerprint.setVisibility(View.GONE);
                    constraintLayoutInfo.setVisibility(View.VISIBLE);
                }
                return false;
            }
        });

        Connection connection = new Connection(context, myApp);
        new Thread(connection.new UploadPublicKey(handler)).start();

    }

    private void grantCameraPermissionAndDispatchPhotoTaking(){
        int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_CAMERA,
                    REQUEST_CAMERA
            );
        }

        dispatchTakePictureIntent();    // Going to let the user take a photo and back.
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_new_user);
        Toolbar toolbar = findViewById(R.id.register_new_user_toolbar);
        setSupportActionBar(toolbar);

        myApp = (MyApp) getApplication();

        String token = myApp.getToken();
        if(token.isEmpty()){
            new Connection(context, myApp).getToken();
        } else {
            Log.d("Myles ", getString(R.string.msg_token_fmt, token));
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        firstNameEditText = findViewById(R.id.register_new_user_first_name);
        lastNameEditText = findViewById(R.id.register_new_user_last_name);
        emailEditText = findViewById(R.id.register_new_user_email);
        passwordEditText = findViewById(R.id.register_new_user_password);
        passwordForConfirmation = findViewById(R.id.register_new_user_password_for_confirmation);
        circularProgressButton = findViewById(R.id.register_new_user_confirm_button);

        constraintLayoutInfo = findViewById(R.id.register_new_user_info_container);
        relativeLayoutFingerprint = findViewById(R.id.register_new_user_fingerprint_container);
        textViewFingerprint = findViewById(R.id.register_new_user_fingerprint_textView);
        imageViewFingerprint = findViewById(R.id.register_new_user_fingerprint_imageView);

        circularProgressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lockEverything();
                circularProgressButton.startAnimation();
                if(checkIfAnyBlank()){
                    Toast.makeText(context, R.string.please_fill_all_blank, Toast.LENGTH_LONG).show();
                    circularProgressButton.revertAnimation(new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            return null;
                        }
                    });
                    unlockEverything();
                } else if (!checkIfTwoPasswordsEqual()){
                    Toast.makeText(context, R.string.two_passwords_not_equal, Toast.LENGTH_LONG).show();
                    circularProgressButton.revertAnimation(new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            return null;
                        }
                    });
                    unlockEverything();
                } else {
                    grantCameraPermissionAndDispatchPhotoTaking();
                }
            }
        });

        String tempEmailAddress = myApp.getEmailAddress();
        if(tempEmailAddress != null && !tempEmailAddress.isEmpty()){
            emailEditText.setText(tempEmailAddress);
        }

        if(myApp.isReRegistering()){
            setTitle(R.string.re_registering_label);
            lockEmailEditText();  // Disable editing email address in re-registering mode
        }

        ic = this;
        fingerprint_init();

        executor = Executors.newSingleThreadExecutor();
        fragmentActivity = this;

        // Should not init cipher here
        /*
        if (initCipher()) {
            Log.d(TAG, "Cipher has been inited successfully (first try).");
            //If the cipher is initialized successfully, then create a CryptoObject instance//
            cryptoObject = new FingerprintManager.CryptoObject(cipher);

            // Here, I’m referencing the FingerprintHandler class that we’ll create in the next section. This class will be responsible
            // for starting the authentication process (via the startAuth method) and processing the authentication process events//
            FingerprintHandler helper = new FingerprintHandler(this, this);
            helper.startAuth(fingerprintManager, cryptoObject);
        }
        */

    }

    private boolean checkIfAnyBlank(){
        return firstNameEditText.getText().toString().isEmpty() ||
                lastNameEditText.getText().toString().isEmpty() ||
                emailEditText.getText().toString().isEmpty() ||
                passwordEditText.getText().toString().isEmpty() ||
                passwordForConfirmation.getText().toString().isEmpty();
    }

    private boolean checkIfTwoPasswordsEqual(){
        return passwordEditText.getText().toString()
                .equals(passwordForConfirmation.getText().toString());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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
                Toast.makeText(context, "No fingerprint configured. Please register at least one " +
                        "fingerprint in your device's Settings", Toast.LENGTH_LONG).show();
            }

            //Check that the lockscreen is secured//
            if (!keyguardManager.isKeyguardSecure()) {
                // If the user hasn’t secured their lockscreen with a PIN password or pattern, then display the following text//
                Toast.makeText(context, "Please enable lockscreen security in your device's Settings", Toast.LENGTH_LONG).show();
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

    private class FingerprintException extends Exception {
        public FingerprintException(Exception e) {
            super(e);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                myApp.setEmailAddress(emailEditText.getText().toString());
                photoFile = myApp.createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "edu.uci.ics.charm.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void performAuthentication(){
        Log.d(TAG, "performAuthentication: Going to perform Biometric Authentication.");
        onStateNotSaved();
        Log.d(TAG, "performAuthentication: Creating new BiometricPrompt.");
        BiometricPrompt biometricPrompt =
                new BiometricPrompt((FragmentActivity) context, Executors.newSingleThreadExecutor(),
                        new BiometricPrompt.AuthenticationCallback() {
                            @Override
                            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                                super.onAuthenticationSucceeded(result);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        onAuthenticated();
                                    }
                                });
                            }

                            @Override
                            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                                super.onAuthenticationError(errorCode, errString);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, getString(R.string.registration_failed), Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                });
                                /*
                                if(errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON){
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            circularProgressButton.revertAnimation(new Function0<Unit>() {
                                                @Override
                                                public Unit invoke() {
                                                    return null;
                                                }
                                            });
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            new AlertDialog.Builder(context)
                                                    .setTitle(R.string.notification)
                                                    .setCancelable(false)
                                                    .setMessage(getString(R.string.unknown_error_for_biometric))
                                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            Log.d(TAG, "onAuthenticationHelp: OK for Error has been clicked!");
                                                            if(initCipher()){
                                                                cryptoObject = new FingerprintManager.CryptoObject(cipher);
                                                                FingerprintHandler helper = new FingerprintHandler(context, ic);
                                                                Toast.makeText(context, getString(R.string.put_finger_on_sensor), Toast.LENGTH_LONG).show();
                                                                helper.startAuth(fingerprintManager, cryptoObject);
                                                            }
                                                        }
                                                    }).show();
                                        }
                                    });
                                }
                                */
                            }

                            @Override
                            public void onAuthenticationFailed() {
                                super.onAuthenticationFailed();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, getString(R.string.failed), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });
        Log.d(TAG, "performAuthentication: Creating new PromptInfo.");
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_title_register))
                .setSubtitle(getString(R.string.biometric_subtitle_register))
                .setDescription(getString(R.string.biometric_description_register))
                .setNegativeButtonText(getString(R.string.cancel))
                .build();
        Log.d(TAG, "performAuthentication: Creating new CryptoObject.");
        BiometricPrompt.CryptoObject cryptoObject = new BiometricPrompt.CryptoObject(cipher);
        Log.d(TAG, "performAuthentication: Calling authenticate.");
        biometricPrompt.authenticate(promptInfo, cryptoObject);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            if (initCipher()) {
                Log.d(TAG, "Cipher has been inited successfully (second try).");

                constraintLayoutInfo.setVisibility(View.GONE);
                relativeLayoutFingerprint.setVisibility(View.VISIBLE);
                imageViewFingerprint.setImageBitmap(myApp.getCurrentUserPhoto());
                // Here, I’m referencing the FingerprintHandler class that we’ll create in the next section. This class will be responsible
                // for starting the authentication process (via the startAuth method) and processing the authentication process events//

                try {
                    performAuthentication();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                /*
                Log.d(TAG, " onActivityResult: CurrentAPI is: " + android.os.Build.VERSION.SDK_INT);
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    // Have a new biometric authentication interface for Android Pie
                    final CancellationSignal cancellationSignal = new CancellationSignal();
                    BiometricPrompt biometricPrompt = new BiometricPrompt.Builder(context)
                            .setTitle(getString(R.string.biometric_title_register))
                            .setSubtitle(getString(R.string.biometric_subtitle_register))
                            .setDescription(getString(R.string.biometric_description_register))
                            .setNegativeButton(getString(R.string.cancel), getMainExecutor(), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    cancellationSignal.cancel();
                                    circularProgressButton.revertAnimation(new Function0<Unit>() {
                                        @Override
                                        public Unit invoke() {
                                            return null;
                                        }
                                    });
                                }
                            })
                            .build();
                    BiometricPrompt.CryptoObject cryptoObjectForPie = new BiometricPrompt.CryptoObject(cipher);
                    BiometricPrompt.PromptInfo
                    biometricPrompt.authenticate(cryptoObjectForPie, cancellationSignal, getMainExecutor(), new onAuthenticatedForPie());
                } else {
                    // Should have a UI here for fingerprint identification
                    //If the cipher is initialized successfully, then create a CryptoObject instance//
                    cryptoObject = new FingerprintManager.CryptoObject(cipher);
                    FingerprintHandler helper = new FingerprintHandler(context, ic);
                    Toast.makeText(context, getString(R.string.put_finger_on_sensor), Toast.LENGTH_LONG).show();
                    helper.startAuth(fingerprintManager, cryptoObject);
                }
                */
            }
        }
    }

}
