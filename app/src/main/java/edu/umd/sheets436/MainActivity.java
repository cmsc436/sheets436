package edu.umd.sheets436;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import edu.umd.cmsc436.sheets.Sheets;

public class MainActivity extends AppCompatActivity implements Sheets.Host {

    public static final int LIB_ACCOUNT_NAME_REQUEST_CODE = 1001;
    public static final int LIB_AUTHORIZATION_REQUEST_CODE = 1002;
    public static final int LIB_PERMISSION_REQUEST_CODE = 1003;
    public static final int LIB_PLAY_SERVICES_REQUEST_CODE = 1004;
    public static final int LIB_CONNECTION_REQUEST_CODE = 1005;
    private static final int MIN = 0;
    private static final int MAX = 100;

    private Sheets sheet;
    private Spinner spinner;

    private double random() {
        double range = Math.abs(MAX - MIN);
        return (Math.random() * range) + MIN;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.test_array, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        sheet = new Sheets(this, this, getString(R.string.app_name), getString(R.string.CMSC436_testing_spreadsheet), getString(R.string.CMSC436_private_test_spreadsheet));

        // The next two lines of code allows network calls on the UI thread. Do not do this in a
        // real app. I'm just doing this for ease of coding/reading, since this is a sample of how
        // to use the API.
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Grab some random picture from the Internet.
        Bitmap bitmap = null;
        try {
            URL url = new URL("https://www.umiacs.umd.edu/sites/default/files/styles/medium/public/web-Memon09.jpg");
            bitmap = BitmapFactory.decodeStream((InputStream) url.getContent());
        } catch (IOException e) {
            e.printStackTrace();
        }

        sheet.uploadToDrive(getString(R.string.CMSC436_test_folder), getString(R.string.image_name), bitmap);
    }

    public void sendToSheets(View v) {
        float f = (float)random();
        float[] trialData = {(float)random(), (float)random(), (float)random()};
        switch (spinner.getSelectedItem().toString()) {
            case "Tap (Hands)":
                sheet.writeData(Sheets.TestType.LH_TAP, getString(R.string.user_id), f);
                sheet.writeData(Sheets.TestType.RH_TAP, getString(R.string.user_id), f);
                sheet.writeTrials(Sheets.TestType.LH_TAP, getString(R.string.user_id), trialData);
                sheet.writeTrials(Sheets.TestType.RH_TAP, getString(R.string.user_id), trialData);
                break;
            case "Tap (Feet)":
                sheet.writeData(Sheets.TestType.LF_TAP, getString(R.string.user_id), f);
                sheet.writeData(Sheets.TestType.RF_TAP, getString(R.string.user_id), f);
                sheet.writeTrials(Sheets.TestType.LF_TAP, getString(R.string.user_id), trialData);
                sheet.writeTrials(Sheets.TestType.RF_TAP, getString(R.string.user_id), trialData);
                break;
            case "Spiral":
                sheet.writeData(Sheets.TestType.LH_SPIRAL, getString(R.string.user_id), f);
                sheet.writeData(Sheets.TestType.RH_SPIRAL, getString(R.string.user_id), f);
                sheet.writeTrials(Sheets.TestType.LH_SPIRAL, getString(R.string.user_id), trialData);
                sheet.writeTrials(Sheets.TestType.RH_SPIRAL, getString(R.string.user_id), trialData);
                break;
            case "Level":
                sheet.writeData(Sheets.TestType.LH_LEVEL, getString(R.string.user_id), f);
                sheet.writeData(Sheets.TestType.RH_LEVEL, getString(R.string.user_id), f);
                sheet.writeTrials(Sheets.TestType.LH_LEVEL, getString(R.string.user_id), trialData);
                sheet.writeTrials(Sheets.TestType.RH_LEVEL, getString(R.string.user_id), trialData);
                break;
            case "Balloon":
                sheet.writeData(Sheets.TestType.LH_POP, getString(R.string.user_id), f);
                sheet.writeData(Sheets.TestType.RH_POP, getString(R.string.user_id), f);
                sheet.writeTrials(Sheets.TestType.LH_POP, getString(R.string.user_id), trialData);
                sheet.writeTrials(Sheets.TestType.RH_POP, getString(R.string.user_id), trialData);
                break;
            case "Curl":
                sheet.writeData(Sheets.TestType.LH_CURL, getString(R.string.user_id), f);
                sheet.writeData(Sheets.TestType.RH_CURL, getString(R.string.user_id), f);
                sheet.writeTrials(Sheets.TestType.LH_CURL, getString(R.string.user_id), trialData);
                sheet.writeTrials(Sheets.TestType.RH_CURL, getString(R.string.user_id), trialData);
                break;
            case "Sway":
                sheet.writeData(Sheets.TestType.SWAY_OPEN_APART, getString(R.string.user_id), f);
                sheet.writeTrials(Sheets.TestType.SWAY_OPEN_APART, getString(R.string.user_id), trialData);
                sheet.writeData(Sheets.TestType.SWAY_OPEN_TOGETHER, getString(R.string.user_id), f);
                sheet.writeTrials(Sheets.TestType.SWAY_OPEN_TOGETHER, getString(R.string.user_id), trialData);
                sheet.writeData(Sheets.TestType.SWAY_CLOSED, getString(R.string.user_id), f);
                sheet.writeTrials(Sheets.TestType.SWAY_CLOSED, getString(R.string.user_id), trialData);
                break;
            case "Walk (Indoors)":
                sheet.writeData(Sheets.TestType.INDOOR_WALKING, getString(R.string.user_id), f);
                sheet.writeTrials(Sheets.TestType.INDOOR_WALKING, getString(R.string.user_id), trialData);
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        sheet.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        sheet.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public int getRequestCode(Sheets.Action action) {
        switch (action) {
            case REQUEST_ACCOUNT_NAME:
                return LIB_ACCOUNT_NAME_REQUEST_CODE;
            case REQUEST_AUTHORIZATION:
                return LIB_AUTHORIZATION_REQUEST_CODE;
            case REQUEST_PERMISSIONS:
                return LIB_PERMISSION_REQUEST_CODE;
            case REQUEST_PLAY_SERVICES:
                return LIB_PLAY_SERVICES_REQUEST_CODE;
            case REQUEST_CONNECTION_RESOLUTION:
                return LIB_CONNECTION_REQUEST_CODE;
            default:
                return -1;
        }
    }

    @Override
    public void notifyFinished(Exception e) {
        if (e != null) {
            throw new RuntimeException(e);
        }
        Log.i(getClass().getSimpleName(), "Done");
    }

}
