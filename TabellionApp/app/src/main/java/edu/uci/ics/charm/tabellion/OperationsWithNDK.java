package edu.uci.ics.charm.tabellion;

/*
Created Date: 03/27/2019
Created By: Myles Liu
Last Modified: 03/27/2019
Last Modified By: Myles Liu
Notes:

 */

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class OperationsWithNDK {

    private final static String TAG = "OperationsWithNDK ";

    private static boolean canExecuteSuCommand(){
        // This method of detecting Root is not used since it will send a Request for Root
        try
        {
            Runtime.getRuntime().exec("su");
            return true;
        }
        catch (IOException localIOException)
        {
            return false;
        }
    }

    private static boolean hasSuperuserApk(){
        String[] paths = { "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
                "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"};
        for (String path : paths) {
            if(new File(path).exists()){
                return true;
            }
        }
        return false;
    }

    private static boolean isTestKeyBuild(){
        String str = Build.TAGS;
        return ((str != null) && str.contains("test-keys"));
    }

    public static boolean isRootedPhone(){
        // We use two ways to detect whether the phone is rooted
        Log.d(TAG, "isRootedPhone: hasSuperuserApk: " + hasSuperuserApk());
        Log.d(TAG, "isRootedPhone: isTestKeyBuild: " + isTestKeyBuild());
        return (hasSuperuserApk() | isTestKeyBuild());
    }

    private static void backUpCurrentLEDStatus(){
        Log.d(TAG, "backUpCurrentLEDStatus: Backing up LED status...");
        // Back up LED switches
        boolean result = sudo("mv /sys/class/leds/red/rgb_start /sys/class/leds/red/rgb_start_bak");
        sudo("mv /sys/class/leds/green/rgb_start /sys/class/leds/green/rgb_start_bak");
        sudo("mv /sys/class/leds/blue/rgb_start /sys/class/leds/blue/rgb_start_bak");

        Log.d(TAG, "backUpCurrentLEDStatus isSuccess: " + String.valueOf(result));

        // Back up LED color
        sudo("mv /sys/class/leds/red/brightness /sys/class/leds/red/brightness_bak");
        sudo("mv /sys/class/leds/green/brightness /sys/class/leds/green/brightness_bak");
        sudo("mv /sys/class/leds/blue/brightness /sys/class/leds/blue/brightness_bak");
    }

    private static void restoreLEDStatus(){
        Log.d(TAG, "restoreLEDStatus: Restoring LED status...");
        // Turn LED off
        sudo("echo \"0\" > /sys/class/leds/red/rgb_start");
        sudo("echo \"0\" > /sys/class/leds/green/rgb_start");
        sudo("echo \"0\" > /sys/class/leds/blue/rgb_start");

        // Restore LED color
        sudo("echo \"$(</sys/class/leds/red/brightness_bak)\" > /sys/class/leds/red/brightness");
        sudo("echo \"$(</sys/class/leds/green/brightness_bak)\" > /sys/class/leds/green/brightness");
        sudo("echo \"$(</sys/class/leds/blue/brightness_bak)\" > /sys/class/leds/blue/brightness");

        // Restore LED switches
        boolean result = sudo("echo \"$(</sys/class/leds/red/rgb_start_bak)\" > /sys/class/leds/red/rgb_start");
        sudo("echo \"$(</sys/class/leds/green/rgb_start_bak)\" > /sys/class/leds/green/rgb_start");
        sudo("echo \"$(</sys/class/leds/blue/rgb_start_bak)\" > /sys/class/leds/blue/rgb_start");

        Log.d(TAG, "restoreLEDStatus isSuccess: " + String.valueOf(result));
    }

    static void turnOnLedLightForSigning(){
        if(sudo("true")){
            startshell();
            backUpCurrentLEDStatus();
            // Turn LED off
            sudo("echo \"0\" > /sys/class/leds/red/rgb_start");
            sudo("echo \"0\" > /sys/class/leds/green/rgb_start");
            sudo("echo \"0\" > /sys/class/leds/blue/rgb_start");
            // Change LED color
            sudo("echo \"255\" > /sys/class/leds/red/brightness");
            sudo("echo \"0\" > /sys/class/leds/green/brightness");
            sudo("echo \"0\" > /sys/class/leds/blue/brightness");
            // Turn LED back on
            sudo("echo \"1\" > /sys/class/leds/red/rgb_start");
            sudo("echo \"1\" > /sys/class/leds/green/rgb_start");
            sudo("echo \"1\" > /sys/class/leds/blue/rgb_start");
            closeshell();
        }
    }

    static void turnOffLedLightForSigning(){
        if(sudo("true")){
            startshell();
            restoreLEDStatus();
            closeshell();
        }
    }

    // The following declarations are for calling Native Functions; Refer to the corresponding C definitions for more info
    public static native String stringFromJNI();
    public static native boolean sudo(String xmd);
    public static native void startshell();
    public static native void closeshell();


    static {
        System.loadLibrary("xmd-actions");
    }
}
