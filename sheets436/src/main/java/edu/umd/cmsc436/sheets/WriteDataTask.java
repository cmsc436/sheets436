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

import java.util.ArrayList;
import java.util.List;

/**
 * Background task to write the data
 */
class WriteDataTask extends AsyncTask<WriteDataTask.WriteData, Void, Exception> {

    private com.google.api.services.sheets.v4.Sheets sheetsService = null;
    private Sheets sheets;
    private String spreadsheetId;
    private Sheets.Host host;
    private Activity hostActivity;

    WriteDataTask (Sheets sheets, GoogleAccountCredential credential, String spreadsheetId, String applicationName, Sheets.Host host, Activity hostActivity) {

        this.spreadsheetId = spreadsheetId;
        this.host = host;
        this.hostActivity = hostActivity;
        this.sheets = sheets;

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        sheetsService = new com.google.api.services.sheets.v4.Sheets.Builder(
                transport, jsonFactory, credential)
                .setApplicationName(applicationName)
                .build();
    }

    @Override
    protected Exception doInBackground(WriteData... params) {
        for (WriteData wd : params) {
            try {
                ValueRange response = sheetsService.spreadsheets().values()
                        .get(spreadsheetId, wd.testType.toId() + "!A2:A")
                        .execute();
                List<List<Object>> sheet = response.getValues();
                int rowIdx = 2;
                if (sheet != null) {
                    for (List row : sheet) {
                        // TODO: write new column header if the cell extends past a column with a header
                        if (row.size() == 0 || row.get(0).toString().length() == 0 || row.get(0).toString().equals(wd.userId)) {
                            break;
                        }
                        rowIdx++;
                    }
                }

                response = sheetsService.spreadsheets().values()
                        .get(spreadsheetId, wd.testType.toId() + "!" + rowIdx + ":" + rowIdx)
                        .execute();

                sheet = response.getValues();
                String colIdx = "A";
                if (sheet != null) {
                    colIdx = columnToLetter(sheet.get(0).size() + 1);
                }

                String updateCell = wd.testType.toId() + "!" + colIdx + rowIdx;
                List<List<Object>> values = new ArrayList<>();
                List<Object> row = new ArrayList<>();

                if (colIdx.equals("A")) {
                    row.add(wd.userId);
                    updateCell += ":B" + rowIdx;
                }

                row.add(wd.value);
                values.add(row);

                ValueRange valueRange = new ValueRange();
                valueRange.setValues(values);

                sheetsService.spreadsheets().values()
                        .update(spreadsheetId, updateCell, valueRange)
                        .setValueInputOption("RAW")
                        .execute();
            } catch (Exception e) {
                return e;
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute (Exception e) {
        if (e != null && e instanceof GooglePlayServicesAvailabilityIOException) {
            sheets.showGooglePlayErrorDialog();
        } else if (e != null && e instanceof UserRecoverableAuthIOException) {
            hostActivity.startActivityForResult(((UserRecoverableAuthIOException) e).getIntent(),
                    host.getRequestCode(Sheets.Action.REQUEST_AUTHORIZATION));
        } else {
            host.notifyFinished(e);
        }
    }

    private String columnToLetter(int column) {
        int temp;
        String letter = "";
        while (column > 0) {
            temp = (column - 1) % 26;
            letter = ((char)(temp + 65)) + letter;
            column = (column - temp - 1) / 26;
        }
        return letter;
    }

    static class WriteData {
        Sheets.TestType testType;
        String userId;
        float value;

        WriteData (Sheets.TestType testType, String userId, float value) {
            this.testType = testType;
            this.userId = userId;
            this.value = value;
        }
    }
}