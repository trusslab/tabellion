package edu.uci.ics.charm.tabellion;

/*
Created Date: 01/23/2019
Created By: Myles Liu
Last Modified: 03/22/2020
Last Modified By: Myles Liu
Notes:
        1. For currentRole, 0 means offeror, 1 means offeree.  (Default is 0)
        2. For contractStatus, 0 means wait for offeror to sign, 1 means wait for offeree to sign,
            2 means wait for normally finishing, 3 means revoked by offeror, 4 means rev'/oked by offeror (abandoned),
            5 means revision needed by offeree, 6 means aborted by offeree, 7 means normally
            finished by both offeror and offeree, 8 means waiting for revoke, 9 means waiting for aborted by offeree,
            10 means verifying offeror's signature, 11 means verifying offeree's signature,
            12 means an external temp_contract wait for offeree signing,
            13 means an external temp_contract is being verified for offeree,
            14 means an external temp_contract does not pass verification for offeree (abandoned),
            15 means an external temp_contract passes verification for offeree,
            16 means an external temp_contract is in unknown status[like an unknown error],
            17 means an external temp_contract wait for offeror signing,
            18 means an external temp_contract is being verified for offeror
            (Default is 0)
        3. For confirmStatus, 0 means not confirmed by both persons, 1 menas confirmed by offeree,
            2 means confirmed by offeror.   (Default is 0)
        4. The Copy Constructor should be updated each time a variable definition is changed in the class.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Contract {

    private static final String TAG = "Contract ";
    private String contractId = "null_waiting_for_contractID";
    private String contractName;
    private int currentRole = 0;
    private Date createdDate = new Date();
    private int contractStatus = 0;
    private boolean isOpened = true;
    private long latestTimeStamp = System.currentTimeMillis();
    private String offerorEmailAddress;
    private String offereeEmailAddress;
    private String contractDescription;
    private Bitmap contractPreviewImage = null;
    private Set<Integer> pendingToDownloadPagesNum = new HashSet<>();
    private Set<Integer> pendingToDownalodReviewPagesNum = new HashSet<>();
    private boolean isDownloaded = false;
    private boolean isDownloadFailed = false;
    private File contractFile = null;
    private int totalImageNums = 0;
    private int confirmStatus = 0;
    private String lastActionTimeInterval = "";
    private int transitionTime = 1000;
    private boolean isSwipeBackEnabled = true;
    private HashMap<Integer, HashMap<byte[], String>> contractScreenshots = new HashMap<>();  // currently for external contract only
    // First Integer is for page num (Start from 0)
    // First byte[] is for screenshot
    // First String is for according screenshot's signature

    private ArrayList<String> originalScreenshots = new ArrayList<>();  // currently for external contract only

    private int currentMode = 0;  // currently for external contract only
    // This currentMode is for deciding either the contract is in signing mode or revision mode
    // currently for external contract only
    // currentMode: 0 for waiting for continue signing
    // 1 for revision requested by the offeree (should be used along with contract status to decide
    // either to let offeror update the contract or let offeree continuing signing)

    private HashMap<Integer, String> originalContractText = new HashMap<>();    // currently for external contract only
    // Integer for content ID
    // String for actual text

    private HashMap<Integer, byte[]> originalContractImage = new HashMap<>();   // currently for external contract only
    // For storing images being shown in the contract (in ImageView)
    // Integer is for content ID
    // Bitmap is for the actual image

    private HashMap<Integer, HashMap<String, String>> offereeRevisions = new HashMap<>();  // currently for external contract only
    // This is for storing all revisions requested by the offeree
    // First Integer is the content ID
    // First String is the start and end sign, which means if start is 3, end is 8, it will be "3:8" (comment Id)
    // Second String is the revision comment

    private HashMap<Integer, HashMap<String, HashMap<Bitmap, String>>> commentScreenshots =
            new HashMap<>();  // currently for external contract only
    // First Integer is the content ID
    // First String is the start and end sign, which means if start is 3, end is 8, it will be "3:8" (comment Id)
    // First Bitmap is for screenshot
    // Second String is for according screenshot's signature

    private HashMap<Integer, String> originalContractContentKey = new HashMap<>();
    // currently for external contract only
    // First Integer is the content ID
    // First String is the corresponding String key

    private int revisionCount = 0;  // currently for external contract only
    private int offerorOriginalScreenshotCount = 0;  // currently for external contract only
    private int offereeOriginalScreenshotCount = 0;  // currently for external contract only

    private boolean isContractCreatedByTabellion = false;

    private ArrayList<Integer> signedPages = new ArrayList<>();     // Counted from 1

    private int revistedNumCount = 0;   // How many times the contract has been revised (new Tabellion used, not for external)

    public Contract(String contractName){
        createdDate.setTime(System.currentTimeMillis());
        this.contractName= contractName;
        Random r = new Random();
        setContractToTestDefault(); // For test only
    }

    public Contract(Contract contractForCopy){
        // Note that this should be updated each time a variable definition changed
        this.contractId = contractForCopy.contractId;
        this.contractName = contractForCopy.contractName;
        this.currentRole = contractForCopy.currentRole;
        this.createdDate = contractForCopy.createdDate;
        this.contractStatus = contractForCopy.contractStatus;
        this.isOpened = contractForCopy.isOpened;
        this.latestTimeStamp = contractForCopy.latestTimeStamp;
        this.offerorEmailAddress = contractForCopy.offerorEmailAddress;
        this.offereeEmailAddress = contractForCopy.offereeEmailAddress;
        this.contractDescription = contractForCopy.contractDescription;
        this.contractPreviewImage = Bitmap.createBitmap(contractForCopy.contractPreviewImage);
        this.pendingToDownloadPagesNum.addAll(contractForCopy.pendingToDownloadPagesNum);
        this.pendingToDownalodReviewPagesNum.addAll(contractForCopy.pendingToDownalodReviewPagesNum);
        this.isDownloaded = contractForCopy.isDownloaded;
        this.isDownloadFailed = contractForCopy.isDownloadFailed;
        this.contractFile = new File(contractForCopy.contractFile.getAbsolutePath());
        this.totalImageNums = contractForCopy.totalImageNums;
        this.confirmStatus = contractForCopy.confirmStatus;
        this.lastActionTimeInterval = contractForCopy.lastActionTimeInterval;
        this.transitionTime = contractForCopy.transitionTime;
        this.isSwipeBackEnabled = contractForCopy.isSwipeBackEnabled;
        this.contractScreenshots.putAll(contractForCopy.contractScreenshots);
        this.currentMode = contractForCopy.currentMode;
        this.originalContractText.putAll(contractForCopy.originalContractText);
        this.originalContractImage.putAll(contractForCopy.originalContractImage);
        this.offereeRevisions.putAll(contractForCopy.offereeRevisions);
        this.commentScreenshots.putAll(contractForCopy.commentScreenshots);
        this.originalContractContentKey.putAll(contractForCopy.originalContractContentKey);
        this.revisionCount = contractForCopy.revisionCount;
        this.offerorOriginalScreenshotCount = contractForCopy.offerorOriginalScreenshotCount;
        this.offereeOriginalScreenshotCount = contractForCopy.offereeOriginalScreenshotCount;
        this.isContractCreatedByTabellion = contractForCopy.isContractCreatedByTabellion;
        this.signedPages = contractForCopy.signedPages;
        this.revistedNumCount = contractForCopy.revistedNumCount;
    }

    private void setContractToTestDefault(){
        // This function is for test only
        offerorEmailAddress = "lyx981mike@gmail.com";
        offereeEmailAddress = "yuxil11@uci.edu";
        contractDescription = "This position is reserved for the description of this temp_contract." +
                "Normally this description should not be longer than 50 words.";
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public void setCurrentRole(int currentRole) {
        this.currentRole = currentRole;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public void setContractStatus(int contractStatus) {
        this.contractStatus = contractStatus;
        refreshTimeStamp();
    }

    public void setContractFile(File contractFile){
        this.contractFile = contractFile;
    }

    public Bitmap getImage(String imageCount){
        File imageFile = new File(MainActivity.sContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                + "/" +  contractId + "/Ndoc-" + imageCount + ".png");
        return BitmapFactory.decodeFile(imageFile.getPath());
    }

    public Bitmap getImage(String imageCount, Context context){
        File imageFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                + "/" +  contractId + "/Ndoc-" + imageCount + ".png");
        return BitmapFactory.decodeFile(imageFile.getPath());
    }

    public void downloadPages(String start, String end, Connection connection, Handler handler){
        Log.d(TAG, "DownloadPages: " + "Going to download from " + start + " to " + end);
        for(int i = Integer.valueOf(start); i <= Integer.valueOf(end); ++i){
            Handler handler_for_remove_pending_download_num = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    if(msg.getData().getBoolean("is_success")){
                        pendingToDownloadPagesNum.remove(msg.getData().getInt("page_num"));
                        Log.d("DownloadPages ", "Remaining: " + pendingToDownloadPagesNum);
                    } else {
                        Log.d(TAG, "Download failed when downloading: " + "Ndoc-" +
                                msg.getData().getInt("page_num") + ".png");
                        setDownloadFailed(true);
                    }
                    return false;
                }
            });
            downloadPage("Ndoc-" + i + ".png", connection, handler_for_remove_pending_download_num, i);
        }
        downloadLastPage(connection, handler);
    }

    public void downloadFirstPage(Connection connection, Handler handler){
        pendingToDownloadPagesNum.remove(0);
        downloadPage("Ndoc-1.png", connection, handler, 1);
    }

    private void downloadLastPage(Connection connection, Handler handler){
        // 0 means last page
        downloadPage("Nlast-1.png", connection, handler, 0);
    }

    private void downloadPage(String fileName, Connection connection, Handler handler, int page_num){
        new Thread(connection.new DownloadContractImage(fileName, contractId, handler, page_num, revistedNumCount)).start();
    }

    public void addRangePendingToDownloadPagesNum(int start, int end){
        for(int i = start; i <= end; ++i){
            pendingToDownloadPagesNum.add(i);
            pendingToDownalodReviewPagesNum.add(i);
        }
    }

    public Set<Integer> getPendingToDownloadPagesNum(){
        return pendingToDownloadPagesNum;
    }

    public void removePendingToDownloadPagesNum(int index){
        pendingToDownloadPagesNum.remove(index);
    }

    public int getCurrentRole() {
        return currentRole;
    }

    public int getContractStatus() {
        return contractStatus;
    }

    public Date getCreatedDate() {
        return createdDate;
    }


    public void syncStatus(Contract contractUsedToSync){
        // Should only be used for internal contract
        // Direction: this: local contract; contractUsedToSync: online contract
        Log.d(TAG, "syncStatus: This function should only be called for internal use.");
        setContractDescription(contractUsedToSync.getContractDescription());
        setContractStatus(contractUsedToSync.getContractStatus());
        if(!isDownloaded){
            setTotalImageNums(contractUsedToSync.getTotalImageNums());
            addRangePendingToDownloadPagesNum(2, contractUsedToSync.getTotalImageNums());
        }
        setOfferorEmailAddress(contractUsedToSync.getOfferorEmailAddress());
        setOffereeEmailAddress(contractUsedToSync.getOffereeEmailAddress());
        setConfirmStatus(contractUsedToSync.getConfirmStatus());
        setLastActionTimeInterval(contractUsedToSync.getLastActionTimeInterval());
        setCurrentRole(contractUsedToSync.getCurrentRole());
        contractName = contractUsedToSync.getContractName();
        setSignedPages(contractUsedToSync.getSignedPages());
        if(contractScreenshots == null){
            contractScreenshots = new HashMap<>();
        }
        if(contractScreenshots.size() < contractUsedToSync.contractScreenshots.size()){
            contractScreenshots.putAll(contractUsedToSync.contractScreenshots);
        }
        if(isContractCreatedByTabellion != contractUsedToSync.isContractCreatedByTabellion){
            isContractCreatedByTabellion = true;
        }

        // If revision is made, need reDownloaded
        Log.d(TAG, "syncStatus: comparing local revistedNumCount: " +
                this.revistedNumCount + " with online revistedNumCount: " +
                contractUsedToSync.revistedNumCount);
        if(this.revistedNumCount != contractUsedToSync.revistedNumCount){
            this.revistedNumCount = contractUsedToSync.revistedNumCount;
            this.isDownloaded = false;
            setTotalImageNums(contractUsedToSync.getTotalImageNums());
            Log.d(TAG, "syncStatus: totalImageNums: " + contractUsedToSync.getTotalImageNums());
            addRangePendingToDownloadPagesNum(1, getTotalImageNums());
        }

    }

    private void refreshTimeStamp(){
        latestTimeStamp = System.currentTimeMillis();
    }

    public long getLatestTimeStamp(){
        return latestTimeStamp;
    }

    public String getContractId(){
        return contractId;
    }

    public String getContractName(){
        return contractName;
    }

    public String getOfferorEmailAddress(){
        return offerorEmailAddress;
    }

    public String getOffereeEmailAddress() {
        return offereeEmailAddress;
    }

    public void setContractPreviewImage(Bitmap contractPreviewImage) {
        this.contractPreviewImage = contractPreviewImage;
    }

    public Bitmap getContractPreviewImage() {
        return contractPreviewImage;
    }

    public void setContractDescription(String contractDescription) {
        this.contractDescription = contractDescription;
    }

    public String getContractDescription() {
        return contractDescription;
    }

    public void setOffereeEmailAddress(String offereeEmailAddress) {
        this.offereeEmailAddress = offereeEmailAddress;
    }

    public void setOfferorEmailAddress(String offerorEmailAddress) {
        this.offerorEmailAddress = offerorEmailAddress;
    }

    public void setIsDownloaded(boolean isDownloaded){
        this.isDownloaded = isDownloaded;
    }

    public Boolean isDownloaded(){
        return isDownloaded;
    }

    public Boolean isDownloadFailed(){
        return isDownloadFailed;
    }

    private void setDownloadFailed(Boolean downloadFailed){
        isDownloadFailed = downloadFailed;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null){
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        Contract temp_obj = (Contract) obj;
        return contractId == temp_obj.getContractId();
    }

    public void setTotalImageNums(int i){
        totalImageNums = i;
    }

    public int getTotalImageNums() {
        return totalImageNums;
    }

    public int getConfirmStatus(){
        return confirmStatus;
    }

    public void setConfirmStatus(int confirmStatus){
        this.confirmStatus = confirmStatus;
    }

    public void setConfirmStatus(String confirmStatus){
        this.confirmStatus = Integer.valueOf(confirmStatus);
    }

    public void setLastActionTimeInterval(String lastActionTimeInterval) {
        this.lastActionTimeInterval = lastActionTimeInterval;
    }

    public String getLastActionTimeInterval(){
        return lastActionTimeInterval;
    }

    public void setTransitionTime(int transitionTime){
        this.transitionTime = transitionTime;
    }

    public Integer getTransitionTime(){
        return transitionTime;
    }

    public void setIsSwipeBackEnabled(boolean isSwipeBackEnabled){
        this.isSwipeBackEnabled = isSwipeBackEnabled;
    }

    public boolean isSwipeBackEnabled(){
        return isSwipeBackEnabled;
    }

    public Set<Integer> getPendingToDownalodReviewPagesNum(){
        return pendingToDownalodReviewPagesNum;
    }

    public float getDownloadProgress(){
        /*
        return ((float)1.0 - (((float) pendingToDownloadPagesNum.size() + (float)pendingToDownalodReviewPagesNum.size())
                / ((totalImageNums) * (float) 2.0)));
                */
        return ((float)1.0 - ((float) pendingToDownloadPagesNum.size()
                / totalImageNums));
    }

    public boolean isExternalContract(){
        return contractStatus >= 12 && contractStatus <= 18;
    }

    public int getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(int currentMode) {
        this.currentMode = currentMode;
    }
    public HashMap<Integer, HashMap<String, String>> getOffereeRevisions() {
        return offereeRevisions;
    }

    public void addRevisionToOffereeRevisions(Integer contentId, Integer start, Integer end, String revisionComment){
        HashMap<String, String> pageRevision = null;
        if(offereeRevisions.containsKey(contentId)){
            pageRevision = offereeRevisions.get(contentId);
        }
        if(pageRevision == null){
            pageRevision = new HashMap<>();
        }
        pageRevision.put(String.valueOf(start) + ":" + String.valueOf(end), revisionComment);
        offereeRevisions.put(contentId, pageRevision);
    }

    public void removeRevisionFromOffereeRevision(Integer contentId, Integer start, Integer end){
        HashMap<String, String> pageRevision = null;
        if(offereeRevisions.containsKey(contentId)){
            pageRevision = offereeRevisions.get(contentId);
        }
        if(pageRevision != null){
            pageRevision.remove(String.valueOf(start) + ":" + String.valueOf(end));
            offereeRevisions.put(contentId, pageRevision);
        }
    }

    public void setSignedImage(Integer index, Bitmap screenshot, String signature){
        setSignedImage(index, getEncodedBitmap(screenshot), signature);
    }

    public void setSignedImage(Integer index, byte[] screenshot, String signature){
        HashMap<byte[], String> sign = new HashMap<>();
        sign.put(screenshot, signature);
        contractScreenshots.remove(index);
        contractScreenshots.put(index, sign);
    }

    private String tryGetFirstValueInHashMap(HashMap<byte[], String> hashMap){
        for(String result: hashMap.values()){
            return result;
        }
        return "";
    }

    public int getNumOfSignedImages(){
        int result = 0;
        for(int pageNum: contractScreenshots.keySet()){
            if(isPageSigned(pageNum)){
                ++result;
            }
        }
        return result;
    }

    public boolean isPageSigned(int pageNum){
        return contractScreenshots.containsKey(pageNum) && contractScreenshots.get(pageNum) != null &&
                !tryGetFirstValueInHashMap(contractScreenshots.get(pageNum)).isEmpty();
    }

    public ArrayList<Integer> getSignedPagesNums(){
        // For external contract only
        ArrayList<Integer> result = new ArrayList<>();
        for(int pageNum: contractScreenshots.keySet()){
            if(isPageSigned(pageNum)){
                result.add(pageNum);
            }
        }
        return result;
    }

    public void clearAllSignedPages(){
        for(int pageNum: contractScreenshots.keySet()){
            if(isPageSigned(pageNum)){
                Bitmap bitmap = (Bitmap) (contractScreenshots.get(pageNum).keySet().toArray()[0]);
                contractScreenshots.remove(pageNum);
                HashMap<byte[], String> sign = new HashMap<>();
                sign.put(getEncodedBitmap(bitmap), "");
                contractScreenshots.put(pageNum, sign);
            }
        }
    }

    public boolean isContentIdBelongsToText(int contentId){
        return originalContractText.keySet().contains(contentId);
    }

    public void setOriginalContractText(int contentId, String text){
        originalContractText.put(contentId, text);
    }

    public String getOriginalContractText(int contentId){
        if(originalContractText.containsKey(contentId) && originalContractText.get(contentId) != null){
            return originalContractText.get(contentId);
        }
        return "";
    }

    public int isTextInOriginalContractText(String text){
        // Will return -1 if nothing is found
        int resultContentId = -1;
        for(int contentId: originalContractText.keySet()){
            if(originalContractText.get(contentId).equals(text)){
                resultContentId = contentId;
                break;
            }
        }
        return resultContentId;
    }

    private byte[] getEncodedBitmap(Bitmap bitmap){
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bStream);
        return bStream.toByteArray();
    }


    private Bitmap getBitmapFromEncoded(byte[] encodedBitmap){
        return BitmapFactory.decodeByteArray(encodedBitmap, 0, encodedBitmap.length);
    }

    public boolean isContentIdBelongsToImage(int contentId){
        return originalContractImage.keySet().contains(contentId);
    }

    public void setOriginalContractImage(int contentId, Bitmap image){
        originalContractImage.put(contentId, getEncodedBitmap(image));
    }

    public Bitmap getOriginalContractImage(int contentId){
        // This function will return null if there is no such image.
        if(originalContractImage.containsKey(contentId) && originalContractImage.get(contentId) != null){
            return getBitmapFromEncoded(originalContractImage.get(contentId));
        }
        return null;
    }

    public int isImageInOriginalContractImage(Bitmap bitmap){
        // Will return -1 if nothing is found
        int resultContentId = -1;
        for(int contentId: originalContractImage.keySet()){
            if(getBitmapFromEncoded(originalContractImage.get(contentId)).sameAs(bitmap)){
                resultContentId = contentId;
                break;
            }
        }
        return resultContentId;
    }

    public void setCommentScreenshot(int contentId, String positionId, Bitmap screenshot, String signature){
        HashMap<String, HashMap<Bitmap, String>> commentScreenshot = new HashMap<>();
        HashMap<Bitmap, String> commentSign = new HashMap<>();
        commentSign.put(screenshot, signature);
        commentScreenshot.put(positionId, commentSign);
        /*
        if(commentScreenshot.containsKey(contentId) && commentScreenshot.get(contentId).containsKey(positionId)){
            commentScreenshot.get(contentId).remove(positionId);
        }
         */
        commentScreenshots.put(contentId, commentScreenshot);
    }

    public Bitmap getCommentScreenshot(int contentId, String positionId){
        // This function will return null if there is no such image.
        for(Bitmap bitmap: commentScreenshots.get(contentId).get(positionId).keySet()){
            return bitmap;
        }
        return null;
    }

    public int getContentIdAccordingToCommentScreenshotBitmap(Bitmap bm){
        // Will return -1 if nothing is found.
        int contentId = -1;
        try {
            for(int cId: commentScreenshots.keySet()){
                for(String pId: commentScreenshots.get(cId).keySet()){
                    for(Bitmap bitmap: commentScreenshots.get(cId).get(pId).keySet()){
                        if(bitmap == bm){
                            contentId = cId;
                            return contentId;
                        }
                    }
                }
            }
        } catch (NullPointerException e) {
            Log.d(TAG, "getContentIdAccordingToCommentScreenshotBitmap: something bad happened: " + e.getMessage());
        }
        return contentId;
    }

    public String getPositionIdAccordingToCommentScreenshotBitmap(Bitmap bm){
        // Will return "" if nothing is found
        String positionId = "";
        try {
            for(int cId: commentScreenshots.keySet()){
                for(String pId: commentScreenshots.get(cId).keySet()){
                    for(Bitmap bitmap: commentScreenshots.get(cId).get(pId).keySet()){
                        if(bitmap == bm){
                            positionId = pId;
                            return positionId;
                        }
                    }
                }
            }
        } catch (NullPointerException e) {
            Log.d(TAG, "getContentIdAccordingToCommentScreenshotBitmap: something bad happened: " + e.getMessage());
        }
        return positionId;
    }

    public Bitmap getFirstUnsignedCommentScreenshot(){
        // This function will return null if there is no image left unsigned.
        try {
            for(int contentId: commentScreenshots.keySet()){
                for(String positionId: commentScreenshots.get(contentId).keySet()){
                    for(Bitmap bitmap: commentScreenshots.get(contentId).get(positionId).keySet()){
                        if(commentScreenshots.get(contentId).get(positionId).get(bitmap) == null ||
                        commentScreenshots.get(contentId).get(positionId).get(bitmap).isEmpty()){
                            return bitmap;
                        }
                    }
                }
            }
        } catch (NullPointerException e) {
            Log.d(TAG, "getFirstUnsignedCommentScreenshot: something bad happened: " + e.getMessage());
            return null;
        }
        return null;
    }

    public int getTotalNumOfTextContent(){
        return originalContractText.size();
    }

    public int getTotalNumOfImageContent(){
        return originalContractImage.size();
    }

    public int getTotalNumOfContent(){
        return getTotalNumOfTextContent() + getTotalNumOfImageContent();
    }

    public Bitmap getImageFromLocalPath(String pathOfImage){
        return BitmapFactory.decodeFile(pathOfImage);
    }

    public void saveImageToLocalPath(Bitmap bitmap, String pathOfFolder, String imageName){
        OutputStream out = null;

        File dir = new File(pathOfFolder);
        dir.mkdirs();

        File imageFile = new File(pathOfFolder + "/", imageName); //this works

        try {
            out = new FileOutputStream(imageFile);
            // choose JPEG format
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            // Myles: If the quality above changed, the quality for saving signature should also be
            // changed, since right now they are both hard-coded.
            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            }

        }
    }

    public Bitmap getScreenshotFromOfferorOriginalScreenshots(int index, Context context) {
        // index should start from 0
        return getImageFromLocalPath(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) +
                        "/tabellion/contracts/" + getContractName() + "/" + revisionCount +
                        "/offerorOriginalScreenshots/" + index);
    }

    public int getTotalNumOfOfferorOriginalScreenshots(){
        return offerorOriginalScreenshotCount + 1;
    }

    public void addScreenshotToOfferorOriginalScreenshots(Bitmap bitmap, Context context) {
        saveImageToLocalPath(bitmap, context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) +
                "/tabellion/contracts/" + getContractName() + "/" + revisionCount + "/offerorOriginalScreenshots",
                String.valueOf(offereeOriginalScreenshotCount++));
    }

    public Bitmap getScreenshotFromOffereeOriginalScreenshots(int index, Context context) {
        // index should start from 0
        return getImageFromLocalPath(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) +
                        "/tabellion/contracts/" + getContractName() + "/" + revisionCount +
                        "/offereeOriginalScreenshots/" + index);
    }

    public int getTotalNumOfOffereeOriginalScreenshots(){
        return offereeOriginalScreenshotCount + 1;
    }

    public void addScreenshotToOffereeOriginalScreenshots(Bitmap bitmap, Context context) {
        saveImageToLocalPath(bitmap, context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) +
                        "/tabellion/contracts/" + getContractName() + "/" + revisionCount + "/offereeOriginalScreenshots",
                String.valueOf(offerorOriginalScreenshotCount++));
    }

    public void setContentKey(Integer contentID, String contentKey){
        originalContractContentKey.put(contentID, contentKey);
    }

    public Integer getContentIDByContentKey(String contentKey){
        // Will return -1 if nothing is found
        for(int contentID: originalContractContentKey.keySet()){
            if(originalContractContentKey.get(contentID).equals(contentKey)){
                return contentID;
            }
        }
        return -1;
    }

    public String getOriginalContractTextByContentKey(String contentKey){
        return getOriginalContractText(getContentIDByContentKey(contentKey));
    }

    public Bitmap getOriginalContractImageByContentKey(String contentKey){
        return getOriginalContractImage(getContentIDByContentKey(contentKey));
    }

    public int setOriginalContractTextByContentKey(String contentKey, String originalText){
        // Will return the contentID
        int contentID = getTotalNumOfContent();
        setContentKey(contentID, contentKey);
        setOriginalContractText(contentID, originalText);
        return contentID;
    }

    public int setOriginalContractImageByContentKey(String contentKey, Bitmap originalImage){
        // Will return the contentID
        int contentID = getTotalNumOfContent();
        setContentKey(contentID, contentKey);
        setOriginalContractImage(contentID, originalImage);
        return contentID;
    }

    public int getRevisionCount() {
        return revisionCount;
    }

    public void setRevisionCount(int revisionCount) {
        this.revisionCount = revisionCount;
    }

    public int getOfferorOriginalScreenshotCount() {
        return offerorOriginalScreenshotCount;
    }

    public void setOfferorOriginalScreenshotCount(int offerorOriginalScreenshotCount) {
        this.offerorOriginalScreenshotCount = offerorOriginalScreenshotCount;
    }

    public int getOffereeOriginalScreenshotCount() {
        return offereeOriginalScreenshotCount;
    }

    public void setOffereeOriginalScreenshotCount(int offereeOriginalScreenshotCount) {
        this.offereeOriginalScreenshotCount = offereeOriginalScreenshotCount;
    }

    public void setIsContractCreatedByTabellion(boolean isContractCreatedByTabellion){
        this.isContractCreatedByTabellion = isContractCreatedByTabellion;
    }

    public boolean isContractCreatedByTabellion(){
        return isContractCreatedByTabellion;
    }

    public ArrayList<Integer> getSignedPages() {
        return signedPages;
    }

    public boolean isPageInSignedPages(int page_num){
        // page_num should be counted from 1
        return this.signedPages.contains(page_num);
    }

    public void setSignedPages(ArrayList<Integer> signedPages) {
        this.signedPages = signedPages;
    }

    public void addToSignedPages(int page_num){
        // page_num should be counted from 1
        this.signedPages.add(page_num);
    }

    public int getRevistedNumCount() {
        return revistedNumCount;
    }

    public void setRevistedNumCount(int revistedNumCount) {
        this.revistedNumCount = revistedNumCount;
    }
}
