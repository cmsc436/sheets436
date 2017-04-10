package edu.umd.cmsc436.sheets;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;

import java.util.Collections;

import static android.app.Activity.RESULT_OK;

/**
 * Class to instantiate and hook into parts of the normal app
 */

public class Sheets {

    private final String[] PERMISSIONS = new String[] {
            Manifest.permission.GET_ACCOUNTS
    };

    private final String SHARED_PREFS_NAME = "edu.umd.cmsc436.sheets";
    private final String PREF_ACCOUNT_NAME = "account name";

    private Host host;
    private Activity hostActivity;
    private GoogleAccountCredential credentials;

    private boolean cache_is_private;
    private TestType cache_type;
    private String cache_userId;
    private float cache_value;
    private float[] cache_trials;
    private String appName;
    private String spreadsheetId;
    private String privateSpreadsheetId;

    public Sheets(Host host, String appName, String spreadsheetId) {
        this.host = host;
        this.hostActivity = (Activity) host;
        this.appName = appName;
        this.spreadsheetId = spreadsheetId;

        credentials = GoogleAccountCredential.usingOAuth2(hostActivity,
                Collections.singletonList(SheetsScopes.SPREADSHEETS)).setBackOff(new ExponentialBackOff());
    }

    public Sheets(Host host, String appName, String spreadsheetId, String privateSpreadsheetId) {
        this(host, appName, spreadsheetId);
        this.privateSpreadsheetId = privateSpreadsheetId;
    }

    public void writeData (TestType testType, String userId, float value) {
        cache_is_private = false;
        cache_type = testType;
        cache_userId = userId;
        cache_value = value;
        if (checkConnection()) {
            // TODO: modify class field visibility (or add public getters) to clear up unnecessary params
            WriteDataTask writeDataTask = new WriteDataTask(this, credentials, spreadsheetId, appName, host, hostActivity);
            writeDataTask.execute(new WriteDataTask.WriteData(testType, userId, value));
        }
    }

    public void writeTrials (TestType testType, String userId, float[] trials) {
        cache_is_private = true;
        cache_type = testType;
        cache_userId = userId;
        cache_trials = trials;
        if (checkConnection()) {
            WriteDataTask writeDataTask = new WriteDataTask(this, credentials, privateSpreadsheetId, appName, host, hostActivity);
            writeDataTask.execute(new WriteDataTask.WriteData(testType, userId, trials));
        }
    }

    public void onRequestPermissionsResult (int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == host.getRequestCode(Action.REQUEST_PERMISSIONS)) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                resume();
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == host.getRequestCode(Action.REQUEST_ACCOUNT_NAME)) {
            String accountName =
                    data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if (accountName != null) {
                SharedPreferences settings =
                        hostActivity.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
                settings.edit().putString(PREF_ACCOUNT_NAME, accountName).apply();
                resume();
            }
        } else if (requestCode == host.getRequestCode(Action.REQUEST_PLAY_SERVICES)) {
            if (resultCode != RESULT_OK) {
                Log.e(this.getClass().getSimpleName(), "Requires Google Play Services");
            } else {
                resume();
            }
        } else if (requestCode == host.getRequestCode(Action.REQUEST_AUTHORIZATION)) {
            if (resultCode == RESULT_OK) {
                resume();
            }
        }
    }

    private void resume() {
        if (cache_is_private) {
            writeTrials(cache_type, cache_userId, cache_trials);
        } else {
            writeData(cache_type, cache_userId, cache_value);
        }
    }

    private boolean checkConnection() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int statusCode = apiAvailability.isGooglePlayServicesAvailable(hostActivity);
        if (statusCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(statusCode)) {
                showGooglePlayErrorDialog();
            }

            return false;
        }

        if (credentials.getSelectedAccountName() == null) {
            // if not on Marshmallow then the manifest takes care of the permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && hostActivity.checkSelfPermission(PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(hostActivity, PERMISSIONS, host.getRequestCode(Action.REQUEST_PERMISSIONS));

                return false;
            }

            SharedPreferences prefs = hostActivity.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
            String accountName = prefs.getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                credentials.setSelectedAccountName(accountName);
            } else {
                hostActivity.startActivityForResult(credentials.newChooseAccountIntent(), host.getRequestCode(Action.REQUEST_ACCOUNT_NAME));
                return false;
            }
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager) hostActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            host.notifyFinished(new NoNetworkException());
            return false;
        }

        return true;
    }

    protected void showGooglePlayErrorDialog () {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int statusCode = apiAvailability.isGooglePlayServicesAvailable(hostActivity);
        apiAvailability.getErrorDialog(hostActivity, statusCode, host.getRequestCode(Action.REQUEST_PLAY_SERVICES)).show();
    }

    public interface Host {

        int getRequestCode(Action action);

        void notifyFinished(Exception e);
    }

    public enum Action {
        REQUEST_PERMISSIONS,
        REQUEST_ACCOUNT_NAME,
        REQUEST_PLAY_SERVICES,
        REQUEST_AUTHORIZATION
    }

    @SuppressWarnings("WeakerAccess")
    public static class NoNetworkException extends Exception {}

    public enum TestType {
        LH_TAP("'Tapping Test (LH)'"),
        RH_TAP("'Tapping Test (RH)'"),
        LF_TAP("'Tapping Test (LF)'"),
        RF_TAP("'Tapping Test (RF)'"),
        LH_SPIRAL("'Spiral Test (LH)'"),
        RH_SPIRAL("'Spiral Test (RH)'"),
        LH_LEVEL("'Level Test (LH)'"),
        RH_LEVEL("'Level Test (RH)'"),
        LH_POP("'Balloon Test (LH)'"),
        RH_POP("'Balloon Test (RH)'"),
        LH_CURL("'Curling Test (LH)'"),
        RH_CURL("'Curling Test (RH)'"),
        HEAD_SWAY("'Swaying Test'");

        private final String id;

        TestType(String sheetId) {
            id = sheetId;
        }

        public String toId() {
            return id;
        }
    }


}
