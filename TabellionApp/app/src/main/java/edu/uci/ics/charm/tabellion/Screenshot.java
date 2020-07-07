package edu.uci.ics.charm.tabellion;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static edu.uci.ics.charm.tabellion.MainActivity.sContext;

/*
Created Date: 2018
Created By: Saeed Mirzamohammadi
Last Modified: 06/03/2019
Last Modified By: Myles Liu
Notes:

 */

public class Screenshot {

    public static Bitmap takescreenshot(View v) {
        v.setDrawingCacheEnabled(true);
        v.buildDrawingCache(true);
        Bitmap b = Bitmap.createBitmap(v.getDrawingCache());
        v.setDrawingCacheEnabled(false);
        return b;
    }

    public static Bitmap takescreenshotOfRootView(View v) {
        return takescreenshot(v.getRootView());
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static String storeScreenshot(Bitmap bitmap, String filename, Contract contract) {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
                + "/" + contract.getContractId() + "/" + filename;
        //String path = "/sdcard/" + filename;
        //Log.d("Saeed: Store path=", sContext.getFilesDir() + filename);
        Log.d("Saeed: Writeable= ", "" + isExternalStorageWritable());
        Log.d("Saeed: Store path=", sContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                + "/" + contract.getContractId() + "/" + filename);

        OutputStream out = null;
        //File imageFile = new File(path);

        File imageFile = new File(sContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                + "/" + contract.getContractId() + "/", filename); //this works
        //File imageFile = new File("/sdcard", filename);


        Log.d("storeScreenshot ", "Going to store screenshot " + imageFile.getAbsolutePath());
        Log.d("Saeed bytecount=", "" + bitmap.getByteCount());
        //File imageFile = new File(sContext.getFilesDir(), filename);

        try {
            out = new FileOutputStream(imageFile);
            // choose JPEG format
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            // Myles: If the quality above changed, the quality for saving signature should also be
            // changed, since right now they are both hard-coded.
            out.flush();
        } catch (FileNotFoundException e) {
            Log.d("Saeed: ", "[1]");
            e.printStackTrace();
            // manage exception ...
        } catch (IOException e) {
            Log.d("Saeed: ", "[2]");
            e.printStackTrace();
            // manage exception ...
        } finally {

            try {
                if (out != null) {
                    out.close();
                }

            } catch (Exception exc) {
                Log.d("Saeed: ", "[3]");
                exc.printStackTrace();
            }

        }
        return imageFile.getAbsolutePath();
    }

    public static void storeScreenshot(Bitmap bitmap, String filename, Contract contract, Context context) {
        File path = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                + "/" + contract.getContractId() + "/");
        path.mkdirs();
        //String path = "/sdcard/" + filename;
        //Log.d("Saeed: Store path=", sContext.getFilesDir() + filename);
        Log.d("Saeed: Writeable= ", "" + isExternalStorageWritable());
        Log.d("Saeed: Store path=", context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                + "/" + contract.getContractId() + "/" + filename);

        OutputStream out = null;
        //File imageFile = new File(path);

        File imageFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                + "/" + contract.getContractId() + "/", filename); //this works
        //File imageFile = new File("/sdcard", filename);


        Log.d("storeScreenshot ", "Going to store screenshot " + imageFile.getAbsolutePath());
        Log.d("Saeed bytecount=", "" + bitmap.getByteCount());
        //File imageFile = new File(sContext.getFilesDir(), filename);

        try {
            out = new FileOutputStream(imageFile);
            // choose JPEG format
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            // Myles: If the quality above changed, the quality for saving signature should also be
            // changed, since right now they are both hard-coded.
            out.flush();
        } catch (FileNotFoundException e) {
            Log.d("Saeed: ", "[1]");
            e.printStackTrace();
            // manage exception ...
        } catch (IOException e) {
            Log.d("Saeed: ", "[2]");
            e.printStackTrace();
            // manage exception ...
        } finally {

            try {
                if (out != null) {
                    out.close();
                }

            } catch (Exception exc) {
                Log.d("Saeed: ", "[3]");
                exc.printStackTrace();
            }

        }
    }

}

