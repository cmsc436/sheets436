package edu.umd.sheets436;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import edu.umd.cmsc436.sheets.CMSC436Sheet;

public class MainActivity extends AppCompatActivity implements CMSC436Sheet.Host {

    private static final int LIB_ACCOUNT_NAME_REQUEST_CODE = 1001;
    private static final int LIB_AUTHORIZATION_REQUEST_CODE = 1002;
    private static final int LIB_PERMISSION_REQUEST_CODE = 1003;
    private static final int LIB_PLAY_SERVICES_REQUEST_CODE = 1004;

    private CMSC436Sheet sheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sheet = new CMSC436Sheet(this, getString(R.string.app_name), getString(R.string.CMSC436Sheet_spreadsheet_id_test_sheet));
        sheet.writeData(CMSC436Sheet.TestType.LH_TAP, "newuser", CMSC436Sheet.unixToSheetsEpoch(System.currentTimeMillis()));
        sheet.writeData(CMSC436Sheet.TestType.LH_TAP, "newuser", 1.23f);
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        sheet.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public int getRequestCode(CMSC436Sheet.Action action) {
        switch (action) {
            case REQUEST_ACCOUNT_NAME:
                return LIB_ACCOUNT_NAME_REQUEST_CODE;
            case REQUEST_AUTHORIZATION:
                return LIB_AUTHORIZATION_REQUEST_CODE;
            case REQUEST_PERMISSIONS:
                return LIB_PERMISSION_REQUEST_CODE;
            case REQUEST_PLAY_SERVICES:
                return LIB_PLAY_SERVICES_REQUEST_CODE;
            default:
                return -1;
        }
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void notifyFinished(Exception e) {
        if (e != null) {
            throw new RuntimeException(e);
        }
        Log.i(getClass().getSimpleName(), "Done");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        sheet.onActivityResult(requestCode, resultCode, data);
    }
}
