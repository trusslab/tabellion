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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
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
Last Modified: 03/22/2020
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
    private TextView contractRelatedEmail;
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
        contractRelatedEmail = findViewById(R.id.contract_viewing_contract_offeree_email);
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
        if(viewingContract.getCurrentRole() == 0){
            contractRelatedEmail.setText(String.format(getString(R.string.to_offeree_email), viewingContract.getOffereeEmailAddress()));
        } else {
            contractRelatedEmail.setText(String.format(getString(R.string.from_offeror_email), viewingContract.getOfferorEmailAddress()));
        }

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

        if(viewingContract.getContractStatus() == 5){
            circularProgressButton.setText(R.string.start_edit);
            showProgressDialog();
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
                  // Start Signing
                    startActivity(new Intent(context, SigningActivity.class));
                    finish();
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
            startConfirmationPage();
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

    private void showProgressDialog(){
        progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(context.getString(R.string.talking_with_server));
        progressDialog.show();
    }

    private void startConfirmationPage(){
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
        setUpHandlerForUpdatingContractConfirmingStatus();

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.confirm),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        showProgressDialog();
                        new Thread(new Connection(context, myApp).new UpdateContractConfirmStatus(
                                handlerForUpdatingContractConfirmingStatus,
                                myApp.getCurrentViewingContract().getContractId(),
                                2)).start();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
        setUpHandlerForUserPhotoViewing();
        new Thread(new Connection(context, myApp).new DownloadFile(
                myApp.getUserPhotoFileNameByEmail(myApp.getCurrentViewingContract().getOffereeEmailAddress()),
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                        myApp.getCurrentViewingContract().getContractId(),
                "users/user_photos/", handler)).start();
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
            showProgressDialog();
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
