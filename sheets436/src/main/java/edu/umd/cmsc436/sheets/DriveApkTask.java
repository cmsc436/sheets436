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
        Map<String, Float> max_versions = new HashMap<>();
        Map<String, com.google.api.services.drive.model.File> type_to_file = new HashMap<>();

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

            // get max versions
            for (com.google.api.services.drive.model.File f : realFiles) {
                String name = f.getName();
                float version = getVersionFromName(name);
                if (!max_versions.containsKey(name) || version > max_versions.get(name)) {
                    max_versions.put(name, version);
                    type_to_file.put(name.substring(0, name.lastIndexOf('-')), f);
                }
            }

            // find which to actually download
            List<com.google.api.services.drive.model.File> toDownload = new ArrayList<>();

            for (String fname : max_versions.keySet()) {
                Log.i(getClass().getCanonicalName(), "max version: " + fname);
                String type = fname.substring(0, fname.lastIndexOf('-'));
                if (mVersionMap.containsKey(type) && mVersionMap.get(type) < max_versions.get(fname)) {
                    toDownload.add(type_to_file.get(type));
                }
            }

            List<File> results = new ArrayList<>();
            Log.i(getClass().getCanonicalName(), "#files to download: " + toDownload.size());
            for (com.google.api.services.drive.model.File f : toDownload) {
                File outputFile = File.createTempFile(f.getName(), "", hostActivity.getCacheDir());
                FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                driveService.files().get(f.getId()).executeMediaAndDownloadTo(fileOutputStream);
                results.add(outputFile);
            }

            if (mListener != null) {
                mListener.onFinish(results);
            }
        } catch (IOException e) {
            return e;
        }

        return null;
    }

    private float getVersionFromName(String name) {
        String version = name.substring(name.lastIndexOf('-') + 1, name.lastIndexOf('.'));

        try {
            return Float.parseFloat(version);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    public interface OnFinishListener {
        void onFinish(List<File> tempFiles);
    }
}
