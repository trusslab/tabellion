package edu.uci.ics.charm.tabellion;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/*
Created Date: 01/28/2019
Created By: Myles Liu
Last Modified: 03/22/2020
Last Modified By: Myles Liu
Notes:

Pending To Do:
    1. Need to make external contract be recognized by the system.
 */

public class MyApp extends Application {

    private static final String TAG = "MyApp";
    private static final String TokenMainTag = "Token";
    private static final String TokenSubTag = "MainToken";
    private List<Contract> pendingToSignContractList = new ArrayList<>();
    private String emailAddress = "";
    private int currentMode = 0; // 0 is in pendingToSign Tab; 1 is in waitForFinish Tab; 2 is in history Tab
    private PublicKey publicKey = null;
    private PrivateKey privateKey = null;
    private boolean isReRegistering = false;
    private boolean isWaitingForNetWork = false;
    private boolean isRefreshingOnMainActivity = false;
    private boolean transitionAndFreezingSwitch = true;

    private Contract currentViewingContract;

    public void setCurrentViewingContract(Contract contract){
        currentViewingContract = contract;
    }

    public Contract getCurrentViewingContract(){
        return currentViewingContract;
    }

    public void saveContractList(List<Contract> contractList, String mainTag, String subTag){
        saveData(mainTag, subTag, contractList);
    }

    @SuppressWarnings("unchecked")
    private List<Contract> getSavedContractList(String mainTag, String subTag){
        List<Contract> savedContractList = (List<Contract>) readData(mainTag, subTag, new TypeToken<List<Contract>>() {}.getType());
        if(savedContractList == null){
            return new ArrayList<>();
        }
        return savedContractList;
    }

    public int getCurrentMode(){
        return currentMode;
    }

    public void setCurrentMode(int mode){
        // In currentMode, 0 means we are in pending_to_sign section,
        // 1 means we are in in progress section,
        // 2 means we are in history contracts section
        currentMode = mode;
    }

    public int getContractTrueStatus(Contract contract){
        Log.d(TAG, "getContractTrueStatus: " + "For contractID: " + contract.getContractId() +
                " we have currentRole: " + contract.getCurrentRole() + " with currentStatus: " + contract.getContractStatus() +
                " where offeror email is: " + contract.getOfferorEmailAddress() + " and offeree email is: " +
                contract.getOffereeEmailAddress() + ", where the current user email is: " + emailAddress);
        // Will return 0 if the temp_contract is in pending to sign section; 1 if it is in wait for finish section; 2 if it is in history section;
        // otherwise will return -1
        Integer[] commonWaitForFinish = {2, 8, 10, 11, 13, 18};
        Integer[] commonHistory = {3, 4, 6, 7, 14, 15, 16};
        Integer[] commonWaitForSigning = {};
        if(contract.getContractStatus() == 0){
            if(contract.getCurrentRole() == 0){
                return 0;
            } else {
                return 1;
            }
        } else if (contract.getContractStatus() == 1){
            if(contract.getCurrentRole() == 0){
                return 1;
            } else {
                return 0;
            }
        } else if (contract.getContractStatus() == 5) {
            if(contract.getCurrentRole() == 0){
                return 0;
            } else {
                return 1;
            }
        } else if (contract.getContractStatus() == 17){
            if(contract.getCurrentRole() == 0){
                return 0;
            } else {
                return 1;
            }
        } else if (contract.getContractStatus() == 12){
            if(contract.getCurrentRole() == 0){
                return 1;
            } else {
                return 0;
            }
        } else if (Arrays.asList(commonWaitForSigning).contains(contract.getContractStatus())) {
            return 0;
        } else if (Arrays.asList(commonWaitForFinish).contains(contract.getContractStatus())){
            return 1;
        } else if (Arrays.asList(commonHistory).contains(contract.getContractStatus())){
            return 2;
        }
        return -1;
    }

    public void addOrUpdateToCorrespondingContractList(Contract contract){
        Log.d(TAG, "addOrUpdateToCorrespondingContractList: Got new temp_contract with id: " + contract.getContractId() + " with status: " +
                contract.getContractStatus() + " with true status: " + getContractTrueStatus(contract));
        if(!getAllContractIDs().contains(contract.getContractId())){
            Log.d(TAG, "addOrUpdateToCorrespondingContractList: No such contract detected, going to add " + contract.getContractId() + " directly.");
            addAndSaveNewContractToCorrespondingContractList(contract);
        } else {
            List<Contract> contractList = getSavedPendingToSignContractList();
            int statusFromList = 0;
            int contractOnlineStatus = getContractTrueStatus(contract);
            if(!isContractInContractList(contract, contractList)){
                statusFromList = 1;
                contractList = getSavedInProgressContractList();
                if(!isContractInContractList(contract, contractList)){
                    statusFromList = 2;
                    contractList = getSavedHistoryContractList();
                }
            }
            int indexOfContract = getIndexOfContractInList(contract, contractList);
            Contract tempContract = contractList.get(indexOfContract);
            int contractLocalStatus = getContractTrueStatus(tempContract);

            Log.d(TAG, "addOrUpdateToCorrespondingContractList: contractLocalStatus: " + contractLocalStatus);
            if(statusFromList == 0){
                removeAndSaveContractFromPendingToSignContractListIfExist(contract);
            } else if (statusFromList == 1){
                removeAndSaveContractWaitForFinishContractListIfExist(contract);
            } else {
                removeAndSaveContractHistoryContractListIfExist(contract);
            }
            tempContract.syncStatus(contract);
            addOrUpdateToCorrespondingContractList(tempContract);
        }
    }

    private boolean isContractInContractList(Contract contract, List<Contract> contractList){
        boolean result = false;
        for(Contract c: contractList){
            if(c.getContractId().equals(contract.getContractId())){
                result = true;
                break;
            }
        }
        return result;
    }

    private int getIndexOfContractInList(Contract contract, List<Contract> contractList){
        // Will return -1 if not exist
        int index = -1;
        for(Contract c: contractList){
            if(c.getContractId().equals(contract.getContractId())){
                index = contractList.indexOf(c);
                break;
            }
        }
        return index;
    }

    public void updateSavedPendingToSignContractList(Contract contract){
        // This will check whether there is a same id temp_contract in current saved contractlist,
        // if there is, the new temp_contract will replace it.
        List<Contract> savedContractList = getSavedContractList("PendingToSignContractList", emailAddress);
        if(savedContractList != null){
            int indexToBeReplaced = -1;
            for(Contract contract_to_be_replaced: savedContractList){
                if(contract_to_be_replaced.getContractId().equals(contract.getContractId())){
                    indexToBeReplaced = savedContractList.indexOf(contract_to_be_replaced);
                    break;
                }
            }
            if(indexToBeReplaced != -1){
                savedContractList.remove(indexToBeReplaced);
                savedContractList.add(indexToBeReplaced, contract);
                saveContractList(savedContractList, "PendingToSignContractList", emailAddress);
            }
        }
    }

    public List<Contract> getSavedPendingToSignContractList(){
        pendingToSignContractList = getSavedContractList("PendingToSignContractList", emailAddress);
        return pendingToSignContractList;
    }

    public void addAndSaveNewContractToPendingToSignContractList(Contract contract){
        Log.d(TAG, "addAndSaveNewContractToPendingToSignContractList: " + contract.getContractId() + " going to be added to PendingToSign.");
        addAndSaveNewContractToContractList(contract, "PendingToSignContractList", emailAddress);
    }

    public void removeAndSaveContractFromPendingToSignContractListIfExist(Contract contract){
        removeAndSaveContractFromContractListIfExist(contract, "PendingToSignContractList", emailAddress);
    }

    private void removeAndSaveContractFromContractListIfExist(Contract contract, String mainTag, String subTag){
        List<Contract> contractList = getSavedContractList(mainTag, subTag);
        if(!contractList.isEmpty()){
            Log.d(TAG, "removeAndSaveContractFromContractListIfExist: " + "Going to delete " + contract.getContractId() + " from " + contractList);
            int indexToDelete = getIndexOfContractInList(contract, contractList);
            if(indexToDelete != -1){
                contractList.remove(indexToDelete);
            }
            saveContractList(contractList, mainTag, subTag);
        }
    }

    public List<Contract> getSavedInProgressContractList(){
        return getSavedContractList("InProgressContractList", emailAddress);
    }

    public void addAndSaveNewContractToInProgressContractList(Contract contract){
        Log.d(TAG, "addAndSaveNewContractToInProgressContractList: " + contract.getContractId() + " going to be added to InProgress.");
        addAndSaveNewContractToContractList(contract, "InProgressContractList", emailAddress);
    }

    public void removeAndSaveContractWaitForFinishContractListIfExist(Contract contract){
        removeAndSaveContractFromContractListIfExist(contract, "InProgressContractList", emailAddress);
    }

    public void removeAndSaveContractHistoryContractListIfExist(Contract contract){
        removeAndSaveContractFromContractListIfExist(contract, "HistoryContractList", emailAddress);
    }

    public List<Contract> getSavedHistoryContractList(){
        return getSavedContractList("HistoryContractList", emailAddress);
    }

    public void addAndSaveNewContractToHistoryContractList(Contract contract){
        Log.d(TAG, "addAndSaveNewContractToHistoryContractList: " + contract.getContractId() + " going to be added to History.");
        addAndSaveNewContractToContractList(contract, "HistoryContractList", emailAddress);
    }

    public boolean isContractExternal(Contract contract){
        return contract.getContractDescription().equals("__external");
    }

    public void addAndSaveNewContractToCorrespondingContractList(Contract contract){
        if(getContractTrueStatus(contract) == 0){
            addAndSaveNewContractToPendingToSignContractList(contract);
        } else if (getContractTrueStatus(contract) == 1){
            addAndSaveNewContractToInProgressContractList(contract);
        } else {
            addAndSaveNewContractToHistoryContractList(contract);
        }
    }

    public String getEmailAddress(){
        emailAddress = (String) readData("SelfEmail", "Address", new TypeToken<String>() {}.getType());
        Log.d(TAG, "getEmailAddress: Here is the current login email: " + emailAddress);
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress){
        this.emailAddress = emailAddress;
        saveData("SelfEmail", "Address", emailAddress);
    }

    public void savePendingToSignContractList(List<Contract> contractList){
        saveData("PendingToSignContractList", emailAddress, contractList);
    }

    public void saveWaitForFinishContractList(List<Contract> contractList){
        saveData("InProgressContractList", emailAddress, contractList);
    }

    @SuppressWarnings("unchecked")
    private void addAndSaveNewContractToContractList(Contract contract, String mainTag, String subTag){
        List<Contract> savedContractList = (List<Contract>) readData(mainTag, subTag, new TypeToken<List<Contract>>() {}.getType());
        if(savedContractList == null){
            savedContractList = new ArrayList<>();
        }
        if(!isContractInContractList(contract, savedContractList)){
            savedContractList.add(0, contract);
            saveData(mainTag, subTag, savedContractList);
        }
    }

    public void saveToken(String token){
        saveData(TokenMainTag, TokenSubTag, token);
    }

    public String getToken(){
        String savedToken = (String) readData(TokenMainTag, TokenSubTag, new TypeToken<String>() {}.getType());
        if(savedToken == null){
            return "";
        }
        Log.d(TAG, "getToken: the token is: " + savedToken);
        return savedToken;
    }

    public PublicKey getPublicKey(){
        //initUserKeyManagement();
        return publicKey;
    }

    private PrivateKey getPrivateKey(){
        initUserKeyManagement();
        return privateKey;
    }

    public String storeScreenshotSign(Bitmap bitmap, String filename){

        Log.d(TAG, "storeScreenshotSign: Trying to sign " + filename);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        // Myles: The quality above should be changed along with storeScreenshot() in Screenshot.
        try {
            byte [] signedData = generateSign(byteArrayOutputStream.toByteArray(), getPrivateKey());
            return writeStringToFile(Base64.encodeToString(signedData, 0), filename,
                    "/" + getCurrentViewingContract().getContractId() + "/");
        } catch (Exception e){
            Log.d(TAG, "storeScreenshotSign: Error happened for " + filename);
            e.printStackTrace();
        }
        return "";

    }

    public String getScreenshotSign(Bitmap bitmap){
        // Return the Signature of the bitmap of quality 90 in UTF-8 format String
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        // Myles: The quality above should be changed along with storeScreenshot() in Screenshot.
        try {
            byte [] signedData = generateSign(byteArrayOutputStream.toByteArray(), getPrivateKey());
            return new String(signedData, StandardCharsets.UTF_8);
        } catch (Exception e){
            Log.d(TAG, "getScreenshotSign: Error happened for signing");
            e.printStackTrace();
        }
        return "";
    }

    private Bitmap getBitmapFromPath(String path){
        return BitmapFactory.decodeFile(path);
    }

    public String storeScreenshotSignInCustomLocation(Bitmap bitmap, String customLocation){

        Log.d(TAG, "storeScreenshotSignInCustomLocation: Trying to sign " + customLocation);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        // Myles: The quality above should be changed along with storeScreenshot() in Screenshot.
        try {
            byte [] signedData = generateSign(byteArrayOutputStream.toByteArray(), getPrivateKey());
            return writeStringToFile(Base64.encodeToString(signedData, 0), customLocation);
        } catch (Exception e){
            Log.d(TAG, "storeScreenshotSignInCustomLocation: Error happened for " + customLocation);
            e.printStackTrace();
        }
        return "";

    }

    public String storeSignatureOfByteArrayInCustomLocation(byte[] bytes, String customLocation){

        Log.d(TAG, "storeSignatureOfByteArrayInCustomLocation: Trying to sign " + customLocation);
        try {
            byte [] signedData = generateSign(bytes, getPrivateKey());
            return writeStringToFile(Base64.encodeToString(signedData, 0), customLocation);
        } catch (Exception e){
            Log.d(TAG, "storeSignatureOfByteArrayInCustomLocation: Error happened for " + customLocation);
            e.printStackTrace();
        }
        return "";

    }

    public String storeScreenshotSign(Bitmap bitmap, String filename, int quality){

        Log.d(TAG, "storeScreenshotSign: Trying to sign " + filename);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
        // Myles: The quality above should be changed along with storeScreenshot() in Screenshot.
        try {
            byte [] signedData = generateSign(byteArrayOutputStream.toByteArray(), getPrivateKey());
            return writeStringToFile(Base64.encodeToString(signedData, 0), filename,
                    "/" + getCurrentViewingContract().getContractId() + "/");
        } catch (Exception e){
            Log.d(TAG, "storeScreenshotSign: Error happened for " + filename);
            e.printStackTrace();
        }
        return "";

    }

    private byte [] generateSign(byte[] data, PrivateKey privateKey) throws Exception {
        Signature s1 = Signature.getInstance("SHA256withRSA");

        s1.initSign(privateKey);
        s1.update(data);

        return s1.sign();
    }

    private String getStringFromPublicKey(PublicKey publicKey){
        return new String(Base64.encode(publicKey.getEncoded(), 0));
    }

    private String getStringFromPrivate(PrivateKey privateKey){
        return new String(Base64.encode(privateKey.getEncoded(), 0));
    }

    private PublicKey getPublicKeyFromString(String keystr) throws Exception{
        byte [] encoded = Base64.decode(keystr, 0);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePublic(keySpec);
    }

    private PrivateKey getPrivateKeyFromString(String keystr) throws Exception{
        byte [] encoded = Base64.decode(keystr, 0);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePrivate(keySpec);
    }

    public void initUserKeyManagement(){
        //generateAndSaveKeyPair();
        // This above line is just for demo, since it will refresh the public
        // and private key each time the function is called.
        loadSavedKeyPair();
        if(publicKey == null | privateKey == null){
            generateAndSaveKeyPair();
            loadSavedKeyPair();
        }
    }

    private void loadSavedKeyPair(){
        try {
            publicKey = getPublicKeyFromString((String) readData("KeyPairPublic", "publicKey", new TypeToken<String>() {}.getType()));
            privateKey = getPrivateKeyFromString((String) readData("KeyPairPrivate", "privateKey", new TypeToken<String>() {}.getType()));
        } catch (Exception e){
            e.printStackTrace();
        }

        if(publicKey != null){
            savePublicKeyToFile();
        }

        if(publicKey != null & privateKey !=null){
            Log.d("Myles ", "loadSavedKeyPair: Successfully load publicKey: " + getStringFromPublicKey(publicKey));
            Log.d("Myles ", "loadSavedKeyPair: Successfully load privateKey: " + getStringFromPrivate(privateKey));
        }
    }

    private void savePublicKeyToFile(){
        String pubKeyToSave = "-----BEGIN PUBLIC KEY-----\n" + getStringFromPublicKey(publicKey) + "-----END PUBLIC KEY-----\n";
        writeStringToFile(pubKeyToSave, "publicKey.pem", "");
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        // External Credit: http://www.java2s.com/Code/Java/File-Input-Output/ConvertInputStreamtoString.htm
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public String getStringFromFile(String path) throws Exception {
        File fileToRead = new File(path);
        FileInputStream fileInputStream = new FileInputStream(fileToRead);
        String result = convertStreamToString(fileInputStream);
        fileInputStream.close();
        return result;
    }

    private String writeStringToFile(String data, String pathOfFile) {
        // The subFolderName is kept for the use of generating signatures for contracts
        try {
            File tempFile = new File(pathOfFile); //this works
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(tempFile));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
            return tempFile.getAbsolutePath();
        }
        catch (IOException e) {
            Log.e("Exception", "writeStringToFile: File write failed: " + e.toString());
        }
        return "";
    }

    public String writeStringToFile(String data, String fileName, String subFolderName) {
        // The subFolderName is kept for the use of generating signatures for contracts
        try {
            File dirs = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/"
                    + subFolderName);
            dirs.mkdirs();

            File tempFile = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/"
                    + subFolderName, fileName); //this works
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(tempFile));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
            return tempFile.getAbsolutePath();
        }
        catch (IOException e) {
            Log.e("Exception", "writeStringToFile: File write failed: " + e.toString());
        }
        return "";
    }

    public String writeStringToExistingFile(String data, String existingFilePath) {
        // The subFolderName is kept for the use of generating signatures for contracts
        try {
            File existingFile = new File(existingFilePath);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(existingFile));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
            return existingFile.getAbsolutePath();
        }
        catch (IOException e) {
            Log.e("Exception", "writeStringToFile: File write failed: " + e.toString());
        }
        return "";
    }

    public void writeLogToFile(String log, String fileName, String path){
        try {
            File dir = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/debug_log/"
                    + path);
            dir.mkdirs();
            File tempFile = new File(dir, fileName); //this works
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(tempFile));
            outputStreamWriter.write(log);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "writeLogToFile: File write failed: " + e.toString());
        }
    }

    private void generateAndSaveKeyPair(){
        KeyPairGenerator generator = null;

        try {
            generator = KeyPairGenerator.getInstance("RSA");
            //Signature s2 = Signature.getInstance("SHA256withRSA");
        } catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }

        KeyPair pair = null;

        if(generator != null){
            pair = generator.generateKeyPair();
        }

        if(pair != null){
            try{
                saveData("KeyPairPublic", "publicKey", getStringFromPublicKey(pair.getPublic()));
                saveData("KeyPairPrivate", "privateKey", getStringFromPrivate(pair.getPrivate()));
            } catch (NullPointerException e){
                e.printStackTrace();
            }
        }
    }

    public List<String> getAllContractIDs(){
        List<String> contractIDs = new ArrayList<>();
        contractIDs.addAll(getAllContractIDsFromList(getSavedPendingToSignContractList()));
        contractIDs.addAll(getAllContractIDsFromList(getSavedInProgressContractList()));
        contractIDs.addAll(getAllContractIDsFromList(getSavedHistoryContractList()));
        return contractIDs;
    }

    public boolean isContractExist(String contractID){
        return getAllContractIDs().contains(contractID);
    }

    private List<String> getAllContractIDsFromList(List<Contract> contractList){
        List<String> result = new ArrayList<>();
        for(Contract contract: contractList){
            result.add(contract.getContractId());
        }
        return result;
    }

    private void removeContractFromCorrespondingContractList(Contract contract){
        // This is not a good solution since it is simply trying to delete the temp_contract in all categories
        removeAndSaveContractFromPendingToSignContractListIfExist(contract);
        removeAndSaveContractWaitForFinishContractListIfExist(contract);
        removeAndSaveContractHistoryContractListIfExist(contract);
    }

    public void removeContractFromCorrespondingContractList(String contractID){
        Log.d(TAG, "removeContractFromCorrespondingContractList: Going to delete " + contractID);
        Contract contract = new Contract("temp_fo_delete");
        contract.setContractId(contractID);
        removeContractFromCorrespondingContractList(contract);
    }

    public Contract newContractByInfo(List<String> contractInfo, String contractID){
        // The format should be
        // 0: contract_name; 1: contract_description; 2: contract_status; 3: page_count; 4: offeror_email;
        // 5: offeree_email; 6: contract_confirmstatus; 7: is_contract_created_by_tabellion;
        // 8: signed_pages_string
        Contract contract = new Contract(contractInfo.get(0));
        contract.setContractId(contractID);
        contract.setContractDescription(contractInfo.get(1));
        contract.setContractStatus(Integer.valueOf(contractInfo.get(2)));
        contract.setTotalImageNums(Integer.valueOf(contractInfo.get(3)));
        contract.addRangePendingToDownloadPagesNum(1, Integer.valueOf(contractInfo.get(3)));
        Log.d(TAG, "newContractByInfo: " + "Creating temp_contract wiht ID: " + contractID + " and the offerorEmail is: " +
                contractInfo.get(4) + ", where the offereeEmail is: " + contractInfo.get(5));
        contract.setOfferorEmailAddress(contractInfo.get(4));
        contract.setOffereeEmailAddress(contractInfo.get(5));
        contract.setConfirmStatus(contractInfo.get(6));
        if(contractInfo.get(7).equals("true")){
            Log.d(TAG, "newContractByInfo: creating a contract created by tabellion");
            contract.setIsContractCreatedByTabellion(true);
        }
        if(contractInfo.size() >= 9 && !contractInfo.get(8).equals("None")){
            String[] signedPagesStringArrray = contractInfo.get(8).split("%");
            for(String signed_page_num_str: signedPagesStringArrray){
                contract.addToSignedPages(Integer.valueOf(signed_page_num_str));
            }
        }
        Log.d(TAG, "newContractByInfo: " + contractInfo);
        if(contractInfo.size() >= 10 && !contractInfo.get(9).equals("None")){
            contract.setRevistedNumCount(Integer.valueOf(contractInfo.get(9)));
        }
        if(contractInfo.get(5).equals(emailAddress)){
            Log.d(TAG, "newContractByInfo: Setting " + contractID + "'s current role as offeree");
            contract.setCurrentRole(1);
        }
        return contract;
    }

    public File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = getCurrentUserPhotoFileName();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(storageDir, imageFileName);
        /*
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        */
        return image;
    }

    public String getCurrentUserPhotoFileName(){
        return getUserPhotoFileNameByEmail(emailAddress);
    }

    public Uri getCurrentUserPhotoUri(){
        return Uri.parse(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + getCurrentUserPhotoFileName());
    }

    private Bitmap getImageFromLocal(String filepath){
        File imageFile = new File(filepath);
        Log.d(TAG, "getImageFromLocal: the path is: " + imageFile.getAbsolutePath() + " with the size of: " + imageFile.getTotalSpace());
        return BitmapFactory.decodeFile(imageFile.getPath());
    }

    public String getUserPhotoFileNameByEmail(String emailAddress){
        return "User_photo_" + emailAddress + ".jpg";
    }

    public Bitmap getContractIDOppositeUserPhotoLocal(String contractID, String emailAddress){
        return getImageFromLocal(Uri.parse(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" +
                contractID + "/" + getUserPhotoFileNameByEmail(emailAddress)).toString());
    }

    public Bitmap getCurrentUserPhoto(){
        return getImageFromLocal(getCurrentUserPhotoUri().toString());
    }

    public void setCurrentUserFirstName(String firstName){
        saveData("userFirstName", emailAddress, firstName);
    }

    public String getCurrentUserFirstName(){
        String firstName = (String) readData("userFirstName", emailAddress, new TypeToken<String>() {}.getType());
        if(firstName == null){
            return "";
        }
        return firstName;
    }

    @SuppressWarnings("unchecked")
    public void addEmailToLocalRegisteredRecord(String emailAddress){
        HashSet<String> localRecordForRegistering = (HashSet<String>) readData("recordForRegistering",
                "localRegisteredRecord", new TypeToken<HashSet<String>>() {}.getType());
        if(localRecordForRegistering == null){
            localRecordForRegistering = new HashSet<>();
        }
        localRecordForRegistering.add(emailAddress);
        saveData("recordForRegistering", "localRegisteredRecord", localRecordForRegistering);
    }

    @SuppressWarnings("unchecked")
    public HashSet<String> getLocalRegisteredRecord(){
        HashSet<String> localRecordForRegistering = (HashSet<String>) readData("recordForRegistering",
                "localRegisteredRecord", new TypeToken<HashSet<String>>() {}.getType());
        if(localRecordForRegistering == null){
            localRecordForRegistering = new HashSet<>();
        }
        return localRecordForRegistering;
    }

    @SuppressWarnings("unchecked")
    public void addEmailToReRegisterRequiredRecord(String emailAddress){
        HashSet<String> localRecordForRegistering = (HashSet<String>) readData("recordForReRegistering",
                "reRegisterRequiredRecord", new TypeToken<HashSet<String>>() {}.getType());
        if(localRecordForRegistering == null){
            localRecordForRegistering = new HashSet<>();
        }
        localRecordForRegistering.add(emailAddress);
        saveData("recordForReRegistering", "reRegisterRequiredRecord", localRecordForRegistering);
    }

    @SuppressWarnings("unchecked")
    public boolean removeEmailFromReRegisterRequiredRecord(String emailAddress){
        boolean result = false;
        HashSet<String> localRecordForRegistering = (HashSet<String>) readData("recordForReRegistering",
                "reRegisterRequiredRecord", new TypeToken<HashSet<String>>() {}.getType());
        if(localRecordForRegistering != null){
            result = localRecordForRegistering.remove(emailAddress);
            saveData("recordForReRegistering", "reRegisterRequiredRecord", localRecordForRegistering);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public HashSet<String> getReRegisterRequiredRecord(){

        HashSet<String> localRecordForRegistering = (HashSet<String>) readData("recordForReRegistering",
                "reRegisterRequiredRecord", new TypeToken<HashSet<String>>() {}.getType());
        if(localRecordForRegistering == null){
            localRecordForRegistering = new HashSet<>();
        }
        return localRecordForRegistering;
    }

    public void setIsReRegistering(boolean isReRegistering){
        this.isReRegistering = isReRegistering;
    }

    public boolean isReRegistering(){
        return isReRegistering;
    }

    public String getCurrentTimeInterval(){
        Date currentDate = new Date();
        String result = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date(currentDate.getTime() - 500))
                + "to" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date(currentDate.getTime() + 500));
        Log.d(TAG, "getCurrentTimeInterval: new time interval generated: " + result);
        return result;
    }

    public boolean isInternetConnected(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public void showWaitingForNetWorkDialog(){
        Log.d(TAG, "showWaitingForNetWorkDialog: No network detected! Going to show dialog.");
        isWaitingForNetWork = true;
        Intent intent = new Intent(this, LauncherActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void hideWaitingForNewWorkDialog(){
        Log.d(TAG, "hideWaitingForNewWorkDialog: Network detected! Going to hide dialog.");
        isWaitingForNetWork = false;
    }

    public boolean isWaitingForNetWork(){
        return isWaitingForNetWork;
    }

    public void setIsWaitingForNetWork(boolean isWaitingForNetWork){
        this.isWaitingForNetWork = isWaitingForNetWork;
    }

    public void startInternetConnectionWatchDog(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                int counter = 0;
                while (true){
                    if(counter == 3){
                        showWaitingForNetWorkDialog();
                        while (!isInternetConnected());
                        hideWaitingForNewWorkDialog();
                        counter = 0;
                    } else {
                        try {
                            Thread.sleep(500);
                            if(isInternetConnected()){
                                counter = 0;
                            } else {
                                ++counter;
                            }
                        } catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    public boolean isRefreshingOnMainActivity() {
        return isRefreshingOnMainActivity;
    }

    public void setIsRefreshingOnMainActivity(boolean isRefreshingOnMainActivity){
        this.isRefreshingOnMainActivity = isRefreshingOnMainActivity;
    }

    public boolean isTransitionAndFreezingOn(){
        return transitionAndFreezingSwitch;
    }

    public void setTransitionAndFreezingSwitch(boolean transitionAndFreezingSwitch){
        this.transitionAndFreezingSwitch = transitionAndFreezingSwitch;
    }

    private void saveData(String mainTag, String subTag, Object object){

        // The parameter subTag should be deleted

        Gson gson = new Gson();
        String dataToBeSaved = gson.toJson(object);
        SharedPreferences.Editor editor = getSharedPreferences(mainTag, MODE_PRIVATE).edit();
        editor.clear();
        editor.putString(subTag, dataToBeSaved);
        editor.commit();
    }

    private Object readData(String mainTag, String subTag, Type objectType){

        Gson gson = new Gson();
        SharedPreferences sharedPreferences = getSharedPreferences(mainTag, MODE_PRIVATE);
        String dataToBeRead = sharedPreferences.getString(subTag, "");
        if(!dataToBeRead.isEmpty()){
            return gson.fromJson(dataToBeRead, objectType);
        }
        return null;
    }
}
