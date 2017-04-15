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
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Background task to write the data
 */
class WriteDataTask extends AsyncTask<WriteDataTask.WriteData, Void, Exception> {

    private com.google.api.services.sheets.v4.Sheets sheetsService = null;
    private String spreadsheetId;
    private Sheets.Host host;
    private Activity hostActivity;

    WriteDataTask (GoogleAccountCredential credential, String spreadsheetId, String applicationName, Sheets.Host host, Activity hostActivity) {

        this.spreadsheetId = spreadsheetId;
        this.host = host;
        this.hostActivity = hostActivity;

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        sheetsService = new com.google.api.services.sheets.v4.Sheets.Builder(
                transport, jsonFactory, credential)
                .setApplicationName(applicationName)
                .build();
    }

    private void writeToCentral(WriteData wd) throws IOException {
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
    }

    private void writeToPrivate(WriteData wd) throws IOException {
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, wd.testType.toId() + "!A2:A")
                .execute();
        List<List<Object>> sheet = response.getValues();
        int rowIdx = 2;
        if (sheet != null) {
            for (List row : sheet) {
                if (row.size() == 0 || row.get(0).toString().length() == 0) {
                    break;
                }
                rowIdx++;
            }
        }

        String updateCell = wd.testType.toId() + "!A" + rowIdx + ":" + columnToLetter(wd.trials.length + 2) + rowIdx;
        List<List<Object>> values = new ArrayList<>();
        List<Object> row = new ArrayList<>();
        row.add(wd.userId);
        row.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        for (float value : wd.trials) {
            row.add(value);
        }
        values.add(row);

        ValueRange valueRange = new ValueRange();
        valueRange.setValues(values);

        sheetsService.spreadsheets().values()
                .update(spreadsheetId, updateCell, valueRange)
                .setValueInputOption("RAW")
                .execute();
    }

    @Override
    protected Exception doInBackground(WriteData... params) {
        for (WriteData wd : params) {
            try {
                if (wd.central) {
                    this.writeToCentral(wd);
                } else {
                    this.writeToPrivate(wd);
                }
            } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
                if (e.getDetails().getErrors().get(0).getMessage().contains("Unable to parse range:")) {
                    // Create request to add a new sheet
                    AddSheetRequest addReq = new AddSheetRequest();
                    addReq.setProperties(new SheetProperties().setTitle(
                            wd.testType.toId().substring(1, wd.testType.toId().length() - 1)));
                    // A bunch of stupid shit for sheets API request building
                    Request req = new Request();
                    req.setAddSheet(addReq);
                    ArrayList<Request> reqList = new ArrayList<>();
                    reqList.add(req);
                    BatchUpdateSpreadsheetRequest batchReq = new BatchUpdateSpreadsheetRequest();
                    batchReq.setRequests(reqList);
                    try {
                        // Update the request
                        sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchReq).execute();
                        // Retry our original write
                        if (wd.central) {
                            this.writeToCentral(wd);
                        } else {
                            this.writeToPrivate(wd);
                        }
                    } catch (Exception e2) {
                        return e2;
                    }
                    return null;
                } else {
                    return e;
                }
            } catch (Exception e) {
                return e;
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute (Exception e) {
        if (e != null && e instanceof GooglePlayServicesAvailabilityIOException) {
            Sheets.showGooglePlayErrorDialog(host, hostActivity);
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
        float[] trials;
        boolean central;

        WriteData(Sheets.TestType testType, String userId, float value) {
            this.testType = testType;
            this.userId = userId;
            this.value = value;
            this.central = true;
        }

        WriteData(Sheets.TestType testType, String userId, float[] trials) {
            this.testType = testType;
            this.userId = userId;
            this.trials = trials;
            this.central = false;
        }
    }
}