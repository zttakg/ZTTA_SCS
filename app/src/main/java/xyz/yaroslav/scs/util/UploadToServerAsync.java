package xyz.yaroslav.scs.util;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UploadToServerAsync extends AsyncTask<String, Void, Integer> {
    @Override
    protected Integer doInBackground(String... strings) {
        try {
            URL url = new URL(strings[0]);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            if (strings[1] != null) {
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
                writer.write(strings[1]);
                writer.flush();
                writer.close();
                os.close();

                conn.connect();
                return conn.getResponseCode();
            }
            conn.disconnect();
        } catch (IOException e) {
            Log.e("HTTP_POST", "IO Exception: " + e.getMessage());
        }
        return 0;
    }
}
