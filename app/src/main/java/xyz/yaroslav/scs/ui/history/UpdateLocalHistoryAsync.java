package xyz.yaroslav.scs.ui.history;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UpdateLocalHistoryAsync extends AsyncTask<String, Void, List<String>> {
    @Override
    protected List<String> doInBackground(String... strings) {
        String[] arr = strings[0].split(";");
        if (arr.length > 0) {
            List<String> temp = new ArrayList<>();
            List<String> list = new ArrayList<>(Arrays.asList(arr));
            for (String str : list) {
                try {
                    JSONObject jsonObject = new JSONObject(str);
                    String tagTime = jsonObject.getString("tag_time");
                    if (isTagRecent(tagTime)) {
                        temp.add(str + ";");
                    }
                } catch (JSONException e) {
                    Log.e("JSON_PARSE", "Exception: " + e.getMessage());
                }
            }
            if (!temp.isEmpty()) {
                return temp;
            }
        }
        return null;
    }

    private boolean isTagRecent(String value) {
        long currentTime = System.currentTimeMillis();
        long comparableTime = Long.parseLong(value);
        return ((currentTime - comparableTime) <= 86400000);
    }
}
