package edu.umd.cmsc436.sheets;

import android.app.Activity;
import android.os.AsyncTask;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Return the row of data for a given username in the form of a list of strings to interpret
 */

public class ReadPrescriptionTask extends AsyncTask<String, Void, Exception> {

    private com.google.api.services.sheets.v4.Sheets sheetsService;
    private String spreadsheetId;
    private Sheets.Host host;
    private Activity hostActivity;
    private List<String> results;
    private Sheets.OnPrescriptionFetchedListener mListener;

    ReadPrescriptionTask (GoogleAccountCredential credential,
                          String spreadsheetId,
                          String applicationName,
                          Sheets.Host host,
                          Activity hostActivity,
                          Sheets.OnPrescriptionFetchedListener listener) {
        this.spreadsheetId = spreadsheetId;
        this.host = host;
        this.hostActivity = hostActivity;
        mListener = listener;

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        sheetsService = new com.google.api.services.sheets.v4.Sheets.Builder(
                transport, jsonFactory, credential)
                .setApplicationName(applicationName)
                .build();
    }

    @Override
    protected Exception doInBackground(String... params) {

        try {
            for (String id : params) {
                ValueRange response = sheetsService.spreadsheets()
                        .values()
                        .get(spreadsheetId, "Sheet1!A2:A")
                        .execute();
                List<List<Object>> sheet = response.getValues();
                int cur_row = 2;
                if (sheet != null) {
                    for (List row : sheet) {
                        if (row.size() == 0 || row.get(0).toString().length() == 0 || row.get(0).toString().equals(id)) {
                            break;
                        }

                        cur_row ++;
                    }
                }

                ValueRange vals = sheetsService.spreadsheets().values()
                        .get(spreadsheetId, "Sheet1!A" + cur_row + ":P" + cur_row)
                        .execute();

                sheet = vals.getValues();
                if (sheet.size() != 1) {
                    throw new Exception("nothing");
                }

                results = new ArrayList<>();
                for (Object o : sheet.get(0)) {
                    results.add(o == null ? null : o.toString());
                }
            }
        } catch (IOException ioe) {
            return ioe;
        } catch (Exception e) {
            return e;
        }

        return null;
    }

    @Override
    protected void onPostExecute(Exception e) {
        if (e != null && e instanceof GooglePlayServicesAvailabilityIOException) {
            Sheets.showGooglePlayErrorDialog(host, hostActivity);
        } else if (e != null && e instanceof UserRecoverableAuthIOException) {
            hostActivity.startActivityForResult(((UserRecoverableAuthIOException) e).getIntent(),
                    host.getRequestCode(Sheets.Action.REQUEST_AUTHORIZATION));
        } else {
            mListener.onPrescriptionFetched(results);
        }
    }
}
