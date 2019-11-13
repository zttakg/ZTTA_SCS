package xyz.yaroslav.scs.ui.history;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HistoryDownloadAsync extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... strings) {
        try {
            URL url = new URL(strings[0]);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(2000);
            urlConnection.setReadTimeout(2000);
            InputStream stream = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder builder = new StringBuilder();

            String inputString;

            while ((inputString = bufferedReader.readLine()) != null) {
                builder.append(inputString);
            }

            urlConnection.disconnect();

            return builder.toString();
        } catch (IOException e) {
            Log.e("GET_HISTORY", "IO Exception: " + "(" + e.getClass() + "): " + e.getMessage());
        }
        return null;
    }
}

