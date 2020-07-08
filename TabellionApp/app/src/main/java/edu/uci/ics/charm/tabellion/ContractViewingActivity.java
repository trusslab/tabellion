package edu.uci.ics.charm.tabellion;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.TimingLogger;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import br.com.simplepass.loadingbutton.customViews.CircularProgressButton;
import de.hdodenhof.circleimageview.CircleImageView;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

/*
Created Date: 01/31/2019
Created By: Myles Liu
Last Modified: 05/01/2020
Last Modified By: Myles Liu
Notes:

 */

public class ContractViewingActivity extends AppCompatActivity {

    private final static String TAG = "ContractViewingActivity";
    private Context context = this;
    private MyApp myApp;
    private TextView contractTitle;
    private ImageView contractPreviewImage;
    private TextView contractDescription;
    private TextView contractOfferorEmail;
    private TextView contractOffereeEmail;
    private CircularProgressButton circularProgressButton;
    private Contract viewingContract;
    private ProgressBar previewImageProgressBar;
    private Button revokeOrReject;

    private Handler handler;
    private TextView textView;
    private CircleImageView imageView;
    private ProgressBar progressBar;
    private AlertDialog alertDialog;
    private Handler handlerForUpdatingContractConfirmingStatus;
    private ProgressDialog progressDialog;

    private TimingLogger timingLogger;
    private StringBuilder logBuilder;
    private Time timeStampForStartDownloading;

    private int CREATEANDEDITCODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, " onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contract_viewing);

        myApp = (MyApp) getApplication();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        contractTitle = findViewById(R.id.contract_viewing_contract_title);
        contractPreviewImage = findViewById(R.id.contract_viewing_contract_preview_image);
        contractDescription = findViewById(R.id.contract_viewing_contract_description);
        contractOfferorEmail = findViewById(R.id.contract_viewing_contract_offeror_email);
        contractOffereeEmail = findViewById(R.id.contract_viewing_contract_offeree_email);
        circularProgressButton = findViewById(R.id.contract_viewing_download_or_sign);
        previewImageProgressBar = findViewById(R.id.contract_viewing_contract_preview_image_progressBar);
        revokeOrReject = findViewById(R.id.contract_viewing_revoke_or_reject);

        viewingContract = myApp.getCurrentViewingContract();

        contractTitle.setText(viewingContract.getContractName());

        Log.d(TAG, " onCreate: half");
        Bitmap previewImage = viewingContract.getImage("1");
        if(previewImage == null){
            Handler handler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    Bundle bundle = msg.getData();
                    if(bundle.getBoolean("is_success")){
                        Bitmap bitmap = myApp.getCurrentViewingContract().getImage("1");
                        myApp.getCurrentViewingContract().setContractPreviewImage(bitmap);
                        contractPreviewImage.setImageBitmap(myApp.getCurrentViewingContract().getContractPreviewImage());
                        viewingContract = myApp.getCurrentViewingContract();
                        previewImageProgressBar.setVisibility(View.GONE);
                    }
                    return false;
                }
            });
            myApp.getCurrentViewingContract().downloadFirstPage(new Connection(context, myApp), handler);
        } else {
            previewImageProgressBar.setVisibility(View.GONE);
        }
        contractPreviewImage.setImageBitmap(previewImage);

        contractDescription.setText(viewingContract.getContractDescription());
        contractOfferorEmail.setText(String.format(getString(R.string.from_offeror_email), viewingContract.getOfferorEmailAddress()));
        contractOffereeEmail.setText(String.format(getString(R.string.to_offeree_email), viewingContract.getOffereeEmailAddress()));

        final Connection connection = new Connection(context, myApp);

        Integer[] revokeVisableStatus = {0, 1, 2, 10};
        if(Arrays.asList(revokeVisableStatus).contains(viewingContract.getContractStatus()) && viewingContract.getCurrentRole() == 0){
            revokeOrReject.setVisibility(View.VISIBLE);
        }

        if(myApp.getContractTrueStatus(viewingContract) != 0){
            circularProgressButton.setVisibility(View.GONE);
        }

        if(viewingContract.getCurrentRole() == 1 && viewingContract.getConfirmStatus() != 2){
            circularProgressButton.setVisibility(View.GONE);
        }

        if(viewingContract.isDownloaded()){
            circularProgressButton.setText(R.string.start_signing);
        }

        Log.d(TAG, "onCreate: The current viewing contract status is: " + viewingContract.getContractStatus());

        // Check if the contract is already properly finished; if so, allow user to view the contract
        if(viewingContract.getContractStatus() == 7){
            circularProgressButton.setVisibility(View.VISIBLE);
            circularProgressButton.setText(R.string.review_contract);

            contractOfferorEmail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!viewingContract.isSignedFullContractDownloaded()){
                        Toast.makeText(context, R.string.full_contract_is_not_downloaded, Toast.LENGTH_LONG).show();
                    } else {
                        startConfirmationPage(2);
                    }
                }
            });
            contractOfferorEmail.setTextColor(getColor(R.color.linkableTextViewColor));

            contractOffereeEmail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!viewingContract.isSignedFullContractDownloaded()){
                        Toast.makeText(context, R.string.full_contract_is_not_downloaded, Toast.LENGTH_LONG).show();
                    } else {
                        startConfirmationPage(3);
                    }
                }
            });
            contractOffereeEmail.setTextColor(getColor(R.color.linkableTextViewColor));
        }

        if(viewingContract.getContractStatus() == 5){
            circularProgressButton.setText(R.string.start_edit);
            showProgressDialog(getString(R.string.talking_with_server));
            final Handler downloadMarkdownHandler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    progressDialog.dismiss();
                    return false;
                }
            });

            Handler getCommentHandler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    Bundle data = msg.getData();
                    if(data.getBoolean("is_success")){
                        contractDescription.setText(String.format(getString(R.string.comment), data.getString("comment")));
                        new Thread(new Connection(context, myApp).new DownloadFile("doc.md",
                                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                                myApp.getCurrentViewingContract().getContractId(),
                                "submitted_files/" + viewingContract.getContractId() + "/",
                                downloadMarkdownHandler)).start();
                    } else {
                        Toast.makeText(context, R.string.cannot_read_comment, Toast.LENGTH_LONG).show();
                        finish();
                    }
                    return false;
                }
            });
            new Thread(new Connection(context, myApp).new GetComment(getCommentHandler, viewingContract.getContractId())).start();
        }

        final Handler contractDownloadWatchDogHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if(msg.getData().getBoolean("is_success")){
                    timingLogger.addSplit("Download Complete");
                    String stringBuilder = "Contract id: " +
                            viewingContract.getContractId() +
                            "\n" +
                            "Contract name: " +
                            viewingContract.getContractName() +
                            "\n" +
                            "Download Time: " +
                            (System.currentTimeMillis() - timeStampForStartDownloading.getTime()) +
                            "\n" +
                            "========================================================" +
                            "\n";
                    myApp.writeLogToFile(stringBuilder, viewingContract.getContractId()
                            + "_download_log.txt", "downloading");
                    timingLogger.dumpToLog();
                    circularProgressButton.revertAnimation(new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            circularProgressButton.setText(R.string.start_signing);
                            return null;
                        }
                    });
                    viewingContract.setDownloaded(true);
                    Log.d(TAG, "contractDownloadWatchDogHandler: " + "download success.");
                    myApp.updateSavedPendingToSignContractList(viewingContract);
                } else {
                    Log.d(TAG, "contractDownloadWatchDogHandler: " + "download failed.");
                    circularProgressButton.revertAnimation(new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            return null;
                        }
                    });
                    Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_LONG).show();
                }
                return false;
            }
        });

        revokeOrReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(viewingContract.getCurrentRole() == 0){
                    viewingContract.setLastActionTimeInterval(myApp.getCurrentTimeInterval());
                    myApp.addOrUpdateToCorrespondingContractList(viewingContract);
                    new Thread(new Connection(context, myApp).new SendRevokeRequest(viewingContract)).start();
                    Toast.makeText(context, R.string.revoke_request_sent, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });

        circularProgressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // First check if contract has revision required for offeror
                if(viewingContract.getContractStatus() == 5){
                    Intent intent = new Intent(context, CreateAndEditContract.class);
                    intent.putExtra("contract_name", viewingContract.getContractName());
                    intent.putExtra("contract_file_path", new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                            myApp.getCurrentViewingContract().getContractId() + "/doc.md").getAbsolutePath());
                    startActivityForResult(intent, CREATEANDEDITCODE);
                    return;
                }

                if(viewingContract.isDownloaded()){
                    // Check if the contract is already properly finished; if so, allow user to view the contract
                    if(viewingContract.getContractStatus() == 7){
                        // Toast.makeText(context, "Going to review contract...", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(context, ContractPostViewerActivity.class));
                    } else {
                        // Start Signing
                        startActivity(new Intent(context, SigningActivity.class));
                        finish();
                    }
                } else {
                    circularProgressButton.startAnimation();
                    Handler handler = new Handler(new Handler.Callback() {
                        @Override
                        public boolean handleMessage(Message msg) {
                            new DownloadWatchDog(contractDownloadWatchDogHandler, viewingContract).execute();
                            return false;
                        }
                    });
                    List<Integer> pendingToDownloadPages = new ArrayList<>(viewingContract.getPendingToDownloadPagesNum());
                    timingLogger = new TimingLogger("MyTag", "downloadingContract");
                    timeStampForStartDownloading = new Time(System.currentTimeMillis());
                    Collections.sort(pendingToDownloadPages);
                    viewingContract.downloadPages(pendingToDownloadPages.get(0).toString(),
                            pendingToDownloadPages.get(pendingToDownloadPages.size() - 1).toString(),
                            connection, handler);
                }
            }
        });

        if(viewingContract.getCurrentRole() == 0 && viewingContract.getConfirmStatus() == 1){
            startConfirmationPage(1);
        }

        // Going to automatically start downloading temp_contract once the user open this activity
        if(circularProgressButton.getVisibility() == View.VISIBLE && !viewingContract.isDownloaded()){
            circularProgressButton.saveInitialState();  // Workaround for this library's bug
            new Thread(new Runnable() {
                @Override
                public void run() {
                    float downloadProgress = viewingContract.getDownloadProgress();
                    while(downloadProgress != 1.0){
                        final float currentDownloadProgress = viewingContract.getDownloadProgress();
                        if(downloadProgress != currentDownloadProgress){
                            downloadProgress = currentDownloadProgress;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    circularProgressButton.setProgress(currentDownloadProgress * 100);
                                }
                            });
                        }
                    }
                }
            }).start();
            circularProgressButton.callOnClick();
        }
        // End of automatically start downloading temp_contract once the user open this activity


        Log.d(TAG, " onCreate: finish");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(myApp.getCurrentViewingContract().getContractStatus() == 7){
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.contract_viewing_activity_option_menu, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.contract_viewing_activity_option_menu_download_full_contract:
                Handler handlerForDownloadFullContract = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(@NonNull Message message) {
                        if(message.getData().getBoolean("is_success")){
                            Toast.makeText(context, R.string.download_full_contract_success, Toast.LENGTH_LONG).show();
                            progressDialog.dismiss();
                            viewingContract.setSignedFullContractDownloaded(true);
                            myApp.addOrUpdateToCorrespondingContractList(viewingContract);
                            return true;
                        }
                        Toast.makeText(context, R.string.download_full_contract_failed, Toast.LENGTH_LONG).show();
                        return false;
                    }
                });
                new Thread(new Connection(context, myApp).new DownloadFile(
                        "final_contract_" + viewingContract.getContractId() + ".tar",
                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                        myApp.getCurrentViewingContract().getContractId(),
                        "submitted_files/" + viewingContract.getContractId() + "/",
                        handlerForDownloadFullContract)).start();
                showProgressDialog(getString(R.string.downloading_full_contract));
                return true;
            case R.id.contract_viewing_activity_option_menu_verify_contract:
                boolean verificationResult = true;
                if(!viewingContract.isSignedFullContractDownloaded()){
                    Toast.makeText(context, R.string.full_contract_is_not_downloaded, Toast.LENGTH_LONG).show();
                } else {
                    showProgressDialog(getString(R.string.verifying_contract));
                    try {
                        // Untar the final_contract
                        FileOperation.untar(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                                myApp.getCurrentViewingContract().getContractId() + "/final_contract_" + viewingContract.getContractId() + ".tar",
                                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                                        myApp.getCurrentViewingContract().getContractId() + "/final_contract");
                        /*
                        // Read offeror's public key
                        String offeror_pubKey_str = myApp.getStringFromFile(
                                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                                        myApp.getCurrentViewingContract().getContractId() +
                                        "/final_contract/offeror_info/user_public_key.pem"
                        );
                        offeror_pubKey_str = offeror_pubKey_str.replace("-----BEGIN PUBLIC KEY-----\n", "");
                        offeror_pubKey_str = offeror_pubKey_str.replace("-----END PUBLIC KEY-----\n", "");

                        Log.d(TAG, "onOptionsItemSelected: offeror_pubKey_str: " + offeror_pubKey_str);
                        PublicKey offeror_pubKey = myApp.getPublicKeyFromString(offeror_pubKey_str);
                        // Log.d(TAG, "onOptionsItemSelected: offeror_pubKey: " + offeror_pubKey.toString());

                        // Read Offeree's public key
                        String offeree_pubKey_str = myApp.getStringFromFile(
                                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                                        myApp.getCurrentViewingContract().getContractId() +
                                        "/final_contract/offeree_info/user_public_key.pem"
                        );
                        offeree_pubKey_str = offeree_pubKey_str.replace("-----BEGIN PUBLIC KEY-----\n", "");
                        offeree_pubKey_str = offeree_pubKey_str.replace("-----END PUBLIC KEY-----\n", "");

                        Log.d(TAG, "onOptionsItemSelected: offeree_pubKey_str: " + offeree_pubKey_str);
                        PublicKey offeree_pubKey = myApp.getPublicKeyFromString(offeree_pubKey_str);
                         */
                        for(int i = 1; i <= viewingContract.getTotalImageNums(); ++i){
                            // Verify offeror
                            boolean offerorVerificationResult = myApp.verifySignatureWithPublicKey(
                                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                                            myApp.getCurrentViewingContract().getContractId() +
                                            "/final_contract/offeror_info/user_public_key.pem",
                                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                                            myApp.getCurrentViewingContract().getContractId() +
                                            "/final_contract/offeror/signature" + i,
                                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                                            myApp.getCurrentViewingContract().getContractId() +
                                            "/final_contract/offeror/screenshot" + i
                            );
                            Log.d(TAG, "onOptionsItemSelected: offeror verifcation result-" + i + ": " + offerorVerificationResult);
                            if (!offerorVerificationResult){
                                verificationResult = false;
                                Toast.makeText(context, R.string.verify_contract_failure, Toast.LENGTH_LONG).show();
                                break;
                            }
                            // Verify offeree
                            boolean offereeVerificationResult = myApp.verifySignatureWithPublicKey(
                                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                                            myApp.getCurrentViewingContract().getContractId() +
                                            "/final_contract/offeree_info/user_public_key.pem",
                                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                                            myApp.getCurrentViewingContract().getContractId() +
                                            "/final_contract/offeree/signature" + i,
                                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                                            myApp.getCurrentViewingContract().getContractId() +
                                            "/final_contract/offeree/screenshot" + i
                            );
                            Log.d(TAG, "onOptionsItemSelected: offeree verifcation result-" + i + ": " + offereeVerificationResult);
                            if (!offereeVerificationResult){
                                verificationResult = false;
                                Toast.makeText(context, R.string.verify_contract_failure, Toast.LENGTH_LONG).show();
                                break;
                            }
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                        verificationResult = false;
                        Toast.makeText(context, R.string.verify_contract_failure, Toast.LENGTH_LONG).show();
                    }
                    progressDialog.dismiss();
                    if(verificationResult){
                        Toast.makeText(context, R.string.verify_contract_success, Toast.LENGTH_LONG).show();
                    }
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showProgressDialog(String msg){
        progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(msg);
        progressDialog.show();
    }

    private void startConfirmationPage(final int mode){
        // mode: 1 is for normal confirmation; 2 is for a user to review the offeror user_photo;
        // 3 is for a user to review the offeree user_photo
        alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(context.getString(R.string.notification));
        LayoutInflater factory = LayoutInflater.from(context);
        final View view = factory.inflate(R.layout.dialog_for_confirming_contract, null);
        imageView = view.findViewById(R.id.dialog_for_confirming_contract_user_photo);
        progressBar = view.findViewById(R.id.dialog_for_confirming_contract_progress_bar);
        textView = view.findViewById(R.id.dialog_for_confirming_text_content);
        textView.setText(R.string.loading);
        alertDialog.setView(view);
        alertDialog.setCancelable(false);
        if(mode == 1){
            setUpHandlerForUpdatingContractConfirmingStatus();
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
        }

        if(mode == 2){
            alertDialog.setTitle(context.getString(R.string.user_photo));
            progressBar.setVisibility(View.GONE);
            textView.setText(String.format(getString(R.string.from_offeror_email), viewingContract.getOfferorEmailAddress()));
            imageView.setImageBitmap(myApp.getImageFromLocal(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                    myApp.getCurrentViewingContract().getContractId() +
                    "/final_contract/offeror_info/user_photo.jpg"));
            imageView.setVisibility(View.VISIBLE);
        } else if (mode == 3){
            alertDialog.setTitle(context.getString(R.string.user_photo));
            progressBar.setVisibility(View.GONE);
            textView.setText(String.format(getString(R.string.to_offeree_email), viewingContract.getOffereeEmailAddress()));
            imageView.setImageBitmap(myApp.getImageFromLocal(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                    myApp.getCurrentViewingContract().getContractId() +
                    "/final_contract/offeree_info/user_photo.jpg"));
            imageView.setVisibility(View.VISIBLE);
        }

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.confirm),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if(mode == 1){
                            showProgressDialog(getString(R.string.talking_with_server));
                            new Thread(new Connection(context, myApp).new UpdateContractConfirmStatus(
                                    handlerForUpdatingContractConfirmingStatus,
                                    myApp.getCurrentViewingContract().getContractId(),
                                    2)).start();
                        }
                    }
                });

        alertDialog.show();
        if(mode == 1){
            setUpHandlerForUserPhotoViewing();
            new Thread(new Connection(context, myApp).new DownloadFile(
                    myApp.getUserPhotoFileNameByEmail(myApp.getCurrentViewingContract().getOffereeEmailAddress()),
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                            myApp.getCurrentViewingContract().getContractId(),
                    "users/user_photos/", handler)).start();
        }
    }

    private void setUpHandlerForUpdatingContractConfirmingStatus(){
        handlerForUpdatingContractConfirmingStatus = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if(!msg.getData().getBoolean("is_success")){
                    Toast.makeText(context, context.getString(R.string.check_connection), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_LONG).show();
                }
                progressDialog.dismiss();
                return false;
            }
        });
    }

    private void setUpHandlerForUserPhotoViewing(){
        // This is used when offeror need to confirm to receive temp_contract from a offeree (Should not be directly called)
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if(msg.getData().getBoolean("is_success")){
                    textView.setText(R.string.confirm_sending_contract_to_offeree);
                    imageView.setImageBitmap(myApp.getContractIDOppositeUserPhotoLocal(
                            myApp.getCurrentViewingContract().getContractId(),
                            myApp.getCurrentViewingContract().getOffereeEmailAddress()));
                    progressBar.setVisibility(View.GONE);
                    imageView.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(context, context.getString(R.string.check_connection), Toast.LENGTH_LONG).show();
                }
                return false;
            }
        });
    }

    public class DownloadWatchDog extends AsyncTask<String, Void, String> {

        private Handler handler;
        private Contract contract;

        public DownloadWatchDog(Handler handler, Contract contract){
            this.handler = handler;
            this.contract = contract;
        }

        @Override
        protected String doInBackground(String... params) {
            Bundle bundle = new Bundle();
            while(!contract.getPendingToDownloadPagesNum().isEmpty()){
                if(contract.isDownloadFailed()){
                    bundle.putBoolean("is_success", false);
                    Message message = new Message();
                    message.setData(bundle);
                    handler.sendMessage(message);
                    return "";
                }
            }
            bundle.putBoolean("is_success", true);
            Message message = new Message();
            message.setData(bundle);
            handler.sendMessage(message);
            return "";
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CREATEANDEDITCODE && resultCode == RESULT_OK && data != null){
            Log.d(TAG, "onActivityResult: the path of edited contract is: " + data.getStringExtra("contract_file_path"));
            showProgressDialog(getString(R.string.talking_with_server));
            final Handler handlerForSubmitRevision = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    if(msg.getData().getBoolean("is_success")){
                        progressDialog.dismiss();
                        Toast.makeText(context, R.string.revised_contract_submitted, Toast.LENGTH_LONG).show();
                        //viewingContract.setIsDownloaded(false);
                        //myApp.addOrUpdateToCorrespondingContractList(viewingContract);
                        finish();
                        return true;
                    } else {
                        Toast.makeText(context, R.string.revised_contract_submit_failed, Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
            });

            Handler handlerForUploadingRevisedContractFile = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    if(msg.getData().getBoolean("is_success")){
                        new Thread(new Connection(context, myApp).new SubmitRevision(
                                viewingContract.getContractId(), handlerForSubmitRevision)).start();
                        return true;
                    } else {
                        Toast.makeText(context, R.string.revised_contract_upload_failed, Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
            });

            // The copy operation should be in a separate worker thread so that it won't affect user experience
            File tempFile = new File(data.getStringExtra("contract_file_path"));
            FileOperation.copyFileOrDirectory(tempFile.getAbsolutePath(),
                    getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                            viewingContract.getContractId() + "/", "doc.md");
            File contractFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                    viewingContract.getContractId() + "/", "doc.md");
            new Thread(new Connection(context, myApp).new UploadRevisedContractFile(contractFile,
                    viewingContract.getContractId(), handlerForUploadingRevisedContractFile)).start();
        }
    }
}
