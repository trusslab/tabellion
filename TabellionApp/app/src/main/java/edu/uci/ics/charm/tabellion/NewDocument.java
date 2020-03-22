package edu.uci.ics.charm.tabellion;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;

import br.com.simplepass.loadingbutton.customViews.CircularProgressButton;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

/*
Created Date: 01/28/2019
Created By: Myles Liu
Last Modified: 11/11/2019
Last Modified By: Myles Liu
Notes:

Pending To Do:
    1. Make it able to determine whether the file is a markdown file.
 */

public class NewDocument extends AppCompatActivity {

    private static final String TAG = "NewDocument";

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
    };

    private static final int FILE_SELECT_CODE = 0;
    private static final int CREATE_CONTRACT_CODE = 1;
    CircularProgressButton circularProgressButton;
    private boolean isUploading = false;
    private Context context = this;
    private File contractFile = null;
    private MyApp myApp;
    private String token;
    private Handler handler;
    private boolean renderFinish = false;
    private boolean uploadFinish = false;
    private boolean renderFail = false;
    private Handler handlerForUploadContractFile;

    private ImageView previewImage;
    private EditText contractNameEditText;
    private EditText contractDescription;
    private EditText emailOfPersonToSend;
    private ProgressBar previewImageWaitingProgressBar;
    private TextView previewImageWaitingTextView;
    private Button uploadContractButton;
    private Button createContractButton;
    private TextView textViewBetweenTwoButtons;

    private boolean isContractCreatedByTabellion = false;

    private Connection connection;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_document);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        myApp = (MyApp) getApplication();

        circularProgressButton = findViewById(R.id.new_document_upload_button);
        contractNameEditText = findViewById(R.id.new_document_contract_name);
        uploadContractButton = findViewById(R.id.new_document_upload_contract);
        contractDescription = findViewById(R.id.new_document_contract_description);
        emailOfPersonToSend = findViewById(R.id.new_document_to_who);
        previewImage = findViewById(R.id.new_document_preview_image);
        previewImageWaitingProgressBar = findViewById(R.id.new_document_progressBar);
        previewImageWaitingTextView = findViewById(R.id.new_document_textView);
        createContractButton = findViewById(R.id.new_document_create_contract);
        textViewBetweenTwoButtons = findViewById(R.id.new_document_textView_or_between_contracts);

        setUpHandler();

        circularProgressButton.saveInitialState();
        circularProgressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!renderFail){
                    if(uploadFinish){
                        onBackPressed();
                    }
                }
                if(renderFinish){
                    onBackPressed();
                }
                if(!renderFinish && !isUploading){
                    if(contractNameEditText.getText().toString().isEmpty() ||
                            contractDescription.getText().toString().isEmpty() ||
                            emailOfPersonToSend.getText().toString().isEmpty()){
                        Toast.makeText(context, R.string.please_fill_all_blank, Toast.LENGTH_LONG).show();
                    } else if (contractFile == null){
                        Toast.makeText(context, R.string.please_select_a_file, Toast.LENGTH_LONG).show();
                    } else {
                        lockOrUnlockContractContent(true);
                        circularProgressButton.startAnimation();
                        // Start Uploading
                        token = myApp.getToken();
                        if(token.isEmpty()){
                            Toast.makeText(context, getString(R.string.please_try_again_later,
                                    getString(R.string.fms_token_not_ready)), Toast.LENGTH_LONG).show();
                        } else {
                            connection = new Connection(context, myApp);
                            setUpUploadContractFileHandler();
                            new Thread(connection.new UploadContractFile(contractFile, handlerForUploadContractFile)).start();
                            isUploading = true;
                            renderFail = false;
                        }
                    }
                }
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        uploadContractButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileChooser(v);
            }
        });

        createContractButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(contractNameEditText.getText().toString().isEmpty()){
                    Toast.makeText(context, R.string.please_fill_contract_name, Toast.LENGTH_LONG).show();
                    return;
                }

                Intent intent = new Intent(context, CreateAndEditContract.class);
                intent.putExtra("contract_name", contractNameEditText.getText().toString());
                if(contractFile != null){
                    intent.putExtra("contract_file_path", contractFile.getAbsolutePath());
                }
                startActivityForResult(intent, CREATE_CONTRACT_CODE);
            }
        });

        Connection connection = new Connection(context, myApp);
        token = myApp.getToken();
        if(token.isEmpty()){
            connection.getToken();
        } else {
            Log.d("Myles ", getString(R.string.msg_token_fmt, token));
        }

    }

    private void setUpUploadContractFileHandler(){
        handlerForUploadContractFile = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                if(bundle.getBoolean("is_success")){
                    Log.d(TAG, "setUpUploadContractFileHandler: finish uploading!!!");
                    hideContractContent();
                    showWaitingForRendering();
                    uploadFinish = true;
                    new Thread(connection.new Submit(bundle.getString("contractid"),
                            handler, contractNameEditText.getText().toString(),
                            emailOfPersonToSend.getText().toString(),
                            contractDescription.getText().toString(),
                            isContractCreatedByTabellion)).start();
                    circularProgressButton.revertAnimation(new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            circularProgressButton.setText(R.string.excuse_the_wait);
                            return null;
                        }
                    });
                } else {
                    Log.d(TAG, "setUpUploadContractFileHandler: upload failed!!!");
                    Toast.makeText(context, R.string.upload_fail, Toast.LENGTH_LONG).show();
                    isUploading = false;
                    lockOrUnlockContractContent(false);
                    circularProgressButton.setText(R.string.start_upload_contract);
                }
                return false;
            }
        });
    }

    private void lockOrUnlockContractNameEditText(boolean status){
        contractNameEditText.setFocusable(status);
        contractNameEditText.setClickable(status);
        contractNameEditText.setFocusableInTouchMode(status);
        contractNameEditText.setCursorVisible(status);
    }

    private void lockOrUnlockUploadContractButton(boolean status){
        uploadContractButton.setClickable(status);
    }

    private void lockOrUnlockCreateAndEditContractButton(boolean status){
        createContractButton.setClickable(status);
    }

    private void lockOrUnlockContractDescription(boolean status){
        contractDescription.setFocusable(status);
        contractDescription.setClickable(status);
        contractDescription.setFocusableInTouchMode(status);
        contractDescription.setCursorVisible(status);
    }

    private void displayProgreeDialog(String msg){
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(msg);
        progressDialog.show();
    }

    private void lockOrUnlockEmailOfPersonToSend(boolean status){
        emailOfPersonToSend.setFocusable(status);
        emailOfPersonToSend.setClickable(status);
        emailOfPersonToSend.setFocusableInTouchMode(status);
        emailOfPersonToSend.setCursorVisible(status);
    }

    private void lockOrUnlockContractContent(boolean status){
        // true: lock; false: unlock
        lockOrUnlockContractNameEditText(!status);
        lockOrUnlockUploadContractButton(!status);
        lockOrUnlockCreateAndEditContractButton(!status);
        lockOrUnlockContractDescription(!status);
        lockOrUnlockEmailOfPersonToSend(!status);
    }

    private void hideContractContent(){
        contractNameEditText.setVisibility(View.GONE);
        uploadContractButton.setVisibility(View.GONE);
        createContractButton.setVisibility(View.GONE);
        contractDescription.setVisibility(View.GONE);
        emailOfPersonToSend.setVisibility(View.GONE);
    }

    private void showContractContent(){
        contractNameEditText.setVisibility(View.VISIBLE);
        uploadContractButton.setVisibility(View.VISIBLE);
        createContractButton.setVisibility(View.VISIBLE);
        contractDescription.setVisibility(View.VISIBLE);
        emailOfPersonToSend.setVisibility(View.VISIBLE);
    }

    private void hideWaitingForRendering(){
        previewImageWaitingProgressBar.setVisibility(View.GONE);
        previewImageWaitingTextView.setVisibility(View.GONE);
    }

    private void showWaitingForRendering(){
        previewImageWaitingProgressBar.setVisibility(View.VISIBLE);
        previewImageWaitingTextView.setVisibility(View.VISIBLE);
    }

    private void prepareNewDocument(int count_of_images, String contract_id){
        if(renderFinish){
            Log.d(TAG, "Going to create a new temp_contract and save and back to previous activity.");
            final Contract contract = new Contract(contractNameEditText.getText().toString());
            contract.setOfferorEmailAddress(myApp.getEmailAddress());
            contract.setContractDescription(contractDescription.getText().toString());
            contract.setOffereeEmailAddress(emailOfPersonToSend.getText().toString());
            contract.setContractFile(contractFile);
            contract.addRangePendingToDownloadPagesNum(1, count_of_images);
            contract.setTotalImageNums(count_of_images);
            contract.setContractId(contract_id);
            myApp.addAndSaveNewContractToPendingToSignContractList(contract);
            Handler handler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    Bundle bundle = msg.getData();
                    if(bundle.getBoolean("is_success")){
                        Bitmap bitmap = contract.getImage("1");
                        previewImage.setImageBitmap(bitmap);
                        previewImage.setVisibility(View.VISIBLE);
                        hideWaitingForRendering();
                        contract.setContractPreviewImage(bitmap);
                        circularProgressButton.setText(R.string.finish);
                    }
                    return false;
                }
            });
            contract.downloadFirstPage(new Connection(context, myApp), handler);
        }
    }

    private void setUpHandler(){
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                if(bundle.getBoolean("is_success")) {
                    Log.d(TAG, "setUpHandler: finish rendering!!!");
                    Bitmap confirmBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.confirm);
                    renderFinish = true;
                    isUploading = false;
                    //circularProgressButton.doneLoadingAnimation(R.color.colorFinish, confirmBitmap);
                    prepareNewDocument(bundle.getInt("count_of_images"), bundle.getString("contract_id"));
                } else {
                    Log.d(TAG, "setUpHandler: render failed!!!");
                    isUploading = false;
                    lockOrUnlockContractContent(false);
                    circularProgressButton.setText(R.string.start_upload_contract);
                    Toast.makeText(context, R.string.render_fail, Toast.LENGTH_LONG).show();
                    showContractContent();
                    lockOrUnlockContractContent(false);
                    renderFail = true;
                }
                return false;
            }
        });
    }

    @NonNull
    static String getMimeType(@NonNull File file) {
        String type = null;
        final String url = file.toString();
        final String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        if (type == null) {
            type = "text/markdown"; // fallback type. You might set it to */*
        }
        return type;
    }

    private void grantPermission(){
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

    private void showFileChooser(View view) {
        grantPermission();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult( Intent.createChooser(intent, ""), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Snackbar.make(view, getResources().getText(R.string.no_file_manager_warning).toString(), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();

                    File tempFile = null;
                    String path;
                    if(uri == null){
                        Toast.makeText(context, R.string.illegal_file_path, Toast.LENGTH_LONG).show();
                        return;
                    } else {
                        Log.d(TAG, "File Uri: " + uri.toString());
                        Log.d(TAG, "Is File end with md: " + uri.toString().endsWith("md"));
                        path = FileOperation.getPathFromUri(context, uri);
                        if(path == null) {
                            Toast.makeText(context, R.string.illegal_file_path, Toast.LENGTH_LONG).show();
                        } else {
                            tempFile = new File(path);
                        }
                    }

                    Log.d(TAG, "onActivityResult: See if we and proceed for pdf: " + (tempFile != null) + " and " + (path.endsWith(".pdf")));

                    // Added for case the user selects a pdf file
                    if(tempFile != null && path.endsWith(".pdf")){
                        displayProgreeDialog(getText(R.string.processing_contract_for_extracting_text_from_pdf).toString());
                        Handler handlerForProcessingPdf = new Handler(new Handler.Callback() {
                            @Override
                            public boolean handleMessage(@NonNull Message message) {
                                Bundle data = message.getData();
                                if(data.getBoolean("is_success")){
                                    String rawText = data.getString("contract_raw_text");
                                    //Log.d(TAG, "onActivityResult: Here is the raw text: " + rawText);
                                    int indexOfFirstNewLine = rawText.indexOf("[detailText]:");
                                    String contractTitle = rawText.substring(0, indexOfFirstNewLine);
                                    String contractDetails = rawText.substring(indexOfFirstNewLine + 13);
                                    Intent intent = new Intent(context, CreateAndEditContract.class);
                                    intent.putExtra("contract_title", contractTitle);
                                    intent.putExtra("contract_details", contractDetails);
                                    progressDialog.dismiss();
                                    startActivityForResult(intent, CREATE_CONTRACT_CODE);
                                    return true;
                                }
                                return false;
                            }
                        });
                        new Thread(new Connection(context, myApp).new GetTextFromPdf(tempFile, handlerForProcessingPdf)).start();
                        return;
                    }

                    if(tempFile == null || !getMimeType(tempFile).endsWith("markdown")){
                        Toast.makeText(context, R.string.illegal_file_path, Toast.LENGTH_LONG).show();
                    } else {
                        uploadContractButton.setText(R.string.re_upload_mark_down_file);
                        // The copy operation should be in a separate worker thread so that it won't affect user experience
                        FileOperation.copyFileOrDirectory(tempFile.getAbsolutePath(),
                                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/temp/", "doc.md");
                        contractFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/temp/", "doc.md");

                        textViewBetweenTwoButtons.setVisibility(View.GONE);
                        createContractButton.setVisibility(View.GONE);
                    }
                }
                break;
            case CREATE_CONTRACT_CODE:
                if(resultCode == RESULT_OK){



                    File tempFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                            + "/temp_for_creating_contract/", data.getStringExtra("contract_file_name"));

                    FileOperation.copyFileOrDirectory(tempFile.getAbsolutePath(),
                            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/temp/", "doc.md");
                    contractFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/temp/", "doc.md");

                    createContractButton.setText(R.string.edit_contract);

                    textViewBetweenTwoButtons.setVisibility(View.GONE);
                    uploadContractButton.setVisibility(View.GONE);
                    isContractCreatedByTabellion = true;
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}
