package edu.uci.ics.charm.tabellion;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.TimingLogger;

import androidx.annotation.NonNull;

import com.android.internal.http.multipart.FilePart;
import com.android.internal.http.multipart.MultipartEntity;
import com.android.internal.http.multipart.Part;
import com.android.internal.http.multipart.StringPart;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
Created Date: 01/31/2019
Created By: Myles Liu
Last Modified: 11/27/2019
Last Modified By: Myles Liu
Notes:

 */

public class Connection {

    //private static final String mainUriTag = "http://128.195.54.65/OneAppTestVersion/";
    private static final String mainUriTag = "http://13.90.224.167/OneAppTestVersion/";
    private static final String TAG = "Connection ";

    private Context sContext;
    private MyApp myApp;

    private boolean isUploadSignsSuccess = true;

    public Connection(Context context, MyApp myApp){
        sContext = context;
        this.myApp = myApp;
    }

    public void getToken() {
        // Get token
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();
                        myApp.saveToken(token);

                        // Log and toast
                        String msg = sContext.getString(R.string.msg_token_fmt, myApp.getToken());
                        Log.d(TAG, "The msg is:" + msg);
                        //Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public class UploadContractFile implements Runnable {

        private File contractFile;
        private Handler handler;
        private Time timeStampForStartUploading;
        private Time timeStampForFinishUploading;
        private Time timeStampForGoingToSendToHandler;

        public UploadContractFile(File contractFile, Handler handler){
            this.contractFile = contractFile;
            this.handler = handler;
            Log.d(TAG, "UploadContractFile: We got the file and is going to upload: " + contractFile.getAbsolutePath());
        }

        @Override
        public void run() {
            TimingLogger timingLogger = new TimingLogger("MyTag", "UploadContractFile");
            timeStampForStartUploading = new Time(System.currentTimeMillis());

            HttpURLConnection connection = null;

            try {
                Part[] parts = {
                        new FilePart("file", contractFile)
                };
                com.android.internal.http.multipart.MultipartEntity multipartEntity =
                        new com.android.internal.http.multipart.MultipartEntity(parts);

                URL url = new URL(mainUriTag + "upload_contract_file.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();


                timingLogger.addSplit("upload temp_contract");
                timeStampForFinishUploading = new Time(System.currentTimeMillis());

                String line = "";
                Bundle bundle = new Bundle();
                Log.d(TAG, "UploadContractFile: Going to print echo data");
                while ((line = reader.readLine()) != null)
                {
                    Log.d(TAG, "UploadContractFile: " + line);
                    if(line.contains("contract_id:")){
                        bundle.putString("contractid", line.substring(line.indexOf("contract_id:")).substring(12));
                    }
                }
                timingLogger.addSplit("going to send message to handler");
                timeStampForGoingToSendToHandler = new Time(System.currentTimeMillis());
                Log.d(TAG, "UploadContractFile: Going to send message back to handler");
                bundle.putBoolean("is_success", true);
                Message message = new Message();
                message.setData(bundle);
                handler.sendMessage(message);
                timingLogger.addSplit("message sent to handler");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Contract id: ");
                stringBuilder.append(bundle.getString("contractid"));
                stringBuilder.append("\n");
                stringBuilder.append("Total upload time: ");
                stringBuilder.append(timeStampForFinishUploading.getTime() - timeStampForStartUploading.getTime());
                stringBuilder.append("\n");
                stringBuilder.append("Time spent for processing msg: ");
                stringBuilder.append(timeStampForGoingToSendToHandler.getTime() - timeStampForFinishUploading.getTime());
                stringBuilder.append("\n");
                stringBuilder.append("Time spent for sending msg to handler(Should not be useful): ");
                stringBuilder.append(System.currentTimeMillis() - timeStampForGoingToSendToHandler.getTime());
                stringBuilder.append("\n");
                stringBuilder.append("========================================================");
                stringBuilder.append("\n");
                myApp.writeLogToFile(stringBuilder.toString(), bundle.getString("contractid")
                        + "_contract_upload_log.txt", "uploading");
                timingLogger.dumpToLog();

            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class Submit implements Runnable {

        private String contractID;
        private Handler handler;
        private String contractName;
        private String offereeEmail;
        private String description;
        private boolean isContractCreatedByTabellion;

        public Submit(String contractID, Handler handler, String contractName, String offereeEmail,
                      String description, boolean isContractCreatedByTabellion){
            this.contractID = contractID;
            this.handler = handler;
            this.contractName = contractName;
            this.offereeEmail = offereeEmail;
            this.description = description;
            this.isContractCreatedByTabellion = isContractCreatedByTabellion;
            Log.d(TAG, "Submit: We got the id and is going to render: " + contractID);
        }

        @Override
        public void run() {

            HttpURLConnection connection = null;

            try {
                Part[] parts = {
                        new StringPart("title", "doc.md"),
                        new StringPart("contractid", contractID),
                        new StringPart("fms_token", myApp.getToken()),
                        new StringPart("contractname", contractName),
                        new StringPart("offeroremail", myApp.getEmailAddress()),
                        new StringPart("offereeemail", offereeEmail),
                        new StringPart("description", description),
                        new StringPart("status", "0"),
                        new StringPart("confirmstatus", "0"),
                        new StringPart("is_contract_created_by_tabellion",
                                String.valueOf(isContractCreatedByTabellion))
                };
                com.android.internal.http.multipart.MultipartEntity multipartEntity =
                        new com.android.internal.http.multipart.MultipartEntity(parts);

                URL url = new URL(mainUriTag + "submit1.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";
                Bundle bundle = new Bundle();
                Log.d(TAG, "Submit: Going to print echo data");
                while ((line = reader.readLine()) != null)
                {

                    Log.d(TAG, "Submit: " + line);

                    if(line.contains("has been inserted")){
                        Log.d(TAG, "Submit: From substring of " + line + ", we get " + line.substring(
                                line.indexOf("printing contract_id") + 20, line.indexOf("has been inserted!")) + " for temp_contract id");
                        bundle.putString("contract_id", line.substring(
                                line.indexOf("printing contract_id") + 20, line.indexOf("has been inserted!")));
                    }

                    if(line.contains("sent!")){
                        bundle.putBoolean("is_success", true);
                        int countOfImages = Integer.valueOf(line.substring(line.indexOf("!") + 1));
                        bundle.putInt("count_of_images", countOfImages);
                    } else if (line.contains("!!!")){
                        bundle.putBoolean("is_success", false);
                    }
                }
                Log.d(TAG, "Submit: Going to send message back to handler");
                Message message = new Message();
                message.setData(bundle);
                handler.sendMessage(message);

            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class RegisterNewUser implements Runnable {

        private Handler handler;
        private String firstName;
        private String lastName;
        private String emailAddress;
        private String password;

        public RegisterNewUser(Handler handler, String firstName, String lastName, String emailAddress, String password){
            this.handler = handler;
            this.firstName = firstName;
            this.lastName = lastName;
            this.emailAddress = emailAddress;
            this.password = password;
            Log.d(TAG, "RegisterNewUser: We got the info and is going to register: " + emailAddress);
        }

        @Override
        public void run() {

            HttpURLConnection connection = null;

            try {
                Log.d(TAG, "RegisterNewUser: Trying to get photo uri: " + myApp.getCurrentUserPhotoUri().toString());
                File image = new File(myApp.getCurrentUserPhotoUri().toString());
                Part[] parts = {
                        new FilePart("file", image),
                        new StringPart("filename", myApp.getCurrentUserPhotoFileName()),
                        new StringPart("email", emailAddress),
                        new StringPart("firstname", firstName),
                        new StringPart("lastname", lastName),
                        new StringPart("password", password),
                        new StringPart("token", myApp.getToken())
                };
                com.android.internal.http.multipart.MultipartEntity multipartEntity =
                        new com.android.internal.http.multipart.MultipartEntity(parts);

                URL url = new URL(mainUriTag + "add_user.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";
                Bundle bundle = new Bundle();
                Log.d(TAG, "RegisterNewUser: Going to print echo data");
                while ((line = reader.readLine()) != null)
                {
                    Log.d(TAG, "RegisterNewUser: " + line);
                    if(line.contains("created.")){
                        bundle.putBoolean("is_success", true);
                    } else {
                        bundle.putBoolean("is_success", false);
                    }
                }
                Log.d(TAG, "RegisterNewUser: Going to send message back to handler");
                Message message = new Message();
                message.setData(bundle);
                handler.sendMessage(message);

            } catch (FileNotFoundException e){
                e.printStackTrace();
            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class TryLogIn implements Runnable {

        private Handler handler;
        private String emailAddress;
        private String password;

        public TryLogIn(Handler handler, String emailAddress, String password){
            this.handler = handler;
            this.emailAddress = emailAddress;
            this.password = password;
            Log.d(TAG, "TryLogIn: We got the info and is going to register: " + emailAddress);
        }

        @Override
        public void run() {

            HttpURLConnection httpURLConnection = null;

            try {
                Part[] parts = {
                        new StringPart("email", emailAddress),
                        new StringPart("password", password)
                };
                MultipartEntity multipartEntity = new MultipartEntity(parts);

                URL url = new URL(mainUriTag + "try_sign_in.php");
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setUseCaches(false);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);

                httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
                httpURLConnection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                httpURLConnection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(httpURLConnection.getOutputStream());
                httpURLConnection.getOutputStream().close();
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                httpURLConnection.connect();

                String line = "";
                Bundle bundle = new Bundle();
                Log.d(TAG, "TryLogIn: Going to print echo data");
                // return_type: 1: success_logged_in ; 2: password_incorrect; 3: account_not_exist
                bundle.putInt("return_type", 3);
                while ((line = reader.readLine()) != null)
                {
                    Log.d(TAG, "TryLogIn: " + line);
                    if(line.contains("Verified")){
                        bundle.putInt("return_type", 1);
                    } else if (line.contains("Password")){
                        bundle.putInt("return_type", 2);
                    }
                }
                Log.d(TAG, "TryLogIn: Going to send message back to handler");
                Message message = new Message();
                message.setData(bundle);
                handler.sendMessage(message);
            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(httpURLConnection != null){
                    httpURLConnection.disconnect();
                }
            }
        }
    }

    public class DownloadContractImage implements Runnable {

        private String filename;
        private String contractID;
        private Handler handler;
        private int page_num;
        private int revistedNumCount;

        public DownloadContractImage(String filename, String contractID, Handler handler, int page_num, int revistedNumCount){
            this.filename = filename;
            this.contractID = contractID;
            this.handler = handler;
            this.page_num = page_num;
            this.revistedNumCount = revistedNumCount;
        }

        @Override
        public void run() {
            String url_str = mainUriTag + "submitted_files/" + contractID + "/" + filename;

            if(revistedNumCount > 0){
                url_str = mainUriTag + "submitted_files/" + contractID + "/revision/" + filename;
            }

            Bundle bundle = new Bundle();
            try {
                URL url = new URL(url_str);
                Log.d("Saeed: ", "Connected to " + url_str);
                int count;
                try {
                    Log.d("Saeed: ", "Downdload background");

                    URLConnection conection = url.openConnection();
                    conection.connect();
                    // getting file length
                    int lenghtOfFile = conection.getContentLength();

                    // input stream to read file - with 8k buffer
                    InputStream input = new BufferedInputStream(url.openStream(), 8192);
                    // Output stream to write file
                    File dir = new File(MainActivity.sContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                            + "/" +  contractID + "/");
                    dir.mkdirs();
                    File outputFile = new File(dir, filename);

                    OutputStream output = new FileOutputStream(outputFile);
                    byte data[] = new byte[1024];

                    long total = 0;
                    while ((count = input.read(data)) != -1) {
                        total += count;

                        // writing data to file
                        output.write(data, 0, count);
                    }
                    // flushing output
                    output.flush();

                    // closing streams
                    output.close();
                    input.close();

                    bundle.putBoolean("is_success", true);
                    bundle.putInt("page_num", page_num);

                    Log.d(TAG, "DownloadContractImage: " + filename + "has been downloaded to " + dir.getAbsolutePath());

                } catch (Exception e) {
                    bundle.putBoolean("is_success", false);
                    Log.e("Saeed Error: ", e.getMessage());
                }
            } catch (Exception e) {
                bundle.putBoolean("is_success", false);
                Log.d("Saeed: Error", "try");
            }
            Message message = new Message();
            message.setData(bundle);
            handler.sendMessage(message);
        }
    }

    public class DownloadFile implements Runnable {

        private String filename;
        private String downloadPath;
        private Handler handler;
        private String downloadSubUrl;

        public DownloadFile(String filename, String downloadPath, String downloadSubUrl, Handler handler){
            this.filename = filename;
            this.downloadPath = downloadPath;
            this.handler = handler;
            this.downloadSubUrl = downloadSubUrl;
        }

        @Override
        public void run() {
            String url_str = mainUriTag + downloadSubUrl + filename;
            Bundle bundle = new Bundle();
            try {
                URL url = new URL(url_str);
                Log.d("Saeed: ", "Connected to " + url_str);
                int count;
                try {
                    Log.d("Saeed: ", "Downdload background");

                    URLConnection conection = url.openConnection();
                    conection.connect();
                    // getting file length
                    int lenghtOfFile = conection.getContentLength();

                    // input stream to read file - with 8k buffer
                    InputStream input = new BufferedInputStream(url.openStream(), 8192);
                    // Output stream to write file
                    File dir = new File(downloadPath);
                    dir.mkdirs();
                    File outputFile = new File(dir, filename);

                    OutputStream output = new FileOutputStream(outputFile);
                    byte data[] = new byte[1024];

                    long total = 0;
                    while ((count = input.read(data)) != -1) {
                        total += count;

                        // writing data to file
                        output.write(data, 0, count);
                    }
                    // flushing output
                    output.flush();

                    // closing streams
                    output.close();
                    input.close();

                    bundle.putBoolean("is_success", true);

                    Log.d(TAG, "DownloadFile: " + filename + "has been downloaded to " + dir.getAbsolutePath());

                } catch (Exception e) {
                    bundle.putBoolean("is_success", false);
                    Log.e("Saeed Error: ", e.getMessage());
                }
            } catch (Exception e) {
                bundle.putBoolean("is_success", false);
                Log.d("Saeed: Error", "try");
            }
            Message message = new Message();
            message.setData(bundle);
            handler.sendMessage(message);
        }
    }

    private class UploadScreenshot implements Runnable {

        private Contract contract;
        private int upload_counter;

        public UploadScreenshot(Contract contract, int upload_counter){
            this.contract = contract;
            this.upload_counter = upload_counter;
        }

        @Override
        public void run() {
            HttpURLConnection connection = null;
            try {

                File imageFile = new File(sContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        + "/" +  contract.getContractId() + "/", "screenshot" + String.valueOf(upload_counter));

                String currentRole = "offeror";
                if(contract.getCurrentRole() == 1){
                    currentRole = "offeree";
                }

                Part[] parts = {
                        new StringPart("contractid", contract.getContractId()),
                        new StringPart("lastscreenshotid", String.valueOf(contract.getTotalImageNums())),
                        new StringPart("useremail", myApp.getEmailAddress()),
                        new StringPart("title", "screenshot" + String.valueOf(upload_counter)),
                        new StringPart("timeinterval", contract.getLastActionTimeInterval()),
                        new FilePart("file", imageFile),
                        new StringPart("current_role", currentRole)
                };
                com.android.internal.http.multipart.MultipartEntity multipartEntity =
                        new com.android.internal.http.multipart.MultipartEntity(parts);

                URL url = new URL(mainUriTag + "upload_screenshots_and_signs.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";

                while ((line = reader.readLine()) != null) {
                    Log.d(TAG, "UploadScreenshot: " + line);
                }
            } catch (MalformedURLException e){
                isUploadSignsSuccess = false;
                e.printStackTrace();
            } catch (IOException e){
                isUploadSignsSuccess = false;
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class UploadScreenshotExternal implements Runnable {

        private Contract contract;
        private int upload_counter; // Start counted from 1
        private Handler handler;

        public UploadScreenshotExternal(Contract contract, int upload_counter, Handler handler){
            this.contract = contract;
            this.upload_counter = upload_counter;
            this.handler = handler;
        }

        @Override
        public void run() {
            HttpURLConnection connection = null;

            Message message = new Message();
            Bundle bundle = new Bundle();

            try {

                File imageFile = new File(sContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        + "/" +  contract.getContractId() + "/", "screenshot" + String.valueOf(upload_counter));

                String currentRole = "offeror";
                if(contract.getCurrentRole() == 1){
                    currentRole = "offeree";
                }

                Part[] parts = {
                        new StringPart("contractid", contract.getContractId()),
                        new StringPart("useremail", myApp.getEmailAddress()),
                        new StringPart("current_page_counter", String.valueOf(upload_counter)),
                        new StringPart("timeinterval", contract.getLastActionTimeInterval()),
                        new FilePart("file", imageFile),
                        new StringPart("current_role", currentRole)
                };
                com.android.internal.http.multipart.MultipartEntity multipartEntity =
                        new com.android.internal.http.multipart.MultipartEntity(parts);

                URL url = new URL(mainUriTag + "upload_screenshots_and_signs_external.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";


                while ((line = reader.readLine()) != null) {
                    Log.d(TAG, "UploadScreenshotExternal: " + line);
                    if(line.contains("The file has been uploaded")){
                        bundle.putBoolean("is_success", true);
                    }
                }
            } catch (MalformedURLException e){
                e.printStackTrace();
                bundle.putBoolean("is_success", true);
            } catch (IOException e){
                e.printStackTrace();
                bundle.putBoolean("is_success", true);
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
                message.setData(bundle);
                handler.sendMessage(message);
            }
        }
    }

    private class UploadSignature implements Runnable {

        private Contract contract;
        private int upload_counter;

        public UploadSignature(Contract contract, int upload_counter){
            this.contract = contract;
            this.upload_counter = upload_counter;
        }

        @Override
        public void run() {
            File imageFile = new File(sContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    + "/" +  contract.getContractId() + "/", "signature" + String.valueOf(upload_counter));

            HttpURLConnection connection = null;

            String currentRole = "offeror";
            if(contract.getCurrentRole() == 1){
                currentRole = "offeree";
            }

            try {
                Part[] parts = {
                        new StringPart("contractid", contract.getContractId()),
                        new StringPart("lastscreenshotid", String.valueOf(contract.getTotalImageNums())),
                        new StringPart("useremail", myApp.getEmailAddress()),
                        new StringPart("timeinterval", contract.getLastActionTimeInterval()),
                        new StringPart("title", "signature" + String.valueOf(upload_counter)),
                        new FilePart("file", imageFile),
                        new StringPart("current_role", currentRole)
                };
                com.android.internal.http.multipart.MultipartEntity multipartEntity =
                        new com.android.internal.http.multipart.MultipartEntity(parts);

                URL url = new URL(mainUriTag + "upload_screenshots_and_signs.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";
                while ((line = reader.readLine()) != null) {
                    Log.d("Myles ", "Upload RealSign Output: " + line);
                }

            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class UploadSignatureExternal implements Runnable {

        private Contract contract;
        private int upload_counter; // Start counted from 1
        private Handler handler;

        public UploadSignatureExternal(Contract contract, int upload_counter, Handler handler){
            this.contract = contract;
            this.upload_counter = upload_counter;
            this.handler = handler;
        }

        @Override
        public void run() {
            HttpURLConnection connection = null;

            Message message = new Message();
            Bundle bundle = new Bundle();

            try {

                File imageFile = new File(sContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        + "/" +  contract.getContractId() + "/", "signature" + String.valueOf(upload_counter));

                String currentRole = "offeror";
                if(contract.getCurrentRole() == 1){
                    currentRole = "offeree";
                }

                Part[] parts = {
                        new StringPart("contractid", contract.getContractId()),
                        new StringPart("useremail", myApp.getEmailAddress()),
                        new StringPart("current_page_counter", String.valueOf(upload_counter)),
                        new StringPart("timeinterval", contract.getLastActionTimeInterval()),
                        new FilePart("file", imageFile),
                        new StringPart("current_role", currentRole)
                };
                com.android.internal.http.multipart.MultipartEntity multipartEntity =
                        new com.android.internal.http.multipart.MultipartEntity(parts);

                URL url = new URL(mainUriTag + "upload_screenshots_and_signs_external.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";

                while ((line = reader.readLine()) != null) {
                    Log.d(TAG, "UploadSignatureExternal: " + line);
                    if(line.contains("The file has been uploaded")){
                        bundle.putBoolean("is_success", true);
                    }
                }
            } catch (MalformedURLException e){
                e.printStackTrace();
                bundle.putBoolean("is_success", false);
            } catch (IOException e){
                e.printStackTrace();
                bundle.putBoolean("is_success", false);
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
                message.setData(bundle);
                handler.sendMessage(message);
            }
        }
    }

    public class UploadScreenshotsAndSigns implements Runnable {

        private Contract contract;
        private Handler handler;
        private Time timeStampForStartUploading;

        public UploadScreenshotsAndSigns(Contract contract, Handler handler){
            this.contract = contract;
            this.handler = handler;
        }

        @Override
        public void run() {

            TimingLogger timingLogger = new TimingLogger("MyTag", "UploadScreenshotsAndSigns");
            timeStampForStartUploading = new Time(System.currentTimeMillis());

            List<Thread> threads = new ArrayList<>();

            for(int upload_counter = 1; upload_counter <= contract.getTotalImageNums(); upload_counter++) {
                threads.add(new Thread(new UploadScreenshot(contract, upload_counter)));
            }

            Bundle bundle = new Bundle();

            for(int upload_counter = 1; upload_counter <= contract.getTotalImageNums(); upload_counter++) {
                threads.add(new Thread(new UploadSignature(contract, upload_counter)));
            }

            for(Thread thread: threads){
                thread.start();
            }

            for(Thread thread: threads){
                try {
                    thread.join();
                } catch (InterruptedException e){
                    e.printStackTrace();
                    Log.d(TAG, "UploadScreenshotsAndSigns: " + "Something went wrong during uploading!!!");
                }
            }

            timingLogger.addSplit("Screenshots and Signatures Uploaded");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Contract id: ");
            stringBuilder.append(contract.getContractId());
            stringBuilder.append("\n");
            stringBuilder.append("Contract name: ");
            stringBuilder.append(contract.getContractName());
            stringBuilder.append("\n");
            stringBuilder.append("Image numbers: ");
            stringBuilder.append("\n");
            stringBuilder.append(contract.getTotalImageNums());
            stringBuilder.append("Total upload time: ");
            stringBuilder.append(System.currentTimeMillis() - timeStampForStartUploading.getTime());
            stringBuilder.append("\n");
            stringBuilder.append("========================================================");
            stringBuilder.append("\n");
            myApp.writeLogToFile(stringBuilder.toString(), contract.getContractId()
                    + "_screenshot_and_signature_upload_log.txt", "Uploading");
            timingLogger.dumpToLog();

            if(isUploadSignsSuccess){
                bundle.putBoolean("is_success", true);
            } else {
                bundle.putBoolean("is_success", false);
            }

            Message msg = new Message();
            msg.setData(bundle);
            handler.sendMessage(msg);
            // End of Uploading Signature
        }
    }

    public class SendVerifyAndProcessRequest implements Runnable {

        private Contract contract;

        public SendVerifyAndProcessRequest(Contract contract){
            this.contract = contract;
            Log.d(TAG, "SendVerifyAndProcessRequest: Going to try revoke temp_contract with id: " + contract.getContractId());
        }

        @Override
        public void run() {

            HttpURLConnection connection = null;

            try {

                Part[] parts = {
                        new StringPart("contractid", contract.getContractId()),
                        new StringPart("lastscreenshotid", String.valueOf(contract.getTotalImageNums())),
                        new StringPart("useremail", myApp.getEmailAddress()),
                        new StringPart("timeinterval", contract.getLastActionTimeInterval())
                };
                MultipartEntity multipartEntity = new MultipartEntity(parts);

                URL url = new URL(mainUriTag + "check_sign_and_process_contract.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";
                Log.d(TAG, "SendVerifyAndProcessRequest: Going to print echo data");
                while ((line = reader.readLine()) != null)
                {
                    Log.d(TAG, "SendVerifyAndProcessRequest: " + line);
                }
                Log.d(TAG, "SendVerifyAndProcessRequest: Going to send message back to handler");

            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class UploadPublicKey implements Runnable {

        private Handler handler;

        public UploadPublicKey(Handler handler){
            this.handler = handler;
            Log.d("UploadPublicKey ", " initialized...");
        }

        @Override
        public void run() {
            File imageFile = new File(sContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "publicKey.pem");

            HttpURLConnection connection = null;

            try {
                Part[] parts = {
                        new StringPart("title", "publicKey.pem"),
                        new StringPart("email", myApp.getEmailAddress()),
                        new StringPart("token", myApp.getToken()),
                        new FilePart("file", imageFile)
                };

                com.android.internal.http.multipart.MultipartEntity multipartEntity =
                        new com.android.internal.http.multipart.MultipartEntity(parts);

                URL url = new URL(mainUriTag + "uploadPublicKey1.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";
                while ((line = reader.readLine()) != null)
                {
                    Log.d(TAG, "UploadPublicKey: " + line);
                }
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putBoolean("is_success", true);
                message.setData(bundle);
                handler.sendMessage(message);
            } catch (FileNotFoundException e){
                e.printStackTrace();
            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    public class SyncAllContracts implements Runnable{

        private Handler handler;
        private String emailAddress;

        public SyncAllContracts(Handler handler, String emailAddress){
            this.handler = handler;
            this.emailAddress = emailAddress;
        }

        @Override
        public void run() {

            Log.d(TAG, " SyncAllContracts: Going to sync contracts ids with server...");

            HttpURLConnection connection = null;

            try {
                Part[] parts = {
                        new StringPart("email", emailAddress)
                };
                com.android.internal.http.multipart.MultipartEntity multipartEntity =
                        new com.android.internal.http.multipart.MultipartEntity(parts);

                URL url = new URL(mainUriTag + "get_one_user_contract_ids.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";
                Bundle bundle = new Bundle();
                Log.d(TAG, "SyncAllContracts: Going to print echo data");
                ArrayList<String> contractIDList = new ArrayList<>();
                while ((line = reader.readLine()) != null)
                {
                    Log.d(TAG, "SyncAllContracts: " + line);
                    if(!line.isEmpty()){
                        contractIDList.addAll(Arrays.asList(line.split("-")));
                    }
                }
                Log.d(TAG, "SyncAllContracts: Going to send message back to handler");
                bundle.putStringArrayList("contractIDs", contractIDList);
                Message message = new Message();
                message.setData(bundle);
                handler.sendMessage(message);

            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class GetOneContract implements Runnable {

        private Handler handler;
        private String contractID;

        public GetOneContract(Handler handler, String contractID){
            this.handler = handler;
            this.contractID = contractID;
        }

        @Override
        public void run() {

            HttpURLConnection connection = null;

            try {
                Part[] parts = {
                        new StringPart("contractid", contractID)
                };
                com.android.internal.http.multipart.MultipartEntity multipartEntity =
                        new com.android.internal.http.multipart.MultipartEntity(parts);

                URL url = new URL(mainUriTag + "get_contract_info.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";
                Bundle bundle = new Bundle();
                Log.d(TAG, "GetOneContract: Going to print echo data");
                ArrayList<String> contractInfo = new ArrayList<>();
                while ((line = reader.readLine()) != null)
                {
                    Log.d(TAG, "GetOneContract: " + line);
                    contractInfo.addAll(Arrays.asList(line.split("#This is for seperating the info#")));
                }
                Log.d(TAG, "GetOneContract: Going to send message back to handler");
                bundle.putStringArrayList("contractInfo", contractInfo);
                Message message = new Message();
                message.setData(bundle);
                handler.sendMessage(message);

            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class UpdateContractConfirmStatus implements Runnable {

        private Handler handler;
        private String contractID;
        private int confirmStatus;

        public UpdateContractConfirmStatus(Handler handler, String contractID, int confirmStatus){
            this.handler = handler;
            this.contractID = contractID;
            this.confirmStatus = confirmStatus;
        }

        @Override
        public void run() {

            HttpURLConnection connection = null;

            try {
                Part[] parts = {
                        new StringPart("contractid", contractID),
                        new StringPart("confirmstatus", String.valueOf(confirmStatus))
                };
                MultipartEntity multipartEntity = new MultipartEntity(parts);

                URL url = new URL(mainUriTag + "set_contract_confirmstatus.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";
                Bundle bundle = new Bundle();
                Log.d(TAG, "UpdateContractConfirmStatus: Going to print echo data");
                while ((line = reader.readLine()) != null)
                {
                    Log.d(TAG, "UpdateContractConfirmStatus: " + line);
                    if(line.contains("sent!")) {
                        bundle.putBoolean("is_success", true);
                    }
                }
                Log.d(TAG, "UpdateContractConfirmStatus: Going to send message back to handler");
                Message message = new Message();
                message.setData(bundle);
                handler.sendMessage(message);

            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class DeleteAUser extends AsyncTask<String, Void, String> {

        private Handler handler;
        private String userEmail;

        public DeleteAUser(Handler handler, String userEmail){
            this.handler = handler;
            this.userEmail = userEmail;
            Log.d(TAG, "DeleteAUser: Going to delete user with email: " + userEmail);
        }

        @Override
        protected String doInBackground(String... params) {

            HttpURLConnection connection = null;

            try {
                Part[] parts = {
                        new StringPart("email", userEmail)
                };
                MultipartEntity multipartEntity = new MultipartEntity(parts);

                URL url = new URL(mainUriTag + "delete_a_user.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";
                Bundle bundle = new Bundle();
                Log.d(TAG, "DeleteAUser: Going to print echo data");
                while ((line = reader.readLine()) != null)
                {
                    Log.d(TAG, "DeleteAUser: " + line);
                    if(line.contains("user deleted")) {
                        bundle.putBoolean("is_success", true);
                    }
                }
                Log.d(TAG, "DeleteAUser: Going to send message back to handler");
                Message message = new Message();
                message.setData(bundle);
                handler.sendMessage(message);

            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }

            return "";
        }
    }

    public class SendRevokeRequest implements Runnable {

        private Contract contract;

        public SendRevokeRequest(Contract contract){
            this.contract = contract;
            Log.d(TAG, "SendRevokeRequest: Going to try revoke temp_contract with id: " + contract.getContractId());
        }

        @Override
        public void run() {

            HttpURLConnection connection = null;

            try {

                Part[] parts = {
                        new StringPart("contractid", contract.getContractId()),
                        new StringPart("timeinterval", contract.getLastActionTimeInterval())
                };
                MultipartEntity multipartEntity = new MultipartEntity(parts);

                URL url = new URL(mainUriTag + "revoke_request.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";
                Log.d(TAG, "SendRevokeRequest: Going to print echo data");
                while ((line = reader.readLine()) != null)
                {
                    Log.d(TAG, "SendRevokeRequest: " + line);
                }
                Log.d(TAG, "SendRevokeRequest: Going to send message back to handler");

            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class CheckIfContractNameExist implements Runnable {

        private String contractName;
        private Handler handler;
        private int countOfScreenshots;
        private String userEmail;
        private String currentRole;

        public CheckIfContractNameExist(String contractName, int countOfScreenshots,
                                        String offereeEmail, Handler handler, String currentRole){
            this.contractName = contractName;
            this.handler = handler;
            this.countOfScreenshots = countOfScreenshots;
            this.userEmail = offereeEmail;
            this.currentRole = currentRole;
        }

        @Override
        public void run() {

            HttpURLConnection connection = null;

            try {

                Log.d(TAG, "CheckIfContractNameExist: contractname: " + contractName +
                        "; countofscreeenshots: " + countOfScreenshots + "; userEmail" + userEmail +
                        "; current_role: " + currentRole);

                Part[] parts = {
                        new StringPart("contractname", contractName),
                        new StringPart("countofscreeenshots", String.valueOf(countOfScreenshots)),
                        new StringPart("userEmail", userEmail),
                        new StringPart("current_role", currentRole)
                };
                MultipartEntity multipartEntity = new MultipartEntity(parts);

                URL url = new URL(mainUriTag + "check_if_contract_name_exist.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                Message message = new Message();
                Bundle bundle = new Bundle();

                String line = "";
                while ((line = reader.readLine()) != null)
                {
                    Log.d(TAG, "CheckIfContractNameExist: " + line);
                    if(line.contains("success")){
                        bundle.putBoolean("is_success", true);
                    }
                }

                message.setData(bundle);
                handler.sendMessage(message);
            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class SendVerifyAndProcessRequestExternal implements Runnable {

        private Contract contract;
        private String currentRole;

        public SendVerifyAndProcessRequestExternal(Contract contract, String currentRole){
            this.contract = contract;
            this.currentRole = currentRole;
            Log.d(TAG, "SendVerifyAndProcessRequestExternal: Going to try verify temp_contract " +
                    "with id: " + contract.getContractId() + " as " + currentRole);
        }

        @Override
        public void run() {

            HttpURLConnection connection = null;

            try {

                Part[] parts = {
                        new StringPart("contractid", contract.getContractId()),
                        new StringPart("lastscreenshotid", String.valueOf(contract.getTotalImageNums())),
                        new StringPart("useremail", myApp.getEmailAddress()),
                        new StringPart("timeinterval", contract.getLastActionTimeInterval()),
                        new StringPart("current_role", currentRole)
                };
                MultipartEntity multipartEntity = new MultipartEntity(parts);

                URL url = new URL(mainUriTag + "check_sign_and_process_contract_external.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";
                Log.d(TAG, "SendVerifyAndProcessRequestExternal: Going to print echo data");
                while ((line = reader.readLine()) != null)
                {
                    Log.d(TAG, "SendVerifyAndProcessRequestExternal: " + line);
                }
                Log.d(TAG, "SendVerifyAndProcessRequestExternal: Going to send message back to handler");

            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class UploadAndVerifyPhotoTakenByUser implements Runnable {

        private String pathOfPhoto;
        private String pathOfSignature;
        //private String contractName;
        private String timeInterval;
        private String userEmail;
        private Handler handler;

        public UploadAndVerifyPhotoTakenByUser(String pathOfPhoto, String pathOfSignature,
                                               String timeInterval, String userEmail, Handler handler){
            this.pathOfPhoto = pathOfPhoto;
            this.pathOfSignature = pathOfSignature;
            //this.contractName = contractName;
            this.timeInterval = timeInterval;
            this.userEmail = userEmail;
            this.handler = handler;
        }

        @Override
        public void run() {
            File imageFile = new File(pathOfPhoto);
            File signatureFile = new File(pathOfSignature);

            HttpURLConnection connection = null;

            try {
                Part[] parts = {
                        //new StringPart("contractname", contractName),
                        new StringPart("timeinterval", timeInterval),
                        new StringPart("useremail", userEmail),
                        new FilePart("file", imageFile),
                        new FilePart("signature", signatureFile)
                };
                com.android.internal.http.multipart.MultipartEntity multipartEntity =
                        new com.android.internal.http.multipart.MultipartEntity(parts);

                URL url = new URL(mainUriTag + "upload_and_verify_photo_taken_by_user_external.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";
                Bundle bundle = new Bundle();
                while ((line = reader.readLine()) != null) {
                    Log.d(TAG, "UploadAndVerifyPhotoTakenByUser: " + line);
                    if(line.contains("photo verified!!!")){
                        bundle.putBoolean("is_success", true);
                    }
                }
                Message msg = new Message();
                msg.setData(bundle);
                handler.sendMessage(msg);

            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class SubmitComment implements Runnable {

        private Handler handler;
        private String contractId;
        private String comment;
        private File commentImage;
        private File commentSignature;

        public SubmitComment(Handler handler, String contractId, String comment, File commentImage, File commentSignature){
            this.handler = handler;
            this.contractId = contractId;
            this.comment = comment;
            this.commentImage = commentImage;
            this.commentSignature = commentSignature;

            Log.d(TAG, "The screenshot's path: " + commentImage.getAbsolutePath());
            Log.d(TAG, "The signature's path: " + commentSignature.getAbsolutePath());
        }

        @Override
        public void run() {

            HttpURLConnection connection = null;

            try {
                Part[] parts = {
                        new StringPart("contractid", contractId),
                        new FilePart("screenshot", commentImage),
                        new FilePart("signature", commentSignature),
                        new StringPart("comment_string", comment)
                };
                com.android.internal.http.multipart.MultipartEntity multipartEntity =
                        new com.android.internal.http.multipart.MultipartEntity(parts);

                URL url = new URL(mainUriTag + "submit_comment.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";
                Bundle bundle = new Bundle();
                Log.d(TAG, "SubmitComment: Going to print echo data");
                while ((line = reader.readLine()) != null)
                {
                    Log.d(TAG, "SubmitComment: " + line);
                    if (line.contains("message sent to")){
                        bundle.putBoolean("is_success", true);
                    }
                }
                Log.d(TAG, "SubmitComment: Going to send message back to handler");
                Message message = new Message();
                message.setData(bundle);
                handler.sendMessage(message);

            } catch (FileNotFoundException e){
                e.printStackTrace();
            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class GetComment implements Runnable {

        private Handler handler;
        private String contractId;

        public GetComment(Handler handler, String contractId){
            this.handler = handler;
            this.contractId = contractId;
        }

        @Override
        public void run() {

            HttpURLConnection connection = null;

            try {
                Part[] parts = {
                        new StringPart("contractid", contractId)
                };
                com.android.internal.http.multipart.MultipartEntity multipartEntity =
                        new com.android.internal.http.multipart.MultipartEntity(parts);

                URL url = new URL(mainUriTag + "get_comment.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";
                StringBuilder commentBuilder = new StringBuilder();
                Bundle bundle = new Bundle();
                Log.d(TAG, "GetComment: Going to print echo data");
                while ((line = reader.readLine()) != null)
                {
                    commentBuilder.append(line);
                    Log.d(TAG, "GetComment: " + line);
                }
                Log.d(TAG, "GetComment: Going to send message back to handler");
                bundle.putString("comment", commentBuilder.toString());
                bundle.putBoolean("is_success", true);
                Message message = new Message();
                message.setData(bundle);
                handler.sendMessage(message);

            } catch (FileNotFoundException e){
                e.printStackTrace();
            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class UploadRevisedContractFile implements Runnable {

        private File contractFile;
        private String contractId;
        private Handler handler;
        private Time timeStampForStartUploading;
        private Time timeStampForFinishUploading;
        private Time timeStampForGoingToSendToHandler;

        public UploadRevisedContractFile(File contractFile, String contractId, Handler handler){
            this.contractFile = contractFile;
            this.contractId = contractId;
            this.handler = handler;
            Log.d(TAG, "UploadRevisedContractFile: We got the file and is going to upload: " + contractFile.getAbsolutePath());
        }

        @Override
        public void run() {
            timeStampForStartUploading = new Time(System.currentTimeMillis());

            HttpURLConnection connection = null;

            try {
                Part[] parts = {
                        new FilePart("file", contractFile),
                        new StringPart("contractid", contractId)
                };
                com.android.internal.http.multipart.MultipartEntity multipartEntity =
                        new com.android.internal.http.multipart.MultipartEntity(parts);

                URL url = new URL(mainUriTag + "upload_revised_contract_file.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                timeStampForFinishUploading = new Time(System.currentTimeMillis());

                String line = "";
                Bundle bundle = new Bundle();
                Log.d(TAG, "UploadRevisedContractFile: Going to print echo data");
                while ((line = reader.readLine()) != null)
                {
                    Log.d(TAG, "UploadRevisedContractFile: " + line);
                    if(line.contains("all success!!!")){
                        bundle.putBoolean("is_success", true);
                    }
                }
                timeStampForGoingToSendToHandler = new Time(System.currentTimeMillis());
                Log.d(TAG, "UploadRevisedContractFile: Going to send message back to handler");
                Message message = new Message();
                message.setData(bundle);
                handler.sendMessage(message);

            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class SubmitRevision implements Runnable {

        private String contractID;
        private Handler handler;

        public SubmitRevision(String contractID, Handler handler){
            this.contractID = contractID;
            this.handler = handler;
            Log.d(TAG, "SubmitRevision: We got the id and is going to render: " + contractID);
        }

        @Override
        public void run() {

            HttpURLConnection connection = null;

            try {
                Part[] parts = {
                        new StringPart("contractid", contractID)
                };
                com.android.internal.http.multipart.MultipartEntity multipartEntity =
                        new com.android.internal.http.multipart.MultipartEntity(parts);

                URL url = new URL(mainUriTag + "submit_revision.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";
                Bundle bundle = new Bundle();
                Log.d(TAG, "SubmitRevision: Going to print echo data");
                while ((line = reader.readLine()) != null)
                {
                    Log.d(TAG, "SubmitRevision: " + line);
                }
                Log.d(TAG, "SubmitRevision: Going to send message back to handler");
                bundle.putBoolean("is_success", true);
                Message message = new Message();
                message.setData(bundle);
                handler.sendMessage(message);

            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

    public class GetTextFromPdf implements Runnable {

        private File contractFile;
        private Handler handler;

        public GetTextFromPdf(File contractFile, Handler handler){
            this.contractFile = contractFile;
            this.handler = handler;
            Log.d(TAG, "GetTextFromPdf: We got the file and is going to upload: " + contractFile.getAbsolutePath());
        }

        @Override
        public void run() {

            HttpURLConnection connection = null;

            try {
                Part[] parts = {
                        new FilePart("file", contractFile)
                };
                com.android.internal.http.multipart.MultipartEntity multipartEntity =
                        new com.android.internal.http.multipart.MultipartEntity(parts);

                URL url = new URL(mainUriTag + "get_text_from_pdf.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.addRequestProperty("Content-length", multipartEntity.getContentLength()+"");
                connection.addRequestProperty(multipartEntity.getContentType().getName(),
                        multipartEntity.getContentType().getValue());

                multipartEntity.writeTo(connection.getOutputStream());
                connection.getOutputStream().close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                connection.connect();

                String line = "";
                Bundle bundle = new Bundle();
                Log.d(TAG, "GetTextFromPdf: Going to print echo data");
                StringBuilder stringBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null)
                {
                    Log.d(TAG, "GetTextFromPdf: " + line);
                    stringBuilder.append(line);
                }
                Log.d(TAG, "GetTextFromPdf: Going to send message back to handler");
                bundle.putString("contract_raw_text", stringBuilder.toString());
                bundle.putBoolean("is_success", true);
                Message message = new Message();
                message.setData(bundle);
                handler.sendMessage(message);

            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    connection.disconnect();
                }
            }
        }
    }

}
