package edu.umd.cmsc436.sheets;

import android.app.Activity;
import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.model.FileList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Task to use the REST API to download files
 */

public class DriveApkTask extends UploadToDriveTask {

    private OnFinishListener mListener;
    private Map<String, Float> mVersionMap;

    public DriveApkTask(GoogleAccountCredential credential, String applicationName, Sheets.Host host, Activity hostActivity) {
        super(credential, applicationName, host, hostActivity);
    }

    public DriveApkTask setOnFinishListener (OnFinishListener listener) {
        mListener = listener;
        return this;
    }

    // type -> version, or -1 if want to guarantee update (aka install)
    public DriveApkTask setVersionMap(Map<String, Float> infoList) {
        mVersionMap = infoList;
        return this;
    }

    @Override
    protected Exception doInBackground(DrivePayload... params) {
        if (!(params.length > 0)) {
            return new Exception("need a DrivePayload with a folder id");
        }

        String folderId = params[0].folderId;

        // list all files
        // TODO only gets the first 100 files, but I'm not going to worry about > 100 APKs right now
        try {
            FileList files = driveService.files().list()
                    .setQ("'" + folderId + "' in parents")
                    .execute();

            List<com.google.api.services.drive.model.File> realFiles = files.getFiles();
            if (realFiles == null) {
                return new Exception("files null");
            }

            // Find the latest APKs of the types given to us
            Map<String, com.google.api.services.drive.model.File> filesToGet = new HashMap<>();
            for (com.google.api.services.drive.model.File f : realFiles) {
                String type = getType(f);
                if (mVersionMap.keySet().contains(type)) {
                    float version = getVersion(f);
                    if (version > mVersionMap.get(type)) {
                        Log.i(getClass().getCanonicalName(), "Found newer " + type + " than " + mVersionMap.get(type) + ": " + version);
                        mVersionMap.put(type, version);
                        filesToGet.put(type, f);
                    }
                }
            }

            Log.i(getClass().getCanonicalName(), "Number of files to download: " + filesToGet.keySet().size());

            // Actually download
            Map<File, Float> results = new HashMap<>();
            for (com.google.api.services.drive.model.File f : filesToGet.values()) {
                File tempFile = File.createTempFile(removeExtension(f), ".apk", hostActivity.getCacheDir());
                FileOutputStream fileOutputStream = new FileOutputStream(tempFile);

                driveService.files().get(f.getId()).executeMediaAndDownloadTo(fileOutputStream);

                results.put(tempFile, getVersion(f));
            }

            if (mListener != null) {
                mListener.onFinish(results);
            }
        } catch (IOException e) {
            return e;
        }

        return null;
    }

    private float getVersion(com.google.api.services.drive.model.File file) {
        String name = file.getName();
        String version = name.substring(name.lastIndexOf('-') + 1, name.lastIndexOf('.'));

        try {
            return Float.parseFloat(version);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    private String getType(com.google.api.services.drive.model.File file) {
        String name = file.getName();
        return name.substring(0, name.lastIndexOf('-'));
    }

    private String removeExtension (com.google.api.services.drive.model.File file) {
        String name = file.getName();
        return name.substring(0, name.lastIndexOf('.'));
    }

    public interface OnFinishListener {
        void onFinish(Map<File, Float> tempFiles);
    }
}
