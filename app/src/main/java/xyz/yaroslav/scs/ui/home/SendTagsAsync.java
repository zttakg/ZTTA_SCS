package xyz.yaroslav.scs.ui.home;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import xyz.yaroslav.scs.R;
import xyz.yaroslav.scs.util.Utilities;

public class SendTagsAsync extends AsyncTask<JSONObject, Void, Void> {
    private Context context;

    public SendTagsAsync(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(JSONObject... jsonObjects) {
        if (jsonObjects.length > 0 && jsonObjects[0] != null) {
            try {
                String _url = new Utilities().buildUrl(context, 0, null);
                if (_url != null && !_url.isEmpty()) {
                    URL url = new URL(_url);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(2000);
                    conn.setReadTimeout(2000);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
                    writer.write(jsonObjects[0].toString());
                    writer.flush();
                    writer.close();
                    os.close();

                    conn.connect();
                    if (conn.getResponseCode() != 200) {
                        saveTagInCache(jsonObjects[0]);
                    }
                }
            } catch (IOException e) {
                Log.e("SEND_TAG_ASYNC", "IO Exception: " + e.getMessage());
                saveTagInCache(jsonObjects[0]);
            }
        } else {
            saveTagInCache(jsonObjects[0]);
        }
        return null;
    }

    private void saveTagInCache(JSONObject object) {
        if (object != null) {
            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(context.getString(R.string.file_cache), Context.MODE_APPEND));
                outputStreamWriter.write(object.toString() + ";");
                outputStreamWriter.close();
            } catch (IOException ex) {
                Log.e("SAVE_TAG_ASYNC", "File write failed: " + ex.toString());
            }
        }
    }
}
