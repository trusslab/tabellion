package edu.uci.ics.charm.tabellion;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/*
Created Date: 09/12/2019
Created By: Myles Liu
Last Modified: 09/14/2019
Last Modified By: Myles Liu
Notes:

 */

public class ZipManager {

    private final static String TAG = "ZipManager";

    private static int BUFFER_SIZE = 4096;

    /*
    // Copied from https://mobikul.com/zip-unzip-file-folder-android-programmatically/
    public static void unzip(String zipFile, String location) throws IOException {
        try {
            File f = new File(location);
            if (!f.isDirectory()) {
                f.mkdirs();
            }
            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
            try {
                ZipEntry ze = null;
                while ((ze = zin.getNextEntry()) != null) {

                    // Get out of all other folders
                    String filePath = ze.getName();
                    Log.d(TAG, "unzip: filePath is: " + filePath);
                    Log.d(TAG, "unzip: index is: " + filePath.indexOf("tabellion/contracts/"));
                    filePath = filePath.substring(filePath.indexOf("tabellion/contracts/") + 20);

                    String path = location + File.separator + ze.getName();

                    if (ze.isDirectory()) {
                        File unzipFile = new File(path);
                        if (!unzipFile.isDirectory()) {
                            unzipFile.mkdirs();
                        }
                    } else {
                        FileOutputStream fout = new FileOutputStream(path, false);

                        try {
                            for (int c = zin.read(); c != -1; c = zin.read()) {
                                fout.write(c);
                            }
                            zin.closeEntry();
                        } finally {
                            fout.close();
                        }
                    }
                }
            } finally {
                zin.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Unzip exception", e);
        }
    }

     */

    // Written on behave of https://stackoverflow.com/questions/9530921/list-all-the-files-from-all-the-folder-in-a-single-list
    public static ArrayList<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFiles(file));
            } else {
                inFiles.add(file);
            }
        }
        return inFiles;
    }

    // Written on behave of https://stackoverflow.com/questions/9530921/list-all-the-files-from-all-the-folder-in-a-single-list
    public static ArrayList<String> getListFilePaths(File parentDir){
        ArrayList<String> inFiles = new ArrayList<>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFilePaths(file));
            } else {
                inFiles.add(file.getAbsolutePath());
            }
        }
        return inFiles;
    }

    // Copied from http://www.java2s.com/Code/Java/File-Input-Output/Makingazipfileofdirectoryincludingitssubdirectoriesrecursively.htm
    public static void zipDir(String zipFileName, String dir) throws Exception {
        File dirObj = new File(dir);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
        System.out.println("Creating : " + zipFileName);
        addDir(dirObj, out);
        out.close();
    }

    // Copied from http://www.java2s.com/Code/Java/File-Input-Output/Makingazipfileofdirectoryincludingitssubdirectoriesrecursively.htm
    private static void addDir(File dirObj, ZipOutputStream out) throws IOException {
        File[] files = dirObj.listFiles();
        byte[] tmpBuf = new byte[1024];

        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                addDir(files[i], out);
                continue;
            }
            FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
            System.out.println(" Adding: " + files[i].getAbsolutePath());
            out.putNextEntry(new ZipEntry(files[i].getAbsolutePath()));
            int len;
            while ((len = in.read(tmpBuf)) > 0) {
                out.write(tmpBuf, 0, len);
            }
            out.closeEntry();
            in.close();
        }
    }

    public static void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String tempPath = entry.getName();
            Log.d(TAG, "unzip: filePath is: " + tempPath);
            Log.d(TAG, "unzip: index is: " + tempPath.indexOf("tabellion/contracts/"));
            tempPath = tempPath.substring(tempPath.indexOf("tabellion/contracts/") + 20);

            String filePath = destDirectory + File.separator + tempPath;
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        Log.d(TAG, "extractFile: Going to create folder: " + filePath.substring(0, filePath.lastIndexOf("/")));
        File tempFileForCreatingFolder = new File(filePath.substring(0, filePath.lastIndexOf("/")));
        tempFileForCreatingFolder.mkdirs();

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

}

